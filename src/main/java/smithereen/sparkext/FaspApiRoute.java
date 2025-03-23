package smithereen.sparkext;

import smithereen.ApplicationContext;
import smithereen.model.fasp.FASPProvider;
import spark.Request;
import spark.Response;

public interface FaspApiRoute{
	Object handle(Request req, Response resp, ApplicationContext ctx, FASPProvider provider);
}
