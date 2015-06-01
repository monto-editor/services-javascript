package de.tudarmstadt.stg.monto.ecmascript.ast;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.stg.monto.ecmascript.region.Regions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import de.tudarmstadt.stg.monto.ecmascript.message.ProductMessage;
import de.tudarmstadt.stg.monto.ecmascript.region.Region;

/**
 * Encodes and decodes messages that contain the result of an arbitrary tokenization.
 * <p>
 * The message contents must have the following format:
 * {@code [{offset:<int>,length:<int>,category:<string>}]}
 * where offset represents the offset of the token in the document,
 * length represents the length of the text of the token and
 * category represents the type of token like keyword or identifier.
 */
public class Tokens {

    /**
     * Encodes a list of tokens in the appropriate message format.
     */
    @SuppressWarnings("unchecked")
    public static JSONArray encode(final List<Token> tokens) {
        final JSONArray tokenArray = new JSONArray();

        for (Token token : tokens) {
            JSONObject jsonToken = new JSONObject();
            jsonToken.putAll(Regions.encode(token));
            jsonToken.put("category", token.getCategory().toString().toLowerCase());
            tokenArray.add(jsonToken);
        }
        return tokenArray;
    }

    /**
     * Decodes a product message that contains a tokenization result
     * into a list of tokens.
     */
    public static List<Token> decode(Reader reader) {
        JSONArray syntaxList = (JSONArray) JSONValue.parse(reader);

        List<Token> tokens = new ArrayList<>();
        for (Object syntaxObj : syntaxList) {
            JSONObject syntax = (JSONObject) syntaxObj;
            Region token = Regions.decode((JSONObject) syntaxObj);
            String category = (String) syntax.get("category");
            tokens.add(new Token(token.getStartOffset(), token.getLength(), Category.fromString(category.toUpperCase())));
        }
        return tokens;

    }

}
