package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.ast.ASTVisitor;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.message.*;
import monto.service.outline.Outline;
import monto.service.outline.Outlines;
import monto.service.region.Region;
import org.zeromq.ZContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ECMAScriptOutliner extends MontoService {

    private static final Product OUTLINE = new Product("outline");
    private static final Product AST = new Product("ast");
    private static final Language JAVASCRIPT = new Language("javascript");

    public ECMAScriptOutliner(ZContext context, String address, String registrationAddress, String serviceID) {
        super(context, address, registrationAddress, serviceID, "Outline service for JavaScript", "An outline service for JavaScript", OUTLINE, JAVASCRIPT, new String[]{"Source", "ast/javascript"});
    }

    @Override
    public ProductMessage onVersionMessage(List<Message> messages) throws ParseException {
        VersionMessage version = Messages.getVersionMessage(messages);
        if (!version.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in version message");
        }
        ProductMessage ast = Messages.getProductMessage(messages, AST, JAVASCRIPT);
        if (!ast.getLanguage().equals(JAVASCRIPT)) {
            throw new IllegalArgumentException("wrong language in ast product message");
        }

        NonTerminal root = (NonTerminal) ASTs.decode(ast);

        OutlineTrimmer trimmer = new OutlineTrimmer();
        root.accept(trimmer);

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                OUTLINE,
                JAVASCRIPT,
                Outlines.encode(trimmer.getConverted()),
                new ProductDependency(ast));
    }

    @Override
    public void onConfigurationMessage(List<Message> list) throws Exception {

    }

    /**
     * Traverses the AST and removes unneeded information.
     */
    private static class OutlineTrimmer implements ASTVisitor {

        private Deque<Outline> converted = new ArrayDeque<>();
        private boolean variableDeclaration = false;

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