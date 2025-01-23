open module smithereen.server{
	requires java.net.http;
	requires java.sql;
	requires java.desktop; // image stuff for captcha

	requires com.google.gson;
	requires com.sun.jna;
	requires io.pebbletemplates;
	requires jetty.servlet.api;
	requires mail;
	requires org.commonmark;
	requires org.commonmark.ext.gfm.strikethrough;
	requires org.commonmark.ext.ins;
	requires org.jetbrains.annotations;
	requires org.jsoup;
	requires org.slf4j;
	requires spark;
	requires unbescape;
	requires unidecode;
}