package de.tudarmstadt.stg.monto.service.message;

import java.util.function.Function;

public class ProductDependency implements Dependency {

	private LongKey versionId;
	private LongKey productId;
	private Source source;
	private Language language;
	private Product product;

	public ProductDependency(LongKey versionId, LongKey productId, Source source, Language language, Product product) {
		this.versionId = versionId;
		this.productId = productId;
		this.source    = source;
		this.language  = language;
		this.product   = product;
	}
	
	public ProductDependency(ProductMessage message) {
		this(message.getVersionId(),message.getProductId(),message.getSource(),message.getLanguage(),message.getProduct());
	}

	@Override
	public <A> A match(Function<VersionDependency, A> v, Function<ProductDependency, A> p) {
		return p.apply(this);
	}

	@Override
	public int hashCode() {
		return (versionId.toString() + productId.toString() + source.toString() + language.toString() + product.toString()).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.hashCode() == this.hashCode() && obj instanceof ProductDependency) {
			ProductDependency other = (ProductDependency) obj;
			return this.versionId == other.versionId
					&& this.productId == other.productId
					&& this.source == other.source
					&& this.language == other.language
					&& this.product == other.product;
		} else {
			return false;
		}
	}
	
	@Override
	public String toString() {
		return String.format("Product (%s,%s,%s,%s,%s)",versionId,productId,source,language,product);
	}

	public LongKey getVersionId() {
		return versionId;
	}

	public LongKey getProductId() {
		return productId;
	}

	public Source getSource() {
		return source;
	}

	public Language getLanguage() {
		return language;
	}

	public Product getProduct() {
		return product;
	}
}
