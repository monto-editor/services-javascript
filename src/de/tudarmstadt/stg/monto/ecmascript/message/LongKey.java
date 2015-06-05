package de.tudarmstadt.stg.monto.ecmascript.message;

public class LongKey implements Key {

	private long key;
	
	public LongKey(long key) {
		this.key = key;
	}
	
	@Override
	public String toString() {
		return String.format("%d", key);
	}

	public synchronized LongKey freshId() {
		return new LongKey(key+1);
	}

	public boolean newerThan(LongKey other) {
		return other.key < this.key;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.hashCode() == this.hashCode() && obj instanceof LongKey) {
			LongKey other = (LongKey) obj;
			return other.key == this.key;
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return new Long(key).hashCode();
	}

	public long longValue() {
		return key;
	}
}
