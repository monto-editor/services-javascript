package monto.service.javascript;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.completion.Completion;
import monto.service.gson.GsonMonto;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.ProductDependency;
import monto.service.registration.ProductDescription;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import monto.service.types.ParseException;

public class JavaScriptCodeCompletion extends MontoService {

  public JavaScriptCodeCompletion(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        JavaScriptServices.JAVASCRIPT_CODE_COMPLETION,
        "Code Completion",
        "A code completion service for JavaScript",
        productDescriptions(new ProductDescription(Products.COMPLETIONS, Languages.JAVASCRIPT)),
        options(),
        dependencies(
            new SourceDependency(Languages.JAVASCRIPT),
            new ProductDependency(
                JavaScriptServices.JAVASCRIPT_PARSER, Products.AST, Languages.JAVASCRIPT)),
        commands());
  }

  private List<Completion> allCompletions(String contents, AST root) {
    AllCompletions completionVisitor = new AllCompletions(contents);
    root.accept(completionVisitor);
    return completionVisitor.getCompletions();
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

    AST root = GsonMonto.fromJson(ast, AST.class);
    List<Completion> allcompletions = allCompletions(version.getContents(), root);

    List<Completion> relevant =
        allcompletions
            .stream()
            .map(
                comp
                    -> new Completion(
                        comp.getDescription() + ": " + comp.getReplacement(),
                        comp.getReplacement(),
                        0,
                        comp.getIcon()))
            .collect(Collectors.toList());

    sendProductMessage(
        version.getId(),
        version.getSource(),
        Products.COMPLETIONS,
        Languages.JAVASCRIPT,
        GsonMonto.toJsonTree(relevant));
  }

  private class AllCompletions implements ASTVisitor {

    private List<Completion> completions = new ArrayList<>();
    private String content;

    public AllCompletions(String content) {
      this.content = content;
    }

    @Override
    public void visit(NonTerminal node) {
      switch (node.getName()) {
        case "Class":
          structureDeclaration(node, "class", getResource("class.png"));
          break;

        case "Const":
          structureDeclaration(node, "const", getResource("const.png"));
          break;

        case "variableDeclaration":
          structureDeclaration(node, "var", getResource("variable.png"));
          break;

        case "functionDeclaration":
          addFuncToConverted(node, "function", getResource("public.png"));

        default:
          node.getChildren().forEach(child -> child.accept(this));
      }
    }

    private void addFuncToConverted(NonTerminal node, String name, URL icon) {
      Object[] terminalChildren =
          node.getChildren().stream().filter(ast -> ast instanceof Terminal).toArray();
      if (terminalChildren.length > 1) {
        Terminal structureIdent = (Terminal) terminalChildren[1];
        completions.add(new Completion(name, structureIdent.extract(content), icon));
      }
      node.getChildren().forEach(child -> child.accept(this));
    }

    @Override
    public void visit(Terminal token) {}

    private void structureDeclaration(NonTerminal node, String name, URL icon) {
      Terminal structureIdent =
          (Terminal)
              node.getChildren()
                  .stream()
                  .filter(ast -> ast instanceof Terminal)
                  .reduce((previous, current) -> current)
                  .get();
      completions.add(new Completion(name, structureIdent.extract(content), icon));
      node.getChildren().forEach(child -> child.accept(this));
    }

    public List<Completion> getCompletions() {
      return completions;
    }
  }
}
