package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.message.*;
import org.zeromq.ZMQ;

import java.io.*;
import java.util.List;

public class FlowTypeChecker extends MontoService {

    private String fileName = "flowTypeCheckerFile.js";
    private File dir = new File("./");
    private String input;
    private String error;

    public FlowTypeChecker(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage onMessage(List<Message> messages) throws Exception {
        VersionMessage version = Messages.getVersionMessage(messages);

        createSourceFile(version.getContent());
        createFlowConfig();
        runFlowTypecheck();

        Contents newContent = new StringContent(input + "\n\n" + error);

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                ECMAScriptServices.ERRORS,
                ECMAScriptServices.JSON,
                newContent);
    }

    private void createSourceFile(Contents content) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println(content);
        writer.close();
    }

    private void createFlowConfig() throws IOException, InterruptedException {
        String[] cmd = new String[]{"/bin/sh", "-c", "./flow init"};
        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        p.waitFor();
    }

    private void runFlowTypecheck() throws IOException, InterruptedException {
        String[] cmd = new String[]{"/bin/sh", "-c", "./flow check-contents < " + fileName};
        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        StringBuffer buffer = new StringBuffer();

        BufferedReader bri = new BufferedReader
                (new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader
                (new InputStreamReader(p.getErrorStream()));
        while ((input = bri.readLine()) != null) {
            buffer.append(input);
            buffer.append("\n");
        }
        input = buffer.toString();
        buffer = new StringBuffer();
        bri.close();
        while ((error = bre.readLine()) != null) {
            buffer.append(error);
            buffer.append("\n");
        }
        error = buffer.toString();
        bre.close();
        p.waitFor();
    }
}
