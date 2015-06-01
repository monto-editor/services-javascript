package de.tudarmstadt.stg.monto.ecmascript.service;

import de.tudarmstadt.stg.monto.ecmascript.message.Message;
import de.tudarmstadt.stg.monto.ecmascript.message.ProductMessage;
import de.tudarmstadt.stg.monto.ecmascript.message.VersionMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQException;

import java.util.ArrayList;
import java.util.List;

public abstract class ECMAScriptService implements Runnable {

    private String address;
    private Context context;
    private Socket socket;
    private Thread thread;

    public ECMAScriptService(String address, Context context) {
        this.address = address;
        this.context = context;
    }

    public void start() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        socket = context.socket(ZMQ.PAIR);
        socket.connect(address);
        while (!thread.isInterrupted()) {
            JSONArray messages;
            try {
                messages = (JSONArray) JSONValue.parse(socket.recvStr());
                List<Message> decodedMessages = new ArrayList<>();
                for (Object object : messages) {
                    JSONObject message = (JSONObject) object;
                    Message decoded = message.containsKey("product") ? ProductMessage.decode(message) : VersionMessage.decode(message);
                    decodedMessages.add(decoded);
                }
                ProductMessage answer = processMessage(decodedMessages);
                socket.send(ProductMessage.encode(answer).toJSONString());
            } catch (ZMQException e) {
                System.out.println("service on " + address + " closing...");
                break;
            }
        }

        socket.close();
    }

    public void stop() {
        thread.interrupt();
    }

    public abstract ProductMessage processMessage(List<Message> messages);

}
