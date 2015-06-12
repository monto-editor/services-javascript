package de.tudarmstadt.stg.monto.service.ast;


import de.tudarmstadt.stg.monto.service.region.IRegion;

public class Terminal implements AST {
	private int offset;
	private int length;
	
	public Terminal(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public int getStartOffset() {
		return offset;
	}
	
	@Override
	public int getLength() {
		return length;
	}
	
	public boolean inRange(IRegion region) {
		return getStartOffset() >= region.getStartOffset()
		    && getStartOffset() + getLength() <= region.getEndOffset() + region.getLength();
	}
	
	public String getText(String document) {
		return document.substring(getStartOffset(),getEndOffset());
	}
	
	@Override
	public String toString() {
		return String.format("{offset: %d, length: %d}",offset,length);
	}
}
