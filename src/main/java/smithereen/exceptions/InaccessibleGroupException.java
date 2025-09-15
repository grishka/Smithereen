package smithereen.exceptions;

import smithereen.model.Group;

public class InaccessibleGroupException extends RuntimeException{
	public final Group group;

	public InaccessibleGroupException(Group group){
		this.group=group;
	}
}
