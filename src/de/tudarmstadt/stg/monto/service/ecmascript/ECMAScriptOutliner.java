package de.tudarmstadt.stg.monto.service.ecmascript;

import de.tudarmstadt.stg.monto.service.ast.*;
import de.tudarmstadt.stg.monto.service.message.*;
import de.tudarmstadt.stg.monto.service.outline.Outline;
import de.tudarmstadt.stg.monto.service.outline.Outlines;
import de.tudarmstadt.stg.monto.service.region.Region;
import de.tudarmstadt.stg.monto.service.MontoService;
import org.zeromq.ZMQ;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ECMAScriptOutliner extends MontoService {

    public ECMAScriptOutliner(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage onMessage(List<Message> messages) throws ParseException {
        VersionMessage version = Messages.getVersionMessage(messages);
        ProductMessage ast = Messages.getProductMessage(messages, ECMAScriptServices.AST, ECMAScriptServices.JSON);

        NonTerminal root = (NonTerminal) ASTs.decode(ast);

        OutlineTrimmer trimmer = new OutlineTrimmer();
        root.accept(trimmer);
        Contents content = new StringContent(Outlines.encode(trimmer.getConverted()).toJSONString());

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                ECMAScriptServices.OUTLINE,
                ECMAScriptServices.JSON,
                content,
                new ProductDependency(ast));
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
            ;
        }

        @Override
        public void visit(Terminal node) {

        }
    }
}