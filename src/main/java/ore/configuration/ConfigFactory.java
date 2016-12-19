package ore.configuration;

public interface ConfigFactory {
	
	public Config createConfig();
	public ConfigValue createConfigValueString(Config config, String configName, String value);
	
	public void addConfigValue(Config config, String configName, ConfigValue value);
	
}
