package smithereen.model;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import spark.utils.StringUtils;

public record ServerAnnouncement(int id, String title, String description, String linkText, String linkUrl, Instant showFrom, Instant showTo, Map<String, Translation> translations){

	public static ServerAnnouncement fromResultSet(ResultSet res) throws SQLException{
		return new ServerAnnouncement(
				res.getInt("id"),
				res.getString("title"),
				res.getString("description"),
				res.getString("link_text"),
				res.getString("link_url"),
				DatabaseUtils.getInstant(res, "show_from"),
				DatabaseUtils.getInstant(res, "show_to"),
				Utils.gson.fromJson(res.getString("translations"), new TypeToken<>(){})
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
		return t==null || StringUtils.isEmpty(t.title) ? title : t.title;
	}

	public String getTranslatedDescription(Locale locale){
		Translation t=getTranslationForLocale(locale);
		return t==null || StringUtils.isEmpty(t.description) ? description : t.description;
	}

	public String getTranslatedLinkText(Locale locale){
		Translation t=getTranslationForLocale(locale);
		return t==null || StringUtils.isEmpty(t.linkText) ? linkText : t.linkText;
	}

	public String getTranslatedLinkURL(Locale locale){
		Translation t=getTranslationForLocale(locale);
		return t==null || StringUtils.isEmpty(t.linkUrl) ? linkUrl : t.linkUrl;
	}

	public record Translation(String title, String description, String linkText, String linkUrl){}
}
