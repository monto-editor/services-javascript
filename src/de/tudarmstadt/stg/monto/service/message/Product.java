package de.tudarmstadt.stg.monto.service.message;

public class Product implements Comparable<Product> {
	public static final Product TOKENS = new Product("tokens");
	public static final Product AST = new Product("ast");
	public static final Product OUTLINE = new Product("outline");
	public static final Product COMPLETIONS = new Product("completions");

	private String name;

	public Product(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj != null && obj.hashCode() == this.hashCode() && obj instanceof Product) {
			Product other = (Product) obj;
			return this.name.equals(other.name);
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int compareTo(Product other) {
		return this.name.compareTo(other.name);
	}
}
