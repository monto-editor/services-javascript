package monto.service.javascript;

import monto.service.ZMQConfiguration;
import monto.service.completion.CodeCompletioner;
import monto.service.types.Languages;

public class JavaScriptCodeCompletioner extends CodeCompletioner {
  public JavaScriptCodeCompletioner(ZMQConfiguration zmqConfig) {
    super(
        zmqConfig,
        JavaScriptServices.CODE_COMPLETIONER,
        "Code Completion",
        "A code completion service for Python",
        Languages.PYTHON,
        JavaScriptServices.IDENTIFIER_FINDER);
  }
}
