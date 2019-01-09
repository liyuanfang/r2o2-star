package org.semanticweb.ore.configuration;

public class ConfigDataValueReader {
	
	
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
	
	
	
	
	public static int getConfigDataValueInteger(Config config, String configName, int defaultValue) {
		int integerValue = defaultValue;
		if (config != null) {
			ConfigValue configValue = config.getConfig(configName);
			if (configValue != null) {
				if (configValue.getConfigType() == ConfigValueType.CONFIG_VALUE_TYPE_STRING) {
					ConfigValueString configValueString = (ConfigValueString)configValue;
					String stringValue = configValueString.getValueString();
					try {
						integerValue = Integer.valueOf(stringValue);
					} catch (NumberFormatException ex) {						
					}
				}
			}
		}
		return integerValue;
	}
	
	public static int getConfigDataValueInteger(Config config, String configName) {
		return getConfigDataValueInteger(config, configName, 0);
	}	
	
	public static int getConfigDataValueInteger(Config config, ConfigType configType, int defaultValue) {
		return getConfigDataValueInteger(config, configType.getConfigTypeString(), defaultValue);
	}	
	
	public static int getConfigDataValueInteger(Config config, ConfigType configType) {
		return getConfigDataValueInteger(config, configType.getConfigTypeString(), 0);
	}		
	
	

	public static long getConfigDataValueLong(Config config, String configName, long defaultValue) {
		long longValue = defaultValue;
		if (config != null) {
			ConfigValue configValue = config.getConfig(configName);
			if (configValue != null) {
				if (configValue.getConfigType() == ConfigValueType.CONFIG_VALUE_TYPE_STRING) {
					ConfigValueString configValueString = (ConfigValueString)configValue;
					String stringValue = configValueString.getValueString();
					try {
						longValue = Long.valueOf(stringValue);
					} catch (NumberFormatException ex) {						
					}
				}
			}
		}
		return longValue;
	}
	
	public static long getConfigDataValueLong(Config config, String configName) {
		return getConfigDataValueLong(config, configName, 0);
	}	
	
	public static long getConfigDataValueLong(Config config, ConfigType configType, long defaultValue) {
		return getConfigDataValueLong(config, configType.getConfigTypeString(), defaultValue);
	}	
	
	public static long getConfigDataValueLong(Config config, ConfigType configType) {
		return getConfigDataValueLong(config, configType.getConfigTypeString(), 0);
	}		
	
		
	

	public static boolean getConfigDataValueBoolean(Config config, String configName, boolean defaultValue) {
		boolean booleanValue = defaultValue;
		if (config != null) {
			ConfigValue configValue = config.getConfig(configName);
			if (configValue != null) {
				if (configValue.getConfigType() == ConfigValueType.CONFIG_VALUE_TYPE_STRING) {
					ConfigValueString configValueString = (ConfigValueString)configValue;
					String stringValue = configValueString.getValueString();
					try {
						booleanValue = Boolean.valueOf(stringValue);
					} catch (NumberFormatException ex1) {						
						try {
							long longValue = Long.valueOf(stringValue);
							if (longValue == 0) {
								booleanValue = false;
							}
						} catch (NumberFormatException ex2) {	
							if (stringValue.compareToIgnoreCase("false") == 0) {
								booleanValue = false;
							} else if (stringValue.compareToIgnoreCase("true") == 0) {
								booleanValue = true;
							}
						}
					}
				}
			}
		}
		return booleanValue;
	}
	
	public static boolean getConfigDataValueBoolean(Config config, String configName) {
		return getConfigDataValueBoolean(config, configName, false);
	}	
	
	public static boolean getConfigDataValueBoolean(Config config, ConfigType configType, boolean defaultValue) {
		return getConfigDataValueBoolean(config, configType.getConfigTypeString(), defaultValue);
	}	
	
	public static boolean getConfigDataValueBoolean(Config config, ConfigType configType) {
		return getConfigDataValueBoolean(config, configType.getConfigTypeString(), false);
	}		
		
	
}
