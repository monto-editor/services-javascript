package monto.service.ecmascript;

import monto.service.MontoService;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;

public class ECMAScriptServices {

    private static final int regPort = 5009;

    public static void main(String[] args) {
        ZContext context = new ZContext(1);
        String addr = "tcp://localhost:";
        List<MontoService> services = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("terminating...");
                for (MontoService service : services) {
                    service.stop();
                }
                context.destroy();
                System.out.println("everything terminated, good bye");
            }
        });

        services.add(new ECMAScriptTokenizer(context, addr, regPort, "ecmascriptTokenizer"));
        services.add(new ECMAScriptParser(context, addr, regPort, "ecmascriptParser"));
        services.add(new ECMAScriptOutliner(context, addr, regPort, "ecmascriptOutliner"));
        services.add(new ECMAScriptCodeCompletion(context, addr, regPort, "ecmascriptCodeCompletioner"));
        services.add(new FlowTypeChecker(context, addr, regPort, "ecmascriptFlowTypeChecker"));

        for (MontoService service : services) {
            service.start();
        }
    }
}
