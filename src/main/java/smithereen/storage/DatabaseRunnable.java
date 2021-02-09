package smithereen.storage;

import java.sql.SQLException;

@FunctionalInterface
public interface DatabaseRunnable{
	void run() throws SQLException;
}
