package smithereen.util;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import smithereen.exceptions.InternalServerErrorException;

public class Passwords{
	private Passwords(){
	}

	private static final SecureRandom random=new SecureRandom();
	private static final String DEFAULT_HASHING_ALGORITHM="SHA-256";

	public static byte @NotNull [] randomSalt(){
		byte[] salt=new byte[32];
		random.nextBytes(salt);
		return salt;
	}

	private static byte[] sha256(byte[] input){
		try{
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			return md.digest(input);
		}catch(NoSuchAlgorithmException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static byte @NotNull [] saltedPassword(byte @NotNull [] hashedPassword, byte @NotNull [] salt){
		byte[] passwordAndSalt=new byte[hashedPassword.length+salt.length];
		System.arraycopy(hashedPassword, 0, passwordAndSalt, 0, hashedPassword.length);
		System.arraycopy(salt, 0, passwordAndSalt, hashedPassword.length, salt.length);
		return sha256(passwordAndSalt);
	}

	/**
	 * Returns the hashed password according to the following formula: <code>sha256(sha256(password) + salt)</code>.
	 * The proper way would be to just use <code>sha256(password + salt)</code>, but the older versions of the database
	 * used to contain only hashed passwords without any salt, we have to be able to migrate those.
	 *
	 * @param password The user password.
	 * @param salt     A random string of bytes. Should be at least 32 bytes long, otherwise it's not secure enough.
	 * @return The salted and hashed password.
	 */
	public static byte @NotNull [] hashPasswordWithSalt(@NotNull String password, byte @NotNull [] salt){
		return saltedPassword(sha256(password.getBytes(StandardCharsets.UTF_8)), salt);
	}
}
