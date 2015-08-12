package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.error.Error;
import monto.service.error.Errors;
import monto.service.message.*;
import org.zeromq.ZContext;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FlowTypeChecker extends MontoService {

    private static final Product ERRORS = new Product("errors");
    private static final Language JAVASCRIPT = new Language("javascript");

    private String fileName;
    private File dir;
    private int[] linesizes;
    private List<Error> errors;

    public FlowTypeChecker(ZContext context, String address, int registrationPort, String serviceID) {
        super(context, address, registrationPort, serviceID, ERRORS, JAVASCRIPT, new String[]{"Source"});
        fileName = "flowTypeCheckerFile.js";
        dir = new File("./");
        errors = new ArrayList<>();
    }

    @Override
    public ProductMessage onMessage(List<Message> messages) throws Exception {
        VersionMessage version = Messages.getVersionMessage(messages);

        createSourceFile(version.getContent());
        createFlowConfig();
        runFlowTypecheck();

        Contents newContent = new StringContent(Errors.encode(errors.stream()).toJSONString());

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                ERRORS,
                JAVASCRIPT,
                newContent);
    }

    /*
     * Creates a file of the source content that later can be checked with flowtype.
     * Also sets linesizes to the bounds of source content.
     */
    private void createSourceFile(Contents content) throws FileNotFoundException, UnsupportedEncodingException {
        String[] lines = content.toString().split("\n");
        linesizes = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            linesizes[i] = lines[i].length();
        }
        errors = new ArrayList<>();
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
                errors.add(new Error(offset, length, category, description.toString()));
                offset = -1;
                length = -1;
                description = new StringBuilder();
            } else {
                String[] parts = input.split(":");
                if (offset == -1) {
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
        System.out.println(error);
        bre.close();
    }
}
