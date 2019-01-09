package org.semanticweb.ore.utilities;

import org.semanticweb.ore.configuration.ConfigType;

public enum RelativeFilePathStringType {
	
	RELATIVE_TO_ONTOLOGIES_DIRECTORY(ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY),
	RELATIVE_TO_BASE_DIRECTORY(ConfigType.CONFIG_TYPE_BASE_DIRECTORY),
	RELATIVE_TO_QUERIES_DIRECTORY(ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY),
	RELATIVE_TO_RESPONSES_DIRECTORY(ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY),
	RELATIVE_TO_CONVERSIONS_DIRECTORY(ConfigType.CONFIG_TYPE_CONVERSIONS_DIRECTORY),
	RELATIVE_TO_EXPECTATIONS_DIRECTORY(ConfigType.CONFIG_TYPE_EXPECTATIONS_DIRECTORY),
	RELATIVE_TO_COMPETITIONS_DIRECTORY(ConfigType.CONFIG_TYPE_COMPETITIONS_DIRECTORY),
	RELATIVE_TO_TEMPLATES_DIRECTORY(ConfigType.CONFIG_TYPE_TEMPLATES_DIRECTORY),
	RELATIVE_TO_SOURCE(),
	RELATIVE_TO_SOURCE_DIRECTORY();
	
	
	private RelativeFilePathStringType(ConfigType configType) {	
		mRelativeConfigType = configType;
	}
	
	private RelativeFilePathStringType() {	
	}	
	
	private ConfigType mRelativeConfigType = null;
	
	public ConfigType getRelativeConfigType() {
		return mRelativeConfigType;
	}

}
