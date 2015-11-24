package monto.service.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTVisitor;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.completion.Completion;
import monto.service.completion.Completions;
import monto.service.message.IconType;
import monto.service.message.Language;
import monto.service.message.LongKey;
import monto.service.message.Message;
import monto.service.message.Messages;
import monto.service.message.ParseException;
import monto.service.message.Product;
import monto.service.message.ProductDependency;
import monto.service.message.ProductMessage;
import monto.service.message.Selection;
import monto.service.message.VersionMessage;
import monto.service.region.IRegion;

public class JavaScriptCodeCompletion extends MontoService {

    private static final Product AST = new Product("ast");
    private static final Product COMPLETIONS = new Product("completions");
    private static final Language JAVASCRIPT = new Language("javascript");

    public JavaScriptCodeCompletion(ZMQConfiguration zmqConfig) {
        super(zmqConfig, "javascriptCompletioner", "Code Completion", "A code completion service for JavaScript", COMPLETIONS, JAVASCRIPT, new String[]{"Source", "ast/javascript"});
    }

    private static List<Completion> allCompletions(String contents, AST root) {
        AllCompletions completionVisitor = new AllCompletions(contents);
        root.accept(completionVisitor);
        return completionVisitor.getCompletions();
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
            Stream<Completion> relevant =
                    allcompletions
                            .stream()
                            .filter(comp -> comp.getReplacement().startsWith(toBeCompleted))
                            .map(comp -> new Completion(
                                    comp.getDescription() + ": " + comp.getReplacement(),
                                    comp.getReplacement().substring(toBeCompleted.length()),
                                    version.getSelections().get(0).getStartOffset(),
                                    comp.getIcon()));

            return new ProductMessage(
                    version.getVersionId(),
                    new LongKey(1),
                    version.getSource(),
                    COMPLETIONS,
                    JAVASCRIPT,
                    Completions.encode(relevant),
                    new ProductDependency(ast));
        }
        throw new IllegalArgumentException("Code completion needs selection");
    }

    private static class AllCompletions implements ASTVisitor {

        private List<Completion> completions = new ArrayList<>();
        private String content;

        public AllCompletions(String content) {
            this.content = content;
        }

        @Override
        public void visit(NonTerminal node) {
            switch (node.getName()) {
                case "Class":
                    structureDeclaration(node, "class", IconType.NO_IMG);
                    break;

                case "Enum":
                    structureDeclaration(node, "enum", IconType.NO_IMG);
                    break;

                case "Const":
                    structureDeclaration(node, "const", IconType.NO_IMG);
                    break;

                case "variableDeclaration":
                    structureDeclaration(node, "var", IconType.NO_IMG);
                    break;

                case "functionDeclaration":
                    addFuncToConverted(node, "function", IconType.NO_IMG);

                default:
                    node.getChildren().forEach(child -> child.accept(this));
            }
        }

        private void addFuncToConverted(NonTerminal node, String name, String icon) {
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

        private void structureDeclaration(NonTerminal node, String name, String icon) {
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
