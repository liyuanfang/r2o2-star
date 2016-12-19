package ore.configuration;

import java.io.File;

public class InitialConfigBaseFactory implements ConfigFactory {

	@Override
	public Config createConfig() {
		ConfigBase initialConfig = new ConfigBase();
		String fs = File.separator;
		String currentDirectory = System.getProperty("user.dir")+fs+"data"+fs;
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_BASE_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_BASE_DIRECTORY.getConfigTypeString(), currentDirectory));		
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY.getConfigTypeString(), currentDirectory+"queries"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY.getConfigTypeString(), currentDirectory+"responses"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY.getConfigTypeString(), currentDirectory+"ontologies"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_CONVERSIONS_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_CONVERSIONS_DIRECTORY.getConfigTypeString(), currentDirectory+"conversions"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXPECTATIONS_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXPECTATIONS_DIRECTORY.getConfigTypeString(), currentDirectory+"expectations"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_COMPETITIONS_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_COMPETITIONS_DIRECTORY.getConfigTypeString(), currentDirectory+"competitions"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY.getConfigTypeString(), currentDirectory+"templates"+fs+"web"+fs+"competition-status-web"+fs+"root"+fs));
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_TEMPLATES_DIRECTORY.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_TEMPLATES_DIRECTORY.getConfigTypeString(), currentDirectory+"templates"+fs));
		
		
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXECUTION_TIMEOUT.getConfigTypeString(), "360000"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_PROCESSING_TIMEOUT.getConfigTypeString(), "300000"));
		
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXECUTION_MEMORY_LIMIT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXECUTION_MEMORY_LIMIT.getConfigTypeString(), "10737418240"));
		
		
		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION.getConfigTypeString(), "FALSE"));
		
		

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_COMPETITION_EXECUTOR_PER_REASONER.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_COMPETITION_EXECUTOR_PER_REASONER.getConfigTypeString(), "TRUE"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_COMPETITION_CONTINUE_EXECUTOR_LOSS.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_COMPETITION_CONTINUE_EXECUTOR_LOSS.getConfigTypeString(), "FALSE"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_COMPETITION_INFINITE_REPEAT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_COMPETITION_INFINITE_REPEAT.getConfigTypeString(), "FALSE"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT.getConfigTypeString(), "FALSE"));


		initialConfig.addConfig(ConfigType.CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT.getConfigTypeString(), "FALSE"));
		
		
		

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_NETWORKING_TERMINATE_AFTER_EXECUTION.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_NETWORKING_TERMINATE_AFTER_EXECUTION.getConfigTypeString(), "FALSE"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_NETWORKING_COMPETITION_SERVER_PORT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_NETWORKING_COMPETITION_SERVER_PORT.getConfigTypeString(), "11010"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_NETWORKING_STATUS_UPDATE_SERVER_PORT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_NETWORKING_STATUS_UPDATE_SERVER_PORT.getConfigTypeString(), "11011"));

		initialConfig.addConfig(ConfigType.CONFIG_TYPE_NETWORKING_WEB_SERVER_PORT.getConfigTypeString(),
				createConfigValueString(initialConfig, ConfigType.CONFIG_TYPE_NETWORKING_WEB_SERVER_PORT.getConfigTypeString(), "8800"));

		return initialConfig;
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
