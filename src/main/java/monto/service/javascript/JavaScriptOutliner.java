package monto.service.javascript;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.gson.GsonMonto;
import monto.service.outline.Outline;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.region.Region;
import monto.service.registration.ProductDependency;
import monto.service.registration.ProductDescription;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import monto.service.types.ParseException;

public class JavaScriptOutliner extends MontoService {

  public JavaScriptOutliner(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        JavaScriptServices.OUTLINER,
        "Outline",
        "An outline service for JavaScript",
        productDescriptions(new ProductDescription(Products.OUTLINE, Languages.JAVASCRIPT)),
        options(),
        dependencies(
            new SourceDependency(Languages.JAVASCRIPT),
            new ProductDependency(JavaScriptServices.PARSER, Products.AST, Languages.JAVASCRIPT)),
        commands());
  }

  @Override
  public void onRequest(Request request) throws ParseException {
    SourceMessage version =
        request
            .getSourceMessage()
            .orElseThrow(() -> new IllegalArgumentException("No version message in request"));
    ProductMessage ast =
        request
            .getProductMessage(Products.AST, Languages.JAVASCRIPT)
            .orElseThrow(() -> new IllegalArgumentException("No AST message in request"));

    NonTerminal root = (NonTerminal) GsonMonto.fromJson(ast, AST.class);

    OutlineTrimmer trimmer = new OutlineTrimmer();
    root.accept(trimmer);

    sendProductMessage(
        version.getId(),
        version.getSource(),
        Products.OUTLINE,
        Languages.JAVASCRIPT,
        GsonMonto.toJsonTree(trimmer.getConverted()));
  }

  /**
   * Traverses the AST and removes unneeded information.
   */
  private class OutlineTrimmer implements ASTVisitor {

    private Deque<Outline> converted = new ArrayDeque<>();

    public Outline getConverted() {
      return converted.getFirst();
    }

    @Override
    public void visit(NonTerminal node) {
      switch (node.getName()) {
        case "program":
          converted.push(new Outline("program", node, null));
          node.getChildren().forEach(child -> child.accept(this));
          // program doesn't get poped from the stack
          // to be available as a return value.
          break;

        case "Class":
          addVarToConverted(node, "class", getResource("class.png"));
          break;

        case "Const":
          addVarToConverted(node, "const", getResource("const.png"));
          break;

        case "variableDeclaration":
          addVarToConverted(node, "var", getResource("variable.png"));
          break;

        case "functionDeclaration":
          addFuncToConverted(node, "function", getResource("public.png"));
          break;

        default:
          node.getChildren().forEach(child -> child.accept(this));
      }
    }

    private void addVarToConverted(NonTerminal node, String name, URL icon) {
      node.getChildren()
          .stream()
          .filter(ast -> ast instanceof Terminal)
          .findFirst()
          .ifPresent(
              ident -> {
                Outline ol =
                    new Outline(name, new Region(ident.getStartOffset(), ident.getLength()), icon);
                converted.peek().addChild(ol);
                converted.push(ol);
                node.getChildren().forEach(child -> child.accept(this));
                converted.pop();
              });
    }

    private void addFuncToConverted(NonTerminal node, String name, URL icon) {
      Object[] terminalChildren =
          node.getChildren().stream().filter(ast -> ast instanceof Terminal).toArray();
      if (terminalChildren.length > 1) {
        Terminal a = (Terminal) terminalChildren[1];
        Outline ol = new Outline(name, new Region(a.getStartOffset(), a.getLength()), icon);
        converted.peek().addChild(ol);
        converted.push(ol);
        node.getChildren().forEach(child -> child.accept(this));
        converted.pop();
      }
    }

    @Override
    public void visit(Terminal node) {}
  }
}
