package smithereen;

import org.junit.jupiter.api.Test;

import smithereen.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static smithereen.model.User.ContactInfoKey.*;
import static smithereen.text.TextProcessor.*;

@SuppressWarnings("HttpUrlsUsage")
public class TextProcessorTest{

	@Test
	public void testNormalizeMatrixUsername(){
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, " @john_appleseed:matrix.org   "));
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, "@john_appleseed@matrix.org"));
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, "john_appleseed@matrix.org"));
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, "john_appleseed:matrix.org"));
		assertEquals("@john_appleseed:www.matrix.org", normalizeContactInfoValue(MATRIX, "@john_appleseed:www.matrix.org"));
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, "https://matrix.to/#/@john_appleseed:matrix.org"));
		assertEquals("@john_appleseed:matrix.org", normalizeContactInfoValue(MATRIX, "http://example.com/@john_appleseed:matrix.org"));

		assertNull(normalizeContactInfoValue(MATRIX, "@john_appleseed"));
		assertNull(normalizeContactInfoValue(MATRIX, "matrix.org"));
		assertNull(normalizeContactInfoValue(MATRIX, "matrix.org/john_appleseed"));
		assertNull(normalizeContactInfoValue(MATRIX, "matrix.org/@john_appleseed"));
		assertNull(normalizeContactInfoValue(MATRIX, "💩@matrix.org"));
	}

	@Test
	public void testGetMatrixUrlFromUsername(){
		assertEquals("https://matrix.to/#/@john_appleseed:matrix.org", getContactInfoValueURL(MATRIX, "@john_appleseed:matrix.org"));
		assertEquals("https://matrix.to/#/@john_appleseed:www.matrix.org", getContactInfoValueURL(MATRIX, "@john_appleseed:www.matrix.org"));
		assertEquals("https://matrix.to/#/💩", getContactInfoValueURL(MATRIX, "💩"));
		assertEquals("https://matrix.to/#/", getContactInfoValueURL(MATRIX, ""));
	}

	@Test
	public void testNormalizeTelegramLink(){
		assertEquals("pavel", normalizeContactInfoValue(TELEGRAM, " https://t.me/pavel "));
		assertEquals("pavel/durov", normalizeContactInfoValue(TELEGRAM, "https://t.me/pavel/durov"));
		assertEquals("pavel", normalizeContactInfoValue(TELEGRAM, "t.me/pavel"));
		assertEquals("pavel", normalizeContactInfoValue(TELEGRAM, "http://t.me/pavel"));
		assertEquals("pavel", normalizeContactInfoValue(TELEGRAM, "pavel"));
		assertEquals("pavel_123", normalizeContactInfoValue(TELEGRAM, "@pavel_123 "));

		assertNull(normalizeContactInfoValue(TELEGRAM, "https://example.com/pavel"));
		assertThrows(StringIndexOutOfBoundsException.class, ()->normalizeContactInfoValue(TELEGRAM, "https://t.me"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "ssh://t.me/pavel"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "@@pavel"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "@"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "pavel@t.me"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "@pavel@t.me"));
		assertNull(normalizeContactInfoValue(TELEGRAM, "💩"));
	}

	@Test
	public void testGetTelegramUrlFromUsername(){
		assertEquals("https://t.me/pavel", getContactInfoValueURL(TELEGRAM, "pavel"));
		assertEquals("https://t.me/@pavel_123", getContactInfoValueURL(TELEGRAM, "@pavel_123"));
		assertEquals("https://t.me/💩", getContactInfoValueURL(TELEGRAM, "💩"));
		assertEquals("https://t.me/", getContactInfoValueURL(TELEGRAM, ""));
	}

	@Test
	public void testNormalizeSignalNumber(){
		assertEquals("moxie.42", normalizeContactInfoValue(SIGNAL, "  @moxie.42 "));
		assertEquals("http://signal.me/moxie", normalizeContactInfoValue(SIGNAL, "http://signal.me/moxie"));
		assertEquals("https://signal.me/moxie", normalizeContactInfoValue(SIGNAL, "https://signal.me/moxie"));
		assertEquals("https://signal.me", normalizeContactInfoValue(SIGNAL, "https://signal.me"));

		assertNull(normalizeContactInfoValue(SIGNAL, "@moxie.42@signal.me"));
		assertNull(normalizeContactInfoValue(SIGNAL, "moxie.42"));
		assertNull(normalizeContactInfoValue(SIGNAL, "moxie.ab"));
		assertNull(normalizeContactInfoValue(SIGNAL, "@moxie.423"));
		assertNull(normalizeContactInfoValue(SIGNAL, "@moxie.4"));
		assertNull(normalizeContactInfoValue(SIGNAL, "@moxie."));
		assertNull(normalizeContactInfoValue(SIGNAL, "@moxie"));
		assertNull(normalizeContactInfoValue(SIGNAL, "moxie"));
		assertNull(normalizeContactInfoValue(SIGNAL, "signal.me/moxie"));
		assertNull(normalizeContactInfoValue(SIGNAL, "https://signal.me/💩"));
	}

	@Test
	public void testGetSignalUrlFromUsername(){
		assertEquals("https://signal.me/moxie", getContactInfoValueURL(SIGNAL, "https://signal.me/moxie"));
		assertEquals("http://signal.me/moxie", getContactInfoValueURL(SIGNAL, "http://signal.me/moxie"));
		assertEquals("signal.me/moxie", getContactInfoValueURL(SIGNAL, "signal.me/moxie"));
		assertEquals("moxie.42", getContactInfoValueURL(SIGNAL, "moxie.42"));

		assertNull(getContactInfoValueURL(SIGNAL, "moxie"));
		assertNull(getContactInfoValueURL(SIGNAL, "signal.me/💩"));
	}

	@Test
	public void testNormalizeTwitterLink(){
		assertEquals("jack", normalizeContactInfoValue(TWITTER, " https://twitter.com/jack "));
		assertEquals("jack", normalizeContactInfoValue(TWITTER, "https://x.com/jack"));
		assertEquals("jack", normalizeContactInfoValue(TWITTER, "http://twitter.com/jack"));
		assertEquals("jack/dorsey", normalizeContactInfoValue(TWITTER, "http://twitter.com/jack/dorsey"));
		assertEquals("jack", normalizeContactInfoValue(TWITTER, "twitter.com/jack"));
		assertEquals("@jack", normalizeContactInfoValue(TWITTER, "twitter.com/@jack"));
		assertEquals("jack", normalizeContactInfoValue(TWITTER, "@jack"));
		assertEquals("jack", normalizeContactInfoValue(TWITTER, "jack"));
		assertEquals("jack_dorsey228", normalizeContactInfoValue(TWITTER, "@jack_dorsey228"));

		assertNull(normalizeContactInfoValue(TWITTER, "https://twitter.com/💩"));
		assertThrows(StringIndexOutOfBoundsException.class, ()->normalizeContactInfoValue(TWITTER, "https://twitter.com"));
		assertNull(normalizeContactInfoValue(TWITTER, "https://example.com/jack"));
		assertNull(normalizeContactInfoValue(TWITTER, "💩"));
		assertNull(normalizeContactInfoValue(TWITTER, "@"));
		assertNull(normalizeContactInfoValue(TWITTER, "@jack@twitter.com"));
	}

	@Test
	public void testGetTwitterUrlFromUsername(){
		assertEquals("https://twitter.com/jack", getContactInfoValueURL(TWITTER, "jack"));
		assertEquals("https://twitter.com/", getContactInfoValueURL(TWITTER, ""));
		assertEquals("https://twitter.com/@jack", getContactInfoValueURL(TWITTER, "@jack"));
	}

	@Test
	public void testNormalizeEmail(){
		assertEquals("user@example.com", normalizeContactInfoValue(EMAIL, " user@example.com "));
		assertEquals("a.b.c.d.1.2.3@a.b.c.d.example.com", normalizeContactInfoValue(EMAIL, "a.b.c.d.1.2.3@a.b.c.d.example.com"));
		assertEquals("user+plus@example.com", normalizeContactInfoValue(EMAIL, "user+plus@example.com"));

		assertNull(normalizeContactInfoValue(EMAIL, "@user@example.com"));
		assertNull(normalizeContactInfoValue(EMAIL, "@example.com"));
		assertNull(normalizeContactInfoValue(EMAIL, "@"));
		assertNull(normalizeContactInfoValue(EMAIL, "user@localhost"));
	}

	@Test
	public void testGetEmailUrlFromEmailAddress(){
		assertEquals("mailto:user@example.com", getContactInfoValueURL(EMAIL, "user@example.com"));
		assertEquals("mailto:", getContactInfoValueURL(EMAIL, ""));
		assertEquals("mailto:@", getContactInfoValueURL(EMAIL, "@"));
	}

	@Test
	public void testNormalizeSnapchatUsernameFromLink(){
		assertEquals("evan", normalizeContactInfoValue(SNAPCHAT, "https://www.snapchat.com/add/evan   "));
		assertEquals("", normalizeContactInfoValue(SNAPCHAT, "https://www.snapchat.com/add/evan/"));
		assertEquals("evan", normalizeContactInfoValue(SNAPCHAT, "http://www.snapchat.com/add/evan"));
		assertEquals("evan", normalizeContactInfoValue(SNAPCHAT, "evan"));
		assertEquals("evan", normalizeContactInfoValue(SNAPCHAT, "@evan"));

		assertNull(normalizeContactInfoValue(SNAPCHAT, "https://www.snapchat.com/evan"));
		assertNull(normalizeContactInfoValue(SNAPCHAT, "https://snapchat.com/add/evan"));
		assertNull(normalizeContactInfoValue(SNAPCHAT, "www.snapchat.com/add/evan"));
		assertNull(normalizeContactInfoValue(SNAPCHAT, "@"));
		assertNull(normalizeContactInfoValue(SNAPCHAT, ""));
		assertNull(normalizeContactInfoValue(SNAPCHAT, "@💩"));
		assertNull(normalizeContactInfoValue(SNAPCHAT, "💩"));
	}

	@Test
	public void testGetSnapchatUrlFromUsername(){
		assertEquals("https://www.snapchat.com/add/evan", getContactInfoValueURL(SNAPCHAT, "evan"));
		assertEquals("https://www.snapchat.com/add/", getContactInfoValueURL(SNAPCHAT, ""));
		assertEquals("https://www.snapchat.com/add/💩", getContactInfoValueURL(SNAPCHAT, "💩"));
		assertEquals("https://www.snapchat.com/add/@evan", getContactInfoValueURL(SNAPCHAT, "@evan"));
	}

	@Test
	public void testNormalizeDiscordUsername(){
		assertEquals("jason", normalizeContactInfoValue(DISCORD, "@jason"));

		assertNull(normalizeContactInfoValue(DISCORD, "@"));
		assertNull(normalizeContactInfoValue(DISCORD, ""));
		assertNull(normalizeContactInfoValue(DISCORD, ""));
	}

	@Test
	public void testGetDiscordUrlFromUsername(){
		assertNull(getContactInfoValueURL(DISCORD, "jason"));
	}

	@Test
	public void testNormalizeGitURL(){
		assertEquals("https://github.com/user/repo", normalizeContactInfoValue(GIT, " github.com/user/repo"));
		assertEquals("https://gitlab.com/123/456", normalizeContactInfoValue(GIT, "https://gitlab.com/123/456"));
		assertEquals("http://example.com", normalizeContactInfoValue(GIT, "http://example.com"));

		assertNull(normalizeContactInfoValue(GIT, "github"));
	}

	@Test
	public void testNormalizeMastodonUsername(){
		assertEquals("@Gargron@mastodon.social", normalizeContactInfoValue(MASTODON, "https://mastodon.social/@Gargron "));
		assertEquals("@Gargron@example.com", normalizeContactInfoValue(MASTODON, "http://example.com/@Gargron/"));
		assertEquals("@Gargron@mastodon.social", normalizeContactInfoValue(MASTODON, "@Gargron@mastodon.social"));
		assertEquals("@Gargron@mastodon.social", normalizeContactInfoValue(MASTODON, "Gargron@mastodon.social"));

		assertNull(normalizeContactInfoValue(MASTODON, "@Gargron:mastodon.social"));
		assertNull(normalizeContactInfoValue(MASTODON, "mastodon.social"));
		assertNull(normalizeContactInfoValue(MASTODON, "@@"));
		assertNull(normalizeContactInfoValue(MASTODON, "@"));
		assertNull(normalizeContactInfoValue(MASTODON, ""));
		assertNull(normalizeContactInfoValue(MASTODON, "ssh://mastodon.social/@Gargron"));
		assertNull(normalizeContactInfoValue(MASTODON, "https://mastodon.social/Gargron"));
		assertNull(normalizeContactInfoValue(MASTODON, "https://mastodon.social/@Gargron/whatever"));
		assertNull(normalizeContactInfoValue(MASTODON, "https://mastodon.social/whatever/@Gargron"));
		assertNull(normalizeContactInfoValue(MASTODON, "mastodon.online/@Gargron"));
	}

	@Test
	public void testGetMastodonUrlFromUsername(){
		assertEquals("https://mastodon.social/@Gargron", getContactInfoValueURL(MASTODON, "@Gargron@mastodon.social"));
		assertEquals("https://mastodon.social/@Gargron", getContactInfoValueURL(MASTODON, "Gargron@mastodon.social"));

		assertNull(getContactInfoValueURL(MASTODON, "@Gargron"));
		assertNull(getContactInfoValueURL(MASTODON, "mastodon.social"));
		assertNull(getContactInfoValueURL(MASTODON, ""));
	}

	@Test
	public void testNormalizePixelfedUsername(){
		assertEquals("@dansup@pixelfed.social", normalizeContactInfoValue(PIXELFED, "https://pixelfed.social/@dansup "));
		assertEquals("@dansup@example.com", normalizeContactInfoValue(PIXELFED, "http://example.com/@dansup/"));
		assertEquals("@dansup@example.com", normalizeContactInfoValue(PIXELFED, "http://example.com/dansup"));
		assertEquals("@dansup@pixelfed.social", normalizeContactInfoValue(PIXELFED, "@dansup@pixelfed.social"));
		assertEquals("@dansup@pixelfed.social", normalizeContactInfoValue(PIXELFED, "dansup@pixelfed.social"));

		assertNull(normalizeContactInfoValue(PIXELFED, "@dansup:pixelfed.social"));
		assertNull(normalizeContactInfoValue(PIXELFED, "pixelfed.social"));
		assertNull(normalizeContactInfoValue(PIXELFED, "@@"));
		assertNull(normalizeContactInfoValue(PIXELFED, "@"));
		assertNull(normalizeContactInfoValue(PIXELFED, ""));
		assertNull(normalizeContactInfoValue(PIXELFED, "ssh://pixelfed.social/@dansup"));
		assertNull(normalizeContactInfoValue(PIXELFED, "https://pixelfed.social/dansup/whatever"));
		assertNull(normalizeContactInfoValue(PIXELFED, "https://pixelfed.social/whatever/@dansup"));
		assertNull(normalizeContactInfoValue(PIXELFED, "pixelfed.online/@dansup"));
	}

	@Test
	public void testGetPixelfedUrlFromUsername(){
		assertEquals("https://pixelfed.social/@dansup", getContactInfoValueURL(PIXELFED, "@dansup@pixelfed.social"));
		assertEquals("https://pixelfed.social/@dansup", getContactInfoValueURL(PIXELFED, "dansup@pixelfed.social"));
		assertEquals("https://example.com/@dansup", getContactInfoValueURL(PIXELFED, "dansup@example.com"));

		assertNull(getContactInfoValueURL(PIXELFED, "@dansup"));
		assertNull(getContactInfoValueURL(PIXELFED, "pixelfed.social"));
		assertNull(getContactInfoValueURL(PIXELFED, ""));
	}

	@Test
	public void testNormalizeFacebookLink(){
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, " https://facebook.com/zuck "));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "https://www.facebook.com/zuck"));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "https://m.facebook.com/zuck"));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "http://facebook.com/zuck"));
		assertEquals("zuck/", normalizeContactInfoValue(FACEBOOK, "http://facebook.com/zuck/"));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "facebook.com/zuck"));
		assertEquals("@zuck", normalizeContactInfoValue(FACEBOOK, "facebook.com/@zuck"));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "@zuck"));
		assertEquals("zuck", normalizeContactInfoValue(FACEBOOK, "zuck"));

		assertNull(normalizeContactInfoValue(FACEBOOK, "https://facebook.com/💩"));
		assertNull(normalizeContactInfoValue(FACEBOOK, "https://fuck.facebook.com/zuck"));
		assertThrows(StringIndexOutOfBoundsException.class, ()->normalizeContactInfoValue(FACEBOOK, "https://facebook.com"));
		assertNull(normalizeContactInfoValue(FACEBOOK, "https://example.com/zuck"));
		assertNull(normalizeContactInfoValue(FACEBOOK, "💩"));
		assertNull(normalizeContactInfoValue(FACEBOOK, "@"));
		assertNull(normalizeContactInfoValue(FACEBOOK, "@zuck@facebook.com"));
	}

	@Test
	public void testGetFacebookUrlFromUsername(){
		assertEquals("https://facebook.com/zuck", getContactInfoValueURL(FACEBOOK, "zuck"));
		assertEquals("https://facebook.com/", getContactInfoValueURL(FACEBOOK, ""));
		assertEquals("https://facebook.com/💩", getContactInfoValueURL(FACEBOOK, "💩"));
	}

	@Test
	public void testNormalizeInstagramLink(){
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, " https://instagram.com/mosseri "));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "https://www.instagram.com/mosseri"));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "https://instagr.am/mosseri"));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "http://instagram.com/mosseri"));
		assertEquals("mosseri/", normalizeContactInfoValue(INSTAGRAM, "http://instagram.com/mosseri/"));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "instagram.com/mosseri"));
		assertEquals("@mosseri", normalizeContactInfoValue(INSTAGRAM, "instagram.com/@mosseri"));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "@mosseri"));
		assertEquals("mosseri", normalizeContactInfoValue(INSTAGRAM, "mosseri"));

		assertNull(normalizeContactInfoValue(INSTAGRAM, "https://instagram.com/💩"));
		assertNull(normalizeContactInfoValue(INSTAGRAM, "https://fuck.instagram.com/mosseri"));
		assertThrows(StringIndexOutOfBoundsException.class, ()->normalizeContactInfoValue(INSTAGRAM, "https://instagram.com"));
		assertNull(normalizeContactInfoValue(INSTAGRAM, "https://example.com/mosseri"));
		assertNull(normalizeContactInfoValue(INSTAGRAM, "💩"));
		assertNull(normalizeContactInfoValue(INSTAGRAM, "@"));
		assertNull(normalizeContactInfoValue(INSTAGRAM, "@mosseri@instagram.com"));
	}

	@Test
	public void testGetInstagramUrlFromUsername(){
		assertEquals("https://instagram.com/mosseri", getContactInfoValueURL(INSTAGRAM, "mosseri"));
		assertEquals("https://instagram.com/", getContactInfoValueURL(INSTAGRAM, ""));
		assertEquals("https://instagram.com/💩", getContactInfoValueURL(INSTAGRAM, "💩"));
	}

	@Test
	public void testNormalizeVkontakteLink(){
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, " https://vk.com/durov "));
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, "https://m.vk.com/durov"));
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, "http://vk.com/durov"));
		assertEquals("durov/", normalizeContactInfoValue(VKONTAKTE, "http://vk.com/durov/"));
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, "vk.com/durov"));
		assertEquals("@durov", normalizeContactInfoValue(VKONTAKTE, "vk.com/@durov"));
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, "@durov"));
		assertEquals("durov", normalizeContactInfoValue(VKONTAKTE, "durov"));
		assertEquals("id1", normalizeContactInfoValue(VKONTAKTE, "id1"));

		assertNull(normalizeContactInfoValue(VKONTAKTE, "https://vk.com/💩"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "https://fuck.vk.com/durov"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "https://www.vk.com/durov"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "https://vkontakte.ru/durov"));
		assertThrows(StringIndexOutOfBoundsException.class, ()->normalizeContactInfoValue(VKONTAKTE, "https://vk.com"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "https://example.com/durov"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "💩"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "@"));
		assertNull(normalizeContactInfoValue(VKONTAKTE, "@durov@vk.com"));
	}

	@Test
	public void testGetVkontakteUrlFromUsername(){
		assertEquals("https://vk.com/durov", getContactInfoValueURL(VKONTAKTE, "durov"));
		assertEquals("https://vk.com/", getContactInfoValueURL(VKONTAKTE, ""));
		assertEquals("https://vk.com/💩", getContactInfoValueURL(VKONTAKTE, "💩"));
		assertEquals("https://vk.com/id1", getContactInfoValueURL(VKONTAKTE, "id1"));
	}
}