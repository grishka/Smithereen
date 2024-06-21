package smithereen.storage;

import java.net.URI;
import java.sql.SQLException;

import smithereen.controllers.ObjectLinkResolver;
import smithereen.storage.sql.SQLQueryBuilder;

public class FederationStorage{
	public static ObjectLinkResolver.ObjectTypeAndID getObjectTypeAndID(URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("ap_id_index")
				.columns("object_type", "object_id")
				.where("ap_id=?", apID.toString())
				.executeAndGetSingleObject(ObjectLinkResolver.ObjectTypeAndID::fromResultSet);
	}
}
