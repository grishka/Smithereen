package smithereen.routes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.fasp.FASPProvider;
import smithereen.model.fasp.requests.FASPRegistrationRequest;
import smithereen.model.fasp.responses.FASPRegistrationResponse;
import smithereen.util.CryptoUtils;
import smithereen.util.XTEA;
import smithereen.util.validation.ObjectValidator;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class FaspApiRoutes{
	private static final Logger LOG=LoggerFactory.getLogger(FaspApiRoutes.class);

	public static Object registration(Request req, Response resp){
		ApplicationContext ctx=context(req);
		FASPRegistrationRequest r=parseAndValidate(req, resp, FASPRegistrationRequest.class);
		LOG.debug("Registration: {}", r);

		PublicKey faspKey;
		try{
			faspKey=CryptoUtils.decodeEcPublicKey(Base64.getDecoder().decode(r.publicKey()));
		}catch(IllegalArgumentException x){
			throw new BadRequestException("Invalid public key", x);
		}

		KeyPair keyPair;
		try{
			KeyPairGenerator kpg=KeyPairGenerator.getInstance("Ed25519");
			keyPair=kpg.generateKeyPair();
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}

		long id=ctx.getFaspController().createRegistration(r.name(), r.baseUrl(), r.serverId(), faspKey, keyPair.getPrivate());

		resp.status(201);
		resp.type("application/json");
		String idString=XTEA.encodeObjectID(id, ObfuscatedObjectIDType.FASP_PROVIDER);
		return gson.toJson(new FASPRegistrationResponse(
				idString,
				Base64.getEncoder().encodeToString(CryptoUtils.encodeEcPublicKey(keyPair.getPublic())),
				Config.localURI("/settings/admin/fasp/requests")
		));
	}

	public static Object debugCallback(Request req, Response resp, ApplicationContext ctx, FASPProvider provider){
		ctx.getFaspController().putDebugCallback(provider, getRequestIP(req), req.body());
		resp.status(201);
		return "";
	}

	private static <T> T parseAndValidate(Request req, Response resp, Class<T> type){
		try{
			T obj=gson.fromJson(req.body(), type);
			ObjectValidator.validate(obj);
			return obj;
		}catch(Throwable x){
			throw new BadRequestException(x);
		}
	}
}
