package monto.service.javascript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.zeromq.ZContext;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.resources.ResourceServer;
import monto.service.types.ServiceID;

public class JavaScriptServices {
	
	public static final ServiceID JAVASCRIPT_TOKENIZER = new ServiceID("javascriptTokenizer");
	public static final ServiceID JAVASCRIPT_PARSER = new ServiceID("javascriptParser");
	public static final ServiceID JAVASCRIPT_OUTLINER = new ServiceID("javascriptOutliner");
	public static final ServiceID JAVASCRIPT_TYPECHECKER = new ServiceID("javascriptTypechecker");
	public static final ServiceID JAVASCRIPT_CODE_COMPLETION = new ServiceID("javascriptCodeCompletion");
	public static final ServiceID ASPELL_SPELLCHECKER = new ServiceID("aspellSpellChecker");
	private static ResourceServer resourceServer;

	public static void main(String[] args) throws Exception {
        String flowLocation = "";
        ZContext context = new ZContext(1);
        List<MontoService> services = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("terminating...");
				try {
					for (MontoService service : services)
						service.stop(); 
					resourceServer.stop();
				} catch (Exception e) {
					e.printStackTrace();
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
                .addOption(required("address", true, "address of services"))
                .addOption(required("registration", true, "address of broker registration"))
                .addOption(required("configuration", true, "address of configuration messages"))
                .addOption(required("resources", true, "port for resource http server"))
                .addOption(required("dyndeps", true, "port for dynamic dependency registration"))
                .addOption("flowlocation", true, "directory in which the flow binaries are located");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        
        ZMQConfiguration zmqConfig = new ZMQConfiguration(
        		context,
        		cmd.getOptionValue("address"),
        		cmd.getOptionValue("registration"),
        		cmd.getOptionValue("configuration"),
                cmd.getOptionValue("dyndeps"),
        		Integer.parseInt(cmd.getOptionValue("resources")));

        resourceServer = new ResourceServer(JavaScriptServices.class.getResource("/images").toExternalForm(), zmqConfig.getResourcePort());
        resourceServer.start();
        
        if (cmd.hasOption("flowlocation")) {
            flowLocation = cmd.getOptionValue("flowlocation");
        }

        if (cmd.hasOption("t")) {
            services.add(new JavaScriptTokenizer(zmqConfig));
        }
        if (cmd.hasOption("p")) {
            services.add(new JavaScriptParser(zmqConfig));
        }
        if (cmd.hasOption("o")) {
            services.add(new JavaScriptOutliner(zmqConfig));
        }
        if (cmd.hasOption("c")) {
            services.add(new JavaScriptCodeCompletion(zmqConfig));
        }
        if (cmd.hasOption("f")) {
            services.add(new JavaScriptFlowTypeChecker(zmqConfig, flowLocation));
        }
        if (cmd.hasOption("s")) {
            try {
                services.add(new AspellSpellChecker(zmqConfig, AspellSpellChecker.getAspellLanguages()));
            } catch (IOException e) {
                System.out.println("AspellSpellChecker could not be started: no aspell languages available\n"+e.getMessage());
            }
        }
        
        for (MontoService service : services) {
            service.start();
        }
    }

	private static Option required(String opt, boolean hasArg, String description) {
		Option option = new Option(opt,hasArg,description);
		option.setRequired(true);
		return option;
	}
}
