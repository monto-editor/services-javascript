package de.tudarmstadt.stg.monto.service.message;

import java.util.function.Function;

public interface Dependency {
	public <A> A match(Function<VersionDependency, A> v, Function<ProductDependency, A> p);
}
