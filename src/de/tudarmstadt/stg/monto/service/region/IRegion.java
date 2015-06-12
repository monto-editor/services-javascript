package de.tudarmstadt.stg.monto.service.region;

public interface IRegion {
	int getStartOffset();
	
	default int getLength() {
		return getEndOffset() - getStartOffset();
	}

	default int getEndOffset() {
		return getStartOffset() + getLength();
	}
	
	
	/**
	 * a.inRange(b) tests if a is in range of b.
	 */
	default boolean inRange(IRegion whole) {
		try {
			return this.getStartOffset() >= whole.getStartOffset()
			    && this.getEndOffset() <= whole.getEndOffset();
		} catch(Exception e) {
			return false;
		}

	}
	
	/**
	 * a.encloses(b) tests if b is in range of a.
	 */
	default boolean encloses(IRegion part) {
		return part.inRange(this);
	}
}
