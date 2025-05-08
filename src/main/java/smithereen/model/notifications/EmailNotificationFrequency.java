package smithereen.model.notifications;

import smithereen.util.TranslatableEnum;

public enum EmailNotificationFrequency implements TranslatableEnum<EmailNotificationFrequency>{
	IMMEDIATE,
	DAILY,
	DISABLED;

	@Override
	public String getLangKey(){
		return "settings_email_notifications_"+switch(this){
			case IMMEDIATE -> "immediate";
			case DAILY -> "daily";
			case DISABLED -> "off";
		};
	}
}
