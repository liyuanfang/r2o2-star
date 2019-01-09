package org.semanticweb.ore.configuration;

public class ConfigExtensionFactory implements ConfigFactory {

	private Config mBaseConfig = null;
	
	@Override
	public Config createConfig() {
		return new ConfigExtension(mBaseConfig);
	}

	@Override
	public ConfigValue createConfigValueString(Config config, String configName, String value) {
		return new ConfigValueString(value);
	}

	@Override
	public void addConfigValue(Config config, String configName, ConfigValue value) {
		ConfigExtension configExtension = (ConfigExtension)config;
		configExtension.addConfig(configName, value);
	}
	
	public ConfigExtensionFactory(Config baseConfig) {
		mBaseConfig = baseConfig;
	}

}
