package de.tudarmstadt.stg.monto.service;

import de.tudarmstadt.stg.monto.service.message.Message;
import de.tudarmstadt.stg.monto.service.message.ProductMessage;
import de.tudarmstadt.stg.monto.service.message.VersionMessage;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class MontoService implements Runnable {

    private String address;
    private Context context;
    private Socket socket;
    private Thread thread;

    public MontoService(String address, Context context) {
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
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("service on " + address + " closing...");
        socket.close();
    }

    public void stop() {
        thread.interrupt();
    }

    public abstract ProductMessage processMessage(List<Message> messages) throws IOException;

}
