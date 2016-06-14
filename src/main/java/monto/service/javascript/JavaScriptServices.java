package monto.service.javascript;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.resources.ResourceServer;
import monto.service.types.ServiceId;
import org.apache.commons.cli.*;
import org.zeromq.ZContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JavaScriptServices {

  public static final ServiceId JAVASCRIPT_TOKENIZER = new ServiceId("javascriptTokenizer");
  public static final ServiceId JAVASCRIPT_PARSER = new ServiceId("javascriptParser");
  public static final ServiceId JAVASCRIPT_OUTLINER = new ServiceId("javascriptOutliner");
  public static final ServiceId JAVASCRIPT_TYPECHECKER = new ServiceId("javascriptTypechecker");
  public static final ServiceId JAVASCRIPT_CODE_COMPLETION =
      new ServiceId("javascriptCodeCompletion");
  public static final ServiceId ASPELL_SPELLCHECKER = new ServiceId("aspellSpellChecker");
  private static ResourceServer resourceServer;

  public static void main(String[] args) throws Exception {
    String flowLocation = "";
    ZContext context = new ZContext(1);
    List<MontoService> services = new ArrayList<>();

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread() {
              @Override
              public void run() {
                System.out.println("terminating...");
                try {
                  for (MontoService service : services) service.stop();
                  resourceServer.stop();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                context.destroy();

                System.out.println("everything terminated, good bye");
              }
            });

    Options options = new Options();
    options
        .addOption("t", false, "enable javascript tokenizer")
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
        .addOption("flowlocation", true, "directory in which the flow binaries are located")
        .addOption("debug", false, "enable debugging output");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    ZMQConfiguration zmqConfig =
        new ZMQConfiguration(
            context,
            cmd.getOptionValue("address"),
            cmd.getOptionValue("registration"),
            cmd.getOptionValue("configuration"),
            cmd.getOptionValue("dyndeps"),
            Integer.parseInt(cmd.getOptionValue("resources")));

    resourceServer =
        new ResourceServer(
            JavaScriptServices.class.getResource("/images").toExternalForm(),
            zmqConfig.getResourcePort());
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
        System.err.println(
            "AspellSpellChecker could not be started: no aspell languages available\n"
                + e.getMessage());
      }
    }
    if (cmd.hasOption("debug")) {
      for (MontoService service : services) service.enableDebugging();
    }

    for (MontoService service : services) {
      service.start();
    }
  }

  private static Option required(String opt, boolean hasArg, String description) {
    Option option = new Option(opt, hasArg, description);
    option.setRequired(true);
    return option;
  }
}
