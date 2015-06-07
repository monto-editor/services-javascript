package de.tudarmstadt.stg.monto.ecmascript.completion;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Iterator;
import java.util.stream.Stream;

public class Completion {
	
	private String description;
	private String replacement;
	private String icon;
	private int insertionOffset;
	public Completion(String description, String replacement, String icon) {
		this(description,replacement,0,icon);
	}
	public Completion(String description, String replacement, int insertionOffset, String icon) {
		this.description = description;
		this.replacement = replacement;
		this.insertionOffset = insertionOffset;
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public String getReplacement() {
		return replacement;
	}
	
	public String getIcon() {
		return icon;
	}

	public int getInsertionOffset() {
		return insertionOffset;
	}

	@Override
	public String toString() {
		return String.format("%s", description);
	}

	@SuppressWarnings("unchecked")
	public static JSONArray encode(Stream<Completion> completions) {
		Iterator<Completion> iter = completions.iterator();
		JSONArray array = new JSONArray();
		while(iter.hasNext()) {
			array.add(encode(iter.next()));
		}
		return array;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject encode(Completion completion) {
		JSONObject object = new JSONObject();
		object.put("description", completion.getDescription());
		object.put("replacement", completion.getReplacement());
		object.put("insertionOffset", completion.getInsertionOffset());
		object.put("icon", completion.getIcon());
		return object;
	}
}
