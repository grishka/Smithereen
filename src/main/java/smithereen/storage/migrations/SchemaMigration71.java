package smithereen.storage.migrations;

import java.sql.SQLException;
import java.util.List;

import smithereen.Utils;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration71 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE wall_posts CHANGE flags flags bit(64) NOT NULL DEFAULT b'0', ADD top_parent_is_wall_to_wall BOOL AS ((flags & 2)=2), ADD KEY top_parent_is_wall_to_wall (top_parent_is_wall_to_wall)");
		List<Integer> wallPostsWithComments=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.columns("id")
				.where("author_id<>owner_user_id AND reply_count>0 AND reply_key IS NULL")
				.executeAndGetIntList();
		for(int postID: wallPostsWithComments){
			new SQLQueryBuilder(conn)
					.update("wall_posts")
					.where("reply_key LIKE BINARY bin_prefix(?)", (Object) Utils.serializeIntArray(new int[]{postID}))
					.valueExpr("flags", "flags | 2")
					.executeNoResult();
		}
	}
}
