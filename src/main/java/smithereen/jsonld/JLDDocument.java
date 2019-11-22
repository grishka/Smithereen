package smithereen.jsonld;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JLDDocument{

	private static HashMap<String, JSONObject> schemaCache=new HashMap<>();
	private static final JSONObject inverseLocalContext;
	private static final JLDContext localContext;

	static{
		JSONObject lc=new JSONObject();
		lc.put("sc", JLD.SCHEMA_ORG);
		lc.put("firstName", idAndTypeObject("sc:givenName", "sc:Text"));
		lc.put("lastName", idAndTypeObject("sc:familyName", "sc:Text"));
		lc.put("gender", idAndTypeObject("sc:gender", "sc:GenderType"));
		lc.put("birthDate", idAndTypeObject("sc:birthDate", "sc:Date"));
		lc.put("sensitive", "as:sensitive");
		localContext=updateContext(new JLDContext(), new JSONArray(Arrays.asList(JLD.ACTIVITY_STREAMS, JLD.W3_SECURITY, lc)), new ArrayList<>());
		inverseLocalContext=createReverseContext(localContext);
	}

	public static JSONArray expandToArray(Object src){
		Object jcontext=null;
		if(src instanceof JSONObject)
			jcontext=((JSONObject) src).opt("@context");
		JLDContext context=updateContext(new JLDContext(), jcontext, new ArrayList<String>());
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

	public static JSONObject expand(Object src){
		Object jcontext=null;
		if(src instanceof JSONObject)
			jcontext=((JSONObject) src).opt("@context");
		JLDContext context=updateContext(new JLDContext(), jcontext, new ArrayList<String>());
		return (JSONObject) expand(context, null, src);
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

	private static String readResourceFile(String name){
		try{
			InputStream in=JLDDocument.class.getResourceAsStream("/jsonld-schemas/"+name+".jsonld");
			byte[] buf=new byte[in.available()];
			in.read(buf);
			in.close();
			return new String(buf, StandardCharsets.UTF_8);
		}catch(IOException x){
			return null;
		}
	}

	private static JSONObject dereferenceContext(String iri) throws JSONException{
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
			default:
				throw new JLDException("loading remote context failed");
		}
		if(file!=null){
			JSONObject obj=new JSONObject(file);
			schemaCache.put(iri, obj);
			return obj;
		}
		return null;
	}

	private static JLDContext updateContext(JLDContext activeContext, Object _localContext, ArrayList<String> remoteContexts) throws JSONException{
		JLDContext result=activeContext.clone();
		if(_localContext==null){
			return new JLDContext();
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
					result=updateContext(result, deref.getJSONObject("@context"), remoteContexts);
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
				throw new JLDException("@base is not supported");
			}
			if(c.has("@vocab")){
				String value=c.optString("@vocab");
				if(value==null){
					if(c.has("@vocab"))
						throw new JLDException("invalid vocab mapping");
					result.vocabularyMapping=null;
				}else if(value.contains(":")){
					result.vocabularyMapping=value;
				}else{
					throw new JLDException("invalid vocab mapping");
				}
			}
			if(c.has("@language")){
				throw new JLDException("@language is not supported");
			}

			for(Iterator<String> it=c.keys(); it.hasNext(); ){
				String k=it.next();
				if(isKeyword(k))
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
			String type=v.getString("@type");
			type=expandIRI(activeContext, type, false, true, localContext, defined);
			definition.typeMapping=type;
		}
		if(v.has("@reverse")){
			if(v.has("@id"))
				throw new JLDException("invalid reverse property");
			String reverse=v.getString("@reverse");
			definition.iriMapping=expandIRI(activeContext, reverse, false, true, localContext, defined);
			if(!definition.iriMapping.contains(":"))
				throw new JLDException("invalid IRI mapping");
			if(v.has("@container")){
				definition.containerMapping=v.getString("@container");
				if(definition.containerMapping!=null && !definition.containerMapping.equals("@set") && !definition.containerMapping.equals("@index")){
					throw new JLDException("invalid reverse property");
				}
				definition.reverseProperty=true;
			}
			activeContext.termDefinitions.put(term, definition);
			defined.put(term, true);
			return;
		}
		definition.reverseProperty=false;
		if(v.has("@id") && !term.equals(v.getString("@id"))){
			definition.iriMapping=expandIRI(activeContext, v.getString("@id"), false, true, localContext, defined);
			if(!isKeyword(definition.iriMapping) && !definition.iriMapping.contains(":"))
				throw new JLDException("invalid IRI mapping");
			if("@context".equals(definition.iriMapping))
				throw new JLDException("invalid keyword mapping");
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
			throw new JLDException("@language is not supported");
		}

		activeContext.termDefinitions.put(term, definition);
		defined.put(term, true);
	}

	private static String expandIRI(JLDContext activeContext, String value, boolean documentRelative, boolean vocab, JSONObject localContext, HashMap<String, Boolean> defined) throws JSONException{
		if(value==null || isKeyword(value))
			return value;
		if(localContext!=null && localContext.has(value) && !defined.get(value)){
			createTermDefinition(activeContext, localContext, value, defined);
		}
		if(vocab && activeContext.termDefinitions.containsKey(value)){
			return activeContext.termDefinitions.get(value).iriMapping;
		}
		if(value.contains(":")){
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
			return activeContext.vocabularyMapping.toString()+value;
		if(documentRelative)
			throw new JLDException("not supported");
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
		if(element==null || element.equals(null))
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
				if("@list".equals(activeProperty) || (activeContext.termDefinitions.containsKey(activeProperty) && "@list".equals(activeContext.termDefinitions.get(activeProperty).typeMapping))){
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
			activeContext=updateContext(activeContext, el.isNull("@context") ? null : el.get("@context"), new ArrayList<>());
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
					if(isListObject(expandedValue))
						throw new JLDException("list of lists");
				}else if("@set".equals(expandedProperty)){
					expandedValue=expand(activeContext, activeProperty, value);
				}else if("@reverse".equals(expandedProperty)){
					if(!(value instanceof JSONObject))
						throw new JLDException("invalid @reverse value");
					throw new JLDException("not supported yet");
				}

				if(expandedValue!=null){
					result.put(expandedProperty, expandedValue);
				}
				continue;
			}

			JLDContext.TermDefinition term=activeContext.termDefinitions.get(key);

			if(term!=null && "@language".equals(term.containerMapping)){
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
						String item=langValueArr.getString(j);
						JSONObject r=jsonObjectWithSingleKey("@value", item);
						r.put("@language", lang.toLowerCase());
						exp.put(r);
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
				throw new JLDException("reverse not supported yet");
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
			// check "The result must not contain any keys other than @value, @language, @type, and @index. It must not contain both the @language key and the @type key. Otherwise, an invalid value object error has been detected and processing is aborted."
			if(result.isNull("@value")){
				result=null;
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
			return jsonObjectWithSingleKey("@id", expandIRI(activeContext, (String)value, true, false, null, null));
		}
		if(term!=null && "@vocab".equals(term.typeMapping)){
			return jsonObjectWithSingleKey("@id", expandIRI(activeContext, (String)value, true, true, null, null));
		}
		JSONObject result=jsonObjectWithSingleKey("@value", value);
		if(term!=null && term.typeMapping!=null)
			result.put("@type", term.typeMapping);
		// @language - we don't support this anyway
		return result;
	}

	private static JSONObject createReverseContext(JLDContext activeContext) throws JSONException{
		JSONObject result=new JSONObject();
		String defaultLanguage="@none";
		ArrayList<String> keys=new ArrayList<>(activeContext.termDefinitions.keySet());
		Collections.sort(keys, new Comparator<String>(){
			@Override
			public int compare(String o1, String o2){
				if(o1.length()!=o2.length())
					return o1.length()-o2.length();
				return o1.compareTo(o2);
			}
		});
		for(String term:keys){
			JLDContext.TermDefinition termDefinition=activeContext.termDefinitions.get(term);
			if(termDefinition==null)
				continue;
			String container="@none";
			if(termDefinition.containerMapping!=null)
				container=termDefinition.containerMapping;
			String var=termDefinition.iriMapping;
			if(!result.has(var)){
				result.put(var, new JSONObject());
			}
			JSONObject containerMap=result.getJSONObject(var);
			if(!containerMap.has(container)){
				JSONObject o=new JSONObject();
				o.put("@language", new JSONObject());
				o.put("@type", new JSONObject());
				o.put("@any", term);
				containerMap.put(container, o);
			}
			JSONObject typeLanguageMap=containerMap.getJSONObject(container);
			if(termDefinition.reverseProperty){
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				if(!typeMap.has("@reverse"))
					typeMap.put("@reverse", term);
			}else if(termDefinition.typeMapping!=null){
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				if(!typeMap.has(termDefinition.typeMapping))
					typeMap.put(termDefinition.typeMapping, term);
			}else if(termDefinition.hasLanguageMapping){
				JSONObject languageMap=typeLanguageMap.getJSONObject("@language");
				String language=termDefinition.languageMapping==null ? "@null" : termDefinition.languageMapping;
				if(!languageMap.has(language))
					languageMap.put(language, term);
			}else{
				JSONObject languageMap=typeLanguageMap.getJSONObject("@language");
				if(!languageMap.has(defaultLanguage))
					languageMap.put(defaultLanguage, term);
				if(!languageMap.has("@none"))
					languageMap.put("@none", term);
				JSONObject typeMap=typeLanguageMap.getJSONObject("@type");
				if(!typeMap.has("@none"))
					typeMap.put("@none", term);
			}
		}
		return result;
	}

	private static boolean isListObject(Object o){
		return o instanceof JSONObject && ((JSONObject) o).has("@list");
	}

	private static boolean isValueObject(Object o){
		return o instanceof JSONObject && ((JSONObject) o).has("@value");
	}

	private static String selectTerm(JSONObject inverseContext, String iri, ArrayList<String> containers, String typeLanguage, ArrayList<String> preferredValues){
		JSONObject containerMap=inverseContext.getJSONObject(iri);
		for(String container:containers){
			if(!containerMap.has(container))
				continue;
			JSONObject typeLanguageMap=containerMap.getJSONObject(container);
			JSONObject valueMap=typeLanguageMap.getJSONObject(typeLanguage);
			for(String item:preferredValues){
				if(!valueMap.has(item))
					continue;
				return valueMap.getString(item);
			}
		}
		return null;
	}

	private static String compactIRI(JLDContext activeContext, JSONObject inverseContext, String iri, Object value, boolean vocab, boolean reverse){
		if(iri==null)
			return null;
		JSONObject vj=value instanceof JSONObject ? (JSONObject)value : null;
		if(vocab && inverseContext.has(iri)){
			String defaultLanguage=activeContext.defaultLanguage!=null ? activeContext.defaultLanguage : "@none";
			ArrayList<String> containers=new ArrayList<>();
			String typeLanguage="@language";
			String typeLanguageValue="@null";
			if(value instanceof JSONObject && ((JSONObject) value).has("@index")){
				containers.add("@index");
			}
			if(reverse){
				typeLanguageValue="@reverse";
				containers.add("@set");
			}else if(isListObject(value)){
				if(!vj.has("@index")){
					containers.add("@list");
				}
				JSONArray list=vj.getJSONArray("@list");
				String commonType=null, commonLanguage=null;
				if(list.isEmpty()){
					commonLanguage=defaultLanguage;
				}
				for(int i=0;i<list.length();i++){
					JSONObject item=list.getJSONObject(i);
					String itemLanguage="@none", itemType="@none";
					if(item.has("@value")){
						if(item.has("@language"))
							itemLanguage=item.getString("@language");
						else if(item.has("@type"))
							itemType=item.getString("@type");
						else
							itemLanguage="@null";
					}else{
						itemType="@id";
					}
					if(commonLanguage==null)
						commonLanguage=itemLanguage;
					else if(!itemLanguage.equals(commonLanguage) && item.has("@value"))
						commonLanguage="@none";
					if(commonType==null)
						commonType=itemType;
					else if(!itemType.equals(commonType))
						commonType="@none";
					if(commonLanguage.equals("@none") && commonType.equals("@none"))
						break;
				}
				if(commonLanguage==null)
					commonLanguage="@none";
				if(commonType==null)
					commonType="@none";
				if(!commonType.equals("@none")){
					typeLanguage="@type";
					typeLanguageValue=commonType;
				}else{
					typeLanguageValue=commonLanguage;
				}
			}else{
				if(isValueObject(value)){
					if(vj.has("@language") && !vj.has("@index")){
						typeLanguageValue=vj.getString("@language");
						containers.add("@language");
					}else if(vj.has("@type")){
						typeLanguageValue=vj.getString("@type");
						typeLanguage="@type";
					}
				}else{
					typeLanguage="@type";
					typeLanguageValue="@id";
				}
				containers.add("@set");
			}
			containers.add("@none");
			if(typeLanguageValue==null)
				typeLanguageValue="@null";
			ArrayList<String> preferredValues=new ArrayList<>();
			if("@reverse".equals(typeLanguageValue))
				preferredValues.add("@reverse");
			if(("@id".equals(typeLanguageValue) || "@reverse".equals(typeLanguageValue)) && vj!=null && vj.has("@id")){
				String r=compactIRI(activeContext, inverseContext, vj.getString("@id"), null, true, false);
				JLDContext.TermDefinition td=activeContext.termDefinitions.get(r);
				if(td!=null && vj.getString("@id").equals(td.iriMapping)){
					preferredValues.add("@vocab");
					preferredValues.add("@id");
					preferredValues.add("@none");
				}else{
					preferredValues.add("@id");
					preferredValues.add("@vocab");
					preferredValues.add("@none");
				}
			}else{
				preferredValues.add(typeLanguageValue);
				preferredValues.add("@none");
			}
			String term=selectTerm(inverseContext, iri, containers, typeLanguage, preferredValues);
			if(term!=null)
				return term;
		}
		if(vocab && activeContext.vocabularyMapping!=null){
			if(iri.startsWith(activeContext.vocabularyMapping) && iri.length()>activeContext.vocabularyMapping.length()){
				String suffix=iri.substring(activeContext.vocabularyMapping.length());
				if(!activeContext.termDefinitions.containsKey(suffix))
					return suffix;
			}
		}
		String compactIRI=null;
		for(String term:activeContext.termDefinitions.keySet()){
			if(term.contains(":"))
				continue;
			JLDContext.TermDefinition termDefinition=activeContext.termDefinitions.get(term);
			if(termDefinition==null || iri.equals(termDefinition.iriMapping) || !iri.startsWith(termDefinition.iriMapping))
				continue;
			String candidate=term+":"+iri.substring(termDefinition.iriMapping.length());
			if((compactIRI==null || candidate.length()<compactIRI.length() || candidate.compareTo(compactIRI)<0) && (!activeContext.termDefinitions.containsKey(candidate) || (value==null && iri.equals(activeContext.termDefinitions.get(candidate).iriMapping)))){
				compactIRI=candidate;
			}
		}
		if(compactIRI!=null)
			return compactIRI;
		// If vocab is false then transform iri to a relative IRI using the document's base IRI.
		// we don't know document IRIs
		return iri;
	}

	private static Object compactValue(JLDContext activeContext, JSONObject inverseContext, String activeProperty, JSONObject value){
		int numberMembers=value.length();
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		if(value.has("@index") && term!=null && "@index".equals(term.containerMapping))
			numberMembers--;
		if(numberMembers>2)
			return value;
		if(value.has("@id")){
			if(numberMembers==1 && term!=null && "@id".equals(term.typeMapping)){
				return compactIRI(activeContext, inverseContext, value.getString("@id"), null, false, false);
			}else if(numberMembers==1 && term!=null && "@vocab".equals(term.typeMapping)){
				return compactIRI(activeContext, inverseContext, value.getString("@id"), null, true, false);
			}
			return value;
		}else if(value.has("@type") && term!=null && value.getString("@type").equals(term.typeMapping)){
			return value.get("@value");
		}else if(value.has("@language") && term!=null && value.getString("@language").equals(term.languageMapping)){
			return value.get("@value");
		}else if(numberMembers==1 && (!(value.get("@value") instanceof String) || activeContext.defaultLanguage==null || (term==null || !term.hasLanguageMapping))){
			return value.get("@value");
		}
		return value;
	}

	private static Object compact(JLDContext activeContext, JSONObject inverseContext, String activeProperty, Object element, boolean compactArrays){
		if(isScalar(element))
			return element;
		JLDContext.TermDefinition term=activeContext.termDefinitions.get(activeProperty);
		if(element instanceof JSONArray){
			JSONArray e=(JSONArray) element;
			JSONArray result=new JSONArray();
			for(int i=0;i<e.length();i++){
				Object item=e.get(i);
				Object compactedElement=compact(activeContext, inverseContext, activeProperty, item, compactArrays);
				if(compactedElement!=null)
					result.put(compactedElement);
			}
			if(result.length()==1 && (term==null || term.containerMapping==null) && compactArrays)
				return result.get(0);
			return result;
		}
		JSONObject e=(JSONObject) element;
		if(e.has("@value") || e.has("@id")){
			Object res=compactValue(activeContext, inverseContext, activeProperty, e);
			if(isScalar(res))
				return res;
		}
		boolean insideReverse="@reverse".equals(activeProperty);
		JSONObject result=new JSONObject();
		ArrayList<String> keys=keysAsList(e);
		Collections.sort(keys);
		for(String expandedProperty:keys){
			Object expandedValue=e.get(expandedProperty);
			Object compactedValue;
			if(expandedProperty.equals("@id") || expandedProperty.equals("@type")){
				if(expandedValue instanceof String){
					compactedValue=compactIRI(activeContext, inverseContext, (String)expandedValue, null, expandedProperty.equals("@type"), false);
				}else{
					JSONArray _compactedValue=new JSONArray();
					JSONArray _expandedValue=(JSONArray)expandedValue;
					for(int i=0;i<_expandedValue.length();i++){
						_compactedValue.put(compactIRI(activeContext, inverseContext, _expandedValue.getString(i), null, true, false));
					}
					if(_compactedValue.length()==1)
						compactedValue=_compactedValue.get(0);
					else
						compactedValue=_compactedValue;
				}
				String alias=compactIRI(activeContext, inverseContext, expandedProperty, null, true, false);
				result.put(alias, compactedValue);
				continue;
			}
			if(expandedProperty.equals("@reverse")){
				JSONObject _compactedValue=(JSONObject) compact(activeContext, inverseContext, "@reverse", expandedValue, compactArrays);
				compactedValue=_compactedValue;
				ArrayList<String> keys2=keysAsList(_compactedValue);
				for(String property:keys2){
					Object value=_compactedValue.get(property);
					JLDContext.TermDefinition pterm=activeContext.termDefinitions.get(property);
					if(pterm!=null && pterm.reverseProperty){
						if(("@set".equals(pterm.containerMapping) || !compactArrays) && !(value instanceof JSONArray))
							value=new JSONArray(Collections.singletonList(value));
						if(!result.has(property)){
							result.put(property, value);
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
						_compactedValue.remove(property);
					}
				}
				if(!_compactedValue.isEmpty()){
					String alias=compactIRI(activeContext, inverseContext, "@reverse", null, true, false);
					result.put(alias, _compactedValue);
				}
				continue;
			}
			if(expandedProperty.equals("@index") && term!=null && "@index".equals(term.containerMapping))
				continue;
			if(expandedProperty.equals("@index") || expandedProperty.equals("@value") || expandedProperty.equals("@language")){
				String alias=compactIRI(activeContext, inverseContext, expandedProperty, null, true, false);
				result.put(alias, expandedValue);
				continue;
			}
			if(expandedValue instanceof JSONArray && ((JSONArray) expandedValue).isEmpty()){
				String itemActiveProperty=compactIRI(activeContext, inverseContext, expandedProperty, expandedValue, true, insideReverse);
				if(!result.has(itemActiveProperty)){
					result.put(itemActiveProperty, Collections.EMPTY_LIST);
				}else if(!(result.get(itemActiveProperty) instanceof JSONArray)){
					result.put(itemActiveProperty, Collections.singletonList(result.getString(itemActiveProperty)));
				}
			}
			JSONArray ev=(JSONArray)expandedValue;
			for(int i=0;i<ev.length();i++){
				Object expandedItem=ev.get(i);
				String itemActiveProperty=compactIRI(activeContext, inverseContext, expandedProperty, expandedItem, true, insideReverse);
				String container=null;
				JLDContext.TermDefinition activeTerm=activeContext.termDefinitions.get(itemActiveProperty);
				if(activeTerm!=null && activeTerm.containerMapping!=null){
					container=activeTerm.containerMapping;
				}
				Object compactedItem=compact(activeContext, inverseContext, itemActiveProperty, expandedItem instanceof JSONObject && ((JSONObject)expandedItem).has("@list") ? ((JSONObject)expandedItem).getJSONObject("@list") : expandedItem, compactArrays);
				if(isListObject(expandedItem)){
					if(!(compactedItem instanceof JSONArray)){
						compactedItem=new JSONArray(Collections.singletonList(compactedItem));
					}
					JSONObject _expandedItem=(JSONObject)expandedItem;
					if(!"@list".equals(container)){
						compactedItem=jsonObjectWithSingleKey(compactIRI(activeContext, inverseContext, "@list", compactedItem, false, false), compactedItem);
						if(_expandedItem.has("@index")){
							((JSONObject)compactedItem).put(compactIRI(activeContext, inverseContext, "@index", _expandedItem.get("@index"), false, false), _expandedItem.get("@index"));
						}
					}else{
						throw new JLDException("compaction to list of lists");
					}
				}
				if("@language".equals(container) || "@index".equals(container)){
					if(!result.has(itemActiveProperty))
						result.put(itemActiveProperty, new JSONObject());
					JSONObject mapObject=result.getJSONObject(itemActiveProperty);
					if("@language".equals(container) && compactedItem instanceof JSONObject && ((JSONObject) compactedItem).has("@value")){
						compactedItem=((JSONObject) compactedItem).get("@value");
						JSONObject _expandedItem=(JSONObject)expandedItem;
						String mapKey=_expandedItem.getString(container);
						if(!mapObject.has(mapKey)){
							mapObject.put(mapKey, compactedItem);
						}else{
							Object o=mapObject.get(mapKey);
							if(o instanceof JSONArray)
								((JSONArray) o).put(compactedItem);
							else
								mapObject.put(mapKey, new JSONArray(Arrays.asList(mapObject.get(mapKey), compactedItem)));
						}
					}
				}else{
					if(!compactArrays && ("@list".equals(container) || "@set".equals(container) || "@list".equals(expandedProperty) || "@graph".equals(expandedProperty)) && !(compactedItem instanceof JSONArray)){
						compactedItem=new JSONArray(Collections.singletonList(compactedItem));
					}
					if(!result.has(itemActiveProperty)){
						result.put(itemActiveProperty, compactedItem);
					}else{
						Object o=result.get(itemActiveProperty);
						if(o instanceof JSONArray)
							((JSONArray) o).put(compactedItem);
						else
							result.put(itemActiveProperty, new JSONArray(Arrays.asList(result.get(itemActiveProperty), compactedItem)));
					}
				}
			}
		}
		return result;
	}
}
