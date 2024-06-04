package smithereen.text;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.ins.InsExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.select.NodeVisitor;
import org.unbescape.html.HtmlEscape;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.jirutka.unidecode.Unidecode;
import smithereen.Config;
import smithereen.Utils;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.storage.UserStorage;
import smithereen.util.TopLevelDomainList;
import spark.utils.StringUtils;

public class TextProcessor{
	private static final Whitelist HTML_SANITIZER=new MicroFormatAwareHTMLWhitelist();
	private static final Whitelist HTML_SANITIZER_MARKDOWN=new MicroFormatAwareHTMLWhitelist().addProtocols("a", "href", "acct");
	private static final Pattern POST_LINE_BREAKS=Pattern.compile("\n+");
	// https://unicode.org/faq/idn.html#33, mostly
	private static final String IDN_VALID_CHAR_REGEX="[[\\u00B7\\u0375\\u05F3\\u05F4\\u30FB\\u002D\\u06FD\\u06FE\\u0F0B\\u3007\\u00DF\\u03C2\\u200C\\u200D][^\\p{IsControl}\\p{IsWhite_Space}\\p{gc=S}\\p{IsPunctuation}\\p{gc=Nl}\\p{gc=No}\\p{gc=Me}\\p{blk=Combining_Diacritical_Marks}\\p{blk=Musical_Symbols}\\p{block=Ancient_Greek_Musical_Notation}\\u0640\\u07FA\\u302E\\u302F\\u3031-\\u3035\\u303B]]";
	// A domain must be at least 2 (possibly IDN) labels
	private static final String IDN_DOMAIN_REGEX=IDN_VALID_CHAR_REGEX+"+(?:\\."+IDN_VALID_CHAR_REGEX+"+)+";
	public static final Pattern USERNAME_DOMAIN_PATTERN=Pattern.compile("@?([a-zA-Z0-9._-]+)@("+IDN_DOMAIN_REGEX+")");
	public static final Pattern MENTION_PATTERN=Pattern.compile("@([a-zA-Z0-9._-]+)(?:@("+IDN_DOMAIN_REGEX+"))?");
	public static final Pattern URL_PATTERN=Pattern.compile("\\b(https?:\\/\\/)?("+IDN_DOMAIN_REGEX+")(?:\\:\\d+)?((?:\\/(?:[\\w\\.~@%:!+-]|\\([^\\s]+?\\))*)*)(\\?(?:\\w+(?:=(?:[\\w\\.~@%:!+-]|\\([^\\s]+?\\))+&?)?)+)?(#(?:[\\w\\.~@%:!+-]|\\([^\\s]+?\\))+)?", Pattern.CASE_INSENSITIVE);
	private static final Pattern MATRIX_USERNAME_PATTERN=Pattern.compile("@?([a-zA-Z0-9._-]+)[:@]("+IDN_DOMAIN_REGEX+")");
	private static final Pattern SIGNAL_USERNAME_PATTERN=Pattern.compile("^@?([a-zA-Z0-9._-]+\\.\\d{2})$");
	private static final Pattern LOCAL_USERNAME_PATTERN=Pattern.compile("^@?([a-zA-Z0-9._-]+)$");
	private static final Unidecode unidecode=Unidecode.toAscii();

	private static final List<Extension> markdownExtensions=List.of(StrikethroughExtension.create(), InsExtension.create());
	private static final org.commonmark.parser.Parser markdownParser=new org.commonmark.parser.Parser.Builder()
			.enabledBlockTypes(Set.of(BlockQuote.class, FencedCodeBlock.class))
			.extensions(markdownExtensions)
			.build();
	private static final HtmlRenderer markdownHtmlRenderer=new HtmlRenderer.Builder()
			.softbreak("<br/>")
			.escapeHtml(true)
			.extensions(markdownExtensions)
			.build();

	public static String sanitizeHTML(String src){
		return sanitizeHTML(src, null);
	}

	public static String sanitizeHTML(String src, URI documentLocation){
		Cleaner cleaner=new Cleaner(HTML_SANITIZER);
		Document doc=Parser.parseBodyFragment(src, Objects.toString(documentLocation, ""));
		doc.body().traverse(new NodeVisitor(){
			private final LinkedList<ListNodeInfo> listStack=new LinkedList<>();

			@Override
			public void head(Node node, int depth){
				if(node instanceof Element el){
					switch(el.tagName()){
						case "em" -> el.tagName("i");
						case "strong" -> el.tagName("b");
						case "ins" -> el.tagName("u");
						case "del" -> el.tagName("s");
					}
					if("ul".equals(el.tagName()) || "ol".equals(el.tagName())){
						listStack.push(new ListNodeInfo("ol".equals(el.tagName()), el));
						el.tagName("p");
					}else if("li".equals(el.tagName()) && !listStack.isEmpty()){
						ListNodeInfo info=listStack.peek();
						String prefix="  ".repeat(listStack.size()-1);
						if(info.isOrdered){
							prefix+=info.currentIndex+". ";
							info.currentIndex++;
						}else{
							prefix+="- ";
						}
						el.prependText(prefix);
						if(el.nextSibling()!=null){
							el.appendChild(doc.createElement("br"));
						}
					}
				}
			}

			@Override
			public void tail(Node node, int depth){
				if(node instanceof Element el){
					if(!listStack.isEmpty() && listStack.peek().element==el){
						listStack.pop();
					}
					if("img".equals(el.tagName())){
						String src=el.attr("src");
						el.tagName("a")
								.removeAttr("src")
								.attr("href", src)
								.text(src);
					}
				}
			}

			private static class ListNodeInfo{
				final boolean isOrdered;
				final Element element;
				int currentIndex=1;

				private ListNodeInfo(boolean isOrdered, Element element){
					this.isOrdered=isOrdered;
					this.element=element;
				}
			}
		});
		doc.getElementsByTag("li").forEach(Element::unwrap);
		doc.getElementsByClass("smithereenPollQuestion").forEach(Element::remove);
		doc.getElementsByTag("span").stream().filter(el->el.classNames().isEmpty()).forEach(Element::unwrap);
		doc.normalise();
		return cleaner.clean(doc).body().html();
	}

	public static String truncateOnWordBoundary(String s, int maxLen){
		s=Jsoup.clean(s, Whitelist.none());
		if(s.length()<=maxLen+20)
			return s;
		int len=Math.max(0, Math.min(s.indexOf(' ', maxLen), maxLen+20));
		return s.substring(0, len)+"...";
	}

	public static String escapeHTML(String s){
		return HtmlEscape.escapeHtml4Xml(s);
	}

	public static String stripHTML(String s, boolean keepLineBreaks){
		if(keepLineBreaks){
			Document doc=new Cleaner(new Whitelist().addTags("p", "br")).clean(Jsoup.parseBodyFragment(s));
			StringBuilder sb=new StringBuilder();
			doc.body().traverse(new NodeVisitor(){
				@Override
				public void head(Node node, int depth){
					if(node instanceof Element el){
						if("p".equalsIgnoreCase(el.tagName()) && !sb.isEmpty())
							sb.append("\n\n");
						else if("br".equalsIgnoreCase(el.tagName()))
							sb.append('\n');
					}else if(node instanceof TextNode tn){
						sb.append(tn.text());
					}
				}
			});
			return sb.toString().trim();
		}else{
			return new Cleaner(Whitelist.none()).clean(Jsoup.parseBodyFragment(s.replace("</p><", "</p> <"))).body().html();
		}
	}

	private static void makeLinksAndMentions(Node node, @Nullable MentionCallback mentionCallback){
		if(node instanceof Element el){
			if(el.tagName().equalsIgnoreCase("pre")){
				return;
			}else if(el.tagName().equalsIgnoreCase("a")){
				if(el.hasClass("mention") && !el.hasAttr("data-user-id")){
					User user;
					if(mentionCallback!=null){
						String href=el.attr("href");
						if(href.startsWith("acct:")){
							String[] parts=href.substring(5).split("@");
							user=mentionCallback.resolveMention(parts[0], parts.length>1 ? parts[1] : null);
						}else{
							user=mentionCallback.resolveMention(href);
						}
					}else{
						user=null;
					}
					if(user==null){
						el.removeClass("mention");
					}else{
						el.attr("href", user.url.toString());
						el.attr("data-user-id", user.id+"");
					}
				}
				return;
			}
			for(int i=0;i<el.childNodeSize();i++){
				makeLinksAndMentions(el.childNode(i), mentionCallback);
			}
		}else if(node instanceof TextNode text){
			Matcher matcher=URL_PATTERN.matcher(text.text());

			outer:
			while(matcher.find()){
				String url=matcher.group();

				// don't make domain.com in a @user@domain.com mention a link
				if(matcher.start()>0 && text.text().charAt(matcher.start()-1)=='@')
					continue;

				String scheme=matcher.group(1);
				String host=matcher.group(2);

				// Additionally validate IPv4 addresses
				if(host.matches("^[\\d.]+")){
					Matcher matcher2=Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})").matcher(url);
					if(!matcher2.find())
						continue;
					for(int i=1;i<=4;i++){
						int b=Utils.parseIntOrDefault(matcher2.group(i), -1);
						if(b<0 || b>255)
							continue outer;
					}
				}

				// If there's no scheme, check the domain against the list of actually existing TLDs
				// Because some people are weird.They write like this.
				if(StringUtils.isEmpty(scheme)){
					String tld=host.substring(host.lastIndexOf('.')+1);
					if(!TopLevelDomainList.contains(tld))
						continue;
				}

				TextNode inner=matcher.start()==0 ? text : text.splitText(matcher.start());
				int len=matcher.end()-matcher.start();

				// Don't include punctuation, if any, following the URL, into it
				char last=url.charAt(url.length()-1);
				if(last=='.' || last=='?' || last=='!' || last==':' || last==';'){
					len--;
					url=url.substring(0, len);
				}

				if(len<inner.text().length())
					inner.splitText(len);
				String realURL=url;
				if(StringUtils.isEmpty(scheme)){
					realURL="http://"+url;
				}
				inner.wrap("<a href=\""+escapeHTML(realURL)+"\">");
				return;
			}

			matcher=MENTION_PATTERN.matcher(text.text());
			while(matcher.find()){
				String u=matcher.group(1);
				String d=matcher.group(2);
				if(d!=null && d.equalsIgnoreCase(Config.domain)){
					d=null;
				}
				User mentionedUser=mentionCallback==null ? null : mentionCallback.resolveMention(u, d);
				if(mentionedUser!=null){
					TextNode inner=matcher.start()==0 ? text : text.splitText(matcher.start());
					int len=matcher.end()-matcher.start();
					if(len<inner.text().length())
						inner.splitText(len);
					inner.wrap("<a href=\""+escapeHTML(mentionedUser.url.toString())+"\" class=\"mention\" data-user-id=\""+mentionedUser.id+"\">");
					return;
				}
			}
		}
	}

	public static String preprocessPostHTML(String text, MentionCallback mentionCallback){
		return preprocessPostText(text, mentionCallback, FormattedTextFormat.HTML);
	}

	public static String preprocessPostText(String text, MentionCallback mentionCallback, FormattedTextFormat format){
		text=text.trim().replace("\r", "");

		Whitelist whitelist;
		if(format==FormattedTextFormat.MARKDOWN){
			text=markdownHtmlRenderer.render(markdownParser.parse(text));
			whitelist=HTML_SANITIZER_MARKDOWN;
		}else{
			whitelist=HTML_SANITIZER;
		}

		Document doc=Jsoup.parseBodyFragment(text);
		Element newBody;
		if(format==FormattedTextFormat.HTML){
			doc=new Cleaner(whitelist).clean(doc);
			Element body=doc.body();
			Document newDoc=new Document("");
			newBody=newDoc.body();
			LinkedList<Element> stack=new LinkedList<>(), tmpStack=new LinkedList<>();
			stack.add(newBody);

			body.traverse(new NodeVisitor(){

				@Override
				public void head(@NotNull Node node, int depth){
					if(depth==0)
						return;
					if(node instanceof Element el){
						String tagName=el.tagName();
						Element newEl=newDoc.createElement(tagName);
						for(Attribute attr: el.attributes().asList()){
							newEl.attr(attr.getKey(), attr.getValue());
						}
						if(depth==1){
							if(!tagName.equalsIgnoreCase("p") && !tagName.equalsIgnoreCase("pre") && !tagName.equals("blockquote")){
								if(stack.size()==1){
									Element p=newDoc.createElement("p");
									newBody.appendChild(p);
									stack.push(p);
								}
							}else if(stack.size()>1){
								stack.pop();
							}
						}
						Objects.requireNonNull(stack.peek()).appendChild(newEl);
						stack.push(newEl);
					}else if(node instanceof TextNode tn){
						if(depth==1 && stack.size()==1){
							Element p=newDoc.createElement("p");
							newBody.appendChild(p);
							stack.push(p);
						}
						String text=tn.getWholeText();
						if(stack.get(stack.size()-2).tagName().equalsIgnoreCase("pre")){
							Objects.requireNonNull(stack.peek()).appendText(text);
						}else{
							Matcher matcher=POST_LINE_BREAKS.matcher(text);
							int lastPos=0;
							while(matcher.find()){
								Objects.requireNonNull(stack.peek()).appendText(text.substring(lastPos, matcher.start()));
								lastPos=matcher.end();
								int length=matcher.end()-matcher.start();
								if(length==1){
									// Don't add a <br> as last element inside <p>
									Element parent=Objects.requireNonNull(stack.peek());
									if(!parent.tagName().equalsIgnoreCase("p") || lastPos<text.length())
										parent.appendChild(newDoc.createElement("br"));
								}else{
									while(stack.size()>1){
										tmpStack.push(stack.pop().shallowClone());
									}
									while(!tmpStack.isEmpty()){
										Element el=tmpStack.pop();
										Objects.requireNonNull(stack.peek()).appendChild(el);
										stack.push(el);
									}
								}
							}
							Objects.requireNonNull(stack.peek()).appendText(text.substring(lastPos));
						}
					}
				}

				@Override
				public void tail(@NotNull Node node, int depth){
					if(depth>0 && node instanceof Element el){
						Element stackEl=stack.pop();
						if(!el.tagName().equals(stackEl.tagName())) // sanity check
							throw new IllegalStateException("Expected tag "+stackEl.tagName()+", got "+el.tagName());
					}
				}
			});
		}else{
			Element body=doc.body();
			body.traverse((node, depth)->{
				if(node instanceof Element el){
					String tagName=el.tagName();
					switch(tagName){
						case "em" -> el.tagName("i");
						case "strong" -> el.tagName("b");
						case "ins" -> el.tagName("u");
						case "del" -> el.tagName("s");
						case "img" -> {
							String src=el.attr("src");
							String alt=el.attr("alt");
							el.tagName("a");
							el.removeAttr("src");
							el.removeAttr("alt");
							el.attr("href", src);
							if(StringUtils.isNotEmpty(alt))
								el.text(alt);
							else
								el.text(src);
						}
						case "a" -> {
							String href=el.attr("href");
							if(href.startsWith("@")){ // Mentions with custom text, e.g. "oh hi [Mark](@zuck@threads.net)"
								Matcher matcher=MENTION_PATTERN.matcher(href);
								if(matcher.find() && matcher.start()==0 && matcher.end()==href.length()){
									String username=matcher.group(1), domain=matcher.group(2);
									String newHref="acct:"+username;
									if(StringUtils.isNotEmpty(domain))
										newHref+="@"+domain;
									el.attr("href", newHref);
									el.addClass("mention");
								}
							}
						}
					};
				}
			});
			doc=new Cleaner(whitelist).clean(doc);
			newBody=doc.body();
		}

		makeLinksAndMentions(newBody, mentionCallback);

		return newBody.html();
	}

	public static String postprocessPostHTMLForDisplay(String text, boolean forceTargetBlank){
		if(text==null)
			return "";
		Document doc=Jsoup.parseBodyFragment(text);

		for(Element el:doc.getElementsByTag("a")){
			if(el.hasClass("mention") && el.hasAttr("data-user-id")){
				int uid=Utils.parseIntOrDefault(el.attr("data-user-id"), 0);
				if(uid>0){
					try{
						User user=UserStorage.getById(uid);
						if(user!=null){
							el.attr("href", "/"+user.getFullUsername());
							if(user instanceof ForeignUser){
								el.attr("rel", "nofollow");
							}
							el.addClass("u-url");
							el.addClass("hoverCardTrigger");
							Element parent=el.parent();
							if(parent==null || !parent.tagName().equalsIgnoreCase("span")){
								el.wrap("<span class=\"h-card hoverCardContainer\">");
							}else{
								parent.addClass("hoverCardContainer");
							}
						}
					}catch(SQLException ignore){}
				}
				if(forceTargetBlank){
					el.attr("target", "_blank");
				}
			}else{
				String href=el.attr("href");
				try{
					URI uri=new URI(href);
					if(forceTargetBlank || (uri.isAbsolute() && (uri.getHost()==null || !Config.isLocal(uri)))){
						el.attr("target", "_blank");
						if(!el.hasAttr("rel"))
							el.attr("rel", "noopener ugc");
					}
				}catch(URISyntaxException x){}
			}
		}

		return doc.body().html();
	}

	public static String postprocessPostHTMLForActivityPub(String text){
		Document doc=Jsoup.parseBodyFragment(text);

		for(Element el:doc.getElementsByTag("a")){
			el.removeAttr("data-user-id");
		}

		return doc.body().html();
	}

	public static String preprocessRemotePostMentions(String text, Map<Integer, User> users){
		Document doc=Jsoup.parseBodyFragment(text);

		for(Element link:doc.select("a.mention")){
			URI href=URI.create(link.attr("href"));
			boolean found=false;
			for(User user:users.values()){
				if(href.equals(user.url) || href.equals(user.activityPubID)){
					link.attr("data-user-id", String.valueOf(user.id));
					found=true;
					break;
				}
			}
			if(!found){
				link.removeClass("mention");
			}
			Element parent=link.parent();
			if(parent!=null && parent.tagName().equalsIgnoreCase("span"))
				parent.unwrap();
		}

		return doc.body().html();
	}

	public static String transliterate(String in){
		if(in==null)
			return null;
		return unidecode.decode(in.trim()).replaceAll(Pattern.quote("[?]"), "");
	}

	public static String substituteLinks(String str, Map<String, Object> links){
		Element root=Jsoup.parseBodyFragment(str).body();
		for(String id:links.keySet()){
			Element link=root.getElementById(id);
			if(link==null)
				continue;
			link.removeAttr("id");
			//noinspection unchecked
			Map<String, Object> attrs=(Map<String, Object>) links.get(id);
			for(String attr:attrs.keySet()){
				Object value=attrs.get(attr);
				if(attr.equals("_")){
					link.tagName(value.toString());
				}else if(value instanceof Boolean b)
					link.attr(attr, b);
				else if(value instanceof String s)
					link.attr(attr, s);
				else if(value!=null)
					link.attr(attr, value.toString());
			}
		}
		return root.html();
	}

	private static String normalizeUsernameOrLink(String value, String... allowedDomains){
		if(Utils.isURL(value)){
			URI uri=URI.create(value);
			for(String domain:allowedDomains){
				if(domain.equalsIgnoreCase(uri.getHost())){
					return uri.getPath().substring(1);
				}
			}
			return null;
		}
		Matcher m=LOCAL_USERNAME_PATTERN.matcher(value);
		if(m.find())
			return m.group(1);
		return null;
	}

	public static String normalizeContactInfoValue(User.ContactInfoKey key, String value){
		value=value.trim();
		return switch(key){
			case MATRIX -> {
				Matcher m=MATRIX_USERNAME_PATTERN.matcher(value);
				if(m.find())
					yield "@"+m.group(1)+":"+m.group(2);
				else
					yield null;
			}
			case TELEGRAM -> normalizeUsernameOrLink(value, "t.me");
			case SIGNAL -> {
				if(Utils.isURL(value)){
					URI uri=URI.create(value);
					if("signal.me".equals(uri.getHost()))
						yield value;
					else
						yield null;
				}
				Matcher m=SIGNAL_USERNAME_PATTERN.matcher(value);
				if(m.find())
					yield m.group(1);
				else
					yield null;
			}
			case TWITTER -> normalizeUsernameOrLink(value, "twitter.com", "x.com");
			case INSTAGRAM -> normalizeUsernameOrLink(value, "instagram.com", "www.instagram.com", "instagr.am");
			case FACEBOOK -> normalizeUsernameOrLink(value, "facebook.com", "www.facebook.com", "m.facebook.com");
			case VKONTAKTE -> normalizeUsernameOrLink(value, "vk.com", "m.vk.com");
			case SNAPCHAT -> {
				if(Utils.isURL(value)){
					URI uri=URI.create(value);
					String path=uri.getPath();
					if("www.snapchat.com".equals(uri.getHost()) && path.startsWith("/add/"))
						yield path.substring(path.lastIndexOf('/')+1);
					yield null;
				}
				Matcher m=LOCAL_USERNAME_PATTERN.matcher(value);
				if(m.find())
					yield m.group(1);
				yield null;
			}
			case DISCORD -> {
				Matcher m=LOCAL_USERNAME_PATTERN.matcher(value);
				if(m.find())
					yield m.group(1);
				yield null;
			}
			case EMAIL -> Utils.isValidEmail(value) ? value : null;
			case GIT -> {
				if(Utils.isURL(value)){
					if(!value.startsWith("https://") && !value.startsWith("http://"))
						yield "https://"+value;
					yield value;
				}
				yield null;
			}
			default -> value;
		};
	}

	public static String getContactInfoValueURL(User.ContactInfoKey key, String value){
		return switch(key){
			case MATRIX -> "https://matrix.to/#/"+value;
			case XMPP -> "xmpp:"+value;
			case TELEGRAM -> "https://t.me/"+value;
			case SIGNAL -> Utils.isURL(value) ? value : null;
			case TWITTER -> "https://twitter.com/"+value;
			case INSTAGRAM -> "https://instagram.com/"+value;
			case FACEBOOK -> "https://facebook.com/"+value;
			case VKONTAKTE -> "https://vk.com/"+value;
			case SNAPCHAT -> "https://www.snapchat.com/add/"+value;
			case EMAIL -> "mailto:"+value;
			case PHONE_NUMBER -> "tel:"+value;
			case GIT -> value;
			default -> null;
		};
	}

	public interface MentionCallback{
		User resolveMention(String username, String domain);
		User resolveMention(String uri);
	}
}
