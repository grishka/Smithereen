package smithereen.model;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import smithereen.Utils;

public record ServerRule(int id, int priority, String title, String description, Map<String, Translation> translations, boolean isDeleted){

	public static ServerRule fromResultSet(ResultSet res) throws SQLException{
		return new ServerRule(
				res.getInt("id"),
				res.getInt("priority"),
				res.getString("title"),
				res.getString("description"),
				Utils.gson.fromJson(res.getString("translations"), new TypeToken<>(){}),
				res.getBoolean("is_deleted")
		);
	}

	private Translation getTranslationForLocale(Locale locale){
		if(translations.isEmpty())
			return null;
		Translation t=translations.get(locale.toLanguageTag());
		if(t!=null)
			return t;
		return translations.get(locale.getLanguage());
	}

	public String getTranslatedTitle(Locale locale){
		Translation t=getTranslationForLocale(locale);
		return t==null ? title : t.title;
	}

	public String getTranslatedDescription(Locale locale){
		Translation t=getTranslationForLocale(locale);
		return t==null ? description : t.description;
	}

	public record Translation(String title, String description){}
}
