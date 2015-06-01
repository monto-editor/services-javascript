package de.tudarmstadt.stg.monto.ecmascript.ast;

import de.tudarmstadt.stg.monto.ecmascript.region.Region;

public class Token extends Region {

	private Category category;
	
	public Token(int offset, int length, Category category) {
		super(offset,length);
		this.category = category;
	}
	
	public Category getCategory() {
		return category;
	}

	@Override
	public String toString() {
		return String.format("(%d,%d,%s)",getStartOffset(),getLength(),category);
	}
}
