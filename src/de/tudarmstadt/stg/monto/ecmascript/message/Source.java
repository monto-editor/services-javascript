package de.tudarmstadt.stg.monto.ecmascript.message;


public class Source {

	private String source;

	public Source(String source) {
		this.source = source;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.hashCode() == this.hashCode() && obj instanceof Source) {
			Source other = (Source) obj;
			return this.source.equals(other.source);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return source.hashCode();
	}
	
	@Override
	public String toString() {
		return source;
	}
}
