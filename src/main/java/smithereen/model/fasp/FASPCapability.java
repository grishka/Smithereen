package smithereen.model.fasp;

import smithereen.util.TranslatableEnum;

public enum FASPCapability implements TranslatableEnum<FASPCapability>{
	DEBUG("callback");

	public final String id;

	FASPCapability(String id){
		this.id=id;
	}

	public static FASPCapability fromID(String id){
		for(FASPCapability c:values()){
			if(c.id.equals(id))
				return c;
		}
		return null;
	}

	@Override
	public String getLangKey(){
		return "admin_fasp_capability_"+this.toString().toLowerCase();
	}

	public String getSupportedVersion(){
		// This will need to be rewritten to support proper capability versioning
		return switch(this){
			case DEBUG -> "0.1";
		};
	}

	public boolean hasSettingsUI(){
		return this==DEBUG;
	}
}
