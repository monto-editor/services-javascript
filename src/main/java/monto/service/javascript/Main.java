package monto.service.javascript;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.zeromq.ZContext;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.resources.ResourceServer;

public class Main {

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
                  for (MontoService service : services) {
                    service.stop();
                  }
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
        .addOption("tokenizer", false, "enable JavaScript tokenizer")
        .addOption("parser", false, "enable JavaScript parser")
        .addOption("outliner", false, "enable JavaScript outliner")
        .addOption("identifierfinder", false, "enable JavaScript identifier finder")
        .addOption("codecompletioner", false, "enable JavaScript code completioner")
        .addOption("flowtypechecker", false, "enable JavaScript FlowType type error checker")
        .addOption("spellchecker", false, "enable JavaScript Aspell spelling error checker")
        .addOption("address", true, "address of services")
        .addOption("registration", true, "address of broker registration")
        .addOption("resources", true, "port for resource http server")
        .addOption("flowlocation", true, "directory in which the flow binaries are located")
        .addOption("debug", false, "enable debugging output");

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = parser.parse(options, args);

    ZMQConfiguration zmqConfig =
        new ZMQConfiguration(
            context,
            cmd.getOptionValue("address"),
            cmd.getOptionValue("registration"),
            Integer.parseInt(cmd.getOptionValue("resources")));

    resourceServer =
        new ResourceServer(
            Main.class.getResource("/icons").toExternalForm(), zmqConfig.getResourcePort());
    try {
      resourceServer.start();
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (cmd.hasOption("flowlocation")) {
      flowLocation = cmd.getOptionValue("flowlocation");
    }

    if (cmd.hasOption("tokenizer")) {
      services.add(new JavaScriptTokenizer(zmqConfig));
    }
    if (cmd.hasOption("parser")) {
      services.add(new JavaScriptParser(zmqConfig));
    }
    if (cmd.hasOption("outliner")) {
      services.add(new JavaScriptOutliner(zmqConfig));
    }
    if (cmd.hasOption("identifierfinder")) {
      services.add(new JavaScriptIdentifierFinder(zmqConfig));
    }
    if (cmd.hasOption("codecompletioner")) {
      services.add(new JavaScriptCodeCompletioner(zmqConfig));
    }
    if (cmd.hasOption("flowtypechecker")) {
      services.add(new JavaScriptFlowTypeChecker(zmqConfig, flowLocation));
    }
    if (cmd.hasOption("spellchecker")) {
      try {
        services.add(new AspellSpellChecker(zmqConfig, AspellSpellChecker.getAspellLanguages()));
      } catch (IOException e) {
        System.err.println(
            "AspellSpellChecker could not be started: no aspell languages available\n"
                + e.getMessage());
      }
    }
    if (cmd.hasOption("debug")) {
      services.forEach(MontoService::enableDebugging);
    }

    for (MontoService service : services) {
      try {
        service.start();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
