package org.semanticweb.ore.competition;

import org.semanticweb.ore.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionEvaluationStatusSourcePathAdpater implements CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluationStatusSourcePathAdpater.class);
	protected Config mConfig = null;
	
	protected CompetitionStatusUpdater mStatusUpdater = null;
		
	protected String mMatchString = null;
	protected String mReplaceString = null;
		
	public CompetitionEvaluationStatusSourcePathAdpater(String matchString, String replaceString, CompetitionStatusUpdater statusUpdater, Config config) {
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		
		mMatchString = matchString;
		mReplaceString = replaceString;
	
	}	
	
	

	@Override
	public void updateCompetitionStatus(CompetitionStatus status) {
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionStatus(status);
		}
	}


	@Override
	public void updateCompetitionReasonerProgressStatus(CompetitionReasonerProgressStatus status) {
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionReasonerProgressStatus(status);
		}
	}


	@Override
	public void updateCompetitionEvaluationStatus(CompetitionEvaluationStatus status) {
		if (mStatusUpdater != null) {
			String evalSourceString = status.getEvaluationSourceString();
			if (evalSourceString.contains(mMatchString)) {
				evalSourceString = evalSourceString.replace(mMatchString, mReplaceString);
				status.setEvaluationSourceString(evalSourceString);
			}
			mStatusUpdater.updateCompetitionEvaluationStatus(status);
		}
	}


	
	
}
