package de.tudarmstadt.stg.monto.ecmascript.message;

import org.json.simple.JSONObject;

public class VersionMessage implements Message {

    private String source;
    private long versionId;
    private String language;
    private String invalid;
    private String contents;
//    private String selections;

    public VersionMessage(String source, long versionId, String language, String invalid, String contents, String selections) {
        this.source = source;
        this.versionId = versionId;
        this.language = language;
        this.invalid = invalid;
        this.contents = contents;
//        this.selections = selections;
    }

    public static VersionMessage decode(JSONObject message) {
        return new VersionMessage(
                (String) message.get("source"),
                (long) message.get("version_id"),
                (String) message.get("language"),
                (String) message.get("invalid"),
                (String) message.get("contents"),
                ""
        );
    }

    public static JSONObject encode(VersionMessage message) {
        return null;
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

    @Override
    public String toString() {
        return source + " " + versionId + " " + language + " " + invalid + " " + contents + " ";
    }
}
