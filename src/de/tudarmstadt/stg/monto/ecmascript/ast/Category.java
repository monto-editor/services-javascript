package de.tudarmstadt.stg.monto.ecmascript.ast;

/**
 * A predefined set of categories of tokens. Derived from
 * <a href="http://vimdoc.sourceforge.net/htmldoc/syntax.html#group-name">vim</a>.
 */
public enum Category {
	
	COMMENT,           // any comment
	             
	CONSTANT,          // any constant
	STRING,            // a string constant: "this is a string"
	CHARACTER,         // a character constant: 'c', '\n'
	NUMBER,            // a number constant: 234, 0xff
	BOOLEAN,           // a boolean constant: TRUE, false
	FLOAT,             // a floating point constant: 2.3e10
	             
	IDENTIFIER,        // any variable name
	             
	STATEMENT,         // any statement
	CONDITIONAL,       // if, then, else, endif, switch, etc.
	REPEAT,            // for, do, while, continue, break, etc.
	LABEL,             // case, default, etc.
	OPERATOR,          // "sizeof", "+", "*", etc.
	KEYWORD,           // any other keyword
	EXCEPTION,         // try, catch, throw
	
	TYPE,              // int, long, char, etc.
	MODIFIER,          // public, private, static, etc.
	STRUCTURE,         // struct, union, enum, class etc.
	PARENTHESIS,       // () [] {}, <> etc.
	DELIMITER,         // , . ; etc.
	META,              // C preprocessor macros, Java Annotations, etc
	WHITESPACE,        // Non visible character
	UNKNOWN;           // Unknown token, can occur during text insertion
	
	public static Category fromString(String name) {
		return Enum.valueOf(Category.class, name);
	}
}
