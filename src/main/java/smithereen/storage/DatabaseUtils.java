package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import smithereen.Utils;

public class DatabaseUtils{

	private static final Object UNIQUE_USERNAME_LOCK=new Object();

	public static ArrayList<Integer> intResultSetToList(ResultSet res) throws SQLException{
		res.beforeFirst();
		ArrayList<Integer> list=new ArrayList<>();
		while(res.next()){
			list.add(res.getInt(1));
		}
		res.close();
		return list;
	}

	public static int oneFieldToInt(final ResultSet res) throws SQLException{
		try(res){
			return res.first() ? res.getInt(1) : -1;
		}
	}

	public static boolean runWithUniqueUsername(String username, DatabaseRunnable action) throws SQLException{
		if(!Utils.isValidUsername(username))
			return false;
		if(Utils.isReservedUsername(username))
			return false;
		synchronized(UNIQUE_USERNAME_LOCK){
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM `users` WHERE username=? AND domain=''");
			stmt.setString(1, username);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				if(res.getInt(1)>0)
					return false;
			}
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `groups` WHERE username=? AND domain=''");
			stmt.setString(1, username);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				if(res.getInt(1)>0)
					return false;
			}
			action.run();
			return true;
		}
	}

	public static int insertAndGetID(PreparedStatement stmt) throws SQLException{
		stmt.execute();
		try(ResultSet keys=stmt.getGeneratedKeys()){
			keys.first();
			return keys.getInt(1);
		}
	}

	public static IntStream intResultSetToStream(ResultSet res) throws SQLException{
		try{
			res.beforeFirst();
			return StreamSupport.intStream(new Spliterators.AbstractIntSpliterator(Long.MAX_VALUE, Spliterator.ORDERED){
				@Override
				public boolean tryAdvance(IntConsumer action){
					try{
						if(res.next()){
							action.accept(res.getInt(1));
							return true;
						}
					}catch(SQLException x){
						throw new UncheckedSQLException(x);
					}
					return false;
				}

				@Override
				public void forEachRemaining(IntConsumer action){
					try{
						while(res.next()){
							action.accept(res.getInt(1));
						}
					}catch(SQLException x){
						throw new UncheckedSQLException(x);
					}
				}
			}, false);
		}catch(UncheckedSQLException x){
			throw x.getCause();
		}
	}

	public static <T> Stream<T> resultSetToObjectStream(ResultSet res, ResultSetDeserializerFunction<T> creator) throws SQLException{
		try{
			res.beforeFirst();
			return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED){
				@Override
				public boolean tryAdvance(Consumer<? super T> action){
					try{
						if(res.next()){
							action.accept(creator.deserialize(res));
							return true;
						}
					}catch(SQLException x){
						throw new UncheckedSQLException(x);
					}
					return false;
				}

				@Override
				public void forEachRemaining(Consumer<? super T> action){
					try{
						while(res.next()){
							action.accept(creator.deserialize(res));
						}
					}catch(SQLException x){
						throw new UncheckedSQLException(x);
					}
				}
			}, false);
		}catch(UncheckedSQLException x){
			throw x.getCause();
		}
	}

	private static class UncheckedSQLException extends RuntimeException{
		public UncheckedSQLException(SQLException cause){
			super(cause);
		}

		@Override
		public synchronized SQLException getCause(){
			return (SQLException) super.getCause();
		}
	}
}
