package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.message.Message;
import monto.service.message.Messages;
import monto.service.message.ProductMessage;
import monto.service.message.VersionMessage;
import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

public class FlowTypeChecker extends MontoService {

    public FlowTypeChecker(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage onMessage(List<Message> list) throws Exception {
        VersionMessage versionMessage = Messages.getVersionMessage(list);
        String fileName = "flowTypeCheckerFile.js";
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println(versionMessage.getContent());
        writer.close();
        String[] cmd = new String[]{"/bin/sh", "-c", "flow check-contents < " + fileName};
        Process p = Runtime.getRuntime().exec(cmd);
        String line;
        BufferedReader bri = new BufferedReader
                (new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader
                (new InputStreamReader(p.getErrorStream()));
        while ((line = bri.readLine()) != null) {
            System.out.println(line);
        }
        bri.close();
        while ((line = bre.readLine()) != null) {
            System.out.println(line);
        }
        bre.close();
        p.waitFor();

        return null;
    }
}
