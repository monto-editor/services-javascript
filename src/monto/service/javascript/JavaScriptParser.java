package monto.service.javascript;

import monto.service.MontoService;
import monto.service.ZMQConfiguration;
import monto.service.ast.AST;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.javascript.antlr.ECMAScriptLexer;
import monto.service.product.ProductMessage;
import monto.service.product.Products;
import monto.service.registration.SourceDependency;
import monto.service.request.Request;
import monto.service.source.SourceMessage;
import monto.service.types.Languages;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JavaScriptParser extends MontoService {

    private ECMAScriptLexer lexer = new ECMAScriptLexer(new ANTLRInputStream());
    private CommonTokenStream tokens = new CommonTokenStream(lexer);
    private monto.service.javascript.antlr.ECMAScriptParser parser = new monto.service.javascript.antlr.ECMAScriptParser(tokens);

    public JavaScriptParser(ZMQConfiguration zmqConfig) {
        super(zmqConfig,
                JavaScriptServices.JAVASCRIPT_PARSER,
                "Parser",
                "A parser that produces an AST for JavaScript using ANTLR",
                Languages.JAVASCRIPT,
                Products.AST,
                options(),
                dependencies(
                        new SourceDependency(Languages.JAVASCRIPT)
                ));
    }

    @Override
    public ProductMessage onRequest(Request request) throws IOException {
        SourceMessage version = request.getSourceMessage()
                .orElseThrow(() -> new IllegalArgumentException("No version message in request"));
        lexer.setInputStream(new ANTLRInputStream(version.getContents()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        parser.setTokenStream(tokens);
        ParserRuleContext root = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();

        Converter converter = new Converter();
        walker.walk(converter, root);

        return productMessage(
                version.getId(),
                version.getSource(),
                Products.AST,
                Languages.JAVASCRIPT,
                ASTs.encode(converter.getRoot()));
    }

    private static class Converter implements ParseTreeListener {

        private Deque<AST> nodes = new ArrayDeque<>();

        @Override
        public void enterEveryRule(ParserRuleContext context) {
            if (context.getChildCount() > 0) {
                String name = monto.service.javascript.antlr.ECMAScriptParser.ruleNames[context.getRuleIndex()];
                List<AST> childs = new ArrayList<>(context.getChildCount());
                NonTerminal node = new NonTerminal(name, childs);
                addChild(node);
                nodes.push(node);
            }
        }

        @Override
        public void exitEveryRule(ParserRuleContext node) {
            // Keep the last node to return
            if (nodes.size() > 1)
                nodes.pop();
        }

        @Override
        public void visitErrorNode(ErrorNode err) {
            org.antlr.v4.runtime.Token symbol = err.getSymbol();
            addChild(new NonTerminal("error", new Terminal(symbol.getStartIndex(), symbol.getStopIndex() - symbol.getStartIndex() + 1)));
        }

        @Override
        public void visitTerminal(TerminalNode terminal) {
            org.antlr.v4.runtime.Token symbol = terminal.getSymbol();
            Terminal token = new Terminal(symbol.getStartIndex(), symbol.getStopIndex() - symbol.getStartIndex() + 1);
            if (nodes.size() == 0)
                nodes.push(token);
            else
                addChild(token);
        }

        private void addChild(AST node) {
            if (!nodes.isEmpty() && nodes.peek() instanceof NonTerminal)
                ((NonTerminal) nodes.peek()).addChild(node);
        }

        public AST getRoot() {
            return nodes.peek();
        }
    }
}
