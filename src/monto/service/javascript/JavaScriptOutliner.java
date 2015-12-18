package monto.service.javascript;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.ASTVisitor;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.message.IconType;
import monto.service.message.Languages;
import monto.service.message.Message;
import monto.service.message.Messages;
import monto.service.message.ParseException;
import monto.service.message.ProductDependency;
import monto.service.message.ProductMessage;
import monto.service.message.Products;
import monto.service.message.VersionMessage;
import monto.service.outline.Outline;
import monto.service.outline.Outlines;
import monto.service.region.Region;
import monto.service.registration.ServiceDependency;
import monto.service.registration.SourceDependency;

public class JavaScriptOutliner extends MontoService {

    public JavaScriptOutliner(ZMQConfiguration zmqConfig) {
        super(zmqConfig,
        		JavaScriptServices.JAVASCRIPT_OUTLINER,
        		"Outline",
        		"An outline service for JavaScript",
        		Languages.JAVASCRIPT,
        		Products.OUTLINE,
        		options(),
        		dependencies(
        				new SourceDependency(Languages.JAVASCRIPT),
        				new ServiceDependency(JavaScriptServices.JAVASCRIPT_PARSER)
        		));
    }

    @Override
    public ProductMessage onVersionMessage(List<Message> messages) throws ParseException {
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(Languages.JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in version message");
        }
        ProductMessage ast = Messages.getProductMessage(messages, Products.AST, Languages.JAVASCRIPT);
        if (!ast.getLanguage().equals(Languages.JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in ast product message");
        }

        NonTerminal root = (NonTerminal) ASTs.decode(ast);

        OutlineTrimmer trimmer = new OutlineTrimmer();
        root.accept(trimmer);

        return productMessage(
                version.getVersionId(),
                version.getSource(),
                Products.OUTLINE,
                Outlines.encode(trimmer.getConverted()),
                new ProductDependency(ast));
    }

    /**
     * Traverses the AST and removes unneeded information.
     */
    private static class OutlineTrimmer implements ASTVisitor {

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
                    addVarToConverted(node, "class", IconType.NO_IMG);
                    break;

                case "Enum":
                    addVarToConverted(node, "enum", IconType.NO_IMG);
                    break;

                case "Const":
                    addVarToConverted(node, "const", IconType.NO_IMG);
                    break;

                case "variableDeclaration":
                    addVarToConverted(node, "var", IconType.NO_IMG);
                    break;

                case "functionDeclaration":
                    addFuncToConverted(node, "function", IconType.NO_IMG);
                    break;

                default:
                    node.getChildren().forEach(child -> child.accept(this));
            }
        }

        private void addVarToConverted(NonTerminal node, String name, String icon) {
            node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .findFirst()
                    .ifPresent(ident -> {
                        Outline ol = new Outline(name, new Region(ident.getStartOffset(), ident.getLength()), icon);
                        converted.peek().addChild(ol);
                        converted.push(ol);
                        node.getChildren().forEach(child -> child.accept(this));
                        converted.pop();
                    });
        }

        private void addFuncToConverted(NonTerminal node, String name, String icon) {
            Object[] terminalChildren = node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .toArray();
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
        public void visit(Terminal node) {

        }
    }
}