package de.tudarmstadt.stg.monto.service.ecmascript;

import de.tudarmstadt.stg.monto.service.MontoService;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public Main() {

    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
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

        for (MontoService service : services) {
            service.start();
        }
    }
}
