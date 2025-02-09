package smithereen.model.filtering;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;

public class WordFilter{
	public final int id;
	public final String name;
	public final List<String> words;
	public final EnumSet<FilterContext> contexts;
	public final Instant expiresAt;
	public final Pattern regex;

	public WordFilter(int id, String name, List<String> words, EnumSet<FilterContext> contexts, Instant expiresAt){
		this.id=id;
		this.name=name;
		this.words=words;
		this.contexts=contexts;
		this.expiresAt=expiresAt;
		this.regex=Pattern.compile("\\b("+words.stream().map(TextProcessor::escapeHTML).map(Pattern::quote).collect(Collectors.joining("|"))+")\\b", Pattern.CASE_INSENSITIVE);
	}

	public static WordFilter fromResultSet(ResultSet res) throws SQLException{
		return new WordFilter(
				res.getInt("id"),
				res.getString("name"),
				Utils.gson.fromJson(res.getString("words"), new TypeToken<>(){}),
				Utils.deserializeEnumSet(FilterContext.class, res.getInt("contexts")),
				DatabaseUtils.getInstant(res, "expires_at")
		);
	}
}
