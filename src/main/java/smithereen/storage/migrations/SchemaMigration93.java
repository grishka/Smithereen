package smithereen.storage.migrations;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.activitypub.objects.Actor;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.UriBuilder;

public class SchemaMigration93 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		{
			ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("users")
					.columns("id", "ap_id", "public_key")
					.executeUpdatable();
			try(res){
				while(res.next()){
					String apID=res.getString("ap_id");
					byte[] key=res.getBytes("public_key");
					URI keyApID=apID==null ? URI.create("#") : URI.create(apID+"#main-key");
					res.updateBytes("public_key", serializeKey(key, keyApID));
					res.updateRow();
				}
			}
		}

		{
			ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("groups")
					.columns("id", "ap_id", "public_key")
					.executeUpdatable();
			try(res){
				while(res.next()){
					String apID=res.getString("ap_id");
					byte[] key=res.getBytes("public_key");
					URI keyApID=apID==null ? URI.create("#") : URI.create(apID+"#main-key");
					res.updateBytes("public_key", serializeKey(key, keyApID));
					res.updateRow();
				}
			}
		}

		{
			ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("api_applications")
					.columns("id", "ap_id", "public_key")
					.where("public_key IS NOT NULL")
					.executeUpdatable();
			try(res){
				while(res.next()){
					String apID=res.getString("ap_id");
					byte[] key=res.getBytes("public_key");
					URI keyApID=apID==null ? URI.create("#") : URI.create(apID+"#main-key");
					res.updateBytes("public_key", serializeKey(key, keyApID));
					res.updateRow();
				}
			}
		}
	}

	private static byte[] serializeKey(byte[] origKey, URI id){
		try{
			ByteArrayOutputStream buf=new ByteArrayOutputStream();
			DataOutputStream out=new DataOutputStream(buf);
			out.write(Actor.SigningKey.Algorithm.RSA.ordinal());
			out.writeUTF(id.toString());
			out.writeShort(origKey.length);
			out.write(origKey);
			return buf.toByteArray();
		}catch(IOException x){
			throw new RuntimeException(x);
		}
	}
}
