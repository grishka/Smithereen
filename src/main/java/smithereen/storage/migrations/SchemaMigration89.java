package smithereen.storage.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import smithereen.Utils;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration89 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE wall_posts ADD immediate_reply_count INTEGER UNSIGNED NOT NULL DEFAULT 0 AFTER reply_count");
		int offset=0;
		while(true){
			LOG.info("Counting wall comments: offset {}", offset);
			try(ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.columns("id", "reply_key")
					.where("reply_count>0")
					.orderBy("id ASC")
					.limit(10_000, offset)
					.execute()){
				if(!res.next())
					break;
				do{
					int id=res.getInt("id");
					ArrayList<Integer> replyKey=new ArrayList<>(Utils.deserializeIntList(res.getBytes("reply_key")));
					replyKey.add(id);

					int count=new SQLQueryBuilder(conn)
							.selectFrom("wall_posts")
							.count()
							.where("reply_key=?", (Object) Utils.serializeIntList(replyKey))
							.executeAndGetInt();

					if(count>0){
						new SQLQueryBuilder(conn)
								.update("wall_posts")
								.where("id=?", id)
								.value("immediate_reply_count", count)
								.executeNoResult();
					}
					offset++;
				}while(res.next());
			}
		}

		conn.createStatement().execute("ALTER TABLE comments ADD immediate_reply_count INTEGER UNSIGNED NOT NULL DEFAULT 0 AFTER reply_count");
		offset=0;
		while(true){
			LOG.info("Counting comments: offset {}", offset);
			try(ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.columns("id", "reply_key")
					.where("reply_count>0")
					.orderBy("id ASC")
					.limit(10_000, offset)
					.execute()){
				if(!res.next())
					break;
				do{
					long id=res.getLong("id");
					ArrayList<Long> replyKey=new ArrayList<>();
					Utils.deserializeLongCollection(res.getBytes("reply_key"), replyKey);
					replyKey.add(id);

					int count=new SQLQueryBuilder(conn)
							.selectFrom("comments")
							.count()
							.where("reply_key=?", (Object) Utils.serializeLongCollection(replyKey))
							.executeAndGetInt();

					if(count>0){
						new SQLQueryBuilder(conn)
								.update("comments")
								.where("id=?", id)
								.value("immediate_reply_count", count)
								.executeNoResult();
					}
					offset++;
				}while(res.next());
			}
		}
	}
}
