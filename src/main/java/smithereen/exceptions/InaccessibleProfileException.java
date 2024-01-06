package smithereen.exceptions;

import smithereen.model.User;

public class InaccessibleProfileException extends RuntimeException{
	public final User user;

	public InaccessibleProfileException(User user){
		this.user=user;
	}
}
