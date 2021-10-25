package smithereen.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetDeserializerFunction<T>{
	T deserialize(ResultSet res) throws SQLException;
}
