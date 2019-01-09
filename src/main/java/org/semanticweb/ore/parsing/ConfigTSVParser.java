package org.semanticweb.ore.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigFactory;
import org.semanticweb.ore.configuration.ConfigValueType;
import org.semanticweb.ore.configuration.ConfigValue;
import org.semanticweb.ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigTSVParser extends TSVParser {

	final private static Logger mLogger = LoggerFactory.getLogger(ConfigTSVParser.class);
	
	private ConfigFactory mConfigFactory = null;
	private Config mConfig = null;	
	
	@Override
	protected boolean handleParsedValues(String[] values) {
		boolean parsed = false;
		if (values.length >= 1 && mConfig != null) {
			String configKeyTypeString = values[0].trim();
			String typeString = "STRING";
			String keyString = configKeyTypeString;
			int typeStartIndex = configKeyTypeString.indexOf('[');
			int typeEndIndex = configKeyTypeString.indexOf(']',typeStartIndex);
			if (typeStartIndex != -1 && typeEndIndex != -1) {
				typeString = configKeyTypeString.substring(typeStartIndex-1, typeEndIndex);
				keyString = configKeyTypeString.substring(0,typeStartIndex);
			}
			if (typeString.compareToIgnoreCase(ConfigValueType.CONFIG_VALUE_TYPE_STRING.getConfigValueTypeString()) == 0) {
				if (values.length >= 2) {
					String valueString = values[1];
					ConfigValue configValue = mConfigFactory.createConfigValueString(mConfig, keyString, valueString);
					mConfigFactory.addConfigValue(mConfig, keyString, configValue);
					parsed = true;
				}
			}
			if (!parsed) {
				mLogger.warn("Cannot parse values '{}' for config.",getValuesString(values));
			}
			
		}
		return parsed;
	}

	@Override
	protected boolean handleStartParsing() {
		mConfig = null;
		if (mConfigFactory != null) {
			mConfig = mConfigFactory.createConfig();
		}
		return true;
	}

	@Override
	protected boolean handleFinishParsing() {
		return true;
	}
	
	
	public Config parseConfig(InputStream inputStream) throws IOException {
		try {
			parse(inputStream);
		} catch (IOException e) {
			mLogger.warn("Parsing failed, got IOException {}.",e.getMessage());
			throw e;
		} 
		return mConfig;
	}	
	

	public Config parseConfig(String fileString) throws IOException {
		parse(fileString);
		return mConfig;
	}		
	

	
	public ConfigTSVParser(ConfigFactory configFactory, Config config, FilePathString parsingSourceString) {
		super(config,parsingSourceString);
		mConfigFactory = configFactory;
	}

}
