package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Join;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Post;

public abstract class ActivityPubObject{

	/*attachment | attributedTo | audience | content | context | name | endTime | generator | icon | image | inReplyTo | location | preview | published | replies | startTime | summary | tag | updated | url | to | bto | cc | bcc | mediaType | duration*/

	public List<ActivityPubObject> attachment;
	public URI attributedTo;
	public URI audience;
	public String content;
	public URI context;
	public String name;
	public Date endTime;
	public LinkOrObject generator;
	public List<Image> image;
	public List<Image> icon;
	public URI inReplyTo;
	public LinkOrObject location;
	public LinkOrObject preview;
	public Date published;
	public ActivityPubCollection replies;
	public Date startTime;
	public String summary;
	public List<ActivityPubObject> tag;
	public Date updated;
	public URI url;
	public List<LinkOrObject> to;
	public List<LinkOrObject> bto;
	public List<LinkOrObject> cc;
	public List<LinkOrObject> bcc;
	public String mediaType;
	public long duration;

	public URI activityPubID;

	public abstract String getType();

	public JsonObject asRootActivityPubObject(){
		ContextCollector contextCollector=new ContextCollector();
		JsonObject obj=asActivityPubObject(null, contextCollector);
		obj.add("@context", contextCollector.toContext());
		return obj;
	}

	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		if(obj==null)
			obj=new JsonObject();

		obj.addProperty("type", getType());
		if(activityPubID!=null)
			obj.addProperty("id", activityPubID.toString());
		if(attachment!=null && !attachment.isEmpty())
			obj.add("attachment", serializeObjectArray(attachment, contextCollector));
		if(attributedTo!=null)
			obj.addProperty("attributedTo", attributedTo.toString());
		if(audience!=null)
			obj.addProperty("audience", audience.toString());
		if(content!=null)
			obj.addProperty("content", content);
		if(context!=null)
			obj.addProperty("context", context.toString());
		if(name!=null)
			obj.addProperty("name", name);
		if(endTime!=null)
			obj.addProperty("endTime", Utils.formatDateAsISO(endTime));
		if(generator!=null)
			obj.add("generator", generator.serialize(contextCollector));
		if(image!=null && !image.isEmpty())
			obj.add("image", serializeObjectArrayCompact(image, contextCollector));
		if(icon!=null && !icon.isEmpty())
			obj.add("icon", serializeObjectArrayCompact(icon, contextCollector));
		if(inReplyTo!=null)
			obj.addProperty("inReplyTo", inReplyTo.toString());
		if(location!=null)
			obj.add("location", location.serialize(contextCollector));
		if(preview!=null)
			obj.add("preview", preview.serialize(contextCollector));
		if(published!=null)
			obj.addProperty("published", Utils.formatDateAsISO(published));
		if(replies!=null)
			obj.add("replies", replies.asActivityPubObject(null, contextCollector));
		if(startTime!=null)
			obj.addProperty("startTime", Utils.formatDateAsISO(startTime));
		if(summary!=null)
			obj.addProperty("summary", summary);
		if(tag!=null && !tag.isEmpty())
			obj.add("tag", serializeObjectArrayCompact(tag, contextCollector));
		if(updated!=null)
			obj.addProperty("updated", Utils.formatDateAsISO(updated));
		if(url!=null)
			obj.addProperty("url", url.toString());
		if(to!=null)
			obj.add("to", serializeLinkOrObjectArray(to, contextCollector));
		if(bto!=null)
			obj.add("bto", serializeLinkOrObjectArray(bto, contextCollector));
		if(cc!=null)
			obj.add("cc", serializeLinkOrObjectArray(cc, contextCollector));
		if(bcc!=null)
			obj.add("bcc", serializeLinkOrObjectArray(bcc, contextCollector));
		if(mediaType!=null)
			obj.addProperty("mediaType", mediaType);
		if(duration!=0)
			obj.addProperty("duration", serializeDuration(duration));

		return obj;
	}

	protected <T extends ActivityPubObject> List<T> parseSingleObjectOrArray(JsonElement o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		try{
			if(o.isJsonObject()){
				T item=(T)parse(o.getAsJsonObject(), parserContext);
				if(item==null)
					return null;
				return (List<T>) Collections.singletonList(item);
			}else if(o.isJsonArray()){
				ArrayList<T> res=new ArrayList<>();
				for(JsonElement el:o.getAsJsonArray()){
					T item=(T) parse(el.getAsJsonObject(), parserContext);
					if(item!=null)
						res.add(item);
				}
				return res.isEmpty() ? null : res;
			}
		}catch(ClassCastException ignore){}
		return null;
	}

	protected <T extends ActivityPubObject> T parseSingleObject(JsonObject o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		try{
			return (T)parse(o, parserContext);
		}catch(ClassCastException x){
			return null;
		}
	}

	protected URI tryParseURL(String url){
		if(url==null || url.isEmpty())
			return null;
		try{
			return new URI(url);
		}catch(URISyntaxException x){
			return null;
		}
	}

	protected Date tryParseDate(String date){
		if(date==null)
			return null;
		return Utils.parseISODate(date);
	}

	protected LinkOrObject tryParseLinkOrObject(JsonElement o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		if(o.isJsonPrimitive() && o.getAsJsonPrimitive().isString()){
			URI url=tryParseURL(o.getAsString());
			if(url!=null)
				return new LinkOrObject(url);
		}else if(o.isJsonObject()){
			ActivityPubObject obj=parse(o.getAsJsonObject(), parserContext);
			if(obj!=null)
				return new LinkOrObject(obj);
		}
		return null;
	}

	protected List<LinkOrObject> tryParseArrayOfLinksOrObjects(JsonElement o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		if(o.isJsonArray()){
			JsonArray ar=o.getAsJsonArray();
			ArrayList<LinkOrObject> res=new ArrayList<>();
			for(JsonElement el:ar){
				LinkOrObject lo=tryParseLinkOrObject(el, parserContext);
				if(lo!=null)
					res.add(lo);
			}
			return res.isEmpty() ? null : res;
		}
		LinkOrObject lo=tryParseLinkOrObject(o, parserContext);
		return lo!=null ? Collections.singletonList(lo) : null;
	}

	protected long tryParseDuration(String duration){
		if(duration==null)
			return 0;
		long result=0;
		Pattern durationPattern=Pattern.compile("^(-)?P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?T?(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?$");
		Matcher matcher=durationPattern.matcher(duration);
		if(matcher.find()){
			int years=Utils.parseIntOrDefault(matcher.group(2), 0);
			int months=Utils.parseIntOrDefault(matcher.group(3), 0);
			int days=Utils.parseIntOrDefault(matcher.group(4), 0);
			int hours=Utils.parseIntOrDefault(matcher.group(5), 0);
			int minutes=Utils.parseIntOrDefault(matcher.group(6), 0);
			int seconds=Utils.parseIntOrDefault(matcher.group(7), 0);
			if(years>0 || months>0){
				System.out.println("W: trying to parse duration "+duration+" with years and/or months");
				return 0;
			}
			result=seconds*1000L
					+minutes*60L*1000L
					+hours*60L*60L*1000L
					+days*24L*60L*60L*1000L;
		}

		return result;
	}

	protected JsonArray serializeObjectArray(List<? extends ActivityPubObject> ar, ContextCollector contextCollector){
		JsonArray res=new JsonArray();
		for(ActivityPubObject obj:ar){
			res.add(obj.asActivityPubObject(null, contextCollector));
		}
		return res;
	}

	protected JsonElement serializeObjectArrayCompact(List<? extends ActivityPubObject> ar, ContextCollector contextCollector){
		return ar.size()==1 ? ar.get(0).asActivityPubObject(null, contextCollector) : serializeObjectArray(ar, contextCollector);
	}

	protected JsonArray serializeLinkOrObjectArray(List<LinkOrObject> ar, ContextCollector contextCollector){
		JsonArray res=new JsonArray();
		for(LinkOrObject l:ar){
			res.add(l.serialize(contextCollector));
		}
		return res;
	}

	protected String serializeDuration(long d){
		StringBuilder sb=new StringBuilder(d>0 ? "P" : "-P");
		d=Math.abs(d/1000L);
		if(d>24L*60L*60L){
			long days=d/(24L*60L*60L);
			sb.append(days);
			sb.append('D');
			d-=days*24L*60L*60L;
		}
		if(d>0){
			sb.append('T');
			if(d>60L*60L){
				long hours=d/(60L*60L);
				sb.append(hours);
				sb.append('H');
				d-=hours*60L*60L;
			}
			if(d>60L){
				long minutes=d/60L;
				sb.append(minutes);
				sb.append('M');
				d-=minutes*60L;
			}
			if(d>0){
				sb.append(d);
				sb.append('S');
			}
		}
		return sb.toString();
	}

	protected String optString(JsonObject obj, String key){
		if(obj.has(key) && obj.get(key).isJsonPrimitive() && obj.getAsJsonPrimitive(key).isString())
			return obj.get(key).getAsString();
		return null;
	}

	protected int optInt(JsonObject obj, String key){
		if(obj.has(key) && obj.get(key).isJsonPrimitive() && obj.getAsJsonPrimitive(key).isNumber())
			return obj.get(key).getAsInt();
		return -1;
	}

	protected boolean optBoolean(JsonObject obj, String key){
		if(obj.has(key) && obj.get(key).isJsonPrimitive() && obj.getAsJsonPrimitive(key).isBoolean())
			return obj.get(key).getAsBoolean();
		return false;
	}

	protected JsonObject optObject(JsonObject obj, String key){
		if(obj.has(key) && obj.get(key).isJsonObject())
			return obj.getAsJsonObject(key);
		return null;
	}

	protected JsonArray optArray(JsonObject obj, String key){
		if(obj.has(key) && obj.get(key).isJsonArray())
			return obj.getAsJsonArray(key);
		return null;
	}

	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext) throws Exception{
		activityPubID=tryParseURL(optString(obj, "id"));
		attachment=parseSingleObjectOrArray(obj.get("attachment"), parserContext);
		attributedTo=tryParseURL(optString(obj, "attributedTo"));
		audience=tryParseURL(optString(obj, "audience"));
		content=optString(obj, "content");
		name=optString(obj, "name");
		endTime=tryParseDate(optString(obj, "endTime"));
		generator=tryParseLinkOrObject(obj.get("generator"), parserContext);
		image=parseSingleObjectOrArray(obj.get("image"), parserContext);
		icon=parseSingleObjectOrArray(obj.get("icon"), parserContext);
		inReplyTo=tryParseURL(optString(obj, "inReplyTo"));
		location=tryParseLinkOrObject(obj.get("location"), parserContext);
		preview=tryParseLinkOrObject(obj.get("preview"), parserContext);
		published=tryParseDate(optString(obj, "published"));
		replies=parseSingleObject(optObject(obj, "replies"), parserContext);
		startTime=tryParseDate(optString(obj, "startTime"));
		summary=optString(obj, "summary");
		tag=parseSingleObjectOrArray(obj.get("tag"), parserContext);
		updated=tryParseDate(optString(obj, "updated"));
		url=tryParseURL(optString(obj, "url"));
		to=tryParseArrayOfLinksOrObjects(obj.get("to"), parserContext);
		bto=tryParseArrayOfLinksOrObjects(obj.get("bto"), parserContext);
		cc=tryParseArrayOfLinksOrObjects(obj.get("cc"), parserContext);
		bcc=tryParseArrayOfLinksOrObjects(obj.get("bcc"), parserContext);
		mediaType=optString(obj, "mediaType");
		duration=tryParseDuration(optString(obj, "duration"));
		return this;
	}

	//abstract String getType();

	public void resolveDependencies(boolean allowFetching, boolean allowStorage) throws SQLException{

	}

	public void storeDependencies() throws SQLException{

	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("ActivityPubObject{");
		if(activityPubID!=null){
			sb.append("id=");
			sb.append(activityPubID);
		}
		if(attachment!=null){
			sb.append(", attachment=");
			sb.append(attachment);
		}
		if(attributedTo!=null){
			sb.append(", attributedTo=");
			sb.append(attributedTo);
		}
		if(audience!=null){
			sb.append(", audience=");
			sb.append(audience);
		}
		if(content!=null){
			sb.append(", content='");
			sb.append(content);
			sb.append('\'');
		}
		if(context!=null){
			sb.append(", context=");
			sb.append(context);
		}
		if(name!=null){
			sb.append(", name='");
			sb.append(name);
			sb.append('\'');
		}
		if(endTime!=null){
			sb.append(", endTime=");
			sb.append(endTime);
		}
		if(generator!=null){
			sb.append(", generator=");
			sb.append(generator);
		}
		if(image!=null){
			sb.append(", image=");
			sb.append(image);
		}
		if(icon!=null){
			sb.append(", icon=");
			sb.append(icon);
		}
		if(inReplyTo!=null){
			sb.append(", inReplyTo=");
			sb.append(inReplyTo);
		}
		if(location!=null){
			sb.append(", location=");
			sb.append(location);
		}
		if(preview!=null){
			sb.append(", preview=");
			sb.append(preview);
		}
		if(published!=null){
			sb.append(", published=");
			sb.append(published);
		}
		if(replies!=null){
			sb.append(", replies=");
			sb.append(replies);
		}
		if(startTime!=null){
			sb.append(", startTime=");
			sb.append(startTime);
		}
		if(summary!=null){
			sb.append(", summary='");
			sb.append(summary);
			sb.append('\'');
		}
		if(tag!=null){
			sb.append(", tag=");
			sb.append(tag);
		}
		if(updated!=null){
			sb.append(", updated=");
			sb.append(updated);
		}
		if(url!=null){
			sb.append(", url=");
			sb.append(url);
		}
		if(to!=null){
			sb.append(", to=");
			sb.append(to);
		}
		if(bto!=null){
			sb.append(", bto=");
			sb.append(bto);
		}
		if(cc!=null){
			sb.append(", cc=");
			sb.append(cc);
		}
		if(bcc!=null){
			sb.append(", bcc=");
			sb.append(bcc);
		}
		if(mediaType!=null){
			sb.append(", mediaType='");
			sb.append(mediaType);
			sb.append('\'');
		}
		if(duration!=0){
			sb.append(", duration=");
			sb.append(duration);
		}
		sb.append('}');
		return sb.toString();
	}

	public static ActivityPubObject parse(JsonObject obj) throws Exception{
		return parse(obj, ParserContext.FOREIGN);
	}

	public static ActivityPubObject parse(JsonObject obj, ParserContext parserContext) throws Exception{
		if(obj==null)
			return null;
		String type=Objects.requireNonNull(obj.get("type"), "type must not be null").getAsString();
		ActivityPubObject res=null;
		switch(type){
			// Actors
			case "Person":
				res=new ForeignUser();
				break;
			case "Service":
				res=obj.has("id") ? new ForeignUser() : new Service();
				break;
			case "Group":
			case "Organization":
				res=new ForeignGroup();
				break;

			// Objects
			case "Note":
			case "Article":
			case "Page":
				res=new Post();
				break;
			case "Image":
				res=new Image();
				break;
			case "_LocalImage":
				if(parserContext.isLocal)
					res=new LocalImage();
				break;
			case "Document":
				res=new Document();
				break;
			case "Tombstone":
				res=new Tombstone();
				break;
			case "Mention":
				res=new Mention();
				break;
			case "Relationship":
				res=new Relationship();
				break;
			case "PropertyValue":
				res=new PropertyValue();
				break;

			// Collections
			case "Collection":
				res=new ActivityPubCollection(false);
				break;
			case "OrderedCollection":
				res=new ActivityPubCollection(true);
				break;
			case "CollectionPage":
				res=new CollectionPage(false);
				break;
			case "OrderedCollectionPage":
				res=new CollectionPage(true);
				break;

			// Activities
			case "Accept":
				res=new Accept();
				break;
			case "Add":
				res=new Add();
				break;
			case "Announce":
				res=new Announce();
				break;
			case "Create":
				res=new Create();
				break;
			case "Delete":
				res=new Delete();
				break;
			case "Follow":
				res=new Follow();
				break;
			case "Join":
				res=new Join();
				break;
			case "Like":
				res=new Like();
				break;
			case "Undo":
				res=new Undo();
				break;
			case "Update":
				res=new Update();
				break;
			case "Reject":
				res=new Reject();
				break;
			case "Offer":
				res=new Offer();
				break;
			case "Block":
				res=new Block();
				break;

			default:
				System.out.println("Unknown object type "+type);
		}
		if(res!=null)
			return res.parseActivityPubObject(obj, parserContext);
		return null;
	}
}
