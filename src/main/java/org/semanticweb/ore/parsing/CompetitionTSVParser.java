package org.semanticweb.ore.parsing;

import org.joda.time.DateTime;
import org.semanticweb.ore.competition.Competition;
import org.semanticweb.ore.competition.CompetitionFactory;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CompetitionTSVParser extends TSVParser {

	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionTSVParser.class);


	private CompetitionFactory mCompetitionFactory = null;
	
	private Competition mCompetition = null;
	private String mCompetitionNameString = null;
	
	private String mOutputPath = null;
	private String mQueryPathFilterString = null;
	private FilePathString mQuerySortingFilePathString = null;
	private HashSet<String> mReasonerNameSet = null;
	
	private long mExecutionTimeout = 0;
	private long mProcessingTimeout = 0;
	
	private DateTime mStartingDateTime = null;
	private DateTime mEndingDateTime = null;
	
	private boolean mAllowProcessingOnlyBetweenDateTimeInterval = false;
	
	

	@Override
	protected boolean handleParsedValues(String[] values) {
		boolean parsed = false;
		if (values.length >= 2) {
			String valueString1 = values[0].trim();
			String valueString2 = values[1].trim();
			if (valueString1.compareToIgnoreCase("CompetitionName") == 0) {
				mCompetitionNameString = valueString2;
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("QuerySortingFilePathString") == 0) {
				mQuerySortingFilePathString = parseTSVFilePathString(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("OutputPathName") == 0) {
				mOutputPath = valueString2;
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("QueryPathFilterString") == 0) {
				mQueryPathFilterString = valueString2;
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("ExecutionTimeout") == 0) {
				mExecutionTimeout = parseTSVLong(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("ProcessingTimeout") == 0) {
				mProcessingTimeout = parseTSVLong(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("StartingDateTime") == 0) {
				mStartingDateTime = parseDateTime(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("EndingDateTime") == 0) {
				mEndingDateTime = parseDateTime(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("AllowProcessingOnlyBetweenDateTimeInterval") == 0) {
				mAllowProcessingOnlyBetweenDateTimeInterval = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("Reasoners") == 0) {
				if (mReasonerNameSet == null) {
					mReasonerNameSet = new HashSet<String>();
				}
				for (int i = 1; i < values.length; ++i) {
					if (!values[i].isEmpty()) {
						mReasonerNameSet.add(values[i]);
					}
				}
				parsed = true;
			}
		}
		if (!parsed) {
			mLogger.warn("Cannot parse values '{}' for competition.",getValuesString(values));
		}		
		return parsed;
	}
	


	@Override
	protected boolean handleStartParsing() {
		mCompetition = null;
		mReasonerNameSet = null;
		return true;
	}

	@Override
	protected boolean handleFinishParsing() {
		if (mCompetitionFactory != null) {
			List<String> reasonerNameList = new ArrayList<String>();
			if (mReasonerNameSet != null && !mReasonerNameSet.isEmpty()) {	
				reasonerNameList.addAll(mReasonerNameSet);
			}
			mCompetition = mCompetitionFactory.createCompetition(mCompetitionNameString, mParsingSourceString, reasonerNameList);
			if (mQueryPathFilterString != null) {
				mCompetition.setQueryFilterString(mQueryPathFilterString);
			}
			if (mOutputPath != null) {
				mCompetition.setOutputPathString(mOutputPath);
			}
			if (mQuerySortingFilePathString != null) {
				mCompetition.setQuerySortingFilePathString(mQuerySortingFilePathString);
			}
			mCompetition.setExecutionTimeout(mExecutionTimeout);
			mCompetition.setProcessingTimeout(mProcessingTimeout);
			mCompetition.setDesiredStartingDate(mStartingDateTime);
			mCompetition.setDesiredEndingDate(mEndingDateTime);
			mCompetition.setRunningOnlyAllowedWithinDates(mAllowProcessingOnlyBetweenDateTimeInterval);
		}
		return true;
	}
	
	
	public Competition parseCompetition(InputStream inputStream) throws IOException {
		try {
			parse(inputStream);
		} catch (IOException e) {
			mLogger.warn("Parsing failed, got IOException {}.",e.getMessage());
			throw e;
		} 
		return mCompetition;
	}	
	

	public Competition parseCompetition(String fileString) throws IOException {
		parse(fileString);
		return mCompetition;
	}		
	
	
	public CompetitionTSVParser(CompetitionFactory competitionFactory, Config config, FilePathString parsingSourceString) {
		super(config,parsingSourceString);
		mCompetitionFactory = competitionFactory;
	}
	


}
