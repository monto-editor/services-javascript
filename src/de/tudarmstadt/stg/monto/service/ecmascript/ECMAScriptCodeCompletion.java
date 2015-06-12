package de.tudarmstadt.stg.monto.service.ecmascript;

import de.tudarmstadt.stg.monto.service.MontoService;
import de.tudarmstadt.stg.monto.service.ast.*;
import de.tudarmstadt.stg.monto.service.completion.Completion;
import de.tudarmstadt.stg.monto.service.completion.Completions;
import de.tudarmstadt.stg.monto.service.message.*;
import de.tudarmstadt.stg.monto.service.region.IRegion;
import org.zeromq.ZMQ;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ECMAScriptCodeCompletion extends MontoService {

    public ECMAScriptCodeCompletion(String address, ZMQ.Context context) {
        super(address, context);
    }

    @Override
    public ProductMessage onMessage(List<Message> messages) throws ParseException{
        //TODO modify for javascript
        VersionMessage version = Messages.getVersionMessage(messages);
        ProductMessage ast = Messages.getProductMessage(messages, ECMAScriptServices.AST, ECMAScriptServices.JSON);

        System.out.println(version.getSelections().toString());

        if (version.getSelections().size() > 0) {
            AST root = ASTs.decode(ast);
            List<Completion> allcompletions = allCompletions(version.getContent(), root);
            List<AST> selectedPath = selectedPath(root, version.getSelections().get(0));

            if (selectedPath.size() > 0 && last(selectedPath) instanceof Terminal) {
                Terminal terminalToBeCompleted = (Terminal) last(selectedPath);
                String toBeCompleted = version.getContent().extract(terminalToBeCompleted).toString();
                Stream<Completion> relevant =
                        allcompletions
                                .stream()
                                .filter(comp -> comp.getReplacement().startsWith(toBeCompleted))
                                .map(comp -> new Completion(
                                        comp.getDescription() + ": " + comp.getReplacement(),
                                        comp.getReplacement().substring(toBeCompleted.length()),
                                        version.getSelections().get(0).getStartOffset(),
                                        comp.getIcon()));

                Contents content = new StringContent(Completions.encode(relevant).toJSONString());
                return new ProductMessage(
                        version.getVersionId(),
                        new LongKey(1),
                        version.getSource(),
                        ECMAScriptServices.COMPLETIONS,
                        ECMAScriptServices.JSON,
                        content,
                        new ProductDependency(ast));
            }
            throw new IllegalArgumentException(String.format("Last token in selection path is not a terminal: %s", selectedPath));
        }

        throw new IllegalArgumentException("Code completion needs selection");
    }

    private static List<Completion> allCompletions(Contents contents, AST root) {
        AllCompletions completionVisitor = new AllCompletions(contents);
        root.accept(completionVisitor);
        return completionVisitor.getCompletions();
    }

    private static class AllCompletions implements ASTVisitor {

        private List<Completion> completions = new ArrayList<>();
        private Contents content;
        private boolean fieldDeclaration;

        public AllCompletions(Contents content) {
            this.content = content;
        }

        @Override
        public void visit(NonTerminal node) {
            switch (node.getName()) {
                case "normalClassDeclaration":
                    structureDeclaration(node, "class", IconType.NO_IMG);
                    break;

                case "enumDeclaration":
                    structureDeclaration(node, "enum", IconType.NO_IMG);
                    break;

                case "enumConstant":
                    leaf(node, "constant", IconType.NO_IMG);
                    break;

                case "fieldDeclaration":
                    fieldDeclaration = true;
                    node.getChildren().forEach(child -> child.accept(this));
                    fieldDeclaration = false;

                case "variableDeclaratorId":
                    if (fieldDeclaration)
                        leaf(node, "field", IconType.NO_IMG);
                    break;

                case "methodDeclarator":
                    leaf(node, "method", IconType.NO_IMG);

                default:
                    node.getChildren().forEach(child -> child.accept(this));
            }
        }

        @Override
        public void visit(Terminal token) {

        }

        private void structureDeclaration(NonTerminal node, String name, String icon) {
            Terminal structureIdent = (Terminal) node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .reduce((previous, current) -> current).get();
            completions.add(new Completion(name, content.extract(structureIdent).toString(), icon));
            node.getChildren().forEach(child -> child.accept(this));
        }

        private void leaf(NonTerminal node, String name, String icon) {
            AST ident = node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .findFirst().get();
            completions.add(new Completion(name, content.extract(ident).toString(), icon));
        }


        public List<Completion> getCompletions() {
            return completions;
        }
    }

    private static List<AST> selectedPath(AST root, Selection sel) {
        SelectedPath finder = new SelectedPath(sel);
        root.accept(finder);
        return finder.getSelected();
    }

    private static class SelectedPath implements ASTVisitor {

        private Selection selection;
        private List<AST> selectedPath = new ArrayList<>();

        public SelectedPath(Selection selection) {
            this.selection = selection;
        }

        @Override
        public void visit(NonTerminal node) {
            if (selection.inRange(node) || rightBehind(selection, node))
                selectedPath.add(node);
            node.getChildren()
                    .stream()
                    .filter(child -> selection.inRange(child) || rightBehind(selection, child))
                    .forEach(child -> child.accept(this));
        }

        @Override
        public void visit(Terminal token) {
            if (rightBehind(selection, token))
                selectedPath.add(token);
        }

        public List<AST> getSelected() {
            return selectedPath;
        }

        private static boolean rightBehind(IRegion region1, IRegion region2) {
            try {
                return region1.getStartOffset() == region2.getEndOffset();
            } catch (Exception e) {
                return false;
            }
        }

    }

    private static <A> A last(List<A> list) {
        return list.get(list.size() - 1);
    }
}
