package org.semanticweb.ore.configuration;

public class ConfigBaseFactory implements ConfigFactory {

	@Override
	public Config createConfig() {
		return new ConfigBase();
	}

	@Override
	public ConfigValue createConfigValueString(Config config, String configName, String value) {
		return new ConfigValueString(value);
	}

	@Override
	public void addConfigValue(Config config, String configName, ConfigValue value) {
		ConfigBase configBase = (ConfigBase)config;
		configBase.addConfig(configName, value);
	}

}
