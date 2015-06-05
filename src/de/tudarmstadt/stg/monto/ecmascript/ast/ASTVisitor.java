package de.tudarmstadt.stg.monto.ecmascript.ast;

public interface ASTVisitor {
	public void visit(NonTerminal node);
	public void visit(Terminal token);
}
