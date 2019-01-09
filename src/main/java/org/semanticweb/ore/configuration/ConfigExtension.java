package org.semanticweb.ore.configuration;

import java.util.HashMap;

public class ConfigExtension implements Config {
	
	private Config mBaseConfig = null;
	private HashMap<String,ConfigValue> mConfigHashMap = new HashMap<String,ConfigValue>();
	
	public void addConfig(String configKey, ConfigValue value) {
		mConfigHashMap.put(configKey, value);
	}
	
	public ConfigValue getConfig(String configKey) {
		ConfigValue configValue = mConfigHashMap.get(configKey);
		if (configValue == null && mBaseConfig != null) {
			configValue = mBaseConfig.getConfig(configKey);
		}
		return configValue;
	}
	
	public ConfigExtension(Config baseConfig) {
		mBaseConfig = baseConfig;
	}

}
