package monto.service.javascript;

import java.util.ArrayList;
import java.util.List;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.gson.GsonMonto;
import monto.service.identifier.Identifier;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.ProductDependency;
import monto.service.registration.ProductDescription;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import monto.service.types.ParseException;

public class JavaScriptIdentifierFinder extends MontoService {

  public JavaScriptIdentifierFinder(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        JavaScriptServices.IDENTIFIER_FINDER,
        "Identifer Finder",
        "Searches AST for identifiers",
        productDescriptions(new ProductDescription(Products.IDENTIFIER, Languages.JAVASCRIPT)),
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

    AST root = GsonMonto.fromJson(ast, AST.class);
    IdentifierVisitor identifierVisitor = new IdentifierVisitor(version.getContents());
    root.accept(identifierVisitor);
    List<Identifier> identifiers = identifierVisitor.getIdentifiers();

    sendProductMessage(
        version.getId(),
        version.getSource(),
        Products.IDENTIFIER,
        Languages.JAVASCRIPT,
        GsonMonto.toJsonTree(identifiers));
  }

  private class IdentifierVisitor implements ASTVisitor {

    private List<Identifier> identifiers = new ArrayList<>();
    private String content;

    public IdentifierVisitor(String content) {
      this.content = content;
    }

    @Override
    public void visit(NonTerminal node) {
      switch (node.getName()) {
        case "Class":
          structureDeclaration(node, "class");
          break;

        case "Const":
          structureDeclaration(node, "constant");
          break;

        case "variableDeclaration":
          structureDeclaration(node, "variable");
          break;

        case "functionDeclaration":
          addFuncToConverted(node, "method");

        default:
          node.getChildren().forEach(child -> child.accept(this));
      }
    }

    private void addFuncToConverted(NonTerminal node, String type) {
      Object[] terminalChildren =
          node.getChildren().stream().filter(ast -> ast instanceof Terminal).toArray();
      if (terminalChildren.length > 1) {
        Terminal structureIdent = (Terminal) terminalChildren[1];
        String identifier = structureIdent.extract(content);
        identifiers.add(new Identifier(identifier, type));
      }
      node.getChildren().forEach(child -> child.accept(this));
    }

    @Override
    public void visit(Terminal token) {}

    private void structureDeclaration(NonTerminal node, String type) {
      Terminal structureIdent =
          (Terminal)
              node.getChildren()
                  .stream()
                  .filter(ast -> ast instanceof Terminal)
                  .reduce((previous, current) -> current)
                  .get();
      String identifier = structureIdent.extract(content);
      identifiers.add(new Identifier(identifier, type));
      node.getChildren().forEach(child -> child.accept(this));
    }

    public List<Identifier> getIdentifiers() {
      return identifiers;
    }
  }
}
