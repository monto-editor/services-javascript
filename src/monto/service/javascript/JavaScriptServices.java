package monto.service.javascript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.zeromq.ZContext;

import monto.service.MontoService;

public class JavaScriptServices {

    public static void main(String[] args) throws ParseException {
        String address = "tcp://*";
        String regAddress = "tcp://*:5004";
        String flowLocation = "";
        ZContext context = new ZContext(1);
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

        Options options = new Options();
        options.addOption("t", false, "enable javascript tokenizer")
                .addOption("p", false, "enable javascript parser")
                .addOption("o", false, "enable javascript outliner")
                .addOption("c", false, "enable javascript code completioner")
                .addOption("f", false, "enable javascript FlowType type error checker")
                .addOption("s", false, "enable javascript Aspell spelling error checker")
                .addOption("address", true, "address of services")
                .addOption("registration", true, "address of broker registration")
                .addOption("flowlocation", true, "directory in which the flow binaries are located");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("address")) {
            address = cmd.getOptionValue("address");
        }

        if (cmd.hasOption("registration")) {
            regAddress = cmd.getOptionValue("registration");
        }

        if (cmd.hasOption("flowlocation")) {
            flowLocation = cmd.getOptionValue("flowlocation");
        }


        if (cmd.hasOption("t")) {
            services.add(new JavaScriptTokenizer(context, address, regAddress, "javascriptTokenizer"));
        }
        if (cmd.hasOption("p")) {
            services.add(new JavaScriptParser(context, address, regAddress, "javascriptParser"));
        }
        if (cmd.hasOption("o")) {
            services.add(new JavaScriptOutliner(context, address, regAddress, "javascriptOutliner"));
        }
        if (cmd.hasOption("c")) {
            services.add(new JavaScriptCodeCompletion(context, address, regAddress, "javascriptCompletioner"));
        }
        if (cmd.hasOption("f")) {
            services.add(new JavaScriptFlowTypeChecker(context, address, regAddress, "javascriptFlowTypeChecker", flowLocation));
        }
        if (cmd.hasOption("s")) {
            try {
                services.add(new AspellSpellChecker(context, address, regAddress, "aspellSpellChecker", AspellSpellChecker.getAspellLanguages()));
            } catch (IOException e) {
                System.out.println("AspellSpellChecker could not be started: no aspell languages available");
            }
        }

        for (MontoService service : services) {
            service.start();
        }
    }
}
