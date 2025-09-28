open module smithereen.server{
	requires java.net.http;
	requires java.sql;
	requires java.desktop; // image stuff for captcha

	requires com.google.gson;
	requires com.sun.jna;
	requires io.pebbletemplates;
	requires java.mail;
	requires org.commonmark;
	requires org.commonmark.ext.gfm.strikethrough;
	requires org.commonmark.ext.ins;
	requires org.jetbrains.annotations;
	requires org.jsoup;
	requires spark;
	requires unbescape;
	requires unidecode;
	requires org.eclipse.jetty.websocket.jetty.api;
	requires org.eclipse.jetty.websocket.jetty.server;
	requires org.eclipse.jetty.websocket.core.server;
	requires mysql.connector.j;
}
