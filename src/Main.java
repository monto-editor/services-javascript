import ECMAScript.ECMAScriptLexer;
import ECMAScript.ECMAScriptParser;
import Services.ECMAScriptService;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.SyntaxTree;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.antlr.v4.runtime.ANTLRFileStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public Main() {

    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
//        try {
//            ANTLRFileStream fileIn = new ANTLRFileStream("/home/qam/test.js");
//            ECMAScriptLexer lexer = new ECMAScriptLexer(fileIn);
//            CommonTokenStream cts = new CommonTokenStream(lexer);
//            ECMAScriptParser parser = new ECMAScriptParser(cts);
//            SyntaxTree asd = parser.program();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        int[] ports = {5010, 5011};
        List<ECMAScriptService> services = new ArrayList<>();
        Context context = ZMQ.context(1);
        Scanner scanner = new Scanner(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("cleaning up before exiting..");
                context.term();
                for (ECMAScriptService service : services) {
                    service.stop();
                }
            }
        });

        for (int port : ports) {
            ECMAScriptService service = new ECMAScriptService("tcp://localhost:" + port, context);
            service.start();
            services.add(service);
        }

        while (true) {
            System.out.print(">");
            String input = scanner.next();
            if (input.equals("stop")) {
                System.exit(1);
            }
        }
    }
}
