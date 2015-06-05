package de.tudarmstadt.stg.monto.ecmascript.message;

import java.util.List;

public interface Message {
	public LongKey getVersionId();
	public Source getSource();
	public List<Dependency> getInvalid();
	public Language getLanguage();
}
