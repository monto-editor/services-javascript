package de.tudarmstadt.stg.monto.service.region;

import java.io.Reader;

import de.tudarmstadt.stg.monto.service.message.ParseException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Regions {

    @SuppressWarnings("unchecked")
    public static JSONObject encode(IRegion range) {
        JSONObject encoding = new JSONObject();
        encoding.put("offset", range.getStartOffset());
        encoding.put("length", range.getLength());
        return encoding;
    }

    public static Region decode(Reader rangeEncoding) {
        return decode((JSONObject) JSONValue.parse(rangeEncoding));
    }

    public static Region decode(JSONObject encoding) {
        Long offset = (Long) encoding.get("offset");
        Long length = (Long) encoding.get("length");
        return new Region(offset.intValue(), length.intValue());
    }
}
