package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.ast.ASTVisitor;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.configuration.*;
import monto.service.error.Error;
import monto.service.error.Errors;
import monto.service.message.*;
import monto.service.token.Category;
import monto.service.token.Token;
import monto.service.token.Tokens;
import org.zeromq.ZContext;

import java.io.*;
import java.util.*;

public class FlowTypeChecker extends MontoService {

    private static final Product ERRORS = new Product("errors");
    private static final Product TOKENS = new Product("tokens");
    private static final Language JAVASCRIPT = new Language("javascript");

    private String fileName;
    private File dir;
    private int[] linesizes;
    private List<Error> errors;
    private String flowCmd;

    private boolean comments = true;
    private String commentLanguage = "en_US";
    private boolean strings = true;
    private String stringLanguage = "en_US";
    private boolean suggestions = false;
    private int suggestionNumber = 5;

    public FlowTypeChecker(ZContext context, String address, String registrationAddress, String serviceID) {
        super(context, address, registrationAddress, serviceID, "FlowType", "A typechecker for JavaScript", JAVASCRIPT, ERRORS, new Option[]{
                new BooleanOption("comments", "Check comments", true),
                new OptionGroup("comments", new Option[]{new XorOption("commentLanguage", "Language for comments", "en_US", Arrays.asList("en_US", "fr_FR", "de_DE", "es_ES"))}),
                new BooleanOption("strings", "Check strings", true),
                new OptionGroup("strings", new Option[]{new XorOption("stringLanguage", "Language for strings", "en_US", Arrays.asList("en_US", "fr_FR", "de_DE", "es_ES"))}),
                new BooleanOption("suggestions", "Show suggestions", false),
                new OptionGroup("suggestions", new Option[]{new NumberOption("suggestionNumber", "Maximum number of suggestions", 5, 0, 10)})
        }, new String[]{"Source", "tokens/javascript"});
        fileName = "flowTypeCheckerFile.js";
        dir = new File("./");
        errors = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            flowCmd = "flow_mac";
        } else if (os.contains("win")) {
            flowCmd = "";
        } else {
            flowCmd = "flow_linux";
        }
        createFlowConfig();
    }

    @Override
    public ProductMessage onVersionMessage(List<Message> messages) throws Exception {
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in version message");
        }
        ProductMessage tokensProduct = Messages.getProductMessage(messages, TOKENS, JAVASCRIPT);
        if (!tokensProduct.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in tokens product message");
        }

        createSourceFile(version.getContent());
        runFlowTypecheck();

        List<Token> tokens = Tokens.decode(tokensProduct);
        spellCheck(tokens, version.getContent().toString());

        Contents newContent = new StringContent(Errors.encode(errors.stream()).toJSONString());

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                ERRORS,
                JAVASCRIPT,
                newContent);
    }

    @Override
    public void onConfigurationMessage(List<Message> messages) throws Exception {
        ConfigurationMessage configMsg = Messages.getConfigurationMessage(messages);
        System.out.println(configMsg.getConfigurations());
    }

    /*
     * Creates a file of the source content that later can be checked with flowtype.
     * Also sets linesizes to the bounds of source content.
     */
    private void createSourceFile(Contents content) throws FileNotFoundException, UnsupportedEncodingException {
        String[] lines = content.toString().split("\n");
        linesizes = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            linesizes[i] = lines[i].length();
        }
        errors = new ArrayList<>();
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println(content);
        writer.close();
    }

    private void createFlowConfig() {
        try {
            String[] cmd = new String[]{"/bin/sh", "-c", "./" + flowCmd + " init"};
            Process p = Runtime.getRuntime().exec(cmd, null, dir);
            p.waitFor();
        } catch (Exception e) {
            System.out.println("FlowType could not be started");
        }
    }

    private void runFlowTypecheck() throws IOException, InterruptedException {
        String[] cmd = new String[]{"/bin/sh", "-c", "./" + flowCmd + " check-contents < " + fileName};

        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        BufferedReader bri = new BufferedReader
                (new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader
                (new InputStreamReader(p.getErrorStream()));

        handleFlowInput(bri);
        handleFlowError(bre);

        p.waitFor();
    }

    private void handleFlowInput(BufferedReader bri) throws IOException {
        int offset = -1;
        int length = -1;
        String category = "type";
        StringBuilder description = new StringBuilder();

        // first get rid of first empty line
        bri.readLine();
        String input;
        while ((input = bri.readLine()) != null) {
            if (input.startsWith("Found ")) {
                break;
            } else if (input.equals("")) {
                errors.add(new Error(offset, length, "error", category, description.toString()));
                offset = -1;
                length = -1;
                description = new StringBuilder();
            } else {
                String[] parts = input.split(":");
                if (offset == -1 && parts.length > 2) {
                    String[] lengthParts = parts[2].split(",");
                    int begin = Integer.parseInt(lengthParts[0]);
                    int end = Integer.parseInt(lengthParts[1]);
                    length = end - begin + 1;
                    offset = convertToOffset(Integer.parseInt(parts[1]), begin) - 1;
                }
                description.append(input);
                description.append("\n");
            }
        }
        bri.close();
    }

    private int convertToOffset(int lineNumber, int position) {
        int offset = position;
        for (int i = 0; i < lineNumber - 1; i++) {
            offset += linesizes[i] + 1;
        }
        return offset;
    }

    private void handleFlowError(BufferedReader bre) throws IOException {
        StringBuilder builder = new StringBuilder();
        String error;
        while ((error = bre.readLine()) != null) {
            builder.append(error);
        }
        error = builder.toString();
        if (!error.equals("")) {
            System.out.println(error);
        }
        bre.close();
    }

    private void spellCheck(List<Token> tokens, String text) throws IOException, InterruptedException {
        for (Token token : tokens) {
            if (token.getCategory().equals(Category.COMMENT) || token.getCategory().equals(Category.STRING)) {
                String tokenText = text.substring(token.getStartOffset(), token.getEndOffset());
                String[] words = tokenText.split("\\s+");
                for (String word : words) {
                    String strippedWord = word.replaceAll("[^a-zA-Z]+", "");
                    int offset = token.getStartOffset();
                    offset += tokenText.indexOf(strippedWord);
                    if (strippedWord == null || strippedWord.equals("")) {
                        continue;
                    }
                    String[] cmd = new String[]{"/bin/sh", "-c", "echo " + strippedWord + " | aspell -a -d " + commentLanguage};

                    Process p = Runtime.getRuntime().exec(cmd, null, dir);
                    BufferedReader bri = new BufferedReader
                            (new InputStreamReader(p.getInputStream()));
                    BufferedReader bre = new BufferedReader
                            (new InputStreamReader(p.getErrorStream()));

                    handleAspellInput(bri, offset, strippedWord);
                    handleAspellError(bre);

                    p.waitFor();
                }
            }
        }
    }

    private void handleAspellInput(BufferedReader bri, int offset, String word) throws IOException {
        String category = "spelling";

        bri.readLine();
        String input;
        while ((input = bri.readLine()) != null) {
            if (input.startsWith("&")) {
                String description = word + ", did you mean: " + input.split(": ")[1];
                errors.add(new Error(offset, word.length(), "warning", category, description));
            }
        }
        bri.close();
    }

    private void handleAspellError(BufferedReader bre) throws IOException {
        StringBuilder builder = new StringBuilder();
        String error;
        while ((error = bre.readLine()) != null) {
            builder.append(error);
        }

        error = builder.toString();
        if (!error.equals("")) {
            System.out.println(error);
        }
        bre.close();
    }
}
