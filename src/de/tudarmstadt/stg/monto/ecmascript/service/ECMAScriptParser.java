package de.tudarmstadt.stg.monto.ecmascript.service;

import de.tudarmstadt.stg.monto.ecmascript.antlr.ECMAScriptLexer;
import de.tudarmstadt.stg.monto.ecmascript.message.Message;
import de.tudarmstadt.stg.monto.ecmascript.message.ProductMessage;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.zeromq.ZMQ;

import java.util.ArrayDeque;
import java.util.List;

public class ECMAScriptParser extends  ECMAScriptService {

//    private ECMAScriptLexer lexer = new ECMAScriptLexer(new ANTLRInputStream());
//    private CommonTokenStream tokens = new CommonTokenStream(lexer);
//    private de.tudarmstadt.stg.monto.ecmascript.antlr.ECMAScriptParser parser = new de.tudarmstadt.stg.monto.ecmascript.antlr.ECMAScriptParser(tokens);

    public ECMAScriptParser(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage processMessage(List<Message> messages) {
        return new ProductMessage();
    }

}
