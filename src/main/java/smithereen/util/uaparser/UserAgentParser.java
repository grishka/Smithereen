package smithereen.util.uaparser;

import org.intellij.lang.annotations.RegExp;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spark.utils.StringUtils;

public class UserAgentParser{
	private static final List<BrowserTypeRule> RULES=List.of(
			new BrowserTypeRule(BrowserType.GOOGLEBOT, "googlebot"),
			new BrowserTypeRule(BrowserType.OPERA_PRESTO, "opera"),
			new BrowserTypeRule(BrowserType.OPERA_CHROMIUM, "opr\\/|opios"),
			new BrowserTypeRule(BrowserType.SAMSUNG_BROWSER, "SamsungBrowser"),
			new BrowserTypeRule(BrowserType.WHALE, "Whale"),
			new BrowserTypeRule(BrowserType.PALE_MOON, "PaleMoon"),
			new BrowserTypeRule(BrowserType.MZ_BROWSER, "MZBrowser"),
			new BrowserTypeRule(BrowserType.FOCUS, "focus"),
			new BrowserTypeRule(BrowserType.SWING, "swing"),
			new BrowserTypeRule(BrowserType.OPERA_COAST, "coast"),
			new BrowserTypeRule(BrowserType.OPERA_TOUCH, "opt\\/\\d+(?:.?_?\\d+)+"),
			new BrowserTypeRule(BrowserType.YANDEX_BROWSER, "yabrowser"),
			new BrowserTypeRule(BrowserType.UC_BROWSER, "ucbrowser"),
			new BrowserTypeRule(BrowserType.MAXTHON, "Maxthon|mxios"),
			new BrowserTypeRule(BrowserType.EPIPHANY, "epiphany"),
			new BrowserTypeRule(BrowserType.PUFFIN, "puffin"),
			new BrowserTypeRule(BrowserType.SLEIPNIR, "sleipnir"),
			new BrowserTypeRule(BrowserType.K_MELEON, "k-meleon"),
			new BrowserTypeRule(BrowserType.WECHAT, "micromessenger"),
			new BrowserTypeRule(BrowserType.QQ_BROWSER, "qqbrowser"),
			new BrowserTypeRule(BrowserType.INTERNET_EXPLORER, "msie|trident"),
			new BrowserTypeRule(BrowserType.MS_EDGE, "\\sedg\\/"),
			new BrowserTypeRule(BrowserType.MS_EDGE_IOS, "edg([ea]|ios)"),
			new BrowserTypeRule(BrowserType.VIVALDI, "vivaldi"),
			new BrowserTypeRule(BrowserType.SEAMONKEY, "seamonkey"),
			new BrowserTypeRule(BrowserType.SAILFISH, "sailfish"),
			new BrowserTypeRule(BrowserType.AMAZON_SILK, "silk"),
			new BrowserTypeRule(BrowserType.PHANTOMJS, "phantom"),
			new BrowserTypeRule(BrowserType.SLIMERJS, "slimerjs"),
			new BrowserTypeRule(BrowserType.BLACKBERRY, "blackberry|\\bbb\\d+", "rim\\stablet"),
			new BrowserTypeRule(BrowserType.WEBOS_BROWSER, "(web|hpw)[o0]s"),
			new BrowserTypeRule(BrowserType.BADA, "bada"),
			new BrowserTypeRule(BrowserType.TIZEN, "tizen"),
			new BrowserTypeRule(BrowserType.QUPZILLA, "qupzilla"),
			new BrowserTypeRule(BrowserType.FIREFOX, "firefox|iceweasel|fxios"),
			new BrowserTypeRule(BrowserType.ELECTRON, "electron"),
			new BrowserTypeRule(BrowserType.MIUI_BROWSER, "MiuiBrowser"),
			new BrowserTypeRule(BrowserType.CHROMIUM, "chromium"),
			new BrowserTypeRule(BrowserType.CHROME, "chrome|crios|crmo"),
			new BrowserTypeRule(BrowserType.GOOGLE_SEARCH, "GSA"),
			new BrowserTypeRule(BrowserType.ANDROID_BROWSER, "(?<!like )android"),
			new BrowserTypeRule(BrowserType.PLAYSTATION_4, "playstation 4"),
			new BrowserTypeRule(BrowserType.SAFARI, "safari|applewebkit"),
			new BrowserTypeRule(BrowserType.OTHER, ".*")
	);
	private static final List<OSRule> OS_RULES=List.of(
			new OSRule(BrowserOSFamily.ROKU, Pattern.compile("Roku/DVP")),
			new OSRule(BrowserOSFamily.WINDOWS_PHONE, "windows phone"),
			new OSRule(BrowserOSFamily.WINDOWS, "windows "),
			new OSRule(BrowserOSFamily.IOS, Pattern.compile("Macintosh(.*?) FxiOS(.*?)/")),
			new OSRule(BrowserOSFamily.MAC_OS, "macintosh"),
			new OSRule(BrowserOSFamily.IOS, "(ipod|iphone|ipad)"),
			new OSRule(BrowserOSFamily.ANDROID, "(?<!like )android"),
			new OSRule(BrowserOSFamily.WEBOS, "(web|hpw)[o0]s"),
			new OSRule(BrowserOSFamily.BLACKBERRY, "(blackberry|\\bbb\\d+|rim\\stablet)"),
			new OSRule(BrowserOSFamily.BADA, "bada"),
			new OSRule(BrowserOSFamily.TIZEN, "tizen"),
			new OSRule(BrowserOSFamily.LINUX, "linux"),
			new OSRule(BrowserOSFamily.CHROME_OS, Pattern.compile("CrOS")),
			new OSRule(BrowserOSFamily.PLAYSTATION_4, Pattern.compile("PlayStation 4"))
	);
	private static final List<PlatformTypeRule> PLATFORM_RULES=List.of(
			new PlatformTypeRule(BrowserPlatformType.BOT, Pattern.compile("googlebot", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, Pattern.compile("huawei", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("nexus\\s*(?:7|8|9|10).*", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("ipad", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("Macintosh(.*?) FxiOS(.*?)/")),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("kftt build", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("silk", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.TABLET, Pattern.compile("tablet(?! pc)", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, Pattern.compile("(?<!like )(ipod|iphone)", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, Pattern.compile("[^-]mobi", Pattern.CASE_INSENSITIVE)),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, BrowserOSFamily.BLACKBERRY),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, BrowserOSFamily.BADA),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, BrowserOSFamily.WINDOWS_PHONE),
			new PlatformTypeRule(BrowserPlatformType.MOBILE, BrowserOSFamily.ANDROID),
			new PlatformTypeRule(BrowserPlatformType.DESKTOP, BrowserOSFamily.MAC_OS),
			new PlatformTypeRule(BrowserPlatformType.DESKTOP, BrowserOSFamily.WINDOWS),
			new PlatformTypeRule(BrowserPlatformType.DESKTOP, BrowserOSFamily.LINUX),
			new PlatformTypeRule(BrowserPlatformType.TV, BrowserOSFamily.PLAYSTATION_4),
			new PlatformTypeRule(BrowserPlatformType.TV, BrowserOSFamily.ROKU)
	);

	private static final Pattern REGEX_WITHOUT_DEVICE_SPEC=Pattern.compile("^(.*)/(.*) ");
	private static final Pattern REGEX_WITH_DEVICE_SPEC=Pattern.compile("^(.*)/(.*)[ \\t]\\((.*)");
	private static final Pattern COMMON_VERSION_IDENTIFIER=Pattern.compile("version/(\\d+(\\.?_?\\d+)+)", Pattern.CASE_INSENSITIVE);

	private static final Pattern GOOGLEBOT_VERSION=Pattern.compile("googlebot\\/(\\d+(\\.\\d+))", Pattern.CASE_INSENSITIVE);
	private static final Pattern OPERA_VERSION1=makeVersionRegexA("opera");
	private static final Pattern OPERA_VERSION2=Pattern.compile("(?:opr|opios)[\\s/](\\S+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SAMSUNG_VERSION=makeVersionRegexA("SamsungBrowser");
	private static final Pattern WHALE_VERSION=Pattern.compile("(?:whale)[\\s/](\\d+(?:\\.\\d+)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern PALE_MOON_VERSION=Pattern.compile("(?:PaleMoon)[\\s/](\\d+(?:\\.\\d+)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern MZ_BROWSER_VERSION=Pattern.compile("(?:MZBrowser)[\\s/](\\d+(?:\\.\\d+)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern FOCUS_VERSION=Pattern.compile("(?:focus)[\\s/](\\d+(?:\\.\\d+)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SWING_VERSION=Pattern.compile("(?:swing)[\\s/](\\d+(?:\\.\\d+)+)", Pattern.CASE_INSENSITIVE);
	private static final Pattern OPERA_COAST_VERSION=makeVersionRegexA("coast");
	private static final Pattern OPERA_TOUCH_VERSION=makeVersionRegexA("opt");
	private static final Pattern YANDEX_VERSION=makeVersionRegexA("yabrowser");
	private static final Pattern UC_VERSION=makeVersionRegexA("ucbrowser");
	private static final Pattern MAXTHON_VERSION=makeVersionRegexA("Maxthon|mxios");
	private static final Pattern EPIPHANY_VERSION=makeVersionRegexA("epiphany");
	private static final Pattern PUFFIN_VERSION=makeVersionRegexA("puffin");
	private static final Pattern SLEIPNIR_VERSION=makeVersionRegexA("sleipnir");
	private static final Pattern K_MELEON_VERSION=makeVersionRegexA("k-meleon");
	private static final Pattern WECHAT_VERSION=makeVersionRegexA("micromessenger");
	private static final Pattern QQ_VERSION=makeVersionRegexB("(?:qqbrowserlite|qqbrowser)");
	private static final Pattern IE_VERSION=makeVersionRegexB("(?:msie |rv:)");
	private static final Pattern EDGE_VERSION=makeVersionRegexB("\\sedg");
	private static final Pattern EDGE_IOS_VERSION=makeVersionRegexB("edg(?:[ea]|ios)");
	private static final Pattern VIVALDI_VERSION=makeVersionRegexB("vivaldi");
	private static final Pattern SEAMONKEY_VERSION=makeVersionRegexB("seamonkey");
	private static final Pattern SAILFISH_VERSION=Pattern.compile("sailfish\\s?browser\\/(\\d+(\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
	private static final Pattern SILK_VERSION=makeVersionRegexB("silk");
	private static final Pattern PHANROMJS_VERSION=makeVersionRegexB("phantomjs");
	private static final Pattern SLIMERJS_VERSION=makeVersionRegexB("slimerjs");
	private static final Pattern BLACKBERRY_VERSION=makeVersionRegexB("blackberry[\\d]+");
	private static final Pattern WEBOS_VERSION=makeVersionRegexB("w(?:eb)?[o0]sbrowser");
	private static final Pattern BADA_VERSION=makeVersionRegexB("dolfin");
	private static final Pattern TIZEN_VERSION=makeVersionRegexB("(?:tizen\\s?)?browser");
	private static final Pattern QUPZILLA_VERSION=makeVersionRegexA("qupzilla");
	private static final Pattern FIREFOX_VERSION=makeVersionRegexA("firefox|iceweasel|fxios");
	private static final Pattern ELECTRON_VERSION=makeVersionRegexB("electron");
	private static final Pattern MIUI_VERSION=makeVersionRegexA("MiuiBrowser");
	private static final Pattern CHROMIUM_VERSION=makeVersionRegexA("chromium");
	private static final Pattern CHROME_VERSION=makeVersionRegexB("(?:chrome|crios|crmo)");
	private static final Pattern GOOGLE_VERSION=makeVersionRegexB("GSA");


	public static BrowserInfo parse(String ua){
		if(StringUtils.isEmpty(ua)){
			return new BrowserInfo("Unknown Browser", null, BrowserPlatformType.DESKTOP, BrowserOSFamily.UNKNOWN);
		}
		BrowserType type=null;
		outer:
		for(BrowserTypeRule rule:RULES){
			for(Pattern regex:rule.regexes){
				if(regex.matcher(ua).find()){
					type=rule.type;
					break outer;
				}
			}
		}
		if(type==null)
			throw new IllegalStateException();
		BrowserOSFamily os=BrowserOSFamily.UNKNOWN;
		for(OSRule rule:OS_RULES){
			if(rule.regex.matcher(ua).find()){
				os=rule.os;
				break;
			}
		}
		BrowserPlatformType platformType=BrowserPlatformType.DESKTOP;
		for(PlatformTypeRule rule:PLATFORM_RULES){
			if(rule.predicate.test(ua, os)){
				platformType=rule.type;
				break;
			}
		}
		return new BrowserInfo(type.getName(ua), type.getVersion(ua), platformType, os);
	}

	private static Pattern makeVersionRegexA(@RegExp String namePart){
		return Pattern.compile("(?:"+namePart+")[\\s/](\\d+(\\.?_?\\d+)+)", Pattern.CASE_INSENSITIVE);
	}

	private static Pattern makeVersionRegexB(@RegExp String namePart){
		return Pattern.compile(namePart+"/(\\d+(\\.?_?\\d+)+)", Pattern.CASE_INSENSITIVE);
	}

	private static String getFirstMatch(String str, Pattern pattern){
		Matcher matcher=pattern.matcher(str);
		return matcher.find() ? matcher.group(1) : null;
	}

	private static String getFirstMatchWithFallback(String str, Pattern pattern, Pattern fallback){
		String match=getFirstMatch(str, pattern);
		if(StringUtils.isNotEmpty(match))
			return match;
		return getFirstMatch(str, fallback);
	}

	private record BrowserTypeRule(BrowserType type, List<Pattern> regexes){
		public BrowserTypeRule(BrowserType type, @RegExp String regex){
			this(type, List.of(Pattern.compile(regex, Pattern.CASE_INSENSITIVE)));
		}

		public BrowserTypeRule(BrowserType type, @RegExp String... regexes){
			this(type, Arrays.stream(regexes).map(r->Pattern.compile(r, Pattern.CASE_INSENSITIVE)).toList());
		}
	}

	private record OSRule(BrowserOSFamily os, Pattern regex){
		public OSRule(BrowserOSFamily os, String regex){
			this(os, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
		}
	}

	private record PlatformTypeRule(BrowserPlatformType type, BiPredicate<String, BrowserOSFamily> predicate){
		public PlatformTypeRule(BrowserPlatformType type, Pattern regex){
			this(type, (ua, os)->regex.matcher(ua).find());
		}

		public PlatformTypeRule(BrowserPlatformType type, BrowserOSFamily _os){
			this(type, (ua, os)->os==_os);
		}
	}

	private enum BrowserType{
		GOOGLEBOT,
		OPERA_PRESTO,
		OPERA_CHROMIUM,
		SAMSUNG_BROWSER,
		WHALE,
		PALE_MOON,
		MZ_BROWSER,
		FOCUS,
		SWING,
		OPERA_COAST,
		OPERA_TOUCH,
		YANDEX_BROWSER,
		UC_BROWSER,
		MAXTHON,
		EPIPHANY,
		PUFFIN,
		SLEIPNIR,
		K_MELEON,
		WECHAT,
		QQ_BROWSER,
		INTERNET_EXPLORER,
		MS_EDGE,
		MS_EDGE_IOS,
		VIVALDI,
		SEAMONKEY,
		SAILFISH,
		AMAZON_SILK,
		PHANTOMJS,
		SLIMERJS,
		BLACKBERRY,
		WEBOS_BROWSER,
		BADA,
		TIZEN,
		QUPZILLA,
		FIREFOX,
		ELECTRON,
		MIUI_BROWSER,
		CHROMIUM,
		CHROME,
		GOOGLE_SEARCH,
		ANDROID_BROWSER,
		PLAYSTATION_4,
		SAFARI,
		OTHER;

		public String getName(String ua){
			return switch(this){
				case GOOGLEBOT -> "Googlebot";
				case OPERA_PRESTO, OPERA_CHROMIUM -> "Opera";
				case SAMSUNG_BROWSER -> "Samsung Internet";
				case WHALE -> "NAVER Whale";
				case PALE_MOON -> "Pale Moon";
				case MZ_BROWSER -> "MZ Browser";
				case FOCUS -> "Focus";
				case SWING -> "Swing";
				case OPERA_COAST -> "Opera Coast";
				case OPERA_TOUCH -> "Opera Touch";
				case YANDEX_BROWSER -> "Yandex Browser";
				case UC_BROWSER -> "UC Browser";
				case MAXTHON -> "Maxthon";
				case EPIPHANY -> "Epiphany";
				case PUFFIN -> "Puffin";
				case SLEIPNIR -> "Sleipnir";
				case K_MELEON -> "K-Meleon";
				case WECHAT -> "WeChat";
				case QQ_BROWSER -> ua.toLowerCase().contains("qqbrowserlite") ? "QQ Browser Lite" : "QQ Browser";
				case INTERNET_EXPLORER -> "Internet Explorer";
				case MS_EDGE, MS_EDGE_IOS -> "Microsoft Edge";
				case VIVALDI -> "Vivaldi";
				case SEAMONKEY -> "SeaMonkey";
				case SAILFISH -> "Sailfish";
				case AMAZON_SILK -> "Amazon Silk";
				case PHANTOMJS -> "PhantomJS";
				case SLIMERJS -> "SlimerJS";
				case BLACKBERRY -> "BlackBerry";
				case WEBOS_BROWSER -> "WebOS Browser";
				case BADA -> "Bada";
				case TIZEN -> "Tizen";
				case QUPZILLA -> "QupZilla";
				case FIREFOX -> "Firefox";
				case ELECTRON -> "Electron";
				case MIUI_BROWSER -> "MIUI Browser";
				case CHROMIUM -> "Chromium";
				case CHROME -> "Chrome";
				case GOOGLE_SEARCH -> "Google Search";
				case ANDROID_BROWSER -> "Android Browser";
				case PLAYSTATION_4 -> "PlayStation 4";
				case SAFARI -> "Safari";
				case OTHER -> {
					Matcher matcher=(ua.contains("(") ? REGEX_WITH_DEVICE_SPEC : REGEX_WITHOUT_DEVICE_SPEC).matcher(ua);
					yield matcher.find() ? matcher.group(1) : "Unknown Browser";
				}
			};
		}

		public String getVersion(String ua){
			return switch(this){
				case GOOGLEBOT -> getFirstMatchWithFallback(ua, GOOGLEBOT_VERSION, COMMON_VERSION_IDENTIFIER);
				case OPERA_PRESTO -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, OPERA_VERSION1);
				case OPERA_CHROMIUM -> getFirstMatchWithFallback(ua, OPERA_VERSION2, COMMON_VERSION_IDENTIFIER);
				case SAMSUNG_BROWSER -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, SAMSUNG_VERSION);
				case WHALE -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, WHALE_VERSION);
				case PALE_MOON -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, PALE_MOON_VERSION);
				case MZ_BROWSER -> getFirstMatchWithFallback(ua, MZ_BROWSER_VERSION, COMMON_VERSION_IDENTIFIER);
				case FOCUS -> getFirstMatchWithFallback(ua, FOCUS_VERSION, COMMON_VERSION_IDENTIFIER);
				case SWING -> getFirstMatchWithFallback(ua, SWING_VERSION, COMMON_VERSION_IDENTIFIER);
				case OPERA_COAST -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, OPERA_COAST_VERSION);
				case OPERA_TOUCH -> getFirstMatchWithFallback(ua, OPERA_TOUCH_VERSION, COMMON_VERSION_IDENTIFIER);
				case YANDEX_BROWSER -> getFirstMatchWithFallback(ua, YANDEX_VERSION, COMMON_VERSION_IDENTIFIER);
				case UC_BROWSER -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, UC_VERSION);
				case MAXTHON -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, MAXTHON_VERSION);
				case EPIPHANY -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, EPIPHANY_VERSION);
				case PUFFIN -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, PUFFIN_VERSION);
				case SLEIPNIR -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, SLEIPNIR_VERSION);
				case K_MELEON -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, K_MELEON_VERSION);
				case WECHAT -> getFirstMatchWithFallback(ua, WECHAT_VERSION, COMMON_VERSION_IDENTIFIER);
				case QQ_BROWSER -> getFirstMatchWithFallback(ua, QQ_VERSION, COMMON_VERSION_IDENTIFIER);
				case INTERNET_EXPLORER -> getFirstMatch(ua, IE_VERSION);
				case MS_EDGE -> getFirstMatch(ua, EDGE_VERSION);
				case MS_EDGE_IOS -> getFirstMatch(ua, EDGE_IOS_VERSION);
				case VIVALDI -> getFirstMatch(ua, VIVALDI_VERSION);
				case SEAMONKEY -> getFirstMatch(ua, SEAMONKEY_VERSION);
				case SAILFISH -> getFirstMatch(ua, SAILFISH_VERSION);
				case AMAZON_SILK -> getFirstMatch(ua, SILK_VERSION);
				case PHANTOMJS -> getFirstMatch(ua, PHANROMJS_VERSION);
				case SLIMERJS -> getFirstMatch(ua, SLIMERJS_VERSION);
				case BLACKBERRY -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, BLACKBERRY_VERSION);
				case WEBOS_BROWSER -> getFirstMatchWithFallback(ua, COMMON_VERSION_IDENTIFIER, WEBOS_VERSION);
				case BADA -> getFirstMatch(ua, BADA_VERSION);
				case TIZEN -> getFirstMatchWithFallback(ua, TIZEN_VERSION, COMMON_VERSION_IDENTIFIER);
				case QUPZILLA -> getFirstMatchWithFallback(ua, QUPZILLA_VERSION, COMMON_VERSION_IDENTIFIER);
				case FIREFOX -> getFirstMatch(ua, FIREFOX_VERSION);
				case ELECTRON -> getFirstMatch(ua, ELECTRON_VERSION);
				case MIUI_BROWSER -> getFirstMatch(ua, MIUI_VERSION);
				case CHROMIUM -> getFirstMatchWithFallback(ua, CHROMIUM_VERSION, COMMON_VERSION_IDENTIFIER);
				case CHROME -> getFirstMatch(ua, CHROME_VERSION);
				case GOOGLE_SEARCH -> getFirstMatch(ua, GOOGLE_VERSION);
				case ANDROID_BROWSER, PLAYSTATION_4, SAFARI -> getFirstMatch(ua, COMMON_VERSION_IDENTIFIER);
				case OTHER -> {
					Matcher matcher=(ua.contains("(") ? REGEX_WITH_DEVICE_SPEC : REGEX_WITHOUT_DEVICE_SPEC).matcher(ua);
					yield matcher.find() ? matcher.group(2) : null;
				}
			};
		}
	}
}
