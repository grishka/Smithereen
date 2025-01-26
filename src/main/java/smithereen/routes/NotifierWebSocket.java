package smithereen.routes;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

import smithereen.ApplicationContext;
import smithereen.lang.Lang;
import smithereen.model.SessionInfo;

@WebSocket
public class NotifierWebSocket{
	private static final Logger LOG=LoggerFactory.getLogger(NotifierWebSocket.class);

	@OnWebSocketConnect
	public void onConnect(Session session){
		LOG.debug("onConnect: {}", session);
		if(!(session.getUpgradeRequest() instanceof JettyServerUpgradeRequest req)){
			LOG.error("The upgrade request is of unexpected type {}", session.getUpgradeRequest().getClass());
			session.close();
			return;
		}
		session.setIdleTimeout(Duration.ofSeconds(90));
		SessionInfo info=(SessionInfo) req.getServletAttribute("sessionInfo");
		ApplicationContext ctx=(ApplicationContext) req.getServletAttribute("context");
		Lang lang=(Lang) req.getServletAttribute("lang");
		ctx.getNotificationsController().registerWebSocket(info, session, lang);
	}

	@OnWebSocketClose
	public void onClose(Session session, int statusCode, String reason){
		LOG.debug("onClose: {} {} {}", session, statusCode, reason);
		if(!(session.getUpgradeRequest() instanceof JettyServerUpgradeRequest req)){
			return;
		}
		ApplicationContext ctx=(ApplicationContext) req.getServletAttribute("context");
		ctx.getNotificationsController().unregisterWebSocket(session);
	}

	@OnWebSocketMessage
	public void onMessage(Session session, String message) throws IOException{
		LOG.trace("onMessage: {} '{}'", session, message);
	}

	@OnWebSocketError
	public void onError(Session session, Throwable err){
		LOG.debug("onError: {}", (Object) err);
	}
}
