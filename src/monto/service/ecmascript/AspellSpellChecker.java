package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.configuration.*;
import monto.service.error.*;
import monto.service.error.Error;
import monto.service.message.*;
import monto.service.token.Category;
import monto.service.token.Token;
import monto.service.token.Tokens;
import org.zeromq.ZContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AspellSpellChecker extends MontoService {

    private static final Product ERRORS = new Product("errors");
    private static final Product TOKENS = new Product("tokens");
    private static final Language JAVASCRIPT = new Language("javascript");

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

    public AspellSpellChecker(ZContext context, String address, String registrationAddress, String serviceID, List<String> languages) {
        super(context, address, registrationAddress, serviceID, "Error Spell checker for several language", "Can check spelling errors using aspell", JAVASCRIPT, ERRORS, new Option[]{
                new BooleanOption("comments", "Check comments", true),
                new OptionGroup("comments", new Option[]{new XorOption("commentLanguage", "Language for comments", languages.get(0), languages)}),
                new BooleanOption("strings", "Check strings", true),
                new OptionGroup("strings", new Option[]{new XorOption("stringLanguage", "Language for strings", languages.get(0), languages)}),
                new BooleanOption("suggestions", "Show suggestions", false),
                new OptionGroup("suggestions", new Option[]{new NumberOption("suggestionNumber", "Maximum number of suggestions", 5, 0, 10)})
        }, new String[]{"Source", "tokens/javascript"});
        errors = new ArrayList<>();
    }

    @Override
    public ProductMessage onVersionMessage(List<Message> messages) throws Exception {
        errors = new ArrayList<>();
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in version message");
        }

        ProductMessage tokensProduct = Messages.getProductMessage(messages, TOKENS, new Language("javascript"));
        if (!tokensProduct.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in token product");
        }

        List<Token> tokens = Tokens.decode(tokensProduct);
        spellCheck(tokens, version.getContent().toString());

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                ERRORS,
                JAVASCRIPT,
                Errors.encode(errors.stream()));
    }

    @Override
    public void onConfigurationMessage(List<Message> messages) throws Exception {
        ConfigurationMessage configMsg = Messages.getConfigurationMessage(messages);
        List<Configuration> configs = configMsg.getConfigurations();
        for (Configuration config : configs) {
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
            if (token.getCategory().equals(Category.COMMENT) && comments || token.getCategory().equals(Category.STRING) && strings) {
                String tokenText = text.substring(token.getStartOffset(), token.getEndOffset());
                String[] words = tokenText.split("\\s+");
                for (String word : words) {
                    String strippedWord = word.replaceAll("[^a-zA-Z]+", "");
                    int offset = token.getStartOffset();
                    offset += tokenText.indexOf(strippedWord);
                    if (strippedWord.equals("")) {
                        continue;
                    }
                    String[] cmd = new String[]{"/bin/sh", "-c", "echo " + strippedWord + " | aspell -a -d " + (token.getCategory().equals(Category.COMMENT) ? commentLanguage : stringLanguage)};

                    Process p = Runtime.getRuntime().exec(cmd, null);
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

    public static List<String> getAspellLanguages() throws IOException {
        List<String> languages = new ArrayList<>();
        String[] cmd = new String[]{"/bin/sh", "-c", "aspell dump dicts"};

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
