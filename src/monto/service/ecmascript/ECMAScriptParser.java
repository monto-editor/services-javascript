package monto.service.ecmascript;

import monto.service.MontoService;
import monto.service.ast.AST;
import monto.service.ast.ASTs;
import monto.service.ast.NonTerminal;
import monto.service.ast.Terminal;
import monto.service.ecmascript.antlr.ECMAScriptLexer;
import monto.service.message.*;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.zeromq.ZContext;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class ECMAScriptParser extends MontoService {

    private static final Product AST = new Product("ast");
    private static final Language JAVASCRIPT = new Language("javascript");

    private ECMAScriptLexer lexer = new ECMAScriptLexer(new ANTLRInputStream());
    private CommonTokenStream tokens = new CommonTokenStream(lexer);
    private monto.service.ecmascript.antlr.ECMAScriptParser parser = new monto.service.ecmascript.antlr.ECMAScriptParser(tokens);

    public ECMAScriptParser(ZContext context, String address, String registrationAddress, String serviceID) {
        super(context, address, registrationAddress, serviceID, "ANTLR JavaScript Parser", "A parser that produces an AST for JavaScript using ANTLR", AST, JAVASCRIPT, new String[]{"Source"});
    }

    @Override
    public ProductMessage onMessage(List<Message> messages) throws IOException {
        VersionMessage version = Messages.getVersionMessage(messages);
        lexer.setInputStream(new ANTLRInputStream(version.getContent().getReader()));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        parser.setTokenStream(tokens);
        ParserRuleContext root = parser.program();
        ParseTreeWalker walker = new ParseTreeWalker();

        Converter converter = new Converter();
        walker.walk(converter, root);

        Contents content = ASTs.encode(converter.getRoot());

        return new ProductMessage(
                version.getVersionId(),
                new LongKey(1),
                version.getSource(),
                AST,
                JAVASCRIPT,
                content);
    }

    private static class Converter implements ParseTreeListener {

        private Deque<AST> nodes = new ArrayDeque<>();

        @Override
        public void enterEveryRule(ParserRuleContext context) {
            if (context.getChildCount() > 0) {
                String name = monto.service.ecmascript.antlr.ECMAScriptParser.ruleNames[context.getRuleIndex()];
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
