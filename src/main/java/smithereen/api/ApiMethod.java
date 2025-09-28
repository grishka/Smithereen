package smithereen.api;

import smithereen.ApplicationContext;

@FunctionalInterface
public interface ApiMethod{
	Object call(ApplicationContext ctx, ApiCallContext actx);
}
