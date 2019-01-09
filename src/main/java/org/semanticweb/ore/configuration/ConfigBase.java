package org.semanticweb.ore.configuration;

import java.util.HashMap;

public class ConfigBase implements Config {
	
	private HashMap<String,ConfigValue> mConfigHashMap = new HashMap<String,ConfigValue>();
	
	public void addConfig(String configKey, ConfigValue value) {
		mConfigHashMap.put(configKey, value);
	}
	
	public ConfigValue getConfig(String configKey) {
		ConfigValue configValue = mConfigHashMap.get(configKey);
		return configValue;
	}

}
