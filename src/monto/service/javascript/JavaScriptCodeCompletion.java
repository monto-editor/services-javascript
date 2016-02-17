package monto.service.javascript;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.completion.Completion;
import monto.service.completion.Completions;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.region.IRegion;
import monto.service.registration.ProductDependency;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import monto.service.types.ParseException;
import monto.service.types.Selection;

public class JavaScriptCodeCompletion extends MontoService {

    public JavaScriptCodeCompletion(ZMQConfiguration zmqConfig) {
        super(zmqConfig,
        		JavaScriptServices.JAVASCRIPT_CODE_COMPLETION,
        		"Code Completion",
        		"A code completion service for JavaScript",
        		Languages.JAVASCRIPT,
        		Products.COMPLETIONS,
        		options(),
        		dependencies(
        				new SourceDependency(Languages.JAVASCRIPT),
        				new ProductDependency(JavaScriptServices.JAVASCRIPT_PARSER,Products.AST,Languages.JAVASCRIPT)
        		));
    }

    private List<Completion> allCompletions(String contents, AST root) {
        AllCompletions completionVisitor = new AllCompletions(contents);
        root.accept(completionVisitor);
        return completionVisitor.getCompletions();
    }

    @Override
    public ProductMessage onRequest(Request request) throws ParseException {
    	SourceMessage version = request.getSourceMessage()
    			.orElseThrow(() -> new IllegalArgumentException("No version message in request"));
        ProductMessage ast = request.getProductMessage(Products.AST, Languages.JAVA)
        		.orElseThrow(() -> new IllegalArgumentException("No AST message in request"));
        
        if (version.getSelections().size() > 0) {
            AST root = ASTs.decode(ast);
            List<Completion> allcompletions = allCompletions(version.getContent(), root);
            List<AST> selectedPath = selectedPath(root, version.getSelections().get(0));

            Terminal terminalToBeCompleted = (Terminal) selectedPath.get(0);
            String text = extract(version.getContent(),terminalToBeCompleted).toString();
            if (terminalToBeCompleted.getEndOffset() >= version.getSelections().get(0).getStartOffset() && terminalToBeCompleted.getStartOffset() <= version.getSelections().get(0).getStartOffset()) {
                int vStart = version.getSelections().get(0).getStartOffset();
                int tStart = terminalToBeCompleted.getStartOffset();
                text = text.substring(0, vStart - tStart);
            }
            String toBeCompleted = text;
            List<Completion> relevant =
                    allcompletions
                            .stream()
                            .filter(comp -> comp.getReplacement().startsWith(toBeCompleted))
                            .map(comp -> new Completion(
                                    comp.getDescription() + ": " + comp.getReplacement(),
                                    comp.getReplacement().substring(toBeCompleted.length()),
                                    version.getSelections().get(0).getStartOffset(),
                                    comp.getIcon()))
                            .collect(Collectors.toList());

            return productMessage(
                    version.getId(),
                    version.getSource(),
                    Products.COMPLETIONS,
                    Languages.JAVASCRIPT,
                    Completions.encode(relevant));
        }
        throw new IllegalArgumentException("Code completion needs selection");
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
            Object[] terminalChildren = node.getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .toArray();
            if (terminalChildren.length > 1) {
                Terminal structureIdent = (Terminal) terminalChildren[1];
                completions.add(new Completion(name, extract(content,structureIdent).toString(), icon));

            }
            node.getChildren().forEach(child -> child.accept(this));
        }

        @Override
        public void visit(Terminal token) {

        }

        private void structureDeclaration(NonTerminal node, String name, URL icon) {
            Terminal structureIdent = (Terminal) node
                    .getChildren()
                    .stream()
                    .filter(ast -> ast instanceof Terminal)
                    .reduce((previous, current) -> current).get();
            completions.add(new Completion(name, extract(content,structureIdent).toString(), icon));
            node.getChildren().forEach(child -> child.accept(this));
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
            node.getChildren()
                    .stream()
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
                return region1.getStartOffset() <= region2.getEndOffset() && region1.getStartOffset() >= region2.getStartOffset();
            } catch (Exception e) {
                return false;
            }
        }

    }


    private static String extract(String str, AST indent) {
    	return str.subSequence(indent.getStartOffset(), indent.getStartOffset()+indent.getLength()).toString();
    }
}
