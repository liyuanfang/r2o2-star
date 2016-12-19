package ore.competition;

import ore.configuration.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompetitionEvaluationStatusSourcePathAdpater implements ore.competition.CompetitionStatusUpdater {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluationStatusSourcePathAdpater.class);
	protected Config mConfig = null;
	
	protected ore.competition.CompetitionStatusUpdater mStatusUpdater = null;
		
	protected String mMatchString = null;
	protected String mReplaceString = null;
		
	public CompetitionEvaluationStatusSourcePathAdpater(String matchString, String replaceString, ore.competition.CompetitionStatusUpdater statusUpdater, Config config) {
		mStatusUpdater = statusUpdater;
		mConfig = config;	
		
		mMatchString = matchString;
		mReplaceString = replaceString;
	
	}	
	
	

	@Override
	public void updateCompetitionStatus(ore.competition.CompetitionStatus status) {
		if (mStatusUpdater != null) {
			mStatusUpdater.updateCompetitionStatus(status);
		}
	}


	@Override
	public void updateCompetitionReasonerProgressStatus(ore.competition.CompetitionReasonerProgressStatus status) {
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
