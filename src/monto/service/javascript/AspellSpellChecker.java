package monto.service.javascript;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.configuration.*;
import monto.service.error.Error;
import monto.service.error.Errors;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.ProductDependency;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.token.Token;
import monto.service.token.TokenCategory;
import monto.service.token.Tokens;
import monto.service.types.Languages;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AspellSpellChecker extends MontoService {

    private List<Error> errors;

    private static final boolean DEFAULT_comments = true;
    private static final String DEFAULT_commentLanguage = "en";
    private static final boolean DEFAULT_strings = true;
    private static final String DEFAULT_stringLanguage = "en";
    private static final boolean DEFAULT_suggestions = false;
    private static final long DEFAULT_suggestionNumber = 5;

    private boolean comments = DEFAULT_comments;
    private String commentLanguage = DEFAULT_commentLanguage;
    private boolean strings = DEFAULT_strings;
    private String stringLanguage = DEFAULT_stringLanguage;
    private boolean suggestions = DEFAULT_suggestions;
    private long suggestionNumber = DEFAULT_suggestionNumber;

    public AspellSpellChecker(ZMQConfiguration zmqConfig, List<String> languages) {
        super(zmqConfig,
                JavaScriptServices.ASPELL_SPELLCHECKER,
                "Spell checker",
                "Can check spelling errors using aspell",
                Languages.JAVASCRIPT,
                Products.ERRORS,
                options(
                        new BooleanOption("comments", "Check comments", true),
                        new OptionGroup("comments", new XorOption("commentLanguage", "Language for comments", languages.get(0), languages)),
                        new BooleanOption("strings", "Check strings", true),
                        new OptionGroup("strings", new XorOption("stringLanguage", "Language for strings", languages.get(0), languages)),
                        new BooleanOption("suggestions", "Show suggestions", false),
                        new OptionGroup("suggestions", new NumberOption("suggestionNumber", "Maximum number of suggestions", 5, 0, 10))
                ),
                dependencies(
                        new SourceDependency(Languages.JAVASCRIPT),
                        new ProductDependency(JavaScriptServices.JAVASCRIPT_TOKENIZER, Products.TOKENS, Languages.JAVASCRIPT)
                ));
        errors = new ArrayList<>();
    }

    @Override
    public ProductMessage onRequest(Request request) throws Exception {
        SourceMessage version = request.getSourceMessage()
                .orElseThrow(() -> new IllegalArgumentException("No version message in request"));
        ProductMessage tokensProduct = request.getProductMessage(Products.TOKENS, Languages.JAVASCRIPT)
                .orElseThrow(() -> new IllegalArgumentException("No AST message in request"));

        errors = new ArrayList<>();
        List<Token> tokens = Tokens.decodeTokenMessage(tokensProduct);
        spellCheck(tokens, version.getContent().toString());

        return productMessage(
                version.getId(),
                version.getSource(),
                Products.ERRORS,
                Languages.JAVASCRIPT,
                Errors.encode(errors.stream()));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onConfigurationMessage(Configuration message) throws Exception {
        for (Setting config : message.getConfigurations()) {
            switch (config.getOptionID()) {
                case "comments":
                    comments = (boolean) config.getValue();
                    break;
                case "commentLanguage":
                    commentLanguage = (String) config.getValue();
                    break;
                case "strings":
                    strings = (boolean) config.getValue();
                    break;
                case "stringLanguage":
                    stringLanguage = (String) config.getValue();
                    break;
                case "suggestions":
                    suggestions = (boolean) config.getValue();
                    break;
                case "suggestionNumber":
                    suggestionNumber = (long) config.getValue();
                    break;
                default:
                    break;
            }
        }
    }

    private void spellCheck(List<Token> tokens, String text) throws IOException, InterruptedException {
        for (Token token : tokens) {
            if (token.getCategory().equals(TokenCategory.COMMENT) && comments || token.getCategory().equals(TokenCategory.STRING) && strings) {
                String tokenText = text.substring(token.getStartOffset(), token.getEndOffset());
                String[] words = tokenText.split("\\s+");

                for (String word : words) {
                    String strippedWord = word.replaceAll("[^a-zA-Z]+", "");
                    int offset = token.getStartOffset();
                    offset += tokenText.indexOf(strippedWord);
                    if (strippedWord.equals("")) {
                        continue;
                    }
                    String[] cmd = new String[]{
                            "aspell", "-a", "-d", (token.getCategory().equals(TokenCategory.COMMENT) ? commentLanguage : stringLanguage)
                    };

                    Process p = Runtime.getRuntime().exec(cmd, null);

                    BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
                    processInput.append(strippedWord);
                    processInput.newLine();
                    processInput.flush();
                    processInput.close();

                    BufferedReader processOutput = new BufferedReader
                            (new InputStreamReader(p.getInputStream()));
                    BufferedReader processError = new BufferedReader
                            (new InputStreamReader(p.getErrorStream()));

                    handleAspellInput(processOutput, offset, strippedWord);
                    handleAspellError(processError);

                    p.destroy();
                }
            }
        }
    }

    private void handleAspellInput(BufferedReader processOutput, int offset, String word) throws IOException {
        String category = "spelling";

        processOutput.readLine();
        String input;
        while ((input = processOutput.readLine()) != null) {
            if (input.startsWith("&")) {
                String description = word;
                if (suggestions && suggestionNumber > 0) {
                    description += ", did you mean: ";
                    String[] suggestions = input.split(": ")[1].split(", ");
                    for (int i = 0; i < suggestionNumber && i < suggestions.length; i++) {
                        description += suggestions[i] + ", ";
                    }
                    description = description.substring(0, description.length() - 2);
                }
                errors.add(new Error(offset, word.length(), "warning", category, description));
            }
        }
        processOutput.close();
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

    public static List<String> getAspellLanguages() throws IOException {
        List<String> languages = new ArrayList<>();
        String[] cmd = new String[]{"aspell", "dump", "dicts"};

        Process p = Runtime.getRuntime().exec(cmd, null);
        BufferedReader bri = new BufferedReader
                (new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader
                (new InputStreamReader(p.getErrorStream()));

        String input;
        while ((input = bri.readLine()) != null) {
            languages.add(input);
        }
        bri.close();

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

        return languages;
    }
}
