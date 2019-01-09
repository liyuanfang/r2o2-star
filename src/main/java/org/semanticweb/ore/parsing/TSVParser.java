package org.semanticweb.ore.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TSVParser {	
	
	final private static Logger mLogger = LoggerFactory.getLogger(TSVParser.class);
	
	protected Config mConfig = null;
	protected FilePathString mParsingSourceString = null;
	protected String mParsingSourceDirectoryString = null;

	protected boolean parse(String fileString) throws IOException {
		return parse(new File(fileString));
	}	
	
	protected boolean parse(File file) throws IOException {
		boolean successfulParsed = false;		
		try {
			FileInputStream inputStream = null;
			inputStream = new FileInputStream(file);
			successfulParsed = parse(inputStream);
			inputStream.close();
		} catch (IOException e) {
			mLogger.warn("Parsing of '{}' failed.",file.toString());
			throw e;
		} 
		return successfulParsed;
	}
	
	protected boolean parse(InputStream stream) throws IOException {
		handleStartParsing();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		String line = null;
		while ((line = reader.readLine()) != null) {	
			String[] strings = line.split("\t");
			if (strings.length >= 2) {
				if (!strings[0].startsWith("#")) {
					handleParsedValues(strings);
				}
			}
		}
		reader.close();
		handleFinishParsing();
		return true;		
	}
	
	
	protected String getValuesString(String[] values) {
		String valuesString = null;
		for (String valueString : values) {
			if (valuesString == null) {
				valuesString = valueString;
			} else {
				valuesString = valuesString+", "+valueString;
			}
		}
		if (valuesString == null) {
			valuesString = "";
		}
		return valuesString;
	}
	
	
	protected boolean parseTSVBoolean(String[] values) {
		if (values.length > 1) {
			String dataString = values[1].trim();
			if (dataString.compareToIgnoreCase("true") == 0 || dataString.compareToIgnoreCase("1") == 0) {
				return true;
			} else if (dataString.compareToIgnoreCase("false") == 0 || dataString.compareToIgnoreCase("0") == 0) {
				return false;
			}
		} 
		return false;
	}	

	protected long parseTSVLong(String[] values) {
		if (values.length > 1) {
			return Long.parseLong(values[1]);
		} 
		return 0;
	}
	

	protected DateTime parseTSVDateTime(String[] values) {
		if (values.length > 1) {
			return ISODateTimeFormat.dateTimeParser().parseDateTime(values[1]);
		} 
		return null;
	}
	
	protected FilePathString parseTSVFilePathString(String[] values) {
		return parseTSVFilePathString(values,new FilePathString(""));
	}
	
	

	protected DateTime parseDateTime(String[] values) {
		DateTime date = null;
		try {
			date = ISODateTimeFormat.dateTimeParser().parseDateTime(values[1]);
		} catch (Exception e) {				
		}
		return date;
	}
	
	protected FilePathString parseTSVFilePathString(String[] values, FilePathString defaultFileString) {
		FilePathString filePathString = defaultFileString;
		if (values.length >= 3) {
			String tmpFileString = values[2].trim();
			String fileTypeString = values[1].trim();
			if (fileTypeString.compareToIgnoreCase("RELATIVETOBASEDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_BASE_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_BASE_DIRECTORY);
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOQUERIESDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETORESPONSESDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_RESPONSES_DIRECTORY);
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOONTOLOGIESDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_ONTOLOGIES_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_ONTOLOGIES_DIRECTORY);				
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOCONVERSIONSDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_CONVERSIONS_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_CONVERSIONS_DIRECTORY);
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOEXPECTATIONSSDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_EXPECTATIONS_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_EXPECTATIONS_DIRECTORY);				
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOCOMPETITIONSDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_COMPETITIONS_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_COMPETITIONS_DIRECTORY);				
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOTEMPLATESDIRECTORY") == 0) {
				String fileString = ConfigDataValueReader.getConfigDataValueString(mConfig,ConfigType.CONFIG_TYPE_TEMPLATES_DIRECTORY,"");
				filePathString = new FilePathString(fileString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_TEMPLATES_DIRECTORY);				
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOSOURCE") == 0) {
				filePathString = new FilePathString(mParsingSourceString.getAbsoluteFilePathString(), tmpFileString, RelativeFilePathStringType.RELATIVE_TO_SOURCE);
			} else if (fileTypeString.compareToIgnoreCase("RELATIVETOSOURCEDIRECTORY") == 0) {				
				filePathString = new FilePathString(mParsingSourceDirectoryString, tmpFileString, RelativeFilePathStringType.RELATIVE_TO_SOURCE_DIRECTORY);
			} else if (fileTypeString.compareToIgnoreCase("ABSOLUTE") == 0) {
				filePathString = new FilePathString(tmpFileString);
			}
		} else if (values.length >= 2) {
			String tmpFileString = values[1].trim();
			filePathString = new FilePathString(tmpFileString);
		}
		return filePathString;
	}	
			
	
	protected abstract boolean handleParsedValues(String[] values);
	protected abstract boolean handleStartParsing();
	protected abstract boolean handleFinishParsing();
	
	
	protected TSVParser(Config config, FilePathString parsingSourceString) {
		mConfig = config;
		mParsingSourceString = parsingSourceString;
		File mParsingSourceStringFile = new File(mParsingSourceString.getAbsoluteFilePathString());
		if (!mParsingSourceStringFile.isDirectory()) {	
			mParsingSourceDirectoryString = mParsingSourceStringFile.getParent()+File.separator; 
		} else {
			mParsingSourceDirectoryString = mParsingSourceString.getAbsoluteFilePathString();
		}
		
	}

}
