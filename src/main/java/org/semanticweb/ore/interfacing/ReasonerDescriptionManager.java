package org.semanticweb.ore.interfacing;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.semanticweb.ore.parsing.ReasonerTSVParser;
import org.semanticweb.ore.utilities.FilePathString;
import org.semanticweb.ore.utilities.FileSystemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ReasonerDescriptionManager {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerDescriptionManager.class);
	
	protected Config mConfig = null;
	protected HashMap<String,ReasonerDescription> mStringReasonerMap = new HashMap<String,ReasonerDescription>();
	protected ReasonerDescriptionFactory mReasonerDescriptionFactory = null;

	
	public ReasonerDescriptionManager(ReasonerDescriptionFactory reasonerDescriptionFactory, Config config) {
		mConfig = config;
		mReasonerDescriptionFactory = reasonerDescriptionFactory;
	}

	
	public List<ReasonerDescription> loadReasonerDescriptions(Collection<String> reasonerNameList) {
		ArrayList<ReasonerDescription> reasonerList = new ArrayList<ReasonerDescription>(); 
		for (String reasonerName : reasonerNameList) {
			ReasonerDescription reasoner = loadReasonerDescription(reasonerName);
			if (reasoner != null) {
				reasonerList.add(reasoner);
			}
		}
		return reasonerList;
	}
	
	
	public ReasonerDescription loadReasonerDescription(String reasonerFileString) {
		FilePathString reasonerDataString = lookForReasonerDataFile(reasonerFileString);
		ReasonerDescription reasoner = mStringReasonerMap.get(reasonerDataString.getAbsoluteFilePathString());
		if (reasoner == null) {
			ReasonerTSVParser reasonerTSVParser = new ReasonerTSVParser(mReasonerDescriptionFactory, mConfig, reasonerDataString);
			try {
				mLogger.info("Loading reasoner data from '{}'.",reasonerDataString);
				reasoner = reasonerTSVParser.parseReasonerDescription(reasonerDataString.getAbsoluteFilePathString());
                mLogger.info("Reasoner description: '{}'.", reasoner);
				mStringReasonerMap.put(reasonerDataString.getAbsoluteFilePathString(),reasoner);
			} catch (IOException e) {
				mLogger.error("Loading of reasoner data from '{}' failed, got IOException '{}'.",reasonerDataString,e.getMessage());
			}			
		}
		return reasoner;
	}
	

	public FilePathString lookForReasonerDataFile(String fileString) {		
		String baseDirectory = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_BASE_DIRECTORY);
		mLogger.info("baseDirectory = " + baseDirectory);
		FilePathString reasonerDataString = new FilePathString(baseDirectory+fileString);
        mLogger.info("initial guessed reasonerDataString = " + reasonerDataString);
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = new FilePathString(fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = new FilePathString(baseDirectory+"reasoners"+File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = new FilePathString(baseDirectory+"reasoners"+File.separator+fileString+File.separator+"reasoner.dat");
		}
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = new FilePathString(System.getProperty("user.dir")+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = new FilePathString(System.getProperty("user.dir")+File.separator+fileString);
		}
		if (!FileSystemHandler.nonEmptyFileExists(reasonerDataString)) {
			reasonerDataString = null;
		}
		mLogger.info("final reasonerDataString = " + reasonerDataString);
		return reasonerDataString;
	}

}
