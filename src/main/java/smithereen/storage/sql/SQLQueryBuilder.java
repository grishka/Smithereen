package smithereen.storage.sql;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import smithereen.storage.DatabaseUtils;
import smithereen.storage.ResultSetDeserializerFunction;
import spark.utils.StringUtils;

public class SQLQueryBuilder{
	private static final Logger LOG=LoggerFactory.getLogger(SQLQueryBuilder.class);
	private final DatabaseConnection conn;
	private final boolean needCloseConnection;

	private Action action;
	private String table;
	private String[] selectColumns;
	private String selectExpr;
	private boolean selectEverything=true;
	private boolean selectCount;
	private boolean selectDistinct;
	private List<Value> values;
	private String condition;
	private List<Object> conditionArgs;
	private int limit, offset;
	private boolean hasLimit;
	private String orderBy;
	private String groupBy;

	public SQLQueryBuilder() throws SQLException{
		conn=DatabaseConnectionManager.getConnection();
		needCloseConnection=true;
	}

	public SQLQueryBuilder(DatabaseConnection conn){
		this.conn=conn;
		needCloseConnection=false;
	}

	public SQLQueryBuilder insertInto(String table){
		action=Action.INSERT;
		this.table=table;
		return this;
	}

	public SQLQueryBuilder insertIgnoreInto(String table){
		action=Action.INSERT_IGNORE;
		this.table=table;
		return this;
	}

	public SQLQueryBuilder selectFrom(String table){
		action=Action.SELECT;
		this.table=table;
		return this;
	}

	public SQLQueryBuilder update(String table){
		action=Action.UPDATE;
		this.table=table;
		return this;
	}

	public SQLQueryBuilder deleteFrom(String table){
		action=Action.DELETE;
		this.table=table;
		return this;
	}

	public SQLQueryBuilder distinct(){
		if(action!=Action.SELECT)
			throw new IllegalArgumentException("This only works with SELECT");
		selectDistinct=true;
		return this;
	}

	public SQLQueryBuilder columns(String... columns){
		selectColumns=columns;
		selectEverything=false;
		return this;
	}

	public SQLQueryBuilder allColumns(){
		selectColumns=null;
		selectEverything=true;
		return this;
	}

	public SQLQueryBuilder count(){
		selectCount=true;
		selectEverything=false;
		selectColumns=null;
		return this;
	}

	public SQLQueryBuilder value(String key, Object value){
		if(values==null)
			values=new ArrayList<>();
		values.add(new Value(key, value));
		return this;
	}

	public SQLQueryBuilder selectExpr(String expr){
		selectExpr=expr;
		selectEverything=false;
		return this;
	}

	public SQLQueryBuilder valueExpr(String key, String expr, Object... args){
		if(values==null)
			values=new ArrayList<>();
		values.add(new ExpressionValue(key, expr, args));
		return this;
	}

	public SQLQueryBuilder where(String where, Object... args){
		condition=where;
		conditionArgs=new ArrayList<>(List.of(args));
		return this;
	}

	public SQLQueryBuilder whereIn(String column, Object... args){
		StringBuilder sb=new StringBuilder();
		sb.append('`');
		sb.append(column);
		sb.append("` IN (");
		for(int i=0;i<args.length;i++){
			sb.append('?');
			if(i<args.length-1)
				sb.append(',');
		}
		sb.append(')');
		condition=sb.toString();
		conditionArgs=new ArrayList<>(List.of(args));
		return this;
	}

	public SQLQueryBuilder andWhere(String where, Object... args){
		if(StringUtils.isNotEmpty(condition))
			condition+=" AND ";
		condition+=where;
		if(conditionArgs==null)
			conditionArgs=new ArrayList<>();
		conditionArgs.addAll(List.of(args));
		return this;
	}

	public SQLQueryBuilder whereIn(String column, Collection<?> args){
		return whereIn(column, args.toArray(new Object[0]));
	}

	public SQLQueryBuilder limit(int limit, int offset){
		this.limit=limit;
		this.offset=offset;
		hasLimit=true;
		return this;
	}

	public SQLQueryBuilder orderBy(String order){
		orderBy=order;
		return this;
	}

	public SQLQueryBuilder groupBy(String group){
		groupBy=group;
		return this;
	}

	public SQLQueryBuilder onDuplicateKeyUpdate(){
		if(action!=Action.INSERT)
			throw new IllegalStateException("ON DUPLICATE KEY UPDATE can only be used with INSERT");
		action=Action.INSERT_OR_UPDATE;
		return this;
	}

	public void executeNoResult() throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(0)){
			stmt.execute();
			if(needCloseConnection)
				conn.close();
		}
	}

	public int executeUpdate() throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(0)){
			int r=stmt.executeUpdate();
			if(needCloseConnection)
				conn.close();
			return r;
		}
	}

	public int executeAndGetID() throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(Statement.RETURN_GENERATED_KEYS)){
			stmt.execute();
			int id=DatabaseUtils.oneFieldToInt(stmt.getGeneratedKeys());
			if(needCloseConnection)
				conn.close();
			return id;
		}
	}

	public ResultSet execute() throws SQLException{
		if(needCloseConnection)
			throw new IllegalStateException("You need to pass an existing connection to use this");
		return createStatementInternal(0).executeQuery();
	}

	public <T> Stream<T> executeAsStream(ResultSetDeserializerFunction<T> creator) throws SQLException{
		return DatabaseUtils.resultSetToObjectStream(createStatementInternal(0).executeQuery(), creator, ()->{
			if(needCloseConnection)
				conn.close();
		});
	}

	@Nullable
	public <T> T executeAndGetSingleObject(ResultSetDeserializerFunction<T> creator) throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(0); ResultSet res=stmt.executeQuery()){
			T result=res.next() ? creator.deserialize(res) : null;
			if(needCloseConnection)
				conn.close();
			return result;
		}
	}

	public int executeAndGetInt() throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(0)){
			int r=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
			if(needCloseConnection)
				conn.close();
			return r;
		}
	}

	public List<Integer> executeAndGetIntList() throws SQLException{
		try(PreparedStatement stmt=createStatementInternal(0)){
			List<Integer> r=DatabaseUtils.intResultSetToList(stmt.executeQuery());
			if(needCloseConnection)
				conn.close();
			return r;
		}
	}

	public IntStream executeAndGetIntStream() throws SQLException{
		return DatabaseUtils.intResultSetToStream(createStatementInternal(0).executeQuery());
	}

	private void appendSelectColumns(StringBuilder sb){
		if(selectCount){
			sb.append("COUNT(*)");
		}else if(selectEverything){
			sb.append("*");
		}else if(selectExpr!=null){
			sb.append(selectExpr);
		}else{
			int i=0;
			for(String col : selectColumns){
				sb.append('`');
				sb.append(col);
				sb.append('`');
				if(i<selectColumns.length-1)
					sb.append(',');
				i++;
			}
		}
	}

	private void appendInsertValues(StringBuilder sb){
		sb.append('(');
		int i=0;
		for(Value e : values){
			sb.append('`');
			sb.append(e.key);
			sb.append('`');
			if(i<values.size()-1)
				sb.append(',');
			i++;
		}
		sb.append(") VALUES (");
		for(i=0;i<values.size();i++){
			values.get(i).write(sb);
			if(i<values.size()-1)
				sb.append(',');
		}
		sb.append(')');
	}

	private void appendUpdateValues(StringBuilder sb){
		int i=0;
		for(Value e : values){
			sb.append('`');
			sb.append(e.key);
			sb.append("`=");
			e.write(sb);
			if(i<values.size()-1)
				sb.append(',');
			i++;
		}
	}

	private void appendCondition(StringBuilder sb){
		if(conditionArgs==null || condition==null)
			return;

		sb.append("WHERE ");
		sb.append(condition);
	}

	private void ensureHasCondition(){
		if(conditionArgs==null || condition==null)
			throw new IllegalArgumentException("This action ("+action+") requires a WHERE condition");
	}

	public PreparedStatement createStatement() throws SQLException{
		if(needCloseConnection)
			throw new IllegalStateException("You need to pass an existing connection to use this");
		return createStatementInternal(0);
	}

	public PreparedStatement createStatement(int autoGeneratedKeys) throws SQLException{
		if(needCloseConnection)
			throw new IllegalStateException("You need to pass an existing connection to use this");
		return createStatementInternal(autoGeneratedKeys);
	}

	public PreparedStatement createStatementInternal(int autoGeneratedKeys) throws SQLException{
		StringBuilder sb=new StringBuilder();
		switch(action){
			case SELECT -> {
				sb.append("SELECT ");
				if(selectDistinct)
					sb.append("DISTINCT ");
				appendSelectColumns(sb);
				sb.append(" FROM `");
				sb.append(table);
				sb.append("` ");
				appendCondition(sb);
			}
			case INSERT -> {
				sb.append("INSERT INTO `");
				sb.append(table);
				sb.append("` ");
				appendInsertValues(sb);
			}
			case INSERT_IGNORE -> {
				sb.append("INSERT IGNORE INTO `");
				sb.append(table);
				sb.append("` ");
				appendInsertValues(sb);
			}
			case UPDATE -> {
				ensureHasCondition();
				sb.append("UPDATE `");
				sb.append(table);
				sb.append("` SET ");
				appendUpdateValues(sb);
				sb.append(' ');
				appendCondition(sb);
			}
			case DELETE -> {
				ensureHasCondition();
				sb.append("DELETE FROM `");
				sb.append(table);
				sb.append("` ");
				appendCondition(sb);
			}
			case INSERT_OR_UPDATE -> {
				sb.append("INSERT INTO `");
				sb.append(table);
				sb.append("` ");
				appendInsertValues(sb);
				sb.append(" ON DUPLICATE KEY UPDATE ");
				appendUpdateValues(sb);
			}
		}

		if(orderBy!=null){
			sb.append(" ORDER BY ");
			sb.append(orderBy);
		}

		if(groupBy!=null){
			sb.append(" GROUP BY ");
			sb.append(groupBy);
		}

		if(hasLimit){
			sb.append(" LIMIT ");
			sb.append(limit);
			sb.append(" OFFSET ");
			sb.append(offset);
		}

		PreparedStatement stmt=conn.prepareStatement(sb.toString(), autoGeneratedKeys);

		int argIndex=1;
		if(action==Action.INSERT || action==Action.INSERT_IGNORE || action==Action.UPDATE || action==Action.INSERT_OR_UPDATE){
			for(Value value : values){
				argIndex+=value.setArguments(stmt, argIndex);
			}
			if(action==Action.INSERT_OR_UPDATE){
				for(Value value : values){
					argIndex+=value.setArguments(stmt, argIndex);
				}
			}
		}

		if(conditionArgs!=null){
			for(Object arg:conditionArgs){
				stmt.setObject(argIndex++, convertValue(arg));
			}
		}

		LOG.debug("{}", stmt);

		return stmt;
	}

	public static PreparedStatement prepareStatement(DatabaseConnection conn, String sql, Object... args) throws SQLException{
		PreparedStatement stmt=conn.prepareStatement(sql);
		int i=1;
		for(Object arg:args){
			if(arg instanceof Enum<?> e)
				stmt.setInt(i, e.ordinal());
			else if(arg instanceof Instant instant)
				stmt.setTimestamp(i, Timestamp.from(instant));
			else if(arg instanceof LocalDate ld)
				stmt.setDate(i, java.sql.Date.valueOf(ld));
			else
				stmt.setObject(i, arg);
			i++;
		}
		LOG.debug("{}", stmt);
		return stmt;
	}

	private static Object convertValue(Object value){
		if(value instanceof Enum<?> e)
			return e.ordinal();
		else if(value instanceof Instant instant)
			return Timestamp.from(instant);
		else if(value instanceof LocalDate localDate)
			return java.sql.Date.valueOf(localDate);
		else if(value instanceof URI uri)
			return uri.toString();
		else if(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof byte[] || value instanceof Timestamp || value instanceof java.sql.Date)
			return value;
		else if(value==null)
			return null;
		else
			throw new IllegalArgumentException("Objects of type "+value.getClass().getName()+" are not supported as SQL query arguments");
	}

	private enum Action{
		SELECT,
		INSERT,
		UPDATE,
		DELETE,
		INSERT_OR_UPDATE,
		INSERT_IGNORE
	}

	private static class Value{
		public String key;
		public Object value;

		public Value(String key, Object value){
			this.key=key;
			this.value=convertValue(value);
		}

		public void write(StringBuilder sb){
			sb.append('?');
		}

		public int setArguments(PreparedStatement stmt, int offset) throws SQLException{
			stmt.setObject(offset, value);
			return 1;
		}
	}

	private static class ExpressionValue extends Value{

		public String expr;
		public Object[] args;

		public ExpressionValue(String key, String value, Object[] args){
			super(key, null);
			expr=value;
			this.args=args;
		}

		@Override
		public void write(StringBuilder sb){
			sb.append(expr);
		}

		@Override
		public int setArguments(PreparedStatement stmt, int offset) throws SQLException{
			for(int i=0;i<args.length;i++){
				stmt.setObject(offset+i, args[i]);
			}
			return args.length;
		}
	}
}
