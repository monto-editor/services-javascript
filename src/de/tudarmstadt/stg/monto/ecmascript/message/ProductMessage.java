package de.tudarmstadt.stg.monto.ecmascript.message;

import org.json.simple.JSONObject;

public class ProductMessage implements Message {

    private String source;
    private long versionId;
    private String language;
    private String invalid;
    private String contents;
    private long productId;
    private String product;
    private String dependencies;

    public ProductMessage() {
    }

    public ProductMessage(String source, long versionId, String language, String invalid, String contents, long productId, String product, String dependencies) {
        this.source = source;
        this.versionId = versionId;
        this.language = language;
        this.invalid = invalid;
        this.contents = contents;
        this.productId = productId;
        this.product = product;
        this.dependencies = dependencies;
    }

    public String getSource() {
        return source;
    }

    public long getVersionId() {
        return versionId;
    }

    public String getLanguage() {
        return language;
    }

    public String getInvalid() {
        return invalid;
    }

    public String getContents() {
        return contents;
    }

    public long getProductId() {
        return productId;
    }

    public String getProduct() {
        return product;
    }

    public String getDependencies() {
        return dependencies;
    }

    public static ProductMessage decode(JSONObject message) {
        return null;
    }

    public static JSONObject encode(ProductMessage message) {
        JSONObject encoding = new JSONObject();
        encoding.put("version_id", message.getVersionId());
        encoding.put("product_id", message.getProductId());
        encoding.put("source", message.getSource());
        encoding.put("product", message.getProduct());
        encoding.put("language", message.getLanguage());
        encoding.put("contents", message.getContents());
        encoding.put("invalid", message.getInvalid());
        encoding.put("dependencies", message.getDependencies());
        return encoding;
    }
}
