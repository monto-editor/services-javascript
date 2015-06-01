package de.tudarmstadt.stg.monto.ecmascript.service;

import de.tudarmstadt.stg.monto.ecmascript.message.Message;
import de.tudarmstadt.stg.monto.ecmascript.message.ProductMessage;
import org.zeromq.ZMQ;

import java.util.List;

public class ECMAScriptCodeCompletion extends  ECMAScriptService {

    public ECMAScriptCodeCompletion(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage processMessage(List<Message> messages) {
        return null;
    }

}
