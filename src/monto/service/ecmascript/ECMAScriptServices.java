package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.message.Language;
import monto.service.message.Product;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import java.util.ArrayList;
import java.util.List;

public class ECMAScriptServices {

    public static final Language JSON = new Language("json");
    public static final Product TOKENS = new Product("tokens");
    public static final Product AST = new Product("ast");
    public static final Product OUTLINE = new Product("outline");
    public static final Product COMPLETIONS = new Product("completions");
    public static final Product ERRORS = new Product("errors");

    public static void main(String[] args) {
        String addr = "tcp://localhost:";
        List<MontoService> services = new ArrayList<>();
        Context context = ZMQ.context(1);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("terminating...");
                context.term();
                for (MontoService service : services) {
                    service.stop();
                }
                System.out.println("terminated");
            }
        });

        services.add(new ECMAScriptTokenizer(addr + 5010, context));
        services.add(new ECMAScriptParser(addr + 5011, context));
        services.add(new ECMAScriptOutliner(addr + 5012, context));
        services.add(new ECMAScriptCodeCompletion(addr + 5013, context));
        services.add(new FlowTypeChecker(addr + 5014, context));

        for (MontoService service : services) {
            service.start();
        }
    }
}
