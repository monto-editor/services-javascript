package de.tudarmstadt.stg.monto.service.ast;

public interface ASTVisitor {
	public void visit(NonTerminal node);
	public void visit(Terminal token);
}
