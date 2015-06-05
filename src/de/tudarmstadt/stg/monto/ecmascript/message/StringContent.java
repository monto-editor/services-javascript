package de.tudarmstadt.stg.monto.ecmascript.message;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

public class StringContent implements Contents {
	
	private String content;

	public StringContent(String content) {
		this.content = content;
	}
	
	public StringContent(CharSequence seq) {
		this.content = seq.toString();
	}

	@Override
	public InputStream getBytes() {
		return new ByteArrayInputStream(content.getBytes());
	}
	
	@Override
	public String toString() {
		return content;
	}

	@Override
	public Reader getReader() {
		return new StringReader(toString());
	}

	@Override
	public Contents extract(int offset, int length) {
		return new StringContent(content.subSequence(offset, offset+length));
	}
	
	
}
