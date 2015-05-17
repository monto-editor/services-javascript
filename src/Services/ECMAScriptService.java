package Services;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQException;

import java.nio.channels.ClosedByInterruptException;
import java.sql.Time;
import java.util.Timer;

/**
 * Created by qam on 17.05.15.
 */
public class ECMAScriptService implements Runnable {

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
            String msg;
            try {
                msg = socket.recvStr();
            } catch (ZMQException e) {
                break;
            }
            System.out.println(msg);
            if (msg != null) {
                socket.send(address);
            }
        }
        socket.close();
    }

    public void stop() {
        thread.interrupt();
    }

}
