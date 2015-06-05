package de.tudarmstadt.stg.monto.ecmascript.message;

import de.tudarmstadt.stg.monto.ecmascript.region.IRegion;

import java.io.InputStream;
import java.io.Reader;

public interface Contents {
	public InputStream getBytes();
	public Reader getReader();
	public Contents extract(int offset, int length);
	public default Contents extract(IRegion region) {
		return extract(region.getStartOffset(),region.getLength());
	}
}
