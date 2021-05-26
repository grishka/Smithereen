package smithereen.jsonld;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

public class JLDProcessor{

	private static HashMap<String, JSONObject> schemaCache=new HashMap<>();
	private static final JSONObject inverseLocalContext;
	private static final JLDContext localContext;

	private static final Comparator<String> SHORTEST_LEAST=(o1, o2)->{
		if(o1.length()!=o2.length())
			return o1.length()-o2.length();
		return o1.compareTo(o2);
	};

	static{
		JSONObject lc=new JSONObject();
		lc.put("sc", JLD.SCHEMA_ORG);
		lc.put("sm", JLD.SMITHEREEN);
		lc.put("toot", JLD.MASTODON);

		// schema.org aliases
		lc.put("firstName", "sc:givenName");
		lc.put("lastName", "sc:familyName");
		lc.put("middleName", "sc:additionalName");
		lc.put("gender", idAndTypeObject("sc:gender", "sc:GenderType"));
		lc.put("birthDate", idAndTypeObject("sc:birthDate", "sc:Date"));
		lc.put("value", "sc:value");
		lc.put("PropertyValue", "sc:PropertyValue");

		// ActivityStreams aliases
		lc.put("sensitive", "as:sensitive");
		lc.put("manuallyApprovesFollowers", "as:manuallyApprovesFollowers");

		// Mastodon aliases
		lc.put("blurhash", "toot:blurhash");

		// custom aliases
		lc.put("supportsFriendRequests", "sm:supportsFriendRequests");
		lc.put("cropRegion", idAndContainerObject("sm:cropRegion", "@list"));
		lc.put("maidenName", "sm:maidenName");
		lc.put("wall", "sm:wall");

		localContext=updateContext(new JLDContext(), new JSONArray(Arrays.asList(JLD.ACTIVITY_STREAMS, JLD.W3_SECURITY, lc)), new ArrayList<>(), null);
		inverseLocalContext=createReverseContext(localContext);
	}

	public static JSONArray expandToArray(Object src){
		return expandToArray(src, null);
	}

	public static JSONArray expandToArray(Object src, URI baseURI){
		Object jcontext=null;
		if(src instanceof JSONObject)
			jcontext=((JSONObject) src).opt("@context");
		JLDContext context=updateContext(new JLDContext(), jcontext, new ArrayList<String>(), baseURI);
		Object result=expand(context, null, src);
		if(result instanceof JSONArray)
			return (JSONArray) result;
		if(result==null)
			return new JSONArray();
		if(result instanceof JSONObject){
			JSONObject _r=(JSONObject) result;
			if(_r.length()==1 && _r.has("@graph"))
				return _r.getJSONArray("@graph");
		}
		return new JSONArray(Collections.singletonList(result));
	}

	public static JSONObject compact(Object src, Object context){
		return compact(src, context, true);
	}

	public static JSONObject compact(Object src, Object context, boolean compactArrays){
		JLDContext localContext=updateContext(new JLDContext(), context, new ArrayList<>(), null);
		JSONObject inverseContext=createReverseContext(localContext);
		Object _result=compact(localContext, inverseContext, null, src, compactArrays);
		JSONObject result;
		if(_result instanceof JSONObject)
			result=(JSONObject)_result;
		else
			result=new JSONObject();
		if(context instanceof JSONObject && !((JSONObject) context).isEmpty())
			result.put("@context", context);
		else if(!(context instanceof JSONObject))
			result.put("@context", context);
		return result;
	}

	public static JSONObject compactToLocalContext(JSONArray src){
		return (JSONObject)compact(localContext, inverseLocalContext, null, src, true);
	}

	public static JSONObject convertToLocalContext(JSONObject src){
		return compactToLocalContext(expandToArray(src));
	}

	private static JSONObject idAndTypeObject(String id, String type){
		JSONObject o=new JSONObject();
		o.put("@id", id);
		o.put("@type", type);
		return o;
	}

	private static JSONObject idAndContainerObject(String id, String container){
		JSONObject o=new JSONObject();
		o.put("@id", id);
		o.put("@container", container);
		return o;
	}

	private static String readResourceFile(String name){
		try{
			InputStream in=JLDProcessor.class.getResourceAsStream("/jsonld-schemas/"+name+".jsonld");
			byte[] buf=new byte[in.available()];
			in.read(buf);
			in.close();
			return new String(buf, StandardCharsets.UTF_8);
		}catch(IOException x){
			return null;
		}
	}

	private static JSONObject dereferenceContext(String iri) throws JSONException{
		if(iri.endsWith("/litepub-0.1.jsonld")){ // This avoids caching multiple copies of the same schema for different instances
			iri="https://example.com/schemas/litepub-0.1.jsonld";
		}
		if(schemaCache.containsKey(iri))
			return schemaCache.get(iri);
		String file=null;
		switch(iri){
			case "https://www.w3.org/ns/activitystreams":
				file=readResourceFile("activitystreams");
				break;
			case "https://w3id.org/security/v1":
				file=readResourceFile("w3-security");
				break;
			case "https://w3id.org/identity/v1":
				file=readResourceFile("w3-identity");
				break;
			case "https://example.com/schemas/litepub-0.1.jsonld":
				file=readResourceFile("litepub-0.1");
				break;
			default:
				System.out.println("Warning: can't dereference remote context '"+iri+"'");
				//throw new JLDException("loading remote context failed");
		}
		if(file!=null){
			JSONObject obj=new JSONObject(file);
			schemaCache.put(iri, obj);
			return obj;
		}
		return null;
	}

	private static JLDContext updateContext(JLDContext activeContext, Object _localContext, ArrayList<String> remoteContexts, URI baseURI) throws JSONException{
		JLDContext result=activeContext.clone();
		result.baseIRI=baseURI;
		result.originalBaseIRI=baseURI;
		if(_localContext==null){
			JLDContext r=new JLDContext();
			r.originalBaseIRI=baseURI;
			r.baseIRI=baseURI;
			return r;
		}

		ArrayList<Object> localContext=new ArrayList<>();
		if(_localContext instanceof JSONArray){
			JSONArray a=(JSONArray) _localContext;
			for(int i=0; i<a.length(); i++){
				localContext.add(a.isNull(i) ? null : a.get(i));
			}
		}else{
			localContext.add(_localContext);
		}

		for(Object context : localContext){
			if(context==null){
				result=new JLDContext();
				continue;
			}
			if(context instanceof String){
				String c=(String) context;
				if(!c.startsWith("http:/") && !c.startsWith("https:/")){
					throw new JLDException("relative context IRIs are not supported");
				}
				if(remoteContexts.contains(c)){
					throw new JLDException("recursive context inclusion");
				}
				remoteContexts.add(c);
				JSONObject deref=dereferenceContext(c);
				if(deref!=null){
					result=updateContext(result, deref.get("@context"), remoteContexts, baseURI);
				}else{
					System.err.println("Failed to dereference "+c);
				}

				continue;
			}
			if(!(context instanceof JSONObject)){
				throw new JLDException("invalid local context");
			}
			JSONObject c=(JSONObject) context;
			if(c.has("@base")){
				Object value=c.get("@base");
				if(value==JSONObject.NULL){
					result.baseIRI=null;
				}else if(value instanceof String){
					try{
						URI uri=new URI((String)value);
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
				Object value=c.get("@vocab");
				if(value==JSONObject.NULL){
					result.vocabularyMapping=null;
				}else if(value instanceof String){
					String s=(String)value;
					if(s.contains(":")){
						result.vocabularyMapping=s;
					}else{
						throw new JLDException("invalid vocab mapping");
					}
				}else{
					throw new JLDException("invalid vocab mapping");
				}
			}
			if(c.has("@language")){
				Object value=c.get("@language");
				if(value==JSONObject.NULL || "und".equals(value)){
					result.defaultLanguage=null;
				}else if(value instanceof String){
					result.defaultLanguage=((String)value).toLowerCase();
				}else{
					throw new JLDException("invalid default language");
				}
			}

			for(Iterator<String> it=c.keys(); it.hasNext(); ){
				String k=it.next();
				if(k.equals("@base") || k.equals("@vocab") || k.equals("@language"))
					continue;
				createTermDefinition(result, c, k, new HashMap<>());
			}
		}

		return result;
	}

	private static void createTermDefinition(JLDContext activeContext, JSONObject localContext, String term, HashMap<String, Boolean> defined) throws JSONException{
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
		Object value=localContext.get(term);
		if(value==JSONObject.NULL) value=null;
		if(value==null || (value instanceof JSONObject && ((JSONObject) value).has("@id") && ((JSONObject) value).isNull("@id"))){
			activeContext.termDefinitions.put(term, null);
			defined.put(term, true);
			return;
		}
		if(value instanceof String){
			JSONObject j=new JSONObject();
			j.put("@id", value);
			value=j;
		}
		if(!(value instanceof JSONObject)){
			throw new JLDException("invalid term definition");
		}
		JLDContext.TermDefinition definition=new JLDContext.TermDefinition();
		JSONObject v=(JSONObject)value;
		if(v.has("@type")){
			try{
				String type=v.getString("@type");
				type=expandIRI(activeContext, type, false, true, localContext, defined);
				if(!"@id".equals(type) && !"@vocab".equals(type)){
					URI uri=new URI(type);
					if(!uri.isAbsolute())
						throw new JLDException("invalid type mapping");
				}
				definition.typeMapping=type;
			}catch(JSONException|URISyntaxException x){
				throw new JLDException("invalid type mapping", x);
			}
		}
		if(v.has("@reverse")){
			if(v.has("@id"))
				throw new JLDException("invalid reverse property");
			try{
				String reverse=v.getString("@reverse");
				definition.iriMapping=expandIRI(activeContext, reverse, false, true, localContext, defined);
				if(!definition.iriMapping.contains(":"))
					throw new JLDException("invalid IRI mapping");
				if(v.has("@container")){
					definition.containerMapping=v.getString("@container");
					if(definition.containerMapping!=null && !definition.containerMapping.equals("@set") && !definition.containerMapping.equals("@index")){
						throw new JLDException("invalid reverse property");
					}
				}
				definition.reverseProperty=true;
				activeContext.termDefinitions.put(term, definition);
				defined.put(term, true);
			}catch(JSONException x){
				throw new JLDException("invalid reverse property");
			}
			return;
		}
		definition.reverseProperty=false;
		if(v.has("@id") && !term.equals(v.get("@id"))){
			try{
				definition.iriMapping=expandIRI(activeContext, v.getString("@id"), false, true, localContext, defined);
				if(!isKeyword(definition.iriMapping) && !definition.iriMapping.contains(":"))
					throw new JLDException("invalid IRI mapping");
				if("@context".equals(definition.iriMapping))
					throw new JLDException("invalid keyword mapping");
			}catch(JSONException x){
				throw new JLDException("invalid IRI mapping", x);
			}
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
			String container=v.getString("@container");
			if(container!=null && !"@list".equals(container) && !"@set".equals(container) && !"@index".equals(container) && !"@language".equals(container))
				throw new JLDException("invalid container mapping");
			definition.containerMapping=container;
		}

		if(v.has("@language")){
			Object _language=v.get("@language");
			if(_language==JSONObject.NULL) _language=null;
			if(_language!=null && !(_language instanceof String))
				throw new JLDException("invalid language mapping");
			String language=(String)_language;
			if(language!=null){
				language=language.toLowerCase();
			}
			definition.languageMapping=language;
			definition.hasLanguageMapping=true;
		}

		activeContext.termDefinitions.put(term, definition);
		defined.put(term, true);
	}

	private static URI fixURI(URI uri){
		try{
			String path=uri.getPath().replace("../", "").replace("./", "");//.replaceAll("(?<!^)/\\./", "/");
			return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment());
		}catch(URISyntaxException e){
			throw new IllegalArgumentException(e);
		}
	}

	private static String expandIRI(JLDContext activeContext, String value, boolean documentRelative, boolean vocab, JSONObject localContext, HashMap<String, Boolean> defined) throws JSONException{
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
		switch(key){
			case "@context":
			case "@id":
			case "@value":
			case "@language":
			case "@type":
			case "@container":
			case "@list":
			case "@set":
			case "@reverse":
			case "@index":
			case "@base":
			case "@vocab":
			case "@graph":
				return true;
			default:
				return false;
		}
	}

	private static boolean isScalar(Object obj){
		return obj instanceof String || obj instanceof Integer || obj instanceof Boolean || obj instanceof Double;
	}

	private static boolean isJsonNativeType(Object obj){
		return obj instanceof Integer || obj instanceof Boolean || obj instanceof Double;
	}

	private static JSONObject jsonObjectWithSingleKey(String key, Object value) throws JSONException{
		JSONObject obj=new JSONObject();
		obj.put(key, value);
		return obj;
	}

	private static ArrayList<String> keysAsList(JSONObject obj) throws JSONException{
		ArrayList<String> keys=new ArrayList<>();
		Iterator<String> _keys=obj.keys();
		while(_keys.hasNext()){
			keys.add(_keys.next());
		}
		return keys;
	}

	private static Object expand(JLDContext activeContext, String activeProperty, Object element) throws JSONException{
		if(element==null || element==JSONObject.NULL)
			return null;
		if(isScalar(element)){
			if(activeProperty==null || "@graph".equals(activeProperty))
				return null;
			return expandValue(activeContext, activeProperty, element);
		}
		if(element instanceof JSONArray){
			JSONArray result=new JSONArray();
			JSONArray el=(JSONArray) element;
			for(int i=0;i<el.length();i++){
				Object item=el.get(i);
				Object expandedItem=expand(activeContext, activeProperty, item);
				if("@list".equals(activeProperty) || (activeContext.termDefinitions.containsKey(activeProperty) && "@list".equals(activeContext.termDefinitions.get(activeProperty).containerMapping))){
					if(expandedItem instanceof JSONArray || isListObject(expandedItem))
						throw new JLDException("list of lists");
				}
				if(expandedItem instanceof JSONArray){
					JSONArray xe=(JSONArray) expandedItem;
					for(int j=0;j<xe.length();j++){
						result.put(xe.get(j));
					}
				}else if(expandedItem!=null){
					result.put(expandedItem);
				}
			}
			return result;
		}
		if(!(element instanceof JSONObject))
			throw new JLDException("JSONObject expected here, found: "+element.getClass().getName());
		JSONObject el=(JSONObject)element;
		if(el.has("@context")){
			activeContext=updateContext(activeContext, el.isNull("@context") ? null : el.get("@context"), new ArrayList<>(), activeContext.originalBaseIRI);
		}
		JSONObject result=new JSONObject();
		ArrayList<String> keys=keysAsList(el);
		Collections.sort(keys);
		for(String key:keys){
			Object value=el.get(key);
			if(el.isNull(key))
				value=null;
			Object expandedValue=null;
			if("@context".equals(key))
				continue;

			String expandedProperty=expandIRI(activeContext, key, false, true, null, null);
			if(expandedProperty==null || (!expandedProperty.contains(":") && !isKeyword(expandedProperty)))
				continue;

			if(isKeyword(expandedProperty)){
				if("@reverse".equals(activeProperty))
					throw new JLDException("invalid reverse property map");
				if(result.has(expandedProperty))
					throw new JLDException("colliding keywords");
				if("@id".equals(expandedProperty)){
					if(!(value instanceof String))
						throw new JLDException("invalid @id value");
					expandedValue=expandIRI(activeContext, (String)value, true, false, null, null);
				}else if("@type".equals(expandedProperty)){
					if(value instanceof JSONArray){
						JSONArray expTypes=new JSONArray();
						for(int i=0;i<((JSONArray) value).length();i++){
							Object e=((JSONArray) value).get(i);
							if(!(e instanceof String)){
								throw new JLDException("invalid type value");
							}
							expTypes.put(expandIRI(activeContext, (String)e, true, true, null, null));
						}
						expandedValue=expTypes;
					}else if(value instanceof String){
						expandedValue=expandIRI(activeContext, (String)value, true, true, null, null);
					}else{
						throw new JLDException("invalid type value");
					}
				}else if("@graph".equals(expandedProperty)){
					expandedValue=expand(activeContext, "@graph", value);
				}else if("@value".equals(expandedProperty)){
					if(value!=null && !isScalar(value))
						throw new JLDException("invalid value object value");
					expandedValue=value;
					if(expandedValue==null){
						result.put("@value", JSONObject.NULL);
						continue;
					}
				}else if("@language".equals(expandedProperty)){
					if(!(value instanceof String))
						throw new JLDException("invalid language-tagged string");
					expandedValue=((String)value).toLowerCase();
				}else if("@index".equals(expandedProperty)){
					if(!(value instanceof String))
						throw new JLDException("invalid @index value");
					expandedValue=value;
				}else if("@list".equals(expandedProperty)){
					if(activeProperty==null || "@graph".equals(activeProperty))
						continue;
					expandedValue=expand(activeContext, activeProperty, value);
					if(isListObject(expandedValue) || (expandedValue instanceof JSONArray && ((JSONArray) expandedValue).length()>0 && isListObject(((JSONArray) expandedValue).get(0))))
						throw new JLDException("list of lists");
				}else if("@set".equals(expandedProperty)){
					expandedValue=expand(activeContext, activeProperty, value);
				}else if("@reverse".equals(expandedProperty)){
					if(!(value instanceof JSONObject))
						throw new JLDException("invalid @reverse value");
					expandedValue=expand(activeContext, "@reverse", value);
					if(expandedValue instanceof JSONObject){
						JSONObject jv=(JSONObject) expandedValue;
						if(jv.has("@reverse")){
							JSONObject rev=jv.getJSONObject("@reverse");
							for(String property:rev.keySet()){
								Object item=rev.get(property);
								if(!result.has(property))
									result.put(property, new JSONArray());
								// spec does not say this but tests do expect this, so...
								if(item instanceof JSONArray){
									JSONArray aitem=(JSONArray) item;
									for(int i=0;i<aitem.length();i++){
										((JSONArray) result.get(property)).put(aitem.get(i));
									}
								}else{
									((JSONArray) result.get(property)).put(item);
								}
							}
							jv.remove("@reverse");
						}
						if(jv.length()>0){
							if(!result.has("@reverse"))
								result.put("@reverse", new JSONObject());
							JSONObject reverseMap=result.getJSONObject("@reverse");
							for(String property:jv.keySet()){
								JSONArray items=jv.getJSONArray(property);
								for(int i=0;i<items.length();i++){
									Object item=items.get(i);
									if(isListObject(item) || isValueObject(item))
										throw new JLDException("invalid reverse property value");
									if(!reverseMap.has(property))
										reverseMap.put(property, new JSONArray());
									reverseMap.getJSONArray(property).put(item);
								}
							}
						}
					}
					continue;
				}

				if(expandedValue!=null){
					result.put(expandedProperty, expandedValue);
				}
				continue;
			}

			JLDContext.TermDefinition term=activeContext.termDefinitions.get(key);

			if(term!=null && "@language".equals(term.containerMapping) && value instanceof JSONObject){
				JSONArray exp=new JSONArray();
				JSONObject objval=(JSONObject)value;
				ArrayList<String> objkeys=keysAsList(objval);
				Collections.sort(objkeys);
				for(String lang:objkeys){
					Object langValue=objval.get(lang);
					if(!(langValue instanceof JSONArray)){
						langValue=new JSONArray(Collections.singletonList(langValue));
					}
					JSONArray langValueArr=(JSONArray) langValue;
					for(int j=0;j<langValueArr.length();j++){
						try{
							String item=langValueArr.getString(j);
							JSONObject r=jsonObjectWithSingleKey("@value", item);
							r.put("@language", lang.toLowerCase());
							exp.put(r);
						}catch(JSONException x){
							throw new JLDException("invalid language map value", x);
						}
					}
				}
				expandedValue=exp;
			}else if(term!=null && "@index".equals(term.containerMapping) && value instanceof JSONObject){
				JSONArray xv=new JSONArray();
				JSONObject v=(JSONObject)value;
				ArrayList<String> vkeys=keysAsList(v);
				Collections.sort(vkeys);
				for(String index:vkeys){
					Object indexValue=v.get(index);
					if(!(indexValue instanceof JSONArray))
						indexValue=new JSONArray(Collections.singletonList(indexValue));
					indexValue=expand(activeContext, key, indexValue);
					JSONArray ival=(JSONArray)indexValue;
					for(int j=0;j<ival.length();j++){
						JSONObject item=ival.getJSONObject(j);
						if(!item.has("@index"))
							item.put("@index", index);
						xv.put(item);
					}
				}
				expandedValue=xv;
			}else{
				expandedValue=expand(activeContext, key, value);
			}
			if(expandedValue==null)
				continue;
			if(term!=null && "@list".equals(term.containerMapping) && !isListObject(expandedValue)){
				if(!(expandedValue instanceof JSONArray))
					expandedValue=new JSONArray(Collections.singletonList(expandedValue));
				expandedValue=jsonObjectWithSingleKey("@list", expandedValue);
			}
			if(isListObject(expandedValue) && !(((JSONObject)expandedValue).get("@list") instanceof JSONArray)){
				expandedValue=jsonObjectWithSingleKey("@list", new JSONArray(Collections.singletonList(((JSONObject)expandedValue).get("@list"))));
			}
			if(term!=null && term.reverseProperty){
				if(!result.has("@reverse"))
					result.put("@reverse", new JSONObject());
				JSONObject reverseMap=result.getJSONObject("@reverse");
				if(!(expandedValue instanceof JSONArray))
					expandedValue=new JSONArray(Collections.singletonList(expandedValue));
				JSONArray xv=(JSONArray) expandedValue;
				for(int i=0;i<xv.length();i++){
					Object item=xv.get(i);
					if(isListObject(item) || isValueObject(item))
						throw new JLDException("invalid reverse property value");
					if(!reverseMap.has(expandedProperty))
						reverseMap.put(expandedProperty, new JSONArray());
					reverseMap.getJSONArray(expandedProperty).put(item);
				}
			}else{
				if(!result.has(expandedProperty))
					result.put(expandedProperty, new JSONArray());
				if(expandedValue instanceof JSONArray){
					JSONArray prop=result.getJSONArray(expandedProperty);
					for(int i=0;i<((JSONArray) expandedValue).length();i++){
						prop.put(((JSONArray) expandedValue).get(i));
					}
				}else{
					result.getJSONArray(expandedProperty).put(expandedValue);
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
			if(result.isNull("@value")){
				result=null;
			}else if(!(result.get("@value") instanceof String) && result.has("@language")){
				throw new JLDException("invalid language-tagged value");
			}else if(result.has("@type")){
				try{
					new URI(result.getString("@type"));
				}catch(URISyntaxException x){
					throw new JLDException("invalid typed value", x);
				}
			}
		}else if(result.has("@type") && !(result.get("@type") instanceof JSONArray)){
			result.put("@type", new JSONArray(Collections.singletonList(result.get("@type"))));
		}else if(result.has("@set") || result.has("@list")){
			if(result.length()>2 || (result.length()>1 && !result.has("@index"))){
				throw new JLDException("invalid set or list object");
			}
			if(result.has("@set"))
				return result.get("@set");
		}

		if(result!=null && result.length()==1 && result.has("@language"))
			result=null;

		if(activeProperty==null || "@graph".equals(activeProperty)){
			if(result!=null && (result.length()==0 || result.has("@value") || result.has("@list")))
				result=null;
			else if(result!=null && result.length()==1 && result.has("@id"))
				result=null;
		}

		return result;
	}

	private static JSONObject expandValue(JLDContext activeContext, String activeProperty, Object value){
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		if(term!=null && "@id".equals(term.typeMapping)){
			// the algorithm spec was clearly written without strongly-typed languages in mind. Sigh.
			if(value instanceof String)
				return jsonObjectWithSingleKey("@id", expandIRI(activeContext, (String)value, true, false, null, null));
			return jsonObjectWithSingleKey("@value", value);
		}
		if(term!=null && "@vocab".equals(term.typeMapping)){
			if(value instanceof String)
				return jsonObjectWithSingleKey("@id", expandIRI(activeContext, (String)value, true, true, null, null));
			return jsonObjectWithSingleKey("@value", value);
		}
		JSONObject result=jsonObjectWithSingleKey("@value", value);
		if(term!=null && term.typeMapping!=null)
			result.put("@type", term.typeMapping);
		else if(value instanceof String){
			if(term!=null && term.hasLanguageMapping){
				if(term.languageMapping!=null)
					result.put("@language", term.languageMapping);
			}else if(activeContext.defaultLanguage!=null){
				result.put("@language", activeContext.defaultLanguage);
			}
		}
		return result;
	}

	private static JSONObject createReverseContext(JLDContext activeContext) throws JSONException{
		// 1) Initialize result to an empty JSON object.
		JSONObject result=new JSONObject();
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
				result.put(iri, new JSONObject());
			}
			// 3.5) Reference the value associated with the iri member in result using the variable container map.
			JSONObject containerMap=result.getJSONObject(iri);
			// 3.6) If container map has no container member, create one and set its value to a new JSON object with two members.
			// The first member is @language and its value is a new empty JSON object, the second member is @type and its value is a new empty JSON object.
			if(!containerMap.has(container)){
				JSONObject o=new JSONObject();
				o.put("@language", new JSONObject());
				o.put("@type", new JSONObject());
//				o.put("@any", term);
				containerMap.put(container, o);
			}
			// 3.7) Reference the value associated with the container member in container map using the variable type/language map.
			JSONObject typeLanguageMap=containerMap.getJSONObject(container);
			// 3.8) If the term definition indicates that the term represents a reverse property:
			if(termDefinition.reverseProperty){
				// 3.8.1) Reference the value associated with the @type member in type/language map using the variable type map.
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				// 3.8.2) If type map does not have a @reverse member, create one and set its value to the term being processed.
				if(!typeMap.has("@reverse"))
					typeMap.put("@reverse", term);

				// 3.9) Otherwise, if term definition has a type mapping:
			}else if(termDefinition.typeMapping!=null){
				// 3.9.1) Reference the value associated with the @type member in type/language map using the variable type map.
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				// 3.9.2) If type map does not have a member corresponding to the type mapping in term definition, create one and set its value to the term being processed.
				if(!typeMap.has(termDefinition.typeMapping))
					typeMap.put(termDefinition.typeMapping, term);

				// 3.10) Otherwise, if term definition has a language mapping (might be null):
			}else if(termDefinition.hasLanguageMapping){
				// 3.10.1) Reference the value associated with the @language member in type/language map using the variable language map.
				JSONObject languageMap=typeLanguageMap.getJSONObject("@language");
				// 3.10.2) If the language mapping equals null, set language to @null; otherwise set it to the language code in language mapping.
				String language=termDefinition.languageMapping==null ? "@null" : termDefinition.languageMapping;
				// 3.10.3) If language map does not have a language member, create one and set its value to the term being processed.
				if(!languageMap.has(language))
					languageMap.put(language, term);

				// 3.11) Otherwise:
			}else{
				// 3.11.1) Reference the value associated with the @language member in type/language map using the variable language map.
				JSONObject languageMap=typeLanguageMap.getJSONObject("@language");
				// 3.11.2) If language map does not have a default language member, create one and set its value to the term being processed.
				if(!languageMap.has(defaultLanguage))
					languageMap.put(defaultLanguage, term);
				// 3.11.3) If language map does not have a @none member, create one and set its value to the term being processed.
				if(!languageMap.has("@none"))
					languageMap.put("@none", term);
				// 3.11.4) Reference the value associated with the @type member in type/language map using the variable type map.
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				// 3.11.5) If type map does not have a @none member, create one and set its value to the term being processed.
				if(!typeMap.has("@none"))
					typeMap.put("@none", term);
			}
		}
		// 4) Return result.
		return result;
	}

	private static boolean isListObject(Object o){
		return o instanceof JSONObject && ((JSONObject) o).has("@list");
	}

	private static boolean isValueObject(Object o){
		return o instanceof JSONObject && ((JSONObject) o).has("@value");
	}

	private static boolean isNodeObject(JSONObject o){
		return !o.has("@value") && !o.has("@list") && !o.has("@set");
	}

	private static String selectTerm(JSONObject inverseContext, String iri, ArrayList<String> containers, String typeLanguage, ArrayList<String> preferredValues){
		// 1. Initialize container map to the value associated with iri in the inverse context.
		JSONObject containerMap=inverseContext.getJSONObject(iri);
		// 2. For each item container in containers:
		for(String container:containers){
			// 2.1. If container is not a key in container map, then there is no term with a matching container mapping for it, so continue to the next container.
			if(!containerMap.has(container))
				continue;
			// 2.2. Initialize type/language map to the value associated with the container member in container map.
			JSONObject typeLanguageMap=containerMap.getJSONObject(container);
			// 2.3. Initialize value map to the value associated with type/language member in type/language map.
			JSONObject valueMap=typeLanguageMap.getJSONObject(typeLanguage);
			// 2.4. For each item in preferred values:
			for(String item:preferredValues){
				// 2.4.1. If item is not a key in value map, then there is no term with a matching type mapping or language mapping, so continue to the next item.
				if(!valueMap.has(item))
					continue;
				// 2.4.2. Otherwise, a matching term has been found, return the value associated with the item member in value map.
				return valueMap.getString(item);
			}
		}
		// 3. No matching term has been found. Return null.
		return null;
	}

	private static String compactIRI(JLDContext activeContext, JSONObject inverseContext, String iri, Object value, boolean vocab, boolean reverse){
		// 1. If iri is null, return null.
		if(iri==null)
			return null;
		if(value==JSONObject.NULL)
			value=null;
		JSONObject vj=value instanceof JSONObject ? (JSONObject)value : null;
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
			if(value instanceof JSONObject && ((JSONObject) value).has("@index")){
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
				JSONArray list=vj.getJSONArray("@list");
				// 2.6.3.Initialize common type and common language to null. If list is empty, set common language to default language.
				String commonType=null, commonLanguage=null;
				if(list.isEmpty()){
					commonLanguage=defaultLanguage;
				}
				// 2.6.4. For each item in list:
				for(int i=0;i<list.length();i++){
					JSONObject item=list.getJSONObject(i);
					// 2.6.4.1. Initialize item language to @none and item type to @none.
					String itemLanguage="@none", itemType="@none";
					// 2.6.4.2. If item contains the key @value:
					if(item.has("@value")){
						// 2.6.4.2.1. If item contains the key @language, then set item language to its associated value.
						if(item.has("@language"))
							itemLanguage=item.getString("@language");
						// 2.6.4.2.2. Otherwise, if item contains the key @type, set item type to its associated value.
						else if(item.has("@type"))
							itemType=item.getString("@type");
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
						typeLanguageValue=vj.getString("@language");
						containers.add("@language");
					// 2.7.1.2. Otherwise, if value contains the key @type, then set type/language value to its associated value
					// and set type/language to @type.
					}else if(vj.has("@type")){
						typeLanguageValue=vj.getString("@type");
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
				String r=compactIRI(activeContext, inverseContext, vj.getString("@id"), null, true, false);
				JLDContext.TermDefinition td=activeContext.termDefinitions.get(r);
				if(td!=null && vj.getString("@id").equals(td.iriMapping)){
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
		// we don't know document IRIs
		// 8. Finally, return iri as is.
		return iri;
	}

	private static Object compactValue(JLDContext activeContext, JSONObject inverseContext, String activeProperty, JSONObject value){
		// 1. Initialize number members to the number of members value contains.
		int numberMembers=value.length();
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
				return compactIRI(activeContext, inverseContext, value.getString("@id"), null, false, false);
			// 4.2. Otherwise, if number members is 1 and the type mapping of active property is set to @vocab, return the result of using the IRI compaction
			// algorithm, passing active context, inverse context, the value of the @id member for iri, and true for vocab.
			}else if(numberMembers==1 && term!=null && "@vocab".equals(term.typeMapping)){
				return compactIRI(activeContext, inverseContext, value.getString("@id"), null, true, false);
			}
			// 4.3. Otherwise, return value as is.
			return value;
		// 5. Otherwise, if value has an @type member whose value matches the type mapping of active property, return the value associated with the @value member of value.
		}else if(value.has("@type") && term!=null && value.getString("@type").equals(term.typeMapping)){
			return value.get("@value");
		// 6. Otherwise, if value has an @language member whose value matches the language mapping of active property, return the value associated with the @value member of value.
		}else if(value.has("@language") && term!=null && value.getString("@language").equals(term.languageMapping)){
			return value.get("@value");
		// 7. Otherwise, if number members equals 1 and either the value of the @value member is not a string, or the active context has no default language,
		// or the language mapping of active property is set to null,, return the value associated with the @value member.
		}else if(numberMembers==1 && (!(value.get("@value") instanceof String) || activeContext.defaultLanguage==null || (term==null || !term.hasLanguageMapping))){
			return value.get("@value");
		}
		// 8. Otherwise, return value as is.
		return value;
	}

	private static Object compact(JLDContext activeContext, JSONObject inverseContext, String activeProperty, Object element, boolean compactArrays){
		// 1. If element is a scalar, it is already in its most compact form, so simply return element.
		if(isScalar(element))
			return element;
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		// 2. If element is an array:
		if(element instanceof JSONArray){
			JSONArray e=(JSONArray) element;
			// 2.1. Initialize result to an empty array.
			JSONArray result=new JSONArray();
			// 2.2. For each item in element:
			for(int i=0;i<e.length();i++){
				Object item=e.get(i);
				// 2.2.1. Initialize compacted item to the result of using this algorithm recursively, passing active context, inverse context,
				// active property, and item for element.
				Object compactedItem=compact(activeContext, inverseContext, activeProperty, item, compactArrays);
				// 2.2.2. If compacted item is not null, then append it to result.
				if(compactedItem!=null)
					result.put(compactedItem);
			}
			// 2.3. If result contains only one item (it has a length of 1), active property has no container
			// mapping in active context, and compactArrays is true, set result to its only item.
			if(result.length()==1 && (term==null || term.containerMapping==null) && compactArrays)
				return result.get(0);
			// 2.4. Return result.
			return result;
		}
		// 3. Otherwise element is a JSON object.
		JSONObject e=(JSONObject) element;
		// 4. If element has an @value or @id member and the result of using the Value Compaction algorithm, passing active context, inverse context,
		// active property, and element as value is a scalar, return that result.
		if(e.has("@value") || e.has("@id")){
			Object res=compactValue(activeContext, inverseContext, activeProperty, e);
			if(isScalar(res))
				return res;
		}
		// 5. Initialize inside reverse to true if active property equals @reverse, otherwise to false.
		boolean insideReverse="@reverse".equals(activeProperty);
		// 6. Initialize result to an empty JSON object.
		JSONObject result=new JSONObject();
		// 7. For each key expanded property and value expanded value in element, ordered lexicographically by expanded property:
		ArrayList<String> keys=keysAsList(e);
		Collections.sort(keys);
		for(String expandedProperty:keys){
			Object expandedValue=e.get(expandedProperty);
			if(expandedValue==JSONObject.NULL)
				continue;
			Object compactedValue;
			// 7.1. If expanded property is @id or @type:
			if(expandedProperty.equals("@id") || expandedProperty.equals("@type")){
				// 7.1.1. If expanded value is a string, then initialize compacted value to the result of using the IRI Compaction algorithm,
				// passing active context, inverse context, expanded value for iri, and true for vocab if expanded property is @type, false otherwise.
				if(expandedValue instanceof String){
					compactedValue=compactIRI(activeContext, inverseContext, (String)expandedValue, null, expandedProperty.equals("@type"), false);
				// 7.1.2. Otherwise, expanded value must be a @type array:
				}else{
					// 7.1.2.1. Initialize compacted value to an empty array.
					JSONArray _compactedValue=new JSONArray();
					// 7.1.2.2. For each item expanded type in expanded value, append the result of of using the IRI Compaction algorithm,
					// passing active context, inverse context, expanded type for iri, and true for vocab, to compacted value.
					JSONArray _expandedValue=(JSONArray)expandedValue;
					for(int i=0;i<_expandedValue.length();i++){
						_compactedValue.put(compactIRI(activeContext, inverseContext, _expandedValue.getString(i), null, true, false));
					}
					// 7.1.2.3. If compacted value contains only one item (it has a length of 1), then set compacted value to its only item.
					if(_compactedValue.length()==1)
						compactedValue=_compactedValue.get(0);
					else
						compactedValue=_compactedValue;
				}
				// 7.1.3. Initialize alias to the result of using the IRI Compaction algorithm, passing active context, inverse context, and expanded property for iri.
				String alias=compactIRI(activeContext, inverseContext, expandedProperty, null, true, false);
				// 7.1.4. Add a member alias to result whose value is set to compacted value and continue to the next expanded property.
				result.put(alias, compactedValue);
				continue;
			}
			// 7.2. If expanded property is @reverse:
			if(expandedProperty.equals("@reverse")){
				// 7.2.1. Initialize compacted value to the result of using this algorithm recursively,
				// passing active context, inverse context, @reverse for active property, and expanded value for element.
				JSONObject _compactedValue=(JSONObject) compact(activeContext, inverseContext, "@reverse", expandedValue, compactArrays);
				compactedValue=_compactedValue;
				ArrayList<String> keys2=keysAsList(_compactedValue);
				// 7.2.2. For each property and value in compacted value:
				for(String property:keys2){
					Object value=_compactedValue.get(property);
					// 7.2.2.1. If the term definition for property in the active context indicates that property is a reverse property
					JLDContext.TermDefinition pterm=activeContext.termDefinitions.get(property);
					if(pterm!=null && pterm.reverseProperty){
						// 7.2.2.1.1. If compactArrays is false and value is not an array, set value to a new array containing only value.
						if(!compactArrays && !(value instanceof JSONArray))
							value=new JSONArray(Collections.singletonList(value));

						// 7.2.2.1.2. If property is not a member of result, add one and set its value to value.
						if(!result.has(property)){
							result.put(property, value);
						// 7.2.2.1.3. Otherwise, if the value of the property member of result is not an array, set it to a new array containing only the value.
						// Then append value to its value if value is not an array, otherwise append each of its items.
						}else{
							Object val=result.get(property);
							JSONArray valArr;
							if(val instanceof JSONArray){
								valArr=(JSONArray) val;
							}else{
								valArr=new JSONArray(Collections.singletonList(val));
								result.put(property, valArr);
							}
							if(value instanceof JSONArray){
								for(int i=0;i<((JSONArray) value).length();i++){
									valArr.put(((JSONArray) value).get(i));
								}
							}else{
								valArr.put(value);
							}
						}
						// 7.2.2.1.4. Remove the property member from compacted value.
						_compactedValue.remove(property);
					}
				}
				// 7.2.3. If compacted value has some remaining members, i.e., it is not an empty JSON object:
				if(!_compactedValue.isEmpty()){
					// 7.2.3.1. Initialize alias to the result of using the IRI Compaction algorithm, passing active context, inverse context, and @reverse for iri.
					String alias=compactIRI(activeContext, inverseContext, "@reverse", null, false, false);
					// 7.2.3.2. Set the value of the alias member of result to compacted value and continue with the next expanded property from element.
					result.put(alias, _compactedValue);
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
				String alias=compactIRI(activeContext, inverseContext, expandedProperty, null, false, false);
				// 7.4.2.Add a member alias to result whose value is set to expanded value and continue with
				// the next expanded property.
				result.put(alias, expandedValue);
				continue;
			}
			// 7.5. If expanded value is an empty array:
			if(expandedValue instanceof JSONArray && ((JSONArray) expandedValue).isEmpty()){
				// 7.5.1.Initialize item active property to the result of using the IRI Compaction algorithm, passing active context, inverse context, expanded property for iri, expanded
				// value for value, true for vocab, and inside reverse.
				String itemActiveProperty=compactIRI(activeContext, inverseContext, expandedProperty, expandedValue, true, insideReverse);
				// 7.5.2.If result does not have the key that equals item active property, set this key's value	in result to an empty array.
				// Otherwise, if the key's value is not an array, then set it to one containing only the value.
				if(!result.has(itemActiveProperty)){
					result.put(itemActiveProperty, new JSONArray());
				}else if(!(result.get(itemActiveProperty) instanceof JSONArray)){
					result.put(itemActiveProperty, new JSONArray(Collections.singletonList(result.getString(itemActiveProperty))));
				}
			}
			// 7.6. At this point, expanded value must be an array due to the Expansion algorithm. For each item expanded item in expanded value:
			JSONArray ev=(JSONArray)expandedValue;
			for(int i=0;i<ev.length();i++){
				Object expandedItem=ev.get(i);
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
				Object compactedItem=compact(activeContext, inverseContext, itemActiveProperty, expandedItem instanceof JSONObject && ((JSONObject)expandedItem).has("@list") ? ((JSONObject)expandedItem).getJSONArray("@list") : expandedItem, compactArrays);
				// 7.6.4. If expanded item is a list object:
				if(isListObject(expandedItem)){
					// 7.6.4.1. If compacted item is not an array, then set it to an array containing only compacted item.
					if(!(compactedItem instanceof JSONArray)){
						compactedItem=new JSONArray(Collections.singletonList(compactedItem));
					}
					JSONObject _expandedItem=(JSONObject)expandedItem;
					// 7.6.4.2. If container is not @list:
					if(!"@list".equals(container)){
						// 7.6.4.2.1. Convert compacted item to a list object by setting it to a JSON object containing key-value pair where the key is the result of
						// the IRI Compaction algorithm, passing active context, inverse context, @list for iri, and compacted item for value.
						compactedItem=jsonObjectWithSingleKey(compactIRI(activeContext, inverseContext, "@list", compactedItem, false, false), compactedItem);
						// 7.6.4.2.2. If expanded item contains the key @index, then add a key-value pair to compacted item where the key is the result of the IRI
						// Compaction algorithm, passing active context, inverse context, @index as iri, and the value associated with the @index key in expanded item as value.
						if(_expandedItem.has("@index")){
							((JSONObject)compactedItem).put(compactIRI(activeContext, inverseContext, "@index", _expandedItem.get("@index"), false, false), _expandedItem.get("@index"));
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
						result.put(itemActiveProperty, new JSONObject());
					JSONObject mapObject=result.getJSONObject(itemActiveProperty);
					// 7.6.5.2. If container is @language and compacted item contains the key @value, then
					// set compacted item to the value associated with its @value key.
					if("@language".equals(container) && compactedItem instanceof JSONObject && ((JSONObject) compactedItem).has("@value")){
						compactedItem=((JSONObject) compactedItem).get("@value");
					}
					// 7.6.5.3. Initialize map key to the value associated with with the key that equals container in expanded item.
					JSONObject _expandedItem=(JSONObject)expandedItem;
					String mapKey=_expandedItem.getString(container);
					// 7.6.5.4. If map key is not a key in map object, then set this key's value in map object to compacted item.
					// Otherwise, if the value is not an array, then set it to one containing only the value and then append compacted item to it.
					if(!mapObject.has(mapKey)){
						mapObject.put(mapKey, compactedItem);
					}else{
						Object o=mapObject.get(mapKey);
						if(!(o instanceof JSONArray))
						//	((JSONArray) o).put(compactedItem);
						//else
							mapObject.put(mapKey, new JSONArray(Arrays.asList(mapObject.get(mapKey), compactedItem)));
					}
				// 7.6.6. Otherwise,
				}else{
					// 7.6.6.1. If compactArrays is false, container is @set or @list, or expanded property is @list or @graph and compacted item is not
					// an array, set it to a new array containing only compacted item.
					if((!compactArrays || "@list".equals(container) || "@set".equals(container) || "@list".equals(expandedProperty) || "@graph".equals(expandedProperty)) && !(compactedItem instanceof JSONArray)){
						compactedItem=new JSONArray(Collections.singletonList(compactedItem));
					}
					// 7.6.6.2. If item active property is not a key in result then add the key-value pair, (item active property-compacted item), to result.
					if(!result.has(itemActiveProperty)){
						result.put(itemActiveProperty, compactedItem);
					// 7.6.6.3. Otherwise, if the value associated with the key that equals item active property in result is not an array, set it to a new array
					// containing only the value. Then append compacted item to the value if compacted item is not an array, otherwise, concatenate it.
					}else{
						Object o=result.get(itemActiveProperty);
						JSONArray ja;
						if(o instanceof JSONArray)
							ja=(JSONArray) o;
						else
							result.put(itemActiveProperty, ja=new JSONArray(Collections.singletonList(result.get(itemActiveProperty))));
						if(compactedItem instanceof JSONArray){
							JSONArray _compactedItem=(JSONArray) compactedItem;
							for(Object o1:_compactedItem){
								ja.put(o1);
							}
						}else{
							ja.put(compactedItem);
						}
					}
				}
			}
		}
		// 8. Return result.
		return result;
	}

	private static boolean jsonArrayContains(JSONArray array, Object what){
		for(Object o:array){
			if(o.getClass().isInstance(what)){
				if(o.toString().equals(what.toString()))
					return true;
			}
		}
		return false;
	}

	private static void generateNodeMap(Object element, JSONObject nodeMap, String activeGraph, /*String or JSONObject*/Object activeSubject, String activeProperty, JSONObject list, BlankNodeIdentifierGenerator idGen){
		if(element instanceof JSONArray){
			JSONArray array=(JSONArray) element;
			for(int i=0;i<array.length();i++){
				generateNodeMap(array.get(i), nodeMap, activeGraph, activeSubject, activeProperty, list, idGen);
			}
			return;
		}
		JSONObject el=(JSONObject) element;
		if(!nodeMap.has(activeGraph))
			nodeMap.put(activeGraph, new JSONObject());
		JSONObject graph=nodeMap.getJSONObject(activeGraph);
		JSONObject node=null;
		if(activeSubject==null){
			node=null;
		}else if(activeSubject instanceof String){
			String _activeSubject=(String)activeSubject;
			if(!graph.has(_activeSubject))
				graph.put(_activeSubject, new JSONObject());
			node=graph.getJSONObject(_activeSubject);
		}
		if(el.has("@type")){
			JSONArray type=el.optJSONArray("@type");
			if(type!=null){
				for(int i=0; i<type.length(); i++){
					String item=type.getString(i);
					if(item.startsWith("_:")){
						item=idGen.generate(item);
						type.put(i, item);
					}
				}
			}else{
				String item=el.getString("@type");
				if(item.startsWith("_:")){
					item=idGen.generate(item);
					el.put("@type", item);
				}
			}
		}
		if(el.has("@value")){
			if(list==null){
				if(!node.has(activeProperty))
					node.put(activeProperty, new JSONArray(Collections.singletonList(el)));
				else if(!jsonArrayContains(node.getJSONArray(activeProperty), el))
					node.getJSONArray(activeProperty).put(el);
			}else{
				list.getJSONArray("@list").put(el);
			}
		}else if(el.has("@list")){
			JSONObject result=new JSONObject();
			result.put("@list", new JSONArray());
			generateNodeMap(el.getJSONArray("@list"), nodeMap, activeGraph, activeSubject, activeProperty, result, idGen);
			node.getJSONArray(activeProperty).put(result);
		}else{
			String id;
			if(el.has("@id")){
				String _id=el.getString("@id");
				el.remove("@id");
				if(_id.startsWith("_:"))
					id=idGen.generate(_id);
				else
					id=_id;
			}else{
				id=idGen.generate(null);
			}
			if(!graph.has(id)){
				graph.put(id, jsonObjectWithSingleKey("@id", id));
			}
			if(activeSubject instanceof JSONObject){
				node=graph.getJSONObject(id);
				if(!node.has(activeProperty)){
					node.put(activeProperty, new JSONArray(Collections.singletonList(activeSubject)));
				}else{
					JSONArray ap=node.getJSONArray(activeProperty);
					if(!jsonArrayContains(ap, activeSubject))
						ap.put(activeSubject);
				}
			}else if(activeProperty!=null){
				JSONObject reference=new JSONObject();
				reference.put("@id", id);
				if(list==null){
					if(!node.has(activeProperty)){
						node.put(activeProperty, new JSONArray(Collections.singletonList(reference)));
					}else{
						JSONArray ap=node.getJSONArray(activeProperty);
						if(!jsonArrayContains(ap, reference))
							ap.put(reference);
					}
				}else{
					list.getJSONArray("@list").put(jsonObjectWithSingleKey("@id", id));
				}
			}
			node=graph.getJSONObject(id);
			if(el.has("@type")){
				JSONArray type=el.getJSONArray("@type");
				if(!node.has("@type"))
					node.put("@type", new JSONArray());
				JSONArray nodeType=node.getJSONArray("@type");
				for(Object _item:type){
					String item=(String)_item;
					if(!jsonArrayContains(nodeType, item))
						nodeType.put(item);
				}
				el.remove("@type");
			}
			if(el.has("@index")){
				if(node.has("@index"))
					throw new JLDException("conflicting indexes");
				node.put("@index", el.remove("@index"));
			}
			if(el.has("@reverse")){
				JSONObject referencedNode=new JSONObject();
				referencedNode.put("@id", id);
				JSONObject reverseMap=el.getJSONObject("@reverse");
				for(String property:reverseMap.keySet()){
					JSONArray values=reverseMap.getJSONArray(property);
					for(Object value:values){
						generateNodeMap(value, nodeMap, activeGraph, referencedNode, property, null, idGen);
					}
				}
				el.remove("@reverse");
			}
			if(el.has("@graph")){
				generateNodeMap(el.get("@graph"), nodeMap, id, null, null, null, idGen);
				el.remove("@graph");
			}
			ArrayList<String> keys=keysAsList(el);
			Collections.sort(keys);
			for(String property:keys){
				Object value=el.get(property);
				if(property.startsWith("_:"))
					property=idGen.generate(property);
				if(!node.has(property))
					node.put(property, new JSONArray());
				generateNodeMap(value, nodeMap, activeGraph, id, property, null, idGen);
			}
		}
	}

	private static Object objectToRDF(JSONObject item){
		if(isNodeObject(item)){
			String _id=item.getString("@id");
			if(_id.startsWith("_:"))
				return _id;
			URI id=URI.create(_id);
			if(!id.isAbsolute())
				return null;
			return id;
		}
		Object value=item.get("@value");
		String datatype=item.optString("@type", null);
		if(value instanceof Boolean){
			value=value.toString();
			if(datatype==null)
				datatype=RDF.NS_XSD+"boolean";
		}else if((value instanceof Double && (double)value%1.0!=0.0) || (value instanceof Integer && (RDF.NS_XSD+"double").equals(datatype))){
			double d;
			if(value instanceof Integer){
				d=(int)(Integer)value;
			}else{
				d=(double)value;
			}
			value=String.format(Locale.US, "%.15E", d).replaceAll("(\\d)0*E\\+?(-?)0+(\\d+)","$1E$2$3");
			if(datatype==null)
				datatype=RDF.NS_XSD+"double";
		}else if(value instanceof Integer || value instanceof Double){
			if(value instanceof Integer)
				value=value.toString();
			else
				value=String.valueOf((int)(double)value);
			if(datatype==null)
				datatype=RDF.NS_XSD+"integer";
		}else if(datatype==null){
			if(item.has("@language"))
				datatype=RDF.NS_RDF+"langString";
			else
				datatype=RDF.NS_RDF+"string";
		}
		return new RDFLiteral((String)value, URI.create(datatype), item.optString("@language", null));
	}

	private static Object listToRDF(JSONArray list, ArrayList<RDFTriple> triples, BlankNodeIdentifierGenerator idGen){
		if(list.isEmpty())
			return URI.create(RDF.NS_RDF+"nil");
		ArrayList<String> bnodes=new ArrayList<>();
		for(int i=0;i<list.length();i++)
			bnodes.add(idGen.generate(null));
		for(int i=0;i<list.length();i++){
			String subject=bnodes.get(i);
			JSONObject item=list.getJSONObject(i);
			Object object=objectToRDF(item);
			if(object!=null){
				triples.add(new RDFTriple(subject, URI.create(RDF.NS_RDF+"first"), object));
			}
			Object rest=i<bnodes.size()-1 ? bnodes.get(i+1) : URI.create(RDF.NS_RDF+"nil");
			triples.add(new RDFTriple(subject, URI.create(RDF.NS_RDF+"rest"), rest));
		}
		return bnodes.isEmpty() ? RDF.NS_RDF+"nil" : bnodes.get(0);
	}

	public static ArrayList<RDFTriple> toRDF(Object input, URI baseURI){
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

		JSONArray expanded=expandToArray(input, baseURI);
		JSONObject nodeMap=new JSONObject();
		nodeMap.put("@default", new JSONObject());
		BlankNodeIdentifierGenerator idGen=new BlankNodeIdentifierGenerator();
		generateNodeMap(expanded, nodeMap, "@default", null, null, null, idGen);
		ArrayList<String> nodeMapKeys=keysAsList(nodeMap);
		Collections.sort(nodeMapKeys, iriComparator);
		for(String graphName:nodeMapKeys){
			if(graphName.charAt(0)!='@' && graphName.charAt(0)!='_'){
				if(!URI.create(graphName).isAbsolute())
					continue;
			}
			JSONObject graph=nodeMap.getJSONObject(graphName);
			ArrayList<RDFTriple> triples=new ArrayList<>();
			ArrayList<String> graphKeys=keysAsList(graph);
			Collections.sort(graphKeys, iriComparator);
			for(String subject:graphKeys){
				URI subjectURI=null;
				if(subject.charAt(0)!='@' && subject.charAt(0)!='_'){
					subjectURI=URI.create(subject);
					if(!subjectURI.isAbsolute())
						continue;
				}
				JSONObject node=graph.getJSONObject(subject);
				ArrayList<String> nodeKeys=keysAsList(node);
				Collections.sort(nodeKeys, iriComparator);
				for(String property:nodeKeys){
					if(property.equals("@id"))
						continue;
					Iterable<Object> values=node.optJSONArray(property);
					if(values==null){
						values=Collections.singletonList(node.get(property));
					}
					if(property.equals("@type")){
						for(Object _type:values){
							String type=(String)_type;
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
							JSONObject item=(JSONObject)_item;
							if(isListObject(item)){
								ArrayList<RDFTriple> listTriples=new ArrayList<>();
								Object listHead=listToRDF(item.getJSONArray("@list"), listTriples, idGen);
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

	public static JSONArray flatten(Object element, URI baseURI){
		JSONObject nodeMap=new JSONObject();
		nodeMap.put("@default", new JSONObject());
		BlankNodeIdentifierGenerator idGen=new BlankNodeIdentifierGenerator();
		JSONArray expanded=expandToArray(element, baseURI);
		System.out.println(expanded.toString(4));
		generateNodeMap(expanded, nodeMap, "@default", null, null, null, idGen);
		//generateNodeMap(element, nodeMap, "@default", null, null, null, idGen);
		System.out.println(nodeMap.toString(4));
		JSONObject defaultGraph=nodeMap.getJSONObject("@default");
		for(String graphName:nodeMap.keySet()){
			if("@default".equals(graphName))
				continue;
			if(!defaultGraph.has(graphName))
				defaultGraph.put(graphName, jsonObjectWithSingleKey("@id", graphName));
			JSONObject entry=defaultGraph.getJSONObject(graphName);
			entry.put("@graph", new JSONArray());
			JSONObject graph=nodeMap.getJSONObject(graphName);
			ArrayList<String> graphKeys=keysAsList(graph);
			Collections.sort(graphKeys);
			for(String id:graphKeys){
				JSONObject node=graph.getJSONObject(id);
				if(!(node.has("@id") && node.length()==1))
					entry.getJSONArray("@graph").put(node);
			}
		}
		JSONArray flattened=new JSONArray();
		ArrayList<String> defaultGraphKeys=keysAsList(defaultGraph);
		Collections.sort(defaultGraphKeys);
		for(String id:defaultGraphKeys){
			JSONObject node=defaultGraph.getJSONObject(id);
			if(!(node.has("@id") && node.length()==1))
				flattened.put(node);
		}
		System.out.println(flattened.toString(4));
		return flattened;
	}
}
