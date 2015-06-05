package de.tudarmstadt.stg.monto.ecmascript.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NonTerminal implements AST {

	private String name;
	private List<AST> children;
	
	public NonTerminal(String name, List<AST> children) {
		this.name = name;
		this.children = children;
	}
	
	public NonTerminal(String name, AST ... children) {
		this(name, new ArrayList<AST>(Arrays.asList(children)));
	}
	
	@Override
	public void accept(ASTVisitor visitor) {
		visitor.visit(this);
	}

	public String getName() {
		return name;
	}

	public AST getChild(int i) {
		return children.get(i);
	}
	
	public List<AST> getChildren() {
		return children;
	}

	public void addChild(AST a) {
		children.add(a);
	}

	@Override
	public int getStartOffset() {
		if(children.size() == 0)
			throw new HasNoChildrenException(name);
		return children.get(0).getStartOffset();
	}
	
	@Override
	public int getEndOffset() {
		if(children.size() == 0)
			throw new HasNoChildrenException(name);
		return children.get(children.size()-1).getEndOffset();
	}
	
	@Override
	public String toString() {
		return name;
	}
}
