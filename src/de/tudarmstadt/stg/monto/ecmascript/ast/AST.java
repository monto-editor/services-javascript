package de.tudarmstadt.stg.monto.ecmascript.ast;

import de.tudarmstadt.stg.monto.ecmascript.region.IRegion;

public interface AST extends IRegion {
    public void accept(ASTVisitor visitor);
}
