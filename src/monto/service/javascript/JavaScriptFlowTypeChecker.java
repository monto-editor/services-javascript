package monto.service.javascript;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.error.Error;
import monto.service.error.Errors;
import monto.service.message.Languages;
import monto.service.message.Message;
import monto.service.message.Messages;
import monto.service.message.ProductMessage;
import monto.service.message.Products;
import monto.service.message.VersionMessage;
import monto.service.registration.ServiceDependency;
import monto.service.registration.SourceDependency;

public class JavaScriptFlowTypeChecker extends MontoService {

    private String fileName;
    private File dir;
    private int[] linesizes;
    private List<Error> errors;
    private String flowCmd;


    public JavaScriptFlowTypeChecker(ZMQConfiguration zmqConfig, String flowLocation) {
        super(zmqConfig,
        		JavaScriptServices.JAVASCRIPT_TYPECHECKER,
        		"Error Checker",
        		"Can check type errors using FlowType",
        		Products.ERRORS,
        		Languages.JAVASCRIPT,
        		dependencies(
        				new SourceDependency(Languages.JAVASCRIPT),
        				new ServiceDependency(JavaScriptServices.JAVASCRIPT_TOKENIZER)
        		));

        fileName = flowLocation + "flowTypeCheckerFile.js";
        dir = new File("./");
        errors = new ArrayList<>();
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            flowCmd = flowLocation + "flow_mac";
        } else if (os.contains("win")) {
            flowCmd = "";
        } else {
            flowCmd = flowLocation + "flow_linux";
        }
        createFlowConfig();
    }

    @Override
    public ProductMessage onVersionMessage(List<Message> messages) throws Exception {
        errors = new ArrayList<>();
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(Languages.JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in version message");
        }

        createSourceFile(version.getContent());
        runFlowTypecheck();

        return productMessage(
                version.getVersionId(),
                version.getSource(),
                Errors.encode(errors.stream()));
    }

    /*
     * Creates a file of the source content that later can be checked with flowtype.
     * Also sets linesizes to the bounds of source content.
     */
    private void createSourceFile(String content) throws FileNotFoundException, UnsupportedEncodingException {
        String[] lines = content.split("\n");
        linesizes = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            linesizes[i] = lines[i].length();
        }
        PrintWriter writer = new PrintWriter(fileName, "UTF-8");
        writer.println(content);
        writer.close();
    }

    private void createFlowConfig() {
        try {
            String[] cmd = new String[]{"/bin/sh", "-c", "./" + flowCmd + " init"};
            Process p = Runtime.getRuntime().exec(cmd, null, dir);
            p.waitFor();
        } catch (Exception e) {
            System.out.println("FlowType could not be started");
        }
    }

    private void runFlowTypecheck() throws IOException, InterruptedException {
        String[] cmd = new String[]{"/bin/sh", "-c", "./" + flowCmd + " check-contents < " + fileName};

        Process p = Runtime.getRuntime().exec(cmd, null, dir);
        BufferedReader bri = new BufferedReader
                (new InputStreamReader(p.getInputStream()));
        BufferedReader bre = new BufferedReader
                (new InputStreamReader(p.getErrorStream()));

        handleFlowInput(bri);
        handleFlowError(bre);

        p.waitFor();
    }

    private void handleFlowInput(BufferedReader bri) throws IOException {
        int offset = -1;
        int length = -1;
        String category = "type";
        StringBuilder description = new StringBuilder();

        // first get rid of first empty line
        bri.readLine();
        String input;
        while ((input = bri.readLine()) != null) {
            if (input.startsWith("Found ")) {
                break;
            } else if (input.equals("")) {
                errors.add(new Error(offset, length, "error", category, description.toString()));
                offset = -1;
                length = -1;
                description = new StringBuilder();
            } else {
                String[] parts = input.split(":");
                if (offset == -1 && parts.length > 2) {
                    String[] lengthParts = parts[2].split(",");
                    int begin = Integer.parseInt(lengthParts[0]);
                    int end = Integer.parseInt(lengthParts[1]);
                    length = end - begin + 1;
                    offset = convertToOffset(Integer.parseInt(parts[1]), begin) - 1;
                }
                description.append(input);
                description.append("\n");
            }
        }
        bri.close();
    }

    private int convertToOffset(int lineNumber, int position) {
        int offset = position;
        for (int i = 0; i < lineNumber - 1; i++) {
            offset += linesizes[i] + 1;
        }
        return offset;
    }

    private void handleFlowError(BufferedReader bre) throws IOException {
        StringBuilder builder = new StringBuilder();
        String error;
        while ((error = bre.readLine()) != null) {
            builder.append(error);
        }
        error = builder.toString();
        if (!error.equals("")) {
            System.out.println(error);
        }
        bre.close();
    }
}
