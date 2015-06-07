package de.tudarmstadt.stg.monto.ecmascript.outline;

import de.tudarmstadt.stg.monto.ecmascript.region.IRegion;
import de.tudarmstadt.stg.monto.ecmascript.region.Region;
import de.tudarmstadt.stg.monto.ecmascript.region.Regions;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Outline extends Region {
	
	private String description;
	private Optional<String> icon;
	private List<Outline> childs;
	
	public Outline(String description, IRegion region, String icon, List<Outline> childs) {
		super(region.getStartOffset(),region.getLength());
		this.description = description;
		this.icon = Optional.ofNullable(icon);
		this.childs = childs;
	}
	
	public Outline(String description, IRegion region, String icon) {
		this(description, region, icon, new ArrayList<>());
	}
	
	public void addChild(Outline outline) {
		childs.add(outline);
	}
	
	public List<Outline> getChildren() {
		return childs;
	}

	public Optional<String> getIcon() {
		return icon;
	}
	
	public boolean isLeaf() {
		return getChildren().size() == 0;
	}

	public String getDescription() {
		return description;
	}

	public IRegion getIdentifier() {
		return this;
	}
	
	@Override
	public String toString() {
		return description;
	}

	@SuppressWarnings("unchecked")
	public static JSONObject encode(Outline outline) {
		JSONObject encoding = new JSONObject();

		encoding.put("description", outline.getDescription());
		encoding.put("identifier", Regions.encode(outline.getIdentifier()));

		if(! outline.isLeaf()) {
			JSONArray children = new JSONArray();
			outline.getChildren().forEach(child -> children.add(encode(child)));
			encoding.put("children", children);
		}

		if(outline.getIcon().isPresent()) {
			encoding.put("icon", outline.getIcon().get());
		}

		return encoding;
	}
}
