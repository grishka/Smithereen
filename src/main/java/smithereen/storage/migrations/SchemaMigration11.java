package smithereen.storage.migrations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;
import smithereen.text.TextProcessor;

class SchemaMigration11 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `qsearch_index` (
				  `string` text NOT NULL,
				  `user_id` int(10) unsigned DEFAULT NULL,
				  `group_id` int(10) unsigned DEFAULT NULL,
				  KEY `user_id` (`user_id`),
				  KEY `group_id` (`group_id`),
				  FULLTEXT KEY `string` (`string`),
				  CONSTRAINT `qsearch_index_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `qsearch_index_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=ascii;""");
		try(ResultSet res=conn.createStatement().executeQuery("SELECT id, fname, lname, middle_name, maiden_name, username, domain FROM users")){
			PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, user_id) VALUES (?, ?)");
			while(res.next()){
				int id=res.getInt("id");
				String fname=res.getString("fname");
				String lname=res.getString("lname");
				String mname=res.getString("middle_name");
				String mdname=res.getString("maiden_name");
				String uname=res.getString("username");
				String domain=res.getString("domain");
				StringBuilder sb=new StringBuilder(TextProcessor.transliterate(fname));
				if(lname!=null){
					sb.append(' ');
					sb.append(TextProcessor.transliterate(lname));
				}
				if(mname!=null){
					sb.append(' ');
					sb.append(TextProcessor.transliterate(mname));
				}
				if(mdname!=null){
					sb.append(' ');
					sb.append(TextProcessor.transliterate(mdname));
				}
				sb.append(' ');
				sb.append(uname);
				if(domain!=null){
					sb.append(' ');
					sb.append(domain);
				}
				stmt.setString(1, sb.toString());
				stmt.setInt(2, id);
				stmt.execute();
			}
		}
		try(ResultSet res=conn.createStatement().executeQuery("SELECT id, name, username, domain FROM groups")){
			PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, group_id) VALUES (?, ?)");
			while(res.next()){
				String s=TextProcessor.transliterate(res.getString("name"))+" "+res.getString("username");
				String domain=res.getString("domain");
				if(domain!=null)
					s+=" "+domain;
				stmt.setString(1, s);
				stmt.setInt(2, res.getInt("id"));
				stmt.execute();
			}
		}
	}
}
