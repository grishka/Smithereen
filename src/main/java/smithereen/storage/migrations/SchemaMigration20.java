package smithereen.storage.migrations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration20 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		// Make room for a new column
		conn.createStatement().execute("ALTER TABLE `users` DROP KEY `ap_outbox`");
		conn.createStatement().execute("ALTER TABLE `users` CHANGE `ap_outbox` `ap_outbox` TEXT");
		conn.createStatement().execute("ALTER TABLE `groups` CHANGE `ap_outbox` `ap_outbox` TEXT");
		// Then add the new column
		conn.createStatement().execute("ALTER TABLE `users` ADD `endpoints` json DEFAULT NULL");
		conn.createStatement().execute("ALTER TABLE `groups` ADD `endpoints` json DEFAULT NULL");
		PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `endpoints`=? WHERE `id`=?");
		try(ResultSet res=conn.createStatement().executeQuery("SELECT `id`,`ap_outbox`,`ap_followers`,`ap_following`,`ap_wall`,`ap_friends`,`ap_groups` FROM `users` WHERE `ap_id` IS NOT NULL")){
			while(res.next()){
				int id=res.getInt(1);
				Actor.EndpointsStorageWrapper ep=new Actor.EndpointsStorageWrapper();
				ep.outbox=res.getString(2);
				ep.followers=res.getString(3);
				ep.following=res.getString(4);
				ep.wall=res.getString(5);
				ep.friends=res.getString(6);
				ep.groups=res.getString(7);
				stmt.setString(1, Utils.gson.toJson(ep));
				stmt.setInt(2, id);
				stmt.execute();
			}
		}
		stmt=conn.prepareStatement("UPDATE `groups` SET `endpoints`=? WHERE `id`=?");
		try(ResultSet res=conn.createStatement().executeQuery("SELECT `id`,`ap_outbox`,`ap_followers`,`ap_wall` FROM `groups` WHERE `ap_id` IS NOT NULL")){
			while(res.next()){
				int id=res.getInt(1);
				Actor.EndpointsStorageWrapper ep=new Actor.EndpointsStorageWrapper();
				ep.outbox=res.getString(2);
				ep.followers=res.getString(3);
				ep.wall=res.getString(4);
				stmt.setString(1, Utils.gson.toJson(ep));
				stmt.setInt(2, id);
				stmt.execute();
			}
		}
		conn.createStatement().execute("ALTER TABLE `users` DROP `ap_outbox`, DROP `ap_followers`, DROP `ap_following`, DROP `ap_wall`, DROP `ap_friends`, DROP `ap_groups`");
		conn.createStatement().execute("ALTER TABLE `groups` DROP `ap_outbox`, DROP `ap_followers`, DROP `ap_wall`");
		conn.createStatement().execute("ALTER TABLE `groups` ADD `access_type` tinyint NOT NULL DEFAULT '0'");
	}
}
