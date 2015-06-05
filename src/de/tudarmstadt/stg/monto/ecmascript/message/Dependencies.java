package de.tudarmstadt.stg.monto.ecmascript.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Dependencies {
	
	public static VersionDependency decodeVersionDependency(JSONObject obj) throws ParseException {
		try {
			final LongKey versionId = new LongKey((Long) obj.get("version_id"));
			final Source source = new Source((String) obj.get("source"));
			final Language language = new Language((String) obj.get("language"));
			return new VersionDependency(versionId, source, language);
		} catch(Exception e) {
			throw new ParseException(e);
		}
	}
	
	public static ProductDependency decodeProductDependency(JSONObject obj) throws ParseException {
		try {
			final LongKey versionId = new LongKey((Long) obj.get("version_id"));
			final LongKey productId = new LongKey((Long) obj.get("product_id"));
			final Source source = new Source((String) obj.get("source"));
			final Language language = new Language((String) obj.get("language"));
			final Product product = new Product((String) obj.get("product"));
			return new ProductDependency(versionId, productId, source, language, product);
		} catch(Exception e) {
			throw new ParseException(e);
		}
	}
	
	public static Dependency decode(JSONObject obj) throws ParseException {
		try {
			switch((String) obj.get("tag")) {
			case "version":
				return decodeVersionDependency(obj);
			case "product":
				return decodeProductDependency(obj);
			default:
				throw new ParseException("dependency is neither a version nor a product");
			}
		} catch(Exception e) {
			throw new ParseException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Dependency> decode(JSONArray array) throws ParseException {
		try {
			List<Dependency> dependencies = new ArrayList<>(array.size());
			Iterator<JSONObject> iterator = array.iterator();
			while(iterator.hasNext())
				dependencies.add(Dependencies.decode(iterator.next()));
			return dependencies;
		} catch(Exception e) {
			throw new ParseException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static JSONObject encode(Dependency dep) {
		return dep.match(
			version -> {
				final JSONObject encoding = new JSONObject();
				encoding.put("tag","version");
				encoding.put("version_id", version.getVersionId().longValue());
				encoding.put("source", version.getSource().toString());
				encoding.put("language", version.getLanguage().toString());
				return encoding;
			},
			product -> {
				final JSONObject encoding = new JSONObject();
				encoding.put("tag",	"product");
				encoding.put("version_id", product.getVersionId().longValue());
				encoding.put("product_id", product.getProductId().longValue());
				encoding.put("source", product.getSource().toString());
				encoding.put("language", product.getLanguage().toString());
				encoding.put("product", product.getProduct().toString());
				return encoding;
			}
		);
	}

	@SuppressWarnings("unchecked")
	public static <D extends Dependency> JSONArray encode(List<D> dependencies) {
		JSONArray encoding = new JSONArray();
		for(Dependency dependency : dependencies)
			encoding.add(encode(dependency));
		return encoding;
	}
	
}
