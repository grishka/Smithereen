package smithereen.jsonld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class JLDProcessor{
	private static final Logger LOG=LoggerFactory.getLogger(JLDProcessor.class);

	private static HashMap<String, JsonObject> schemaCache=new HashMap<>();
	private static final JsonObject inverseLocalContext;
	private static final JLDContext localContext;

	private static final Comparator<String> SHORTEST_LEAST=(o1, o2)->{
		if(o1.length()!=o2.length())
			return o1.length()-o2.length();
		return o1.compareTo(o2);
	};

	static{
		JsonObject lc=new JsonObject();
		lc.addProperty("sc", JLD.SCHEMA_ORG);
		lc.addProperty("sm", JLD.SMITHEREEN);
		lc.addProperty("toot", JLD.MASTODON);
		lc.addProperty("vcard", JLD.VCARD);
		lc.addProperty("litepub", JLD.LITEPUB);

		// schema.org aliases
		lc.addProperty("firstName", "sc:givenName");
		lc.addProperty("lastName", "sc:familyName");
		lc.addProperty("middleName", "sc:additionalName");
		lc.add("gender", idAndTypeObject("sc:gender", "sc:GenderType"));
		lc.addProperty("value", "sc:value");
		lc.addProperty("PropertyValue", "sc:PropertyValue");

		// ActivityStreams aliases
		lc.addProperty("sensitive", "as:sensitive");
		lc.addProperty("manuallyApprovesFollowers", "as:manuallyApprovesFollowers");
		lc.add("movedTo", idAndTypeObject("as:movedTo", "@id"));
		lc.add("alsoKnownAs", idAndTypeObject("as:alsoKnownAs", "@id"));
		lc.addProperty("quoteUrl", "as:quoteUrl");

		// Mastodon aliases
		lc.addProperty("blurhash", "toot:blurhash");
		lc.addProperty("votersCount", "toot:votersCount");

		// Misskey quote-reposts
		lc.add("_misskey_quote", idAndTypeObject(JLD.MISSKEY+"_misskey_quote", "@id"));

		// custom aliases
		lc.addProperty("supportsFriendRequests", "sm:supportsFriendRequests");
		lc.add("cropRegion", idAndContainerObject("sm:cropRegion", "@list"));
		lc.addProperty("maidenName", "sm:maidenName");
		lc.add("wall", idAndTypeObject("sm:wall", "@id"));
		lc.add("friends", idAndTypeObject("sm:friends", "@id"));
		lc.add("groups", idAndTypeObject("sm:groups", "@id"));
		lc.addProperty("nonAnonymous", "sm:nonAnonymous");
		lc.addProperty("tentativeMembership", "sm:tentativeMembership");
		lc.add("members", idAndTypeObject("sm:members", "@id"));
		lc.add("tentativeMembers", idAndTypeObject("sm:tentativeMembers", "@id"));
		lc.addProperty("TentativeJoin", "sm:TentativeJoin");
		lc.addProperty("accessType", "sm:accessType");
		lc.addProperty("actorToken", "sm:actorToken");
		lc.addProperty("collectionSimpleQuery", "sm:collectionSimpleQuery");
		lc.addProperty("CollectionQueryResult", "sm:CollectionQueryResult");
		lc.addProperty("tentative", "sm:tentative");
		lc.addProperty("graffiti", "sm:graffiti");
		lc.addProperty("privacySettings", "sm:privacySettings");
		lc.addProperty("allowedTo", "sm:allowedTo");
		lc.addProperty("except", "sm:except");
		// privacy settings keys
		lc.addProperty("wallPosting", "sm:wallPosting");
		lc.addProperty("wallPostVisibility", "sm:wallPostVisibility");
		lc.addProperty("commenting", "sm:commenting");
		lc.addProperty("groupInvitations", "sm:groupInvitations");
		lc.addProperty("directMessages", "sm:directMessages");
		// profile fields
		lc.addProperty("activities", "sm:activities");
		lc.addProperty("interests", "sm:interests");
		lc.addProperty("favoriteMusic", "sm:favoriteMusic");
		lc.addProperty("favoriteMovies", "sm:favoriteMovies");
		lc.addProperty("favoriteTvShows", "sm:favoriteTvShows");
		lc.addProperty("favoriteBooks", "sm:favoriteBooks");
		lc.addProperty("favoriteQuotes", "sm:favoriteQuotes");
		lc.add("politicalViews", idAndTypeObject("sm:politicalViews", "@id"));
		lc.addProperty("religion", "sm:religion");
		lc.add("personalPriority", idAndTypeObject("sm:personalPriority", "@id"));
		lc.add("peoplePriority", idAndTypeObject("sm:peoplePriority", "@id"));
		lc.add("smokingViews", idAndTypeObject("sm:smokingViews", "@id"));
		lc.add("alcoholViews", idAndTypeObject("sm:alcoholViews", "@id"));
		lc.addProperty("inspiredBy", "sm:inspiredBy");

		// litepub aliases
		lc.addProperty("capabilities", "litepub:capabilities");
		lc.addProperty("acceptsJoins", "litepub:acceptsJoins");

		localContext=updateContext(new JLDContext(), makeArray(JLD.ACTIVITY_STREAMS, JLD.W3_SECURITY, lc), new ArrayList<>(), null);
		inverseLocalContext=createReverseContext(localContext);
	}

	public static JsonArray expandToArray(JsonElement src){
		return expandToArray(src, null);
	}

	public static JsonArray expandToArray(JsonElement src, URI baseURI){
		JsonElement jcontext=null;
		if(src.isJsonObject())
			jcontext=src.getAsJsonObject().get("@context");
		JLDContext context=updateContext(new JLDContext(), jcontext, new ArrayList<String>(), baseURI);
		Object result=expand(context, null, src);
		if(result instanceof JsonArray)
			return (JsonArray) result;
		if(result==null)
			return new JsonArray();
		if(result instanceof JsonObject _r){
			if(_r.size()==1 && _r.has("@graph"))
				return _r.get("@graph").getAsJsonArray();
		}
		return makeArray(result);
	}

	public static JsonObject compact(JsonElement src, JsonElement context){
		return compact(src, context, true, null);
	}

	public static JsonObject compact(JsonElement src, JsonElement context, boolean compactArrays, URI baseURI){
		JLDContext localContext=updateContext(new JLDContext(), context, new ArrayList<>(), baseURI);
		JsonObject inverseContext=createReverseContext(localContext);
		JsonElement _result=compact(localContext, inverseContext, null, src, compactArrays);
		JsonObject result;
		if(_result.isJsonObject())
			result=_result.getAsJsonObject();
		else if(_result.isJsonArray() && _result.getAsJsonArray().size()>0){
			result=new JsonObject();
			result.add(compactIRI(localContext, inverseContext, "@graph", null, true, false), _result);
		}else
			result=new JsonObject();
		if(context.isJsonObject() && context.getAsJsonObject().size()>0)
			result.add("@context", context);
		else if(!context.isJsonObject())
			result.add("@context", context);
		return result;
	}

	public static JsonObject compactToLocalContext(JsonArray src){
		return (JsonObject)compact(localContext, inverseLocalContext, null, src, true);
	}

	public static JsonObject convertToLocalContext(JsonObject src){
		return compactToLocalContext(expandToArray(src));
	}

	private static JsonObject idAndTypeObject(String id, String type){
		JsonObject o=new JsonObject();
		o.addProperty("@id", id);
		o.addProperty("@type", type);
		return o;
	}

	private static JsonObject idAndContainerObject(String id, String container){
		JsonObject o=new JsonObject();
		o.addProperty("@id", id);
		o.addProperty("@container", container);
		return o;
	}

	private static String readResourceFile(String name){
		try{
			return new String(Objects.requireNonNull(JLDProcessor.class.getResourceAsStream("/jsonld-schemas/"+name+".jsonld")).readAllBytes(), StandardCharsets.UTF_8);
		}catch(IOException x){
			return null;
		}
	}

	private static JsonObject dereferenceContext(String iri){
		if(iri.endsWith("/litepub-0.1.jsonld")){ // This avoids caching multiple copies of the same schema for different instances
			iri="https://example.com/schemas/litepub-0.1.jsonld";
		}
		if(schemaCache.containsKey(iri))
			return schemaCache.get(iri);
		String file=switch(iri){
			case "https://www.w3.org/ns/activitystreams" -> readResourceFile("activitystreams");
			case "https://w3id.org/security/v1" -> readResourceFile("w3-security");
			case "https://w3id.org/identity/v1" -> readResourceFile("w3-identity");
			case "https://example.com/schemas/litepub-0.1.jsonld" -> readResourceFile("litepub-0.1");
			default -> {
				LOG.warn("Can't dereference remote context '{}'", iri);
				yield null;
			}

			//throw new JLDException("loading remote context failed");
		};
		if(file!=null){
			JsonObject obj=JsonParser.parseString(file).getAsJsonObject();
			schemaCache.put(iri, obj);
			return obj;
		}
		return null;
	}

	private static JLDContext updateContext(JLDContext activeContext, JsonElement _localContext, ArrayList<String> remoteContexts, URI baseURI){
		JLDContext result=activeContext.clone();
		result.baseIRI=baseURI;
		result.originalBaseIRI=baseURI;
		if(_localContext==null){
			JLDContext r=new JLDContext();
			r.originalBaseIRI=baseURI;
			r.baseIRI=baseURI;
			return r;
		}

		ArrayList<JsonElement> localContext=new ArrayList<>();
		if(_localContext.isJsonArray()){
			JsonArray a=_localContext.getAsJsonArray();
			for(JsonElement el:a){
				localContext.add(el.isJsonNull() ? null : el);
			}
		}else{
			localContext.add(_localContext);
		}

		for(JsonElement context : localContext){
			if(context==null){
				result=new JLDContext();
				continue;
			}
			if(context.isJsonPrimitive() && context.getAsJsonPrimitive().isString()){
				String c=context.getAsString();
				if(!c.startsWith("http:/") && !c.startsWith("https:/")){
					throw new JLDException("relative context IRIs are not supported");
				}
				if(remoteContexts.contains(c)){
					throw new JLDException("recursive context inclusion");
				}
				remoteContexts.add(c);
				JsonObject deref=dereferenceContext(c);
				if(deref!=null){
					result=updateContext(result, deref.get("@context"), remoteContexts, baseURI);
				}else{
					System.err.println("Failed to dereference "+c);
				}

				continue;
			}
			if(!context.isJsonObject()){
				throw new JLDException("invalid local context");
			}
			JsonObject c=context.getAsJsonObject();
			if(c.has("@base")){
				JsonElement value=c.get("@base");
				if(value.isJsonNull()){
					result.baseIRI=null;
				}else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()){
					try{
						URI uri=new URI(value.getAsString());
						if(uri.isAbsolute()){
							result.baseIRI=uri;
						}else if(result.baseIRI!=null){
							result.baseIRI=result.baseIRI.resolve(uri);
						}else{
							throw new JLDException("invalid base IRI");
						}
					}catch(URISyntaxException x){
						throw new JLDException("invalid base IRI", x);
					}
				}else{
					throw new JLDException("invalid base IRI");
				}
			}
			if(c.has("@vocab")){
				JsonElement value=c.get("@vocab");
				if(value.isJsonNull()){
					result.vocabularyMapping=null;
				}else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()){
					String s=value.getAsString();
					if(s.contains(":")){
						result.vocabularyMapping=s;
//					}else if(result.baseIRI!=null){
//						result.vocabularyMapping=fixURI(result.baseIRI.resolve(s)).toString();
					}else{
						throw new JLDException("invalid vocab mapping");
					}
				}else{
					throw new JLDException("invalid vocab mapping");
				}
			}
			if(c.has("@language")){
				JsonElement value=c.get("@language");
				if(value.isJsonNull() || (value.isJsonPrimitive() && "und".equals(value.getAsString()))){
					result.defaultLanguage=null;
				}else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()){
					result.defaultLanguage=value.getAsString().toLowerCase();
				}else{
					throw new JLDException("invalid default language");
				}
			}

			for(String k:c.keySet()){
				if(k.equals("@base") || k.equals("@vocab") || k.equals("@language"))
					continue;
				createTermDefinition(result, c, k, new HashMap<>());
			}
		}

		return result;
	}

	private static void createTermDefinition(JLDContext activeContext, JsonObject localContext, String term, HashMap<String, Boolean> defined){
		if(defined.containsKey(term)){
			if(defined.get(term))
				return;
			else
				throw new JLDException("cyclic IRI mapping");
		}
		defined.put(term, false);
		if(isKeyword(term))
			throw new JLDException("keyword redefinition");
		activeContext.termDefinitions.remove(term);
		JsonElement value=localContext.get(term);
		if(value.isJsonNull()) value=null;
		if(value==null || (value.isJsonObject() && value.getAsJsonObject().has("@id") && value.getAsJsonObject().get("@id").isJsonNull())){
			activeContext.termDefinitions.put(term, null);
			defined.put(term, true);
			return;
		}
		if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()){
			JsonObject j=new JsonObject();
			j.add("@id", value);
			value=j;
		}
		if(!value.isJsonObject() || term.isEmpty()){
			throw new JLDException("invalid term definition");
		}
		JLDContext.TermDefinition definition=new JLDContext.TermDefinition();
		JsonObject v=value.getAsJsonObject();
		if(v.has("@type")){
			try{
				String type=v.get("@type").getAsString();
				type=expandIRI(activeContext, type, false, true, localContext, defined);
				if(!"@id".equals(type) && !"@vocab".equals(type)){
					URI uri=new URI(type);
					if(!uri.isAbsolute())
						throw new JLDException("invalid type mapping");
				}
				definition.typeMapping=type;
			}catch(IllegalStateException|URISyntaxException x){
				throw new JLDException("invalid type mapping", x);
			}
		}
		if(v.has("@reverse")){
			if(v.has("@id"))
				throw new JLDException("invalid reverse property");
			try{
				String reverse=v.get("@reverse").getAsString();
				definition.iriMapping=expandIRI(activeContext, reverse, false, true, localContext, defined);
				if(!definition.iriMapping.contains(":"))
					throw new JLDException("invalid IRI mapping");
				if(v.has("@container")){
					definition.containerMapping=v.get("@container").getAsString();
					if(definition.containerMapping!=null && !definition.containerMapping.equals("@set") && !definition.containerMapping.equals("@index")){
						throw new JLDException("invalid reverse property");
					}
				}
				definition.reverseProperty=true;
				activeContext.termDefinitions.put(term, definition);
				defined.put(term, true);
			}catch(IllegalStateException x){
				throw new JLDException("invalid reverse property");
			}
			return;
		}
		definition.reverseProperty=false;
		if(v.has("@id") && !term.equals(v.get("@id").getAsString())){
//			try{
				definition.iriMapping=expandIRI(activeContext, v.get("@id").getAsString(), false, true, localContext, defined);
				if(!isKeyword(definition.iriMapping) && !definition.iriMapping.contains(":"))
					throw new JLDException("invalid IRI mapping");
				if("@context".equals(definition.iriMapping))
					throw new JLDException("invalid keyword mapping");
//			}catch(JSONException x){
//				throw new JLDException("invalid IRI mapping", x);
//			}
		}else if(term.contains(":")){
			String[] sp=term.split(":", 2);
			String prefix=sp[0];
			String suffix=sp[1];
			if(localContext.has(prefix)){
				createTermDefinition(activeContext, localContext, prefix, defined);
			}
			if(activeContext.termDefinitions.containsKey(prefix))
				definition.iriMapping=activeContext.termDefinitions.get(prefix).iriMapping+suffix;
			else
				definition.iriMapping=term;
		}else if(activeContext.vocabularyMapping!=null){
			definition.iriMapping=activeContext.vocabularyMapping+term;
		}else{
			throw new JLDException("invalid IRI mapping");
		}

		if(v.has("@container")){
			String container=v.get("@container").getAsString();
			if(container!=null && !"@list".equals(container) && !"@set".equals(container) && !"@index".equals(container) && !"@language".equals(container))
				throw new JLDException("invalid container mapping");
			definition.containerMapping=container;
		}

		if(v.has("@language")){
			JsonElement _language=v.get("@language");
			if(_language.isJsonNull()) _language=null;
			if(_language!=null && !(_language.isJsonPrimitive() && _language.getAsJsonPrimitive().isString()))
				throw new JLDException("invalid language mapping");
			definition.languageMapping=_language==null ? null : _language.getAsString().toLowerCase();
			definition.hasLanguageMapping=true;
		}

		activeContext.termDefinitions.put(term, definition);
		defined.put(term, true);
	}

	private static URI fixURI(URI uri){
		try{
			String path=uri.getPath().replace("../", "").replace("./", "");//.replaceAll("(?<!^)/\\./", "/");
			if(!path.startsWith("/"))
				path="/"+path;
			return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
		}catch(URISyntaxException e){
			throw new IllegalArgumentException(e);
		}
	}

	private static String expandIRI(JLDContext activeContext, String value, boolean documentRelative, boolean vocab, JsonObject localContext, HashMap<String, Boolean> defined){
		if(value==null || isKeyword(value))
			return value;
		if(localContext!=null && localContext.has(value) && (!defined.containsKey(value) || !defined.get(value))){
			createTermDefinition(activeContext, localContext, value, defined);
		}
		if(vocab && activeContext.termDefinitions.containsKey(value)){
			JLDContext.TermDefinition def=activeContext.termDefinitions.get(value);
			if(def!=null)
				return def.iriMapping;
			else
				return null;
		}
		if(value.contains(":") && !value.startsWith("#")){
			String[] sp=value.split(":", 2);
			String prefix=sp[0];
			String suffix=sp[1];
			if("_".equals(prefix) || suffix.startsWith("//")){
				return value;
			}
			if(localContext!=null && localContext.has(prefix) && (!defined.containsKey(prefix) || !defined.get(prefix))){
				createTermDefinition(activeContext, localContext, prefix, defined);
			}
			if(activeContext.termDefinitions.containsKey(prefix)){
				return activeContext.termDefinitions.get(prefix).iriMapping+suffix;
			}
			return value;
		}
		if(vocab && activeContext.vocabularyMapping!=null)
			return activeContext.vocabularyMapping+value;
		if(documentRelative && activeContext.baseIRI!=null){
			if(value.isEmpty())
				return activeContext.baseIRI.toString();
			if(URI.create(value).isAbsolute())
				return value;
			try{
				if(value.startsWith("?")){
					URI b=activeContext.baseIRI;
					return new URI(b.getScheme(), b.getAuthority(), b.getPath(), value.substring(1), null).toString();
				}
			}catch(URISyntaxException ignore){}
			if(value.startsWith("#"))
				return activeContext.baseIRI.resolve(value).toString();
			else if(value.startsWith("//"))
				return fixURI(URI.create(activeContext.baseIRI.getScheme()+":"+value).normalize()).toString();
			else
				return fixURI(activeContext.baseIRI.resolve(value)).toString(); // URI.resolve leaves /../ parts that go beyond root
		}
		return value;
	}

	private static boolean isKeyword(String key){
		return switch(key){
			case "@context", "@id", "@value", "@language", "@type", "@container", "@list", "@set", "@reverse", "@index", "@base", "@vocab", "@graph" -> true;
			default -> false;
		};
	}

	private static JsonArray makeArray(Object... items){
		JsonArray arr=new JsonArray(items.length);
		for(Object item:items){
			if(item instanceof JsonElement jsonElement)
				arr.add(jsonElement);
			else if(item instanceof String str)
				arr.add(str);
			else if(item instanceof Number num)
				arr.add(num);
			else if(item instanceof Boolean b)
				arr.add(b);
			else
				throw new IllegalArgumentException("Unsupported type "+item.getClass().getName());
		}
		return arr;
	}

	private static JsonObject jsonObjectWithSingleKey(String key, JsonElement value){
		JsonObject obj=new JsonObject();
		obj.add(key, value);
		return obj;
	}

	private static JsonElement expand(JLDContext activeContext, String activeProperty, JsonElement element){
		if(element==null || element.isJsonNull())
			return null;
		if(element.isJsonPrimitive()){
			if(activeProperty==null || "@graph".equals(activeProperty))
				return null;
			return expandValue(activeContext, activeProperty, element);
		}
		if(element.isJsonArray()){
			JsonArray result=new JsonArray();
			JsonArray el=element.getAsJsonArray();
			for(JsonElement item:el){
				JsonElement expandedItem=expand(activeContext, activeProperty, item);
				if("@list".equals(activeProperty) || (activeContext.termDefinitions.containsKey(activeProperty) && "@list".equals(activeContext.termDefinitions.get(activeProperty).containerMapping))){
					if(expandedItem.isJsonArray() || isListObject(expandedItem))
						throw new JLDException("list of lists");
				}
				if(expandedItem!=null && expandedItem.isJsonArray()){
					JsonArray xe=expandedItem.getAsJsonArray();
					for(JsonElement xitem:xe){
						result.add(xitem);
					}
				}else if(expandedItem!=null){
					result.add(expandedItem);
				}
			}
			return result;
		}
		if(!element.isJsonObject())
			throw new JLDException("JsonObject expected here, found: "+element.getClass().getName());
		JsonObject el=element.getAsJsonObject();
		if(el.has("@context")){
			activeContext=updateContext(activeContext, el.get("@context").isJsonNull() ? null : el.get("@context"), new ArrayList<>(), activeContext.originalBaseIRI);
		}
		JsonObject result=new JsonObject();
		ArrayList<String> keys=new ArrayList<>(el.keySet());
		Collections.sort(keys);
		for(String key:keys){
			JsonElement value=el.get(key);
			if(value.isJsonNull())
				value=null;
			JsonElement expandedValue=null;
			if("@context".equals(key))
				continue;

			String expandedProperty=expandIRI(activeContext, key, false, true, null, null);
			if(expandedProperty==null || (!expandedProperty.contains(":") && !isKeyword(expandedProperty)))
				continue;

			boolean valueIsString=value!=null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();

			if(isKeyword(expandedProperty)){
				if("@reverse".equals(activeProperty))
					throw new JLDException("invalid reverse property map");
				if(result.has(expandedProperty))
					throw new JLDException("colliding keywords");
				if("@id".equals(expandedProperty)){
					if(!valueIsString)
						throw new JLDException("invalid @id value");
					expandedValue=new JsonPrimitive(expandIRI(activeContext, value.getAsString(), true, false, null, null));
				}else if("@type".equals(expandedProperty)){
					if(value!=null && value.isJsonArray()){
						JsonArray expTypes=new JsonArray();
						for(JsonElement e:value.getAsJsonArray()){
							if(!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()){
								throw new JLDException("invalid type value");
							}
							expTypes.add(expandIRI(activeContext, e.getAsString(), true, true, null, null));
						}
						expandedValue=expTypes;
					}else if(valueIsString){
						expandedValue=new JsonPrimitive(expandIRI(activeContext, value.getAsString(), true, true, null, null));
					}else{
						throw new JLDException("invalid type value");
					}
				}else if("@graph".equals(expandedProperty)){
					expandedValue=expand(activeContext, "@graph", value);
				}else if("@value".equals(expandedProperty)){
					if(value!=null && !value.isJsonPrimitive())
						throw new JLDException("invalid value object value");
					expandedValue=value;
					if(expandedValue==null){
						result.add("@value", JsonNull.INSTANCE);
						continue;
					}
				}else if("@language".equals(expandedProperty)){
					if(!valueIsString)
						throw new JLDException("invalid language-tagged string");
					expandedValue=new JsonPrimitive(value.getAsString().toLowerCase());
				}else if("@index".equals(expandedProperty)){
					if(!(valueIsString))
						throw new JLDException("invalid @index value");
					expandedValue=value;
				}else if("@list".equals(expandedProperty)){
					if(activeProperty==null || "@graph".equals(activeProperty))
						continue;
					expandedValue=expand(activeContext, activeProperty, value);
					if(expandedValue!=null && (isListObject(expandedValue) || (expandedValue.isJsonArray() && expandedValue.getAsJsonArray().size()>0 && isListObject(expandedValue.getAsJsonArray().get(0)))))
						throw new JLDException("list of lists");
				}else if("@set".equals(expandedProperty)){
					expandedValue=expand(activeContext, activeProperty, value);
				}else if("@reverse".equals(expandedProperty)){
					if(!value.isJsonObject())
						throw new JLDException("invalid @reverse value");
					expandedValue=expand(activeContext, "@reverse", value);
					if(expandedValue!=null && expandedValue.isJsonObject()){
						JsonObject jv=expandedValue.getAsJsonObject();
						if(jv.has("@reverse")){
							JsonObject rev=jv.get("@reverse").getAsJsonObject();
							for(String property:rev.keySet()){
								JsonElement item=rev.get(property);
								if(!result.has(property))
									result.add(property, new JsonArray());
								// spec does not say this but tests do expect this, so...
								if(item.isJsonArray()){
									JsonArray aitem=item.getAsJsonArray();
									for(JsonElement aaitem:aitem){
										result.get(property).getAsJsonArray().add(aaitem);
									}
								}else{
									result.get(property).getAsJsonArray().add(item);
								}
							}
							jv.remove("@reverse");
						}
						if(jv.size()>0){
							if(!result.has("@reverse"))
								result.add("@reverse", new JsonObject());
							JsonObject reverseMap=result.get("@reverse").getAsJsonObject();
							for(String property:jv.keySet()){
								JsonArray items=jv.get(property).getAsJsonArray();
								for(JsonElement item:items){
									if(isListObject(item) || isValueObject(item))
										throw new JLDException("invalid reverse property value");
									if(!reverseMap.has(property))
										reverseMap.add(property, new JsonArray());
									reverseMap.get(property).getAsJsonArray().add(item);
								}
							}
						}
					}
					continue;
				}

				if(expandedValue!=null){
					result.add(expandedProperty, expandedValue);
				}
				continue;
			}

			JLDContext.TermDefinition term=activeContext.termDefinitions.get(key);

			if(term!=null && "@language".equals(term.containerMapping) && value!=null && value.isJsonObject()){
				JsonArray exp=new JsonArray();
				JsonObject objval=value.getAsJsonObject();
				ArrayList<String> objkeys=new ArrayList<>(objval.keySet());
				Collections.sort(objkeys);
				for(String lang:objkeys){
					JsonElement langValue=objval.get(lang);
					if(!langValue.isJsonArray()){
						langValue=makeArray(langValue);
					}
					JsonArray langValueArr=langValue.getAsJsonArray();
					for(JsonElement item:langValueArr){
						if(!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString())
							throw new JLDException("invalid language map value");
						JsonObject r=jsonObjectWithSingleKey("@value", item);
						r.addProperty("@language", lang.toLowerCase());
						exp.add(r);
					}
				}
				expandedValue=exp;
			}else if(term!=null && "@index".equals(term.containerMapping) && value!=null && value.isJsonObject()){
				JsonArray xv=new JsonArray();
				JsonObject v=value.getAsJsonObject();
				ArrayList<String> vkeys=new ArrayList<>(v.keySet());
				Collections.sort(vkeys);
				for(String index:vkeys){
					JsonElement indexValue=v.get(index);
					if(!indexValue.isJsonArray()){
						indexValue=makeArray(indexValue);
					}
					indexValue=expand(activeContext, key, indexValue);
					JsonArray ival=indexValue.getAsJsonArray();
					for(JsonElement _item:ival){
						JsonObject item=_item.getAsJsonObject();
						if(!item.has("@index"))
							item.addProperty("@index", index);
						xv.add(item);
					}
				}
				expandedValue=xv;
			}else{
				expandedValue=expand(activeContext, key, value);
			}
			if(expandedValue==null)
				continue;
			if(term!=null && "@list".equals(term.containerMapping) && !isListObject(expandedValue)){
				if(!expandedValue.isJsonArray()){
					expandedValue=makeArray(expandedValue);
				}
				expandedValue=jsonObjectWithSingleKey("@list", expandedValue);
			}
			if(isListObject(expandedValue) && !expandedValue.getAsJsonObject().get("@list").isJsonArray()){
				JsonArray ja=new JsonArray();
				ja.add(expandedValue.getAsJsonObject().get("@list"));
				expandedValue=jsonObjectWithSingleKey("@list", ja);
			}
			if(term!=null && term.reverseProperty){
				if(!result.has("@reverse"))
					result.add("@reverse", new JsonObject());
				JsonObject reverseMap= result.get("@reverse").getAsJsonObject();
				if(!(expandedValue instanceof JsonArray))
					expandedValue=makeArray(expandedValue);
				JsonArray xv=(JsonArray) expandedValue;
				for(JsonElement item:xv){
					if(isListObject(item) || isValueObject(item))
						throw new JLDException("invalid reverse property value");
					if(!reverseMap.has(expandedProperty))
						reverseMap.add(expandedProperty, new JsonArray());
					reverseMap.get(expandedProperty).getAsJsonArray().add(item);
				}
			}else{
				if(!result.has(expandedProperty))
					result.add(expandedProperty, new JsonArray());
				if(expandedValue instanceof JsonArray){
					JsonArray prop=result.get(expandedProperty).getAsJsonArray();
					for(JsonElement item:expandedValue.getAsJsonArray()){
						prop.add(item);
					}
				}else{
					result.get(expandedProperty).getAsJsonArray().add(expandedValue);
				}
			}
		}

		if(result.has("@value")){
			for(String k:result.keySet()){
				if(!"@value".equals(k) && !"@language".equals(k) && !"@type".equals(k) && !"@index".equals(k))
					throw new JLDException("invalid value object");
			}
			if(result.has("@language") && result.has("@type"))
				throw new JLDException("invalid value object");
			if(result.get("@value").isJsonNull()){
				result=null;
			}else if(!(result.get("@value").isJsonPrimitive() && result.get("@value").getAsJsonPrimitive().isString()) && result.has("@language")){
				throw new JLDException("invalid language-tagged value");
			}else if(result.has("@type")){
				try{
					new URI(result.get("@type").getAsString());
				}catch(URISyntaxException|IllegalStateException x){
					throw new JLDException("invalid typed value", x);
				}
			}
		}else if(result.has("@type") && !result.get("@type").isJsonArray()){
			result.add("@type", makeArray(result.get("@type")));
		}else if(result.has("@set") || result.has("@list")){
			if(result.size()>2 || (result.size()>1 && !result.has("@index"))){
				throw new JLDException("invalid set or list object");
			}
			if(result.has("@set"))
				return result.get("@set");
		}

		if(result!=null && result.size()==1 && result.has("@language"))
			result=null;

		if(activeProperty==null || "@graph".equals(activeProperty)){
			if(result!=null && (result.size()==0 || result.has("@value") || result.has("@list")))
				result=null;
			else if(result!=null && result.size()==1 && result.has("@id"))
				result=null;
		}

		return result;
	}

	private static JsonObject expandValue(JLDContext activeContext, String activeProperty, JsonElement value){
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		if(term!=null && "@id".equals(term.typeMapping)){
			// the algorithm spec was clearly written without strongly-typed languages in mind. Sigh.
			if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
				return jsonObjectWithSingleKey("@id", new JsonPrimitive(expandIRI(activeContext, value.getAsString(), true, false, null, null)));
			return jsonObjectWithSingleKey("@value", value);
		}
		if(term!=null && "@vocab".equals(term.typeMapping)){
			if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString())
				return jsonObjectWithSingleKey("@id", new JsonPrimitive(expandIRI(activeContext, value.getAsString(), true, true, null, null)));
			return jsonObjectWithSingleKey("@value", value);
		}
		JsonObject result=jsonObjectWithSingleKey("@value", value);
		if(term!=null && term.typeMapping!=null)
			result.addProperty("@type", term.typeMapping);
		else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()){
			if(term!=null && term.hasLanguageMapping){
				if(term.languageMapping!=null)
					result.addProperty("@language", term.languageMapping);
			}else if(activeContext.defaultLanguage!=null){
				result.addProperty("@language", activeContext.defaultLanguage);
			}
		}
		return result;
	}

	private static JsonObject createReverseContext(JLDContext activeContext){
		// 1) Initialize result to an empty JSON object.
		JsonObject result=new JsonObject();
		// 2) Initialize default language to @none. If the active context has a default language, set default language to it.
		String defaultLanguage="@none";
		if(activeContext.defaultLanguage!=null)
			defaultLanguage=activeContext.defaultLanguage;
		// 3) For each key term and value term definition in the active context, ordered by shortest term first (breaking ties by choosing the lexicographically least term):
		ArrayList<String> keys=new ArrayList<>(activeContext.termDefinitions.keySet());
		keys.sort(SHORTEST_LEAST);
		for(String term:keys){
			JLDContext.TermDefinition termDefinition=activeContext.termDefinitions.get(term);
			// 3.1) If the term definition is null, term cannot be selected during compaction, so continue to the next term.
			if(termDefinition==null)
				continue;
			// 3.2) Initialize container to @none. If there is a container mapping in term definition, set container to its associated value.
			String container="@none";
			if(termDefinition.containerMapping!=null)
				container=termDefinition.containerMapping;
			// 3.3) Initialize iri to the value of the IRI mapping for the term definition.
			String iri=termDefinition.iriMapping;
			// 3.4) If iri is not a key in result, add a key-value pair where the key is iri and the value is an empty JSON object to result.
			if(!result.has(iri)){
				result.add(iri, new JsonObject());
			}
			// 3.5) Reference the value associated with the iri member in result using the variable container map.
			JsonObject containerMap=result.get(iri).getAsJsonObject();
			// 3.6) If container map has no container member, create one and set its value to a new JSON object with two members.
			// The first member is @language and its value is a new empty JSON object, the second member is @type and its value is a new empty JSON object.
			if(!containerMap.has(container)){
				JsonObject o=new JsonObject();
				o.add("@language", new JsonObject());
				o.add("@type", new JsonObject());
//				o.put("@any", term);
				containerMap.add(container, o);
			}
			// 3.7) Reference the value associated with the container member in container map using the variable type/language map.
			JsonObject typeLanguageMap=containerMap.get(container).getAsJsonObject();
			// 3.8) If the term definition indicates that the term represents a reverse property:
			if(termDefinition.reverseProperty){
				// 3.8.1) Reference the value associated with the @type member in type/language map using the variable type map.
				JsonObject typeMap=typeLanguageMap.get("@type").getAsJsonObject();
				// 3.8.2) If type map does not have a @reverse member, create one and set its value to the term being processed.
				if(!typeMap.has("@reverse"))
					typeMap.addProperty("@reverse", term);

				// 3.9) Otherwise, if term definition has a type mapping:
			}else if(termDefinition.typeMapping!=null){
				// 3.9.1) Reference the value associated with the @type member in type/language map using the variable type map.
				JsonObject typeMap=typeLanguageMap.get("@type").getAsJsonObject();
				// 3.9.2) If type map does not have a member corresponding to the type mapping in term definition, create one and set its value to the term being processed.
				if(!typeMap.has(termDefinition.typeMapping))
					typeMap.addProperty(termDefinition.typeMapping, term);

				// 3.10) Otherwise, if term definition has a language mapping (might be null):
			}else if(termDefinition.hasLanguageMapping){
				// 3.10.1) Reference the value associated with the @language member in type/language map using the variable language map.
				JsonObject languageMap=typeLanguageMap.get("@language").getAsJsonObject();
				// 3.10.2) If the language mapping equals null, set language to @null; otherwise set it to the language code in language mapping.
				String language=termDefinition.languageMapping==null ? "@null" : termDefinition.languageMapping;
				// 3.10.3) If language map does not have a language member, create one and set its value to the term being processed.
				if(!languageMap.has(language))
					languageMap.addProperty(language, term);

				// 3.11) Otherwise:
			}else{
				// 3.11.1) Reference the value associated with the @language member in type/language map using the variable language map.
				JsonObject languageMap=typeLanguageMap.get("@language").getAsJsonObject();
				// 3.11.2) If language map does not have a default language member, create one and set its value to the term being processed.
				if(!languageMap.has(defaultLanguage))
					languageMap.addProperty(defaultLanguage, term);
				// 3.11.3) If language map does not have a @none member, create one and set its value to the term being processed.
				if(!languageMap.has("@none"))
					languageMap.addProperty("@none", term);
				// 3.11.4) Reference the value associated with the @type member in type/language map using the variable type map.
				JsonObject typeMap=typeLanguageMap.get("@type").getAsJsonObject();
				// 3.11.5) If type map does not have a @none member, create one and set its value to the term being processed.
				if(!typeMap.has("@none"))
					typeMap.addProperty("@none", term);
			}
		}
		// 4) Return result.
		return result;
	}

	private static boolean isListObject(JsonElement o){
		return o!=null && o.isJsonObject() && o.getAsJsonObject().has("@list");
	}

	private static boolean isValueObject(JsonElement o){
		return o!=null && o.isJsonObject() && o.getAsJsonObject().has("@value");
	}

	private static boolean isNodeObject(JsonObject o){
		return !o.has("@value") && !o.has("@list") && !o.has("@set");
	}

	private static String selectTerm(JsonObject inverseContext, String iri, ArrayList<String> containers, String typeLanguage, ArrayList<String> preferredValues){
		// 1. Initialize container map to the value associated with iri in the inverse context.
		JsonObject containerMap=inverseContext.get(iri).getAsJsonObject();
		// 2. For each item container in containers:
		for(String container:containers){
			// 2.1. If container is not a key in container map, then there is no term with a matching container mapping for it, so continue to the next container.
			if(!containerMap.has(container))
				continue;
			// 2.2. Initialize type/language map to the value associated with the container member in container map.
			JsonObject typeLanguageMap=containerMap.get(container).getAsJsonObject();
			// 2.3. Initialize value map to the value associated with type/language member in type/language map.
			JsonObject valueMap=typeLanguageMap.get(typeLanguage).getAsJsonObject();
			// 2.4. For each item in preferred values:
			for(String item:preferredValues){
				// 2.4.1. If item is not a key in value map, then there is no term with a matching type mapping or language mapping, so continue to the next item.
				if(!valueMap.has(item))
					continue;
				// 2.4.2. Otherwise, a matching term has been found, return the value associated with the item member in value map.
				return valueMap.get(item).getAsString();
			}
		}
		// 3. No matching term has been found. Return null.
		return null;
	}

	private static String relativizePath(String target, String base){
		String[] baseComponents=base.split("/");
		String[] targetComponents=target.split("/");

		int index=0;
		for (;index<targetComponents.length && index<baseComponents.length;++index){
			if(!targetComponents[index].equals(baseComponents[index]))
				break;
		}

		StringBuilder result=new StringBuilder();
		if(index!=baseComponents.length){
			// backtrack to base directory
			result.append("../".repeat(Math.max(0, baseComponents.length-index-1)));
		}
		for(;index<targetComponents.length;++index){
			result.append(targetComponents[index]);
			result.append('/');
		}
		if(!target.endsWith("/")){
			// remove final path separator
			result.delete(result.length()-1, result.length());
		}
		return result.toString();
	}

	private static String compactIRI(JLDContext activeContext, JsonObject inverseContext, String iri){
		return compactIRI(activeContext, inverseContext, iri, null, false, false);
	}

	private static String compactIRI(JLDContext activeContext, JsonObject inverseContext, String iri, JsonElement value, boolean vocab, boolean reverse){
		// 1. If iri is null, return null.
		if(iri==null)
			return null;
		JsonObject vj=value!=null && value.isJsonObject() ? value.getAsJsonObject() : null;
		if(value!=null && value.isJsonNull())
			value=null;
		// 2. If vocab is true and iri is a key in inverse context:
		if(vocab && inverseContext.has(iri)){
			// 2.1. Initialize default language to active context's default language, if it has one, otherwise to @none.
			String defaultLanguage=activeContext.defaultLanguage!=null ? activeContext.defaultLanguage : "@none";
			// 2.2. Initialize containers to an empty array. This array will be used to keep track of an ordered list of
			// preferred container mappings for a term, based on what is compatible with value.
			ArrayList<String> containers=new ArrayList<>();
			// 2.3. Initialize type/language to @language, and type/language value to @null. These two variables will
			// keep track of the preferred type mapping or language mapping for a term, based on what is compatible with value.
			String typeLanguage="@language";
			String typeLanguageValue="@null";
			// 2.4. If value is a JSON object that contains the key @index, then append the value @index to containers.
			if(vj!=null && vj.has("@index")){
				containers.add("@index");
			}
			// 2.5. If reverse is true, set type/language to @type, type/language value to @reverse, and append @set to containers.
			if(reverse){
				typeLanguage="@type";
				typeLanguageValue="@reverse";
				containers.add("@set");
			// 2.6. Otherwise, if value is a list object, then set type/language and type/language value to the most specific
			// values that work for all items in the list as follows:
			}else if(isListObject(value)){
				// 2.6.1. If @index is a not key in value, then append @list to containers.
				if(!vj.has("@index")){
					containers.add("@list");
				}
				// 2.6.2. Initialize list to the array associated with the key @list in value.
				JsonArray list=vj.get("@list").getAsJsonArray();
				// 2.6.3.Initialize common type and common language to null. If list is empty, set common language to default language.
				String commonType=null, commonLanguage=null;
				if(list.size()==0){
					commonLanguage=defaultLanguage;
					for(Map.Entry<String, JLDContext.TermDefinition> def:activeContext.termDefinitions.entrySet()){
						JLDContext.TermDefinition td=def.getValue();
						if(iri.equals(td.iriMapping)){
							commonType=td.typeMapping;
							break;
						}
					}
				}
				// 2.6.4. For each item in list:
				for(int i=0;i<list.size();i++){
					JsonObject item=list.get(i).getAsJsonObject();
					// 2.6.4.1. Initialize item language to @none and item type to @none.
					String itemLanguage="@none", itemType="@none";
					// 2.6.4.2. If item contains the key @value:
					if(item.has("@value")){
						// 2.6.4.2.1. If item contains the key @language, then set item language to its associated value.
						if(item.has("@language"))
							itemLanguage=item.get("@language").getAsString();
						// 2.6.4.2.2. Otherwise, if item contains the key @type, set item type to its associated value.
						else if(item.has("@type"))
							itemType=item.get("@type").getAsString();
						// 2.6.4.2.3. Otherwise, set item language to @null.
						else
							itemLanguage="@null";
					// 2.6.4.3. Otherwise, set item type to @id.
					}else{
						itemType="@id";
					}
					// 2.6.4.4. If common language is null, set it to item language.
					if(commonLanguage==null)
						commonLanguage=itemLanguage;
					// 2.6.4.5. Otherwise, if item language does not equal common language and item contains the key @value, then
					// set common language to @none because list items have conflicting languages.
					else if(!itemLanguage.equals(commonLanguage) && item.has("@value"))
						commonLanguage="@none";

					// 2.6.4.6. If common type is null, set it to item type.
					if(commonType==null)
						commonType=itemType;
					// 2.6.4.7.Otherwise, if item type does not equal common type, then set common type to @none because list items
					// have conflicting types.
					else if(!itemType.equals(commonType))
						commonType="@none";

					// 2.6.4.8.If common language is @none and common type is @none, then stop processing items in the list because it
					// has been detected that there is no common language or type amongst the items.
					if(commonLanguage.equals("@none") && commonType.equals("@none"))
						break;
				}

				// 2.6.5. If common language is null, set it to @none.
				if(commonLanguage==null)
					commonLanguage="@none";

				// 2.6.6. If common type is null, set it to @none.
				if(commonType==null)
					commonType="@none";

				// 2.6.7. If common type is not @none then set type/language to @type and type/language value to common type.
				if(!commonType.equals("@none")){
					typeLanguage="@type";
					typeLanguageValue=commonType;
				// 2.6.8. Otherwise, set type/language value to common language.
				}else{
					typeLanguageValue=commonLanguage;
				}
			// 2.7. Otherwise:
			}else{
				// 2.7.1. If value is a value object:
				if(isValueObject(value)){
					// 2.7.1.1. If value contains the key @language and does not contain the key @index, then set type/language
					// value to its associated value and append @language to containers.
					if(vj.has("@language") && !vj.has("@index")){
						typeLanguageValue=vj.get("@language").getAsString();
						containers.add("@language");
					// 2.7.1.2. Otherwise, if value contains the key @type, then set type/language value to its associated value
					// and set type/language to @type.
					}else if(vj.has("@type")){
						typeLanguageValue=vj.get("@type").getAsString();
						typeLanguage="@type";
					}
				// 2.7.2. Otherwise, set type/language to @type and set type/language value to @id.
				}else{
					typeLanguage="@type";
					typeLanguageValue="@id";
				}
				// 2.7.3. Append @set to containers.
				containers.add("@set");
			}
			// 2.8. Append @none to containers. This represents the non-existence of a container mapping,
			// and it will be the last container mapping value to be checked as it is the most generic.
			containers.add("@none");
			// 2.9. If type/language value is null, set it to @null. This is the key under which null values are
			// stored in the inverse context entry.
			if(typeLanguageValue==null)
				typeLanguageValue="@null";
			// 2.10. Initialize preferred values to an empty array. This array will indicate, in order, the preferred
			// values for a term's type mapping or language mapping.
			ArrayList<String> preferredValues=new ArrayList<>();
			// 2.11. If type/language value is @reverse, append @reverse to preferred values.
			if("@reverse".equals(typeLanguageValue))
				preferredValues.add("@reverse");
			// 2.12. If type/language value is @id or @reverse and value has an @id member:
			if(("@id".equals(typeLanguageValue) || "@reverse".equals(typeLanguageValue)) && vj!=null && vj.has("@id")){
				// 2.12.1. If the result of using the IRI compaction algorithm, passing active context, inverse context, the
				// value associated with the @id key in value for iri, true for vocab, and true for document relative has a
				// term definition in the active context with an IRI mapping that equals the value associated with the @id
				// key in value, then append @vocab, @id, and @none, in that order, to preferred values.
				String r=compactIRI(activeContext, inverseContext, vj.get("@id").getAsString(), null, true, false);
				JLDContext.TermDefinition td=activeContext.termDefinitions.get(r);
				if(td!=null && vj.get("@id").getAsString().equals(td.iriMapping)){
					preferredValues.add("@vocab");
					preferredValues.add("@id");
					preferredValues.add("@none");
				// 2.12.2. Otherwise, append @id, @vocab, and @none, in that order, to preferred values.
				}else{
					preferredValues.add("@id");
					preferredValues.add("@vocab");
					preferredValues.add("@none");
				}
			// 2.13. Otherwise, append type/language value and @none, in that order, to preferred values.
			}else{
				preferredValues.add(typeLanguageValue);
				preferredValues.add("@none");
			}
			// 2.14. Initialize term to the result of the Term Selection algorithm, passing inverse context, iri, containers, type/language,
			// and preferred values.
			String term=selectTerm(inverseContext, iri, containers, typeLanguage, preferredValues);
			// 2.15. If term is not null, return term.
			if(term!=null)
				return term;
		}
		// 3. At this point, there is no simple term that iri can be compacted to. If vocab is true and active
		// context has a vocabulary mapping:
		if(vocab && activeContext.vocabularyMapping!=null){
			// 3.1. If iri begins with the vocabulary mapping's value but is longer, then initialize suffix to the substring of iri that does not match.
			if(iri.startsWith(activeContext.vocabularyMapping) && iri.length()>activeContext.vocabularyMapping.length()){
				String suffix=iri.substring(activeContext.vocabularyMapping.length());
				// If suffix does not have a term definition in active context, then return suffix.
				if(!activeContext.termDefinitions.containsKey(suffix))
					return suffix;
			}
		}
		// 4. The iri could not be compacted using the active context's vocabulary mapping. Try to create
		// a compact IRI, starting by initializing compact IRI to null. This variable will be used to tore the created compact IRI, if any.
		String compactIRI=null;
		// 5. For each key term and value term definition in the active context:
		for(String term:activeContext.termDefinitions.keySet()){
			// 5.1. If the term contains a colon (:), then continue to the next term because terms with colons can't be used as prefixes.
			if(term.contains(":"))
				continue;
			JLDContext.TermDefinition termDefinition=activeContext.termDefinitions.get(term);
			// 5.2. If the term definition is null, its IRI mapping equals iri, or its IRI mapping is not a substring at the beginning of iri,
			// the term cannot be used as a prefix because it is not a partial match with iri. Continue with the next term.
			if(termDefinition==null || iri.equals(termDefinition.iriMapping) || !iri.startsWith(termDefinition.iriMapping))
				continue;
			// 5.3. Initialize candidate by concatenating term, a colon (:), and the substring of iri that follows after the value of the term
			// definition's IRI mapping.
			String candidate=term+":"+iri.substring(termDefinition.iriMapping.length());
			// 5.4. If either ( ( [compact IRI is null] or [ [candidate is shorter] or [the same length but lexicographically less than compact IRI] ] ) and
			// [candidate does not have a term definition in active context] ) or if ( [the term definition has an IRI mapping that equals iri] and [value
			// is null] ) ), set compact IRI to candidate.

			boolean isUsableCurie = (!(activeContext.termDefinitions.containsKey(candidate)) || (value == null && activeContext.termDefinitions.containsKey(candidate)
					&& iri.equals(activeContext.termDefinitions.get(candidate).iriMapping) ));

			if(isUsableCurie && (compactIRI==null || SHORTEST_LEAST.compare(candidate, compactIRI)<0)){
				compactIRI=candidate;
			}
		}
		// 6. If compact IRI is not null, return compact IRI.
		if(compactIRI!=null)
			return compactIRI;
		// 7. If vocab is false then transform iri to a relative IRI using the document's base IRI.
		if(!vocab && activeContext.baseIRI!=null && iri.contains(":")){
			URI uri=URI.create(iri);
			uri=activeContext.baseIRI.relativize(uri);
			if(activeContext.baseIRI.getAuthority().equals(uri.getAuthority()) && activeContext.baseIRI.getScheme().equals(uri.getScheme())){
				iri=relativizePath(uri.getPath(), activeContext.baseIRI.getPath())+(uri.getRawQuery()!=null ? uri.getRawQuery() : "")+(uri.getRawFragment()!=null ? ('#'+uri.getRawFragment()) : "");
			}else{
				iri=uri.toString();
			}
			if(iri.isEmpty()){
				iri=activeContext.baseIRI.getRawPath().substring(activeContext.baseIRI.getRawPath().lastIndexOf('/')+1);
			}
		}
		// 8. Finally, return iri as is.
		return iri;
	}

	private static JsonElement compactValue(JLDContext activeContext, JsonObject inverseContext, String activeProperty, JsonObject value){
		// 1. Initialize number members to the number of members value contains.
		int numberMembers=value.size();
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		// 2. If value has an @index member and the container mapping associated to active property is set to @index, decrease number members by 1.
		if(value.has("@index") && term!=null && "@index".equals(term.containerMapping))
			numberMembers--;
		// 3. If number members is greater than 2, return value as it cannot be compacted.
		if(numberMembers>2)
			return value;
		// 4. If value has an @id member:
		if(value.has("@id")){
			// 4.1. If number members is 1 and the type mapping of active property is set to @id, return the result of using the IRI compaction algorithm,
			// passing active context, inverse context, and the value of the @id member for iri.
			if(numberMembers==1 && term!=null && "@id".equals(term.typeMapping)){
				return new JsonPrimitive(compactIRI(activeContext, inverseContext, value.get("@id").getAsString(), null, false, false));
			// 4.2. Otherwise, if number members is 1 and the type mapping of active property is set to @vocab, return the result of using the IRI compaction
			// algorithm, passing active context, inverse context, the value of the @id member for iri, and true for vocab.
			}else if(numberMembers==1 && term!=null && "@vocab".equals(term.typeMapping)){
				return new JsonPrimitive(compactIRI(activeContext, inverseContext, value.get("@id").getAsString(), null, true, false));
			}
			// 4.3. Otherwise, return value as is.
			return value;
		// 5. Otherwise, if value has an @type member whose value matches the type mapping of active property, return the value associated with the @value member of value.
		}else if(value.has("@type") && term!=null && value.get("@type").getAsString().equals(term.typeMapping)){
			return value.get("@value");
		// 6. Otherwise, if value has an @language member whose value matches the language mapping of active property, return the value associated with the @value member of value.
		}else if(value.has("@language") && term!=null && value.get("@language").getAsString().equals(term.languageMapping==null ? activeContext.defaultLanguage : term.languageMapping)){
			return value.get("@value");
		// 7. Otherwise, if number members equals 1 and either the value of the @value member is not a string, or the active context has no default language,
		// or the language mapping of active property is set to null,, return the value associated with the @value member.
		}else if(numberMembers==1 && (!(value.get("@value").isJsonPrimitive() && value.get("@value").getAsJsonPrimitive().isString()) || activeContext.defaultLanguage==null || (term!=null && term.hasLanguageMapping && term.languageMapping==null))){
			return value.get("@value");
		}
		// 8. Otherwise, return value as is.
		return value;
	}

	private static JsonElement compact(JLDContext activeContext, JsonObject inverseContext, String activeProperty, JsonElement element, boolean compactArrays){
		// 1. If element is a scalar, it is already in its most compact form, so simply return element.
		if(element.isJsonPrimitive())
			return element;
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		// 2. If element is an array:
		if(element.isJsonArray()){
			JsonArray e=element.getAsJsonArray();
			// 2.1. Initialize result to an empty array.
			JsonArray result=new JsonArray();
			// 2.2. For each item in element:
			for(JsonElement item:e){
				// 2.2.1. Initialize compacted item to the result of using this algorithm recursively, passing active context, inverse context,
				// active property, and item for element.
				JsonElement compactedItem=compact(activeContext, inverseContext, activeProperty, item, compactArrays);
				// 2.2.2. If compacted item is not null, then append it to result.
				if(compactedItem!=null)
					result.add(compactedItem);
			}
			// 2.3. If result contains only one item (it has a length of 1), active property has no container
			// mapping in active context, and compactArrays is true, set result to its only item.
			if(result.size()==1 && (term==null || term.containerMapping==null) && compactArrays)
				return result.get(0);
			// 2.4. Return result.
			return result;
		}
		// 3. Otherwise element is a JSON object.
		JsonObject e=element.getAsJsonObject();
		// 4. If element has an @value or @id member and the result of using the Value Compaction algorithm, passing active context, inverse context,
		// active property, and element as value is a scalar, return that result.
		if(e.has("@value") || e.has("@id")){
			JsonElement res=compactValue(activeContext, inverseContext, activeProperty, e);
			if(res.isJsonPrimitive())
				return res;
		}
		// 5. Initialize inside reverse to true if active property equals @reverse, otherwise to false.
		boolean insideReverse="@reverse".equals(activeProperty);
		// 6. Initialize result to an empty JSON object.
		JsonObject result=new JsonObject();
		// 7. For each key expanded property and value expanded value in element, ordered lexicographically by expanded property:
		ArrayList<String> keys=new ArrayList<>(e.keySet());
		Collections.sort(keys);
		for(String expandedProperty:keys){
			JsonElement expandedValue=e.get(expandedProperty);
			if(expandedValue.isJsonNull())
				continue;
			JsonElement compactedValue;
			// 7.1. If expanded property is @id or @type:
			if(expandedProperty.equals("@id") || expandedProperty.equals("@type")){
				// 7.1.1. If expanded value is a string, then initialize compacted value to the result of using the IRI Compaction algorithm,
				// passing active context, inverse context, expanded value for iri, and true for vocab if expanded property is @type, false otherwise.
				if(expandedValue.isJsonPrimitive() && expandedValue.getAsJsonPrimitive().isString()){
					compactedValue=new JsonPrimitive(compactIRI(activeContext, inverseContext, expandedValue.getAsString(), null, expandedProperty.equals("@type"), false));
				// 7.1.2. Otherwise, expanded value must be a @type array:
				}else{
					// 7.1.2.1. Initialize compacted value to an empty array.
					JsonArray _compactedValue=new JsonArray();
					// 7.1.2.2. For each item expanded type in expanded value, append the result of of using the IRI Compaction algorithm,
					// passing active context, inverse context, expanded type for iri, and true for vocab, to compacted value.
					for(JsonElement evi:expandedValue.getAsJsonArray()){
						_compactedValue.add(compactIRI(activeContext, inverseContext, evi.getAsString(), null, true, false));
					}
					// 7.1.2.3. If compacted value contains only one item (it has a length of 1), then set compacted value to its only item.
					if(_compactedValue.size()==1)
						compactedValue=_compactedValue.get(0);
					else
						compactedValue=_compactedValue;
				}
				// 7.1.3. Initialize alias to the result of using the IRI Compaction algorithm, passing active context, inverse context, and expanded property for iri.
				String alias=compactIRI(activeContext, inverseContext, expandedProperty, null, true, false);
				// 7.1.4. Add a member alias to result whose value is set to compacted value and continue to the next expanded property.
				result.add(alias, compactedValue);
				continue;
			}
			// 7.2. If expanded property is @reverse:
			if(expandedProperty.equals("@reverse")){
				// 7.2.1. Initialize compacted value to the result of using this algorithm recursively,
				// passing active context, inverse context, @reverse for active property, and expanded value for element.
				JsonObject _compactedValue=(JsonObject) compact(activeContext, inverseContext, "@reverse", expandedValue, compactArrays);
				compactedValue=_compactedValue;
				// 7.2.2. For each property and value in compacted value:
				for(String property: _compactedValue.keySet()){
					JsonElement value=_compactedValue.get(property);
					// 7.2.2.1. If the term definition for property in the active context indicates that property is a reverse property
					JLDContext.TermDefinition pterm=activeContext.termDefinitions.get(property);
					if(pterm!=null && pterm.reverseProperty){
						// 7.2.2.1.1. If compactArrays is false and value is not an array, set value to a new array containing only value.
						if(!compactArrays && !value.isJsonArray())
							value=makeArray(value);

						// 7.2.2.1.2. If property is not a member of result, add one and set its value to value.
						if(!result.has(property)){
							result.add(property, value);
						// 7.2.2.1.3. Otherwise, if the value of the property member of result is not an array, set it to a new array containing only the value.
						// Then append value to its value if value is not an array, otherwise append each of its items.
						}else{
							JsonElement val=result.get(property);
							JsonArray valArr;
							if(val.isJsonArray()){
								valArr=val.getAsJsonArray();
							}else{
								valArr=makeArray(val);
								result.add(property, valArr);
							}
							if(value.isJsonArray()){
								for(JsonElement item:value.getAsJsonArray()){
									valArr.add(item);
								}
							}else{
								valArr.add(value);
							}
						}
						// 7.2.2.1.4. Remove the property member from compacted value.
						_compactedValue.remove(property);
					}
				}
				// 7.2.3. If compacted value has some remaining members, i.e., it is not an empty JSON object:
				if(_compactedValue.size()>0){
					// 7.2.3.1. Initialize alias to the result of using the IRI Compaction algorithm, passing active context, inverse context, and @reverse for iri.
					String alias=compactIRI(activeContext, inverseContext, "@reverse");
					// 7.2.3.2. Set the value of the alias member of result to compacted value and continue with the next expanded property from element.
					result.add(alias, _compactedValue);
				}
				continue;
			}
			// 7.3. If expanded property is @index and active property has a container mapping in active context that is @index, then the compacted result will be
			// inside of an @index container, drop the @index property by continuing to the next expanded property.
			if(expandedProperty.equals("@index") && term!=null && "@index".equals(term.containerMapping))
				continue;

			// 7.4. Otherwise, if expanded property is @index, @value, or @language:
			if(expandedProperty.equals("@index") || expandedProperty.equals("@value") || expandedProperty.equals("@language")){
				// 7.4.1. Initialize alias to the result of using the IRI Compaction algorithm, passing active
				// context, inverse context, and expanded property for iri.
				String alias=compactIRI(activeContext, inverseContext, expandedProperty);
				// 7.4.2.Add a member alias to result whose value is set to expanded value and continue with
				// the next expanded property.
				result.add(alias, expandedValue);
				continue;
			}
			// 7.5. If expanded value is an empty array:
			if(expandedValue.isJsonArray() && expandedValue.getAsJsonArray().size()==0){
				// 7.5.1.Initialize item active property to the result of using the IRI Compaction algorithm, passing active context, inverse context, expanded property for iri, expanded
				// value for value, true for vocab, and inside reverse.
				String itemActiveProperty=compactIRI(activeContext, inverseContext, expandedProperty, expandedValue, true, insideReverse);
				// 7.5.2.If result does not have the key that equals item active property, set this key's value	in result to an empty array.
				// Otherwise, if the key's value is not an array, then set it to one containing only the value.
				if(!result.has(itemActiveProperty)){
					result.add(itemActiveProperty, new JsonArray());
				}else if(!(result.get(itemActiveProperty) instanceof JsonArray)){
					result.add(itemActiveProperty, makeArray(result.get(itemActiveProperty).getAsString()));
				}
			}
			// 7.6. At this point, expanded value must be an array due to the Expansion algorithm. For each item expanded item in expanded value:
			JsonArray ev=expandedValue.getAsJsonArray();
			for(JsonElement expandedItem:ev){
				// 7.6.1. Initialize item active property to the result of using the IRI Compaction algorithm, passing active context, inverse context, expanded property for iri, expanded
				// item for value, true for vocab, and inside reverse.
				String itemActiveProperty=compactIRI(activeContext, inverseContext, expandedProperty, expandedItem, true, insideReverse);
				// 7.6.2. Initialize container to null. If there is a container mapping for item active property in active context, set container to its value.
				String container=null;
				JLDContext.TermDefinition activeTerm=activeContext.termDefinitions.get(itemActiveProperty);
				if(activeTerm!=null && activeTerm.containerMapping!=null){
					container=activeTerm.containerMapping;
				}
				// 7.6.3.Initialize compacted item to the result of using this algorithm recursively, passing active context, inverse context, item active property for active
				// property, expanded item for element if it does not contain the key @list, otherwise pass the key's associated value for element.
				JsonElement compactedItem=compact(activeContext, inverseContext, itemActiveProperty, expandedItem.isJsonObject() && expandedItem.getAsJsonObject().has("@list") ? expandedItem.getAsJsonObject().get("@list").getAsJsonArray() : expandedItem, compactArrays);
				// 7.6.4. If expanded item is a list object:
				if(isListObject(expandedItem)){
					// 7.6.4.1. If compacted item is not an array, then set it to an array containing only compacted item.
					if(!compactedItem.isJsonArray()){
						compactedItem=makeArray(compactedItem);
					}
					JsonObject _expandedItem=expandedItem.getAsJsonObject();
					// 7.6.4.2. If container is not @list:
					if(!"@list".equals(container)){
						// 7.6.4.2.1. Convert compacted item to a list object by setting it to a JSON object containing key-value pair where the key is the result of
						// the IRI Compaction algorithm, passing active context, inverse context, @list for iri, and compacted item for value.
						compactedItem=jsonObjectWithSingleKey(compactIRI(activeContext, inverseContext, "@list", compactedItem, /*false*/true, false), compactedItem);
						// 7.6.4.2.2. If expanded item contains the key @index, then add a key-value pair to compacted item where the key is the result of the IRI
						// Compaction algorithm, passing active context, inverse context, @index as iri, and the value associated with the @index key in expanded item as value.
						if(_expandedItem.has("@index")){
							compactedItem.getAsJsonObject().add(compactIRI(activeContext, inverseContext, "@index", _expandedItem.get("@index"), /*false*/true, false), _expandedItem.get("@index"));
						}
					// 7.6.4.3. Otherwise, item active property must not be a key in result because there cannot be two list objects associated with an active property that has
					// a container mapping; a compaction to list of lists error has been detected and processing is aborted.
					}else if(result.has(itemActiveProperty)){
						throw new JLDException("compaction to list of lists");
					}
				}
				// 7.6.5. If container is @language or @index:
				if("@language".equals(container) || "@index".equals(container)){
					// 7.6.5.1. If item active property is a key in result, then initialize map object to its
					// associated value, otherwise initialize it to an empty JSON object.
					if(!result.has(itemActiveProperty))
						result.add(itemActiveProperty, new JsonObject());
					JsonObject mapObject=result.get(itemActiveProperty).getAsJsonObject();
					// 7.6.5.2. If container is @language and compacted item contains the key @value, then
					// set compacted item to the value associated with its @value key.
					if("@language".equals(container) && compactedItem.isJsonObject() && compactedItem.getAsJsonObject().has("@value")){
						compactedItem=compactedItem.getAsJsonObject().get("@value");
					}
					// 7.6.5.3. Initialize map key to the value associated with with the key that equals container in expanded item.
					String mapKey=expandedItem.getAsJsonObject().get(container).getAsString();
					// 7.6.5.4. If map key is not a key in map object, then set this key's value in map object to compacted item.
					// Otherwise, if the value is not an array, then set it to one containing only the value and then append compacted item to it.
					if(!mapObject.has(mapKey)){
						mapObject.add(mapKey, compactedItem);
					}else{
						JsonElement o=mapObject.get(mapKey);
						if(!o.isJsonArray())
							mapObject.add(mapKey, makeArray(mapObject.get(mapKey), compactedItem));
						else
							o.getAsJsonArray().add(compactedItem);
					}
				// 7.6.6. Otherwise,
				}else{
					// 7.6.6.1. If compactArrays is false, container is @set or @list, or expanded property is @list or @graph and compacted item is not
					// an array, set it to a new array containing only compacted item.
					if((!compactArrays || "@list".equals(container) || "@set".equals(container) || "@list".equals(expandedProperty) || "@graph".equals(expandedProperty)) && !(compactedItem instanceof JsonArray)){
						compactedItem=makeArray(compactedItem);
					}
					// 7.6.6.2. If item active property is not a key in result then add the key-value pair, (item active property-compacted item), to result.
					if(!result.has(itemActiveProperty)){
						result.add(itemActiveProperty, compactedItem);
					// 7.6.6.3. Otherwise, if the value associated with the key that equals item active property in result is not an array, set it to a new array
					// containing only the value. Then append compacted item to the value if compacted item is not an array, otherwise, concatenate it.
					}else{
						JsonElement o=result.get(itemActiveProperty);
						JsonArray ja;
						if(o.isJsonArray())
							ja=o.getAsJsonArray();
						else
							result.add(itemActiveProperty, ja=makeArray(result.get(itemActiveProperty)));
						if(compactedItem.isJsonArray()){
							for(JsonElement o1:compactedItem.getAsJsonArray()){
								ja.add(o1);
							}
						}else{
							ja.add(compactedItem);
						}
					}
				}
			}
		}
		// 8. Return result.
		return result;
	}

	private static void generateNodeMap(JsonElement element, JsonObject nodeMap, String activeGraph, /*String or JsonObject*/JsonElement activeSubject, String activeProperty, JsonObject list, BlankNodeIdentifierGenerator idGen){
		if(element.isJsonArray()){
			for(JsonElement el:element.getAsJsonArray()){
				generateNodeMap(el, nodeMap, activeGraph, activeSubject, activeProperty, list, idGen);
			}
			return;
		}
		JsonObject el=element.getAsJsonObject();
		if(!nodeMap.has(activeGraph))
			nodeMap.add(activeGraph, new JsonObject());
		JsonObject graph=nodeMap.get(activeGraph).getAsJsonObject();
		JsonObject node=null;
		if(activeSubject==null){
			node=null;
		}else if(activeSubject.isJsonPrimitive()){
			String _activeSubject=activeSubject.getAsString();
			if(!graph.has(_activeSubject))
				graph.add(_activeSubject, new JsonObject());
			node=graph.get(_activeSubject).getAsJsonObject();
		}
		if(el.has("@type")){
			JsonElement type=el.get("@type");
			if(type.isJsonArray()){
				int i=0;
				for(JsonElement _item:type.getAsJsonArray()){
					String item=_item.getAsString();
					if(item.startsWith("_:")){
						item=idGen.generate(item);
						type.getAsJsonArray().set(i, new JsonPrimitive(item));
					}
					i++;
				}
			}else{
				String item=el.get("@type").getAsString();
				if(item.startsWith("_:")){
					item=idGen.generate(item);
					el.addProperty("@type", item);
				}
			}
		}
		if(el.has("@value")){
			if(list==null){
				if(!node.has(activeProperty))
					node.add(activeProperty, makeArray(el));
				else if(!node.get(activeProperty).getAsJsonArray().contains(el))
					node.get(activeProperty).getAsJsonArray().add(el);
			}else{
				list.get("@list").getAsJsonArray().add(el);
			}
		}else if(el.has("@list")){
			JsonObject result=new JsonObject();
			result.add("@list", new JsonArray());
			generateNodeMap(el.get("@list").getAsJsonArray(), nodeMap, activeGraph, activeSubject, activeProperty, result, idGen);
			node.get(activeProperty).getAsJsonArray().add(result);
		}else{
			String id;
			if(el.has("@id")){
				String _id=el.get("@id").getAsString();
				el.remove("@id");
				if(_id.startsWith("_:"))
					id=idGen.generate(_id);
				else
					id=_id;
			}else{
				id=idGen.generate(null);
			}
			if(!graph.has(id)){
				graph.add(id, jsonObjectWithSingleKey("@id", new JsonPrimitive(id)));
			}
			if(activeSubject!=null && activeSubject.isJsonObject()){
				node=graph.get(id).getAsJsonObject();
				if(!node.has(activeProperty)){
					node.add(activeProperty, makeArray(activeSubject));
				}else{
					JsonArray ap=node.get(activeProperty).getAsJsonArray();
					if(!ap.contains(activeSubject))
						ap.add(activeSubject);
				}
			}else if(activeProperty!=null){
				JsonObject reference=new JsonObject();
				reference.addProperty("@id", id);
				if(list==null){
					if(!node.has(activeProperty)){
						node.add(activeProperty, makeArray(reference));
					}else{
						JsonArray ap=node.get(activeProperty).getAsJsonArray();
						if(!ap.contains(reference))
							ap.add(reference);
					}
				}else{
					list.get("@list").getAsJsonArray().add(jsonObjectWithSingleKey("@id", new JsonPrimitive(id)));
				}
			}
			node=graph.get(id).getAsJsonObject();
			if(el.has("@type")){
				JsonArray type=el.get("@type").getAsJsonArray();
				if(!node.has("@type"))
					node.add("@type", new JsonArray());
				JsonArray nodeType=node.get("@type").getAsJsonArray();
				for(JsonElement item:type){
					if(!nodeType.contains(item))
						nodeType.add(item);
				}
				el.remove("@type");
			}
			if(el.has("@index")){
				if(node.has("@index"))
					throw new JLDException("conflicting indexes");
				node.add("@index", el.remove("@index"));
			}
			if(el.has("@reverse")){
				JsonObject referencedNode=new JsonObject();
				referencedNode.addProperty("@id", id);
				JsonObject reverseMap=el.get("@reverse").getAsJsonObject();
				for(String property:reverseMap.keySet()){
					JsonArray values=reverseMap.get(property).getAsJsonArray();
					for(JsonElement value:values){
						generateNodeMap(value, nodeMap, activeGraph, referencedNode, property, null, idGen);
					}
				}
				el.remove("@reverse");
			}
			if(el.has("@graph")){
				generateNodeMap(el.get("@graph"), nodeMap, id, null, null, null, idGen);
				el.remove("@graph");
			}
			ArrayList<String> keys=new ArrayList<>(el.keySet());
			Collections.sort(keys);
			for(String property:keys){
				JsonElement value=el.get(property);
				if(property.startsWith("_:"))
					property=idGen.generate(property);
				if(!node.has(property))
					node.add(property, new JsonArray());
				generateNodeMap(value, nodeMap, activeGraph, new JsonPrimitive(id), property, null, idGen);
			}
		}
	}

	private static Object objectToRDF(JsonObject item){
		if(isNodeObject(item)){
			String _id=item.get("@id").getAsString();
			if(_id.startsWith("_:"))
				return _id;
			URI id=URI.create(_id);
			if(!id.isAbsolute())
				return null;
			return id;
		}
		JsonElement value=item.get("@value");
		String valueStr=null;
		String datatype=item.has("@type") ? item.get("@type").getAsString() : null;
		if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()){
			valueStr=String.valueOf(value.getAsBoolean());
			if(datatype==null)
				datatype=RDF.NS_XSD+"boolean";
		}else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() && (value.getAsDouble()%1.0!=0.0 || (RDF.NS_XSD+"double").equals(datatype))){
			double d=value.getAsDouble();
			valueStr=String.format(Locale.US, "%.15E", d).replaceAll("(\\d)0*E\\+?(-?)0+(\\d+)","$1E$2$3");
			if(datatype==null)
				datatype=RDF.NS_XSD+"double";
		}else if(value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()){
			valueStr=String.valueOf(value.getAsInt());
			if(datatype==null)
				datatype=RDF.NS_XSD+"integer";
		}else if(datatype==null){
			valueStr=value.getAsString();
			if(item.has("@language"))
				datatype=RDF.NS_RDF+"langString";
			else
				datatype=RDF.NS_RDF+"string";
		}else{
			valueStr=value.getAsString();
		}
		return new RDFLiteral(valueStr, URI.create(datatype), item.has("@language") ? item.get("@language").getAsString() : null);
	}

	private static Object listToRDF(JsonArray list, ArrayList<RDFTriple> triples, BlankNodeIdentifierGenerator idGen){
		if(list.size()==0)
			return URI.create(RDF.NS_RDF+"nil");
		ArrayList<String> bnodes=new ArrayList<>();
		for(int i=0;i<list.size();i++)
			bnodes.add(idGen.generate(null));
		for(int i=0;i<list.size();i++){
			String subject=bnodes.get(i);
			JsonObject item=list.get(i).getAsJsonObject();
			Object object=objectToRDF(item);
			if(object!=null){
				triples.add(new RDFTriple(subject, URI.create(RDF.NS_RDF+"first"), object));
			}
			Object rest=i<bnodes.size()-1 ? bnodes.get(i+1) : URI.create(RDF.NS_RDF+"nil");
			triples.add(new RDFTriple(subject, URI.create(RDF.NS_RDF+"rest"), rest));
		}
		return bnodes.isEmpty() ? RDF.NS_RDF+"nil" : bnodes.get(0);
	}

	public static ArrayList<RDFTriple> toRDF(JsonElement input, URI baseURI){
		final Comparator<String> iriComparator=new Comparator<String>(){
			@Override
			public int compare(String o1, String o2){
				if(o1.startsWith("@"))
					o1='<'+RDF.NS_RDF+o1.substring(1)+'>';
				else if(!o1.startsWith("_:"))
					o1='<'+o1+'>';
				if(o2.startsWith("@"))
					o2='<'+RDF.NS_RDF+o2.substring(1)+'>';
				else if(!o2.startsWith("_:"))
					o2='<'+o2+'>';
				return o1.compareTo(o2);
			}
		};
		boolean produceGeneralizedRDF=false;
		ArrayList<RDFTriple> allTriples=new ArrayList<>();

		JsonArray expanded=expandToArray(input, baseURI);
		JsonObject nodeMap=new JsonObject();
		nodeMap.add("@default", new JsonObject());
		BlankNodeIdentifierGenerator idGen=new BlankNodeIdentifierGenerator();
		generateNodeMap(expanded, nodeMap, "@default", null, null, null, idGen);
		ArrayList<String> nodeMapKeys=new ArrayList<>(nodeMap.keySet());
		nodeMapKeys.sort(iriComparator);
		for(String graphName:nodeMapKeys){
			if(graphName.charAt(0)!='@' && graphName.charAt(0)!='_'){
				if(!URI.create(graphName).isAbsolute())
					continue;
			}
			JsonObject graph=nodeMap.get(graphName).getAsJsonObject();
			ArrayList<RDFTriple> triples=new ArrayList<>();
			ArrayList<String> graphKeys=new ArrayList<>(graph.keySet());
			graphKeys.sort(iriComparator);
			for(String subject:graphKeys){
				URI subjectURI=null;
				if(subject.charAt(0)!='@' && subject.charAt(0)!='_'){
					subjectURI=URI.create(subject);
					if(!subjectURI.isAbsolute())
						continue;
				}
				JsonObject node=graph.get(subject).getAsJsonObject();
				ArrayList<String> nodeKeys=new ArrayList<>(node.keySet());
				nodeKeys.sort(iriComparator);
				for(String property:nodeKeys){
					if(property.equals("@id"))
						continue;
					JsonArray values=node.has(property) && node.get(property).isJsonArray() ? node.getAsJsonArray(property) : makeArray(node.get(property));
					if(property.equals("@type")){
						for(JsonElement _type:values){
							String type=_type.getAsString();
							triples.add(new RDFTriple(subjectURI==null ? subject : subjectURI, URI.create(RDF.NS_RDF+"type"), type.charAt(0)=='_' ? type : URI.create(type)));
						}
					}else if(isKeyword(property)){
						continue;
					}else if(property.startsWith("_:b") && !produceGeneralizedRDF){
						continue;
					}else if(!URI.create(property).isAbsolute()){
						continue;
					}else{
						for(Object _item:values){
							JsonObject item=(JsonObject)_item;
							if(isListObject(item)){
								ArrayList<RDFTriple> listTriples=new ArrayList<>();
								Object listHead=listToRDF(item.getAsJsonArray("@list"), listTriples, idGen);
								triples.add(new RDFTriple(subjectURI==null ? subject : subjectURI, URI.create(property), listHead));
								triples.addAll(listTriples);
							}else{
								Object result=objectToRDF(item);
								if(result!=null){
									triples.add(new RDFTriple(subjectURI==null ? subject : subjectURI, URI.create(property), result));
								}
							}
						}
					}
				}
			}
			if(!"@default".equals(graphName)){
				if(graphName.startsWith("_:")){
					for(RDFTriple triple : triples){
						triple.graphName=graphName;
					}
				}else{
					URI graphNameURI=URI.create(graphName);
					for(RDFTriple triple : triples){
						triple.graphName=graphNameURI;
					}
				}
			}
			allTriples.addAll(triples);
		}
		return allTriples;
	}

	public static JsonArray flatten(JsonElement element, URI baseURI){
		JsonObject nodeMap=new JsonObject();
		nodeMap.add("@default", new JsonObject());
		BlankNodeIdentifierGenerator idGen=new BlankNodeIdentifierGenerator();
		JsonArray expanded=expandToArray(element, baseURI);
		generateNodeMap(expanded, nodeMap, "@default", null, null, null, idGen);
		JsonObject defaultGraph=nodeMap.getAsJsonObject("@default");
		for(String graphName:nodeMap.keySet()){
			if("@default".equals(graphName))
				continue;
			if(!defaultGraph.has(graphName))
				defaultGraph.add(graphName, jsonObjectWithSingleKey("@id", new JsonPrimitive(graphName)));
			JsonObject entry=defaultGraph.getAsJsonObject(graphName);
			entry.add("@graph", new JsonArray());
			JsonObject graph=nodeMap.getAsJsonObject(graphName);
			ArrayList<String> graphKeys=new ArrayList<>(graph.keySet());
			Collections.sort(graphKeys);
			for(String id:graphKeys){
				JsonObject node=graph.getAsJsonObject(id);
				if(!(node.has("@id") && node.size()==1))
					entry.getAsJsonArray("@graph").add(node);
			}
		}
		JsonArray flattened=new JsonArray();
		ArrayList<String> defaultGraphKeys=new ArrayList<>(defaultGraph.keySet());
		Collections.sort(defaultGraphKeys);
		for(String id:defaultGraphKeys){
			JsonObject node=defaultGraph.getAsJsonObject(id);
			if(!(node.has("@id") && node.size()==1))
				flattened.add(node);
		}
		return flattened;
	}
}
