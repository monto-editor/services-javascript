package monto.service.javascript;

import monto.service.types.ServiceId;

public final class JavaScriptServices {
  public static final ServiceId TOKENIZER = new ServiceId("javascriptTokenizer");
  public static final ServiceId PARSER = new ServiceId("javascriptParser");
  public static final ServiceId OUTLINER = new ServiceId("javascriptOutliner");
  public static final ServiceId TYPECHECKER = new ServiceId("javascriptTypechecker");
  public static final ServiceId CODE_COMPLETION = new ServiceId("javascriptCodeCompletion");
  public static final ServiceId ASPELL_SPELLCHECKER = new ServiceId("javascriptSspellSpellChecker");
  public static final ServiceId IDENTIFIER_FINDER = new ServiceId("javascriptIdentifierFinder");

  private JavaScriptServices() {}
}
