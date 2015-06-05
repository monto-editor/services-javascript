package de.tudarmstadt.stg.monto.ecmascript.message;

import java.util.function.Function;

public class VersionDependency implements Dependency {

	private LongKey versionId;
	private Source source;
	private Language language;

	public VersionDependency(LongKey versionId, Source source, Language language) {
		this.versionId = versionId;
		this.source = source;
		this.language = language;
	}
	
	public VersionDependency(VersionMessage message) {
		this(message.getVersionId(),message.getSource(),message.getLanguage());
	}

	@Override
	public <A> A match(Function<VersionDependency, A> v, Function<ProductDependency, A> p) {
		return v.apply(this);
	}
	
	@Override
	public int hashCode() {
		return (versionId.toString() + source.toString() + language.toString()).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.hashCode() == this.hashCode() && obj instanceof VersionDependency) {
			VersionDependency other = (VersionDependency) obj;
			return this.versionId.equals(other.versionId) && this.source.equals(other.source) && this.language.equals(other.language);
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return String.format("Version (%s,%s,%s)",versionId,source,language);
	}

	public LongKey getVersionId() {
		return versionId;
	}

	public Source getSource() {
		return source;
	}

	public Language getLanguage() {
		return language;
	}
}
