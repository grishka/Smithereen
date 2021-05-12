package smithereen.activitypub.objects;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import spark.utils.StringUtils;

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

	public JSONObject asRootActivityPubObject(){
		ContextCollector contextCollector=new ContextCollector();
		JSONObject obj=asActivityPubObject(null, contextCollector);
		obj.put("@context", contextCollector.toContext());
		return obj;
	}

	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		if(obj==null)
			obj=new JSONObject();

		obj.put("type", getType());
		if(activityPubID!=null)
			obj.put("id", activityPubID.toString());
		if(attachment!=null && !attachment.isEmpty())
			obj.put("attachment", serializeObjectArray(attachment, contextCollector));
		if(attributedTo!=null)
			obj.put("attributedTo", attributedTo.toString());
		if(audience!=null)
			obj.put("audience", audience.toString());
		if(content!=null)
			obj.put("content", content);
		if(context!=null)
			obj.put("context", context.toString());
		if(name!=null)
			obj.put("name", name);
		if(endTime!=null)
			obj.put("endTime", Utils.formatDateAsISO(endTime));
		if(generator!=null)
			obj.put("generator", generator.serialize(contextCollector));
		if(image!=null && !image.isEmpty())
			obj.put("image", serializeObjectArrayCompact(image, contextCollector));
		if(icon!=null && !icon.isEmpty())
			obj.put("icon", serializeObjectArrayCompact(icon, contextCollector));
		if(inReplyTo!=null)
			obj.put("inReplyTo", inReplyTo.toString());
		if(location!=null)
			obj.put("location", location.serialize(contextCollector));
		if(preview!=null)
			obj.put("preview", preview.serialize(contextCollector));
		if(published!=null)
			obj.put("published", Utils.formatDateAsISO(published));
		if(replies!=null)
			obj.put("replies", replies.asActivityPubObject(null, contextCollector));
		if(startTime!=null)
			obj.put("startTime", Utils.formatDateAsISO(startTime));
		if(summary!=null)
			obj.put("summary", summary);
		if(tag!=null && !tag.isEmpty())
			obj.put("tag", serializeObjectArrayCompact(tag, contextCollector));
		if(updated!=null)
			obj.put("updated", Utils.formatDateAsISO(updated));
		if(url!=null)
			obj.put("url", url.toString());
		if(to!=null)
			obj.put("to", serializeLinkOrObjectArray(to, contextCollector));
		if(bto!=null)
			obj.put("bto", serializeLinkOrObjectArray(bto, contextCollector));
		if(cc!=null)
			obj.put("cc", serializeLinkOrObjectArray(cc, contextCollector));
		if(bcc!=null)
			obj.put("bcc", serializeLinkOrObjectArray(bcc, contextCollector));
		if(mediaType!=null)
			obj.put("mediaType", mediaType);
		if(duration!=0)
			obj.put("duration", serializeDuration(duration));

		return obj;
	}

	protected <T extends ActivityPubObject> List<T> parseSingleObjectOrArray(Object o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		try{
			if(o instanceof JSONObject){
				T item=(T)parse((JSONObject)o, parserContext);
				if(item==null)
					return null;
				return (List<T>) Collections.singletonList(item);
			}else if(o instanceof JSONArray){
				JSONArray ar=(JSONArray) o;
				ArrayList<T> res=new ArrayList<>();
				for(int i=0; i<ar.length(); i++){
					T item=(T) parse(ar.getJSONObject(i), parserContext);
					if(item!=null)
						res.add(item);
				}
				return res.isEmpty() ? null : res;
			}
		}catch(ClassCastException ignore){}
		return null;
	}

	protected <T extends ActivityPubObject> T parseSingleObject(JSONObject o, ParserContext parserContext) throws Exception{
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

	protected LinkOrObject tryParseLinkOrObject(Object o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		if(o instanceof String){
			URI url=tryParseURL((String)o);
			if(url!=null)
				return new LinkOrObject(url);
		}else if(o instanceof JSONObject){
			ActivityPubObject obj=parse((JSONObject)o, parserContext);
			if(obj!=null)
				return new LinkOrObject(obj);
		}
		return null;
	}

	protected List<LinkOrObject> tryParseArrayOfLinksOrObjects(Object o, ParserContext parserContext) throws Exception{
		if(o==null)
			return null;
		if(o instanceof JSONArray){
			JSONArray ar=(JSONArray)o;
			ArrayList<LinkOrObject> res=new ArrayList<>();
			for(int i=0;i<ar.length();i++){
				LinkOrObject lo=tryParseLinkOrObject(ar.get(i), parserContext);
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

	protected JSONArray serializeObjectArray(List<? extends ActivityPubObject> ar, ContextCollector contextCollector){
		JSONArray res=new JSONArray();
		for(ActivityPubObject obj:ar){
			res.put(obj.asActivityPubObject(null, contextCollector));
		}
		return res;
	}

	protected Object serializeObjectArrayCompact(List<? extends ActivityPubObject> ar, ContextCollector contextCollector){
		return ar.size()==1 ? ar.get(0).asActivityPubObject(null, contextCollector) : serializeObjectArray(ar, contextCollector);
	}

	protected JSONArray serializeLinkOrObjectArray(List<LinkOrObject> ar, ContextCollector contextCollector){
		JSONArray res=new JSONArray();
		for(LinkOrObject l:ar){
			res.put(l.serialize(contextCollector));
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

	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		activityPubID=tryParseURL(obj.optString("id"));
		attachment=parseSingleObjectOrArray(obj.opt("attachment"), parserContext);
		attributedTo=tryParseURL(obj.optString("attributedTo", null));
		audience=tryParseURL(obj.optString("audience", null));
		content=obj.optString("content", null);
		name=obj.optString("name", null);
		endTime=tryParseDate(obj.optString("endTime", null));
		generator=tryParseLinkOrObject(obj.opt("generator"), parserContext);
		image=parseSingleObjectOrArray(obj.opt("image"), parserContext);
		icon=parseSingleObjectOrArray(obj.opt("icon"), parserContext);
		inReplyTo=tryParseURL(obj.optString("inReplyTo", null));
		location=tryParseLinkOrObject(obj.opt("location"), parserContext);
		preview=tryParseLinkOrObject(obj.opt("preview"), parserContext);
		published=tryParseDate(obj.optString("published", null));
		replies=parseSingleObject(obj.optJSONObject("replies"), parserContext);
		startTime=tryParseDate(obj.optString("startTime", null));
		summary=obj.optString("summary", null);
		tag=parseSingleObjectOrArray(obj.opt("tag"), parserContext);
		updated=tryParseDate(obj.optString("updated", null));
		url=tryParseURL(obj.optString("url", null));
		to=tryParseArrayOfLinksOrObjects(obj.opt("to"), parserContext);
		bto=tryParseArrayOfLinksOrObjects(obj.opt("bto"), parserContext);
		cc=tryParseArrayOfLinksOrObjects(obj.opt("cc"), parserContext);
		bcc=tryParseArrayOfLinksOrObjects(obj.opt("bcc"), parserContext);
		mediaType=obj.optString("mediaType", null);
		duration=tryParseDuration(obj.optString("duration", null));
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

	public static ActivityPubObject parse(JSONObject obj) throws Exception{
		return parse(obj, ParserContext.FOREIGN);
	}

	public static ActivityPubObject parse(JSONObject obj, ParserContext parserContext) throws Exception{
		if(obj==null)
			return null;
		String type=obj.getString("type");
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
