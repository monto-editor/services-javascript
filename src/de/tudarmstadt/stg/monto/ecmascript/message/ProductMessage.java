package de.tudarmstadt.stg.monto.ecmascript.message;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ProductMessage implements Message {
	
	private final LongKey versionId;
	private final LongKey productId;
	private final Source source;
	private final Product product;
	private final Language language;
	private final Contents contents;
	private final List<Dependency> invalid;
	private final List<Dependency> dependencies;
	
	public ProductMessage(LongKey versionId, LongKey productId, Source source, Product product, Language language, Contents contents, Dependency... dependencies) {
		this(versionId,productId,source,product,language,contents,new ArrayList<>(),Arrays.asList(dependencies));
	}

	public ProductMessage(LongKey versionId, LongKey productId, Source source, Product product, Language language, Contents contents, List<Dependency> invalid2, List<Dependency> dependencies) {
		this.versionId = versionId;
		this.productId= productId;
		this.source = source;
		this.product = product;
		this.language = language;
		this.contents = contents;
		this.invalid = invalid2;
		this.dependencies = dependencies;
	}
	
	public LongKey getVersionId() { return versionId; }
	public LongKey getProductId() { return productId; }
	public Source getSource() { return source; }
	public Product getProduct() { return product; }
	public Language getLanguage() { return language; }
	public Contents getContents() { return contents; }
	@Override public List<Dependency> getInvalid() { return invalid; }
	public List<Dependency> getDependencies() { return dependencies; }
	
	@Override
	public String toString() {
		return String.format("{"
				+ "  vid: %s,\n"
				+ "  pid: %s,\n"
				+ "  source: %s,\n"
				+ "  product: %s,\n"
				+ "  language: %s,\n"
				+ "  contents: %s,\n"
				+ "  dependencies: %s\n"
				+ "}", versionId, productId, source, product, language, contents, dependencies);
	}

	public static ProductMessage decode(Reader reader) throws ParseException {
		JSONObject message = (JSONObject) JSONValue.parse(reader);
		return decode(message);
	}

	@SuppressWarnings("unchecked")
	public static ProductMessage decode(JSONObject message) throws ParseException {
		try {
			long start = System.nanoTime();
			Long versionId = (Long) message.get("version_id");
			Long productId = (Long) message.get("product_id");
			Source source = new Source((String) message.get("source"));
			Product product = new Product((String) message.get("product"));
			Language language = new Language((String) message.get("language"));
			Contents contents = new StringContent((String) message.get("contents"));
			List<Dependency> invalid = Dependencies.decode((JSONArray) message.getOrDefault("invalid", new JSONArray()));
			List<Dependency> dependencies = Dependencies.decode((JSONArray) message.getOrDefault("dependencies", new JSONArray()));
			ProductMessage msg = new ProductMessage(
					new LongKey(versionId),
					new LongKey(productId),
					source,
					product,
					language,
					contents,
					invalid,
					dependencies);
			return msg;
		} catch (Exception e) {
			throw new ParseException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static JSONObject encode(ProductMessage msg) {
		JSONObject encoding = new JSONObject();
		encoding.put("version_id", msg.getVersionId().longValue());
		encoding.put("product_id", msg.getProductId().longValue());
		encoding.put("source", msg.getSource().toString());
		encoding.put("product", msg.getProduct().toString());
		encoding.put("language", msg.getLanguage().toString());
		encoding.put("contents", msg.getContents().toString());
		encoding.put("invalid", Dependencies.encode(msg.getInvalid()));
		encoding.put("dependencies", Dependencies.encode(msg.getDependencies()));
		return encoding;
	}

	public static ProductMessage getProductMessage(List<Message> messages, Product product, Language language) {
		return (ProductMessage) messages.stream().filter(msg -> {
			if (msg instanceof ProductMessage) {
				ProductMessage msg1 = (ProductMessage) msg;
				return msg1.getProduct().equals(product) && msg1.getLanguage().equals(language);
			} else {
				return false;
			}
		}).findAny().get();
	}
}
