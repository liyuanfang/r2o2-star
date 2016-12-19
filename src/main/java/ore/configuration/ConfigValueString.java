package ore.configuration;

public class ConfigValueString extends ConfigValue {
	
	private String mValueString = null;
	
	public ConfigValueType getConfigType() {
		return ConfigValueType.CONFIG_VALUE_TYPE_STRING;
	}
	
	public String getValueString() {
		return mValueString;
	}
	
	public ConfigValueString(String configValueString) {
		mValueString = configValueString;
	}
	
	
	
	public static String getConfigDataValueString(Config config, String configName, String defaultValue) {
		String stringValue = defaultValue;
		if (config != null) {
			ConfigValue configValue = config.getConfig(configName);
			if (configValue != null) {
				if (configValue.getConfigType() == ConfigValueType.CONFIG_VALUE_TYPE_STRING) {
					ConfigValueString configValueString = (ConfigValueString)configValue;
					stringValue = configValueString.getValueString();
				}
			}
		}
		return stringValue;
	}
	
	public static String getConfigDataValueString(Config config, String configName) {
		return getConfigDataValueString(config, configName, "");
	}	
	
	public static String getConfigDataValueString(Config config, ConfigType configType, String defaultValue) {
		return getConfigDataValueString(config, configType.getConfigTypeString(), defaultValue);
	}	
	
	public static String getConfigDataValueString(Config config, ConfigType configType) {
		return getConfigDataValueString(config, configType.getConfigTypeString(), "");
	}	

}
