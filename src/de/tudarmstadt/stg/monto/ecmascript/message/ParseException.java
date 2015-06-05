package de.tudarmstadt.stg.monto.ecmascript.message;

public class ParseException extends Exception {

	public ParseException(Exception e) {
		super(e);
	}

	public ParseException(String reason) {
		super(reason);
	}

	private static final long serialVersionUID = -8652632901411933961L;

}
