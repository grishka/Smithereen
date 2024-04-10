package smithereen.util;

public enum EmailCodeActionType{
	ACCOUNT_UNFREEZE;

	public String actionLangKey(){
		return switch(this){
			case ACCOUNT_UNFREEZE -> "email_confirm_action_unfreeze";
		};
	}
}
