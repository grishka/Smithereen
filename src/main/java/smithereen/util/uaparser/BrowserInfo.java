package smithereen.util.uaparser;

public record BrowserInfo(String name, String version, BrowserPlatformType platformType, BrowserOSFamily os){
	public String getMajorVersion(){
		if(version==null)
			return null;
		int index=version.indexOf('.');
		if(index==-1)
			return version;
		return version.substring(0, index);
	}
}
