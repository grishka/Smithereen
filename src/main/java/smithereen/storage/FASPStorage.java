package smithereen.storage;

import java.net.InetAddress;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import smithereen.Utils;
import smithereen.model.PaginatedList;
import smithereen.model.fasp.FASPDebugCallback;
import smithereen.model.fasp.FASPProvider;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class FASPStorage{
	public static long createOrUpdateFaspProvider(String name, URI baseUrl, String remoteID, byte[] publicKey, byte[] privateKey) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			long existing=new SQLQueryBuilder(conn)
					.selectFrom("fasp_providers")
					.columns("id")
					.where("base_url=?", baseUrl.toString())
					.executeAndGetLong();
			if(existing!=-1){
				new SQLQueryBuilder(conn)
						.update("fasp_providers")
						.where("id=?", existing)
						.value("name", name)
						.value("public_key", publicKey)
						.value("private_key", privateKey)
						.value("remote_id", remoteID)
						.executeNoResult();
				return existing;
			}
			return new SQLQueryBuilder(conn)
					.insertInto("fasp_providers")
					.value("name", name)
					.value("base_url", baseUrl.toString())
					.value("remote_id", remoteID)
					.value("public_key", publicKey)
					.value("private_key", privateKey)
					.value("capabilities", "{}")
					.value("enabled_capabilities", "{}")
					.executeAndGetIDLong();
		}
	}

	public static FASPProvider getFaspProvider(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("fasp_providers")
				.where("id=?", id)
				.executeAndGetSingleObject(FASPProvider::fromResultSet);
	}

	public static List<FASPProvider> getAllFaspProviders(boolean confirmed) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("fasp_providers")
				.where("confirmed=?", confirmed)
				.executeAsStream(FASPProvider::fromResultSet)
				.toList();
	}

	public static int getUnconfirmedFaspProviderCount() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("fasp_providers")
				.count()
				.where("confirmed=0")
				.executeAndGetInt();
	}

	public static void updateFaspProvider(long id, String name, String signInUrl, String capabilities, String privacyPolicy, String contactEmail, int actorID) throws SQLException{
		new SQLQueryBuilder()
				.update("fasp_providers")
				.where("id=?", id)
				.value("name", name)
				.value("sign_in_url", signInUrl)
				.value("capabilities", capabilities)
				.value("privacy_policy", privacyPolicy)
				.value("contact_email", contactEmail)
				.value("actor_id", actorID!=0 ? actorID : null)
				.executeNoResult();
	}

	public static void deleteFaspProvider(long id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("fasp_providers")
				.where("id=?", id)
				.executeNoResult();
	}

	public static void setFaspProviderConfirmed(long id) throws SQLException{
		new SQLQueryBuilder()
				.update("fasp_providers")
				.value("confirmed", true)
				.where("id=?", id)
				.executeNoResult();
	}

	public static void updateFaspProviderEnabledCapabilities(long id, Map<String, String> capabilities) throws SQLException{
		new SQLQueryBuilder()
				.update("fasp_providers")
				.value("enabled_capabilities", Utils.gson.toJson(capabilities))
				.where("id=?", id)
				.executeNoResult();
	}

	public static PaginatedList<FASPDebugCallback> getFaspDebugCallbacks(long providerID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("fasp_debug_callbacks")
					.count()
					.where("provider_id=?", providerID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<FASPDebugCallback> callbacks=new SQLQueryBuilder(conn)
					.selectFrom("fasp_debug_callbacks")
					.allColumns()
					.where("provider_id=?", providerID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(FASPDebugCallback::fromResultSet)
					.toList();
			return new PaginatedList<>(callbacks, total, offset, count);
		}
	}

	public static void putDebugCallback(long providerID, InetAddress ip, String body) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("fasp_debug_callbacks")
				.value("provider_id", providerID)
				.value("ip", Utils.serializeInetAddress(ip))
				.value("body", body)
				.executeNoResult();
	}
}
