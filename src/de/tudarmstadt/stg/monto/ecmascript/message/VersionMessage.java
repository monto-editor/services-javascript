package de.tudarmstadt.stg.monto.ecmascript.message;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class VersionMessage implements Message {

	private final LongKey versionId;
	private final Source source;
	private final Contents content;
	private final Language language;
	private final List<Selection> selections;
	private final List<Dependency> invalid;

	public VersionMessage(LongKey versionId, Source source, Language language, Contents content, Selection ... selections) {
		this(versionId,source,language,content,Arrays.asList(selections));
	}
	
	public VersionMessage(LongKey versionId, Source source, Language language, Contents content, List<Selection> selections) {
		this(versionId,source,language,content,selections,new ArrayList<>());
	}
	
	public VersionMessage(LongKey id,Source source, Language language, Contents content, List<Selection> selections, List<Dependency> invalid) {
		this.versionId = id;
		this.source = source;
		this.language = language;
		this.content = content;
		this.selections = selections;
		this.invalid = invalid;
	}
	
	public LongKey getVersionId() {
		return versionId;
	}

	@Override
	public Source getSource() {
		return source;
	}

	public Contents getContent() {
		return content;
	}

	@Override
	public Language getLanguage() {
		return language;
	}

	public List<Selection> getSelections() {
		return selections;
	}
	
	@Override
	public List<Dependency> getInvalid() {
		return invalid;
	}

	public static VersionMessage decode(final Reader reader) throws ParseException {
		final JSONObject message = (JSONObject) JSONValue.parse(reader);
		return decode(message);
	}

	@SuppressWarnings("unchecked")
	public static VersionMessage decode(JSONObject message) throws ParseException {
		try {
			final long start = System.nanoTime();
			final LongKey id = new LongKey((Long) message.get("version_id"));
			final Source source = new Source((String) message.get("source"));
			final Language language = new Language((String) message.get("language"));
			final Contents contents = new StringContent((String) message.get("contents"));
			final JSONArray selectionsArray = (JSONArray) message.getOrDefault("selections", new JSONArray());
			final List<Selection> selections = new ArrayList<>(selectionsArray.size());
			Iterator<JSONObject> iterator = selectionsArray.iterator();
			while(iterator.hasNext()) {
				final JSONObject selection = iterator.next();
				final Long begin = (Long) selection.get("begin");
				final Long end = (Long) selection.get("end");
				selections.add(new Selection(begin.intValue(), end.intValue() - begin.intValue()));
			}
			JSONArray invalid = (JSONArray) message.getOrDefault("invalid", new JSONArray());
			List<Dependency> invalidProducts = Dependencies.decode(invalid);
			final VersionMessage msg = new VersionMessage(id, source, language, contents, selections, invalidProducts);
			return msg;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject encode(VersionMessage message) {
		JSONObject version = new JSONObject();
		version.put("version_id", message.getVersionId().longValue());
		version.put("source", message.getSource().toString());
		version.put("language", message.getLanguage().toString());
		version.put("contents", message.getContent().toString());
		JSONArray selections = new JSONArray();
		for(Selection selection : message.getSelections()) {
			JSONObject sel = new JSONObject();
			sel.put("begin", selection.getStartOffset());
			sel.put("end", selection.getEndOffset());
			selections.add(sel);
		}
		version.put("selections", selections);
		JSONArray invalidProducts = Dependencies.encode(message.getInvalid());
		version.put("invalid",invalidProducts);
		return version;
	}

	public static VersionMessage getVersionMessage(List<Message> messages) {
		return (VersionMessage) messages.stream().filter(msg -> msg instanceof VersionMessage).findFirst().get();
	}

}
