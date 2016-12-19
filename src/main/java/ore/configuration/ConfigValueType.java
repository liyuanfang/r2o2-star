package ore.configuration;

public enum ConfigValueType {
	
	
	CONFIG_VALUE_TYPE_STRING("STRING"),
	CONFIG_VALUE_TYPE_STRING_LIST("STRINGLIST");
	
	private String mConfigValueTypeString = null;
	
	private ConfigValueType(String typeString) {
		mConfigValueTypeString = typeString.toUpperCase();
	}
	
	public String getConfigValueTypeString() {
		return mConfigValueTypeString;
	}
	

}
