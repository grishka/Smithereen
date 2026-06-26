package smithereen.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.EnumSet;

import smithereen.Config;
import smithereen.Utils;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.admin.UserRole;
import smithereen.storage.migrations.DatabaseSchemaMigration;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=92;
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

	public static void maybeUpdate() throws SQLException{
		if(Config.dbSchemaVersion==0){
			Config.updateInDatabase("SchemaVersion", SCHEMA_VERSION+"");
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
				conn.createStatement().execute("""
						CREATE FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048) DETERMINISTIC
						RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, '\\\\', '\\\\\\\\'), '%', '\\\\%'), '_', '\\\\_'), '%');""");
				createMediaRefCountTriggers(conn);
				createApIdIndexTriggers(conn);
				createApIdIndexTriggersForPhotos(conn);
				createApIdIndexTriggersForComments(conn);
				createApIdIndexTriggersForBoardTopics(conn);
				createApIdIndexTriggersForApps(conn);
				insertDefaultRoles(conn);
			}
		}else{
			for(int i=Config.dbSchemaVersion+1;i<=SCHEMA_VERSION;i++){
				try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
					conn.createStatement().execute("START TRANSACTION");
					try{
						DatabaseSchemaMigration dsm=DatabaseSchemaMigration.get(i);
						LOG.info("Running database migration {}", dsm.getClass().getSimpleName());
						dsm.doMigration(conn);
						Config.updateInDatabase("SchemaVersion", String.valueOf(i));
						Config.dbSchemaVersion=i;
					}catch(Exception x){
						conn.createStatement().execute("ROLLBACK");
						throw new RuntimeException(x);
					}
					conn.createStatement().execute("COMMIT");
				}
			}
		}
	}

	public static void insertDefaultRoles(DatabaseConnection conn) throws SQLException{
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Owner")
				.value("permissions", Utils.serializeEnumSetToBytes(EnumSet.of(UserRole.Permission.SUPERUSER, UserRole.Permission.VISIBLE_IN_STAFF)))
				.executeNoResult();

		EnumSet<UserRole.Permission> adminPermissions=EnumSet.allOf(UserRole.Permission.class);
		adminPermissions.remove(UserRole.Permission.SUPERUSER);
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Admin")
				.value("permissions", Utils.serializeEnumSetToBytes(adminPermissions))
				.executeNoResult();

		EnumSet<UserRole.Permission> moderatorPermissions=EnumSet.of(
				UserRole.Permission.MANAGE_USERS,
				UserRole.Permission.MANAGE_REPORTS,
				UserRole.Permission.VIEW_SERVER_AUDIT_LOG,
				UserRole.Permission.MANAGE_GROUPS
		);
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Moderator")
				.value("permissions", Utils.serializeEnumSetToBytes(moderatorPermissions))
				.executeNoResult();
		Config.reloadRoles();
	}

	public static void createApIdIndexTriggers(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_users_to_ap_ids AFTER INSERT ON `users` FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.USER.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_groups_to_ap_ids AFTER INSERT ON `groups` FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.GROUP.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_posts_to_ap_ids AFTER INSERT ON wall_posts FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.POST.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_messages_to_ap_ids AFTER INSERT ON mail_messages FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.MESSAGE.id).execute();

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_users_from_ap_ids AFTER DELETE ON `users` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_groups_from_ap_ids AFTER DELETE ON `groups` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_posts_from_ap_ids AFTER DELETE ON wall_posts FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_user_posts_from_ap_ids BEFORE DELETE ON `users` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM wall_posts WHERE owner_user_id=OLD.id AND ap_id IS NOT NULL); END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_group_posts_from_ap_ids BEFORE DELETE ON `groups` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM wall_posts WHERE owner_group_id=OLD.id AND ap_id IS NOT NULL); END IF; END;");
	}

	public static void createApIdIndexTriggersForPhotos(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_photo_albums_to_ap_ids AFTER INSERT ON photo_albums FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.PHOTO_ALBUM.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_photos_to_ap_ids AFTER INSERT ON photos FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.PHOTO.id).execute();

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_photo_albums_from_ap_ids BEFORE DELETE ON photo_albums FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF;" +
				"DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM photos WHERE album_id=OLD.id AND ap_id IS NOT NULL);" +
				"END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_photos_from_ap_ids AFTER DELETE ON photos FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	public static void createApIdIndexTriggersForComments(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_comments_to_ap_ids AFTER INSERT ON comments FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.COMMENT.id).execute();
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_comments_from_ap_ids AFTER DELETE ON comments FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	public static void createApIdIndexTriggersForBoardTopics(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_topics_to_ap_ids AFTER INSERT ON board_topics FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.BOARD_TOPIC.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_updated_foreign_topics_to_ap_ids AFTER UPDATE ON board_topics FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL AND OLD.ap_id IS NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.BOARD_TOPIC.id).execute();
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_topics_from_ap_ids AFTER DELETE ON board_topics FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	public static void createApIdIndexTriggersForApps(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_apps_to_ap_ids AFTER INSERT ON api_applications FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.API_APPLICATION.id).execute();
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_apps_from_ap_ids AFTER DELETE ON api_applications FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	public static void createMediaRefCountTriggers(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("CREATE TRIGGER inc_count_on_insert AFTER INSERT ON media_file_refs FOR EACH ROW UPDATE media_files SET ref_count=ref_count+1 WHERE id=NEW.file_id");
		conn.createStatement().execute("CREATE TRIGGER dec_count_on_delete AFTER DELETE ON media_file_refs FOR EACH ROW UPDATE media_files SET ref_count=ref_count-1 WHERE id=OLD.file_id");
	}
}
