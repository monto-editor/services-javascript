package monto.service.javascript;

import monto.service.types.ServiceId;

public final class JavaScriptServices {
  public static final ServiceId TOKENIZER = new ServiceId("javaScriptTokenizer");
  public static final ServiceId PARSER = new ServiceId("javaScriptParser");
  public static final ServiceId OUTLINER = new ServiceId("javaScriptOutliner");
  public static final ServiceId TYPECHECKER = new ServiceId("javaScriptTypechecker");
  public static final ServiceId CODE_COMPLETIONER = new ServiceId("javaScriptCodeCompletioner");
  public static final ServiceId ASPELL_SPELLCHECKER = new ServiceId("javaScriptSpellSpellChecker");
  public static final ServiceId IDENTIFIER_FINDER = new ServiceId("javaScriptIdentifierFinder");

  private JavaScriptServices() {}
}
