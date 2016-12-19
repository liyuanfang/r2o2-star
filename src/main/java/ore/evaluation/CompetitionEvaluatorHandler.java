package ore.evaluation;

import ore.competition.Competition;
import ore.competition.CompetitionHandler;
import ore.competition.CompetitionStatusUpdater;
import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.threading.Thread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CompetitionEvaluatorHandler extends Thread implements Runnable, CompetitionEvaluator {
	
	final private static Logger mLogger = LoggerFactory.getLogger(CompetitionEvaluatorHandler.class);
	
	protected Competition mCompetition = null;
	protected Config mConfig = null;
	protected QueryResultStorage mResultStorage = null;
	protected CompetitionHandler mCompetitionHandler = null;
	protected EvaluationFinishedCallback mCallback = null;
	protected CompetitionStatusUpdater mStatusUpdater = null;
	
			
	public CompetitionEvaluatorHandler(CompetitionHandler competitionHandler, Competition competition, QueryResultStorage resultStorage, CompetitionStatusUpdater statusUpdater, EvaluationFinishedCallback callback, Config config) {		
		mCompetitionHandler = competitionHandler;
		mCompetition = competition;
		mConfig = config;
		mResultStorage = resultStorage;
		mCallback = callback;
		mStatusUpdater = statusUpdater;
	}	
	
	
	public void evaluateCompetition() {
		startThread();
	}
	
	
	protected void threadStart() {	
	}	

	protected void threadFinish() {	
		if (mCallback != null) {
			mCallback.finishedEvaluation();
		}
	}	
	
	protected void threadRun() {	
		
		mLogger.info("Starting evaluation for competition '{}'.",mCompetition.getCompetitionName());
		
		String responseDirectory = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_RESPONSES_DIRECTORY);
		
		String competitionOutputString = mCompetitionHandler.getCompetitionOutputString();
	
		
		String competitionOutputDirectory = responseDirectory+"competition-evaluations"+ File.separator+competitionOutputString;

		
		String expectedResultsResponseDirectory = competitionOutputDirectory+ File.separator+"new-expected-results"+ File.separator;
		String evaluationResultsResponseDirectory = competitionOutputDirectory+ File.separator+"evaluation-results"+ File.separator;
		
		

		
		

		try {
			CompetitionSummaryQueryResultEvaluator compSummStatEvaluator = new CompetitionSummaryQueryResultEvaluator(mConfig,evaluationResultsResponseDirectory,mStatusUpdater);
			compSummStatEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}

		try {
			CorrectAverageRankingQueryResultEvaluator corrAverageRankingEvaluator = new CorrectAverageRankingQueryResultEvaluator(mConfig,evaluationResultsResponseDirectory,mStatusUpdater);
			corrAverageRankingEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
			mLogger.info("Failed to create correct average ranking statistics for competition {}, got Exception {}'.",mCompetition.getCompetitionName(),e.getMessage());
		}			

		try {
			ReasonerQueryResultListEvaluator reasonerResultEvaluator = new ReasonerQueryResultListEvaluator(mConfig,evaluationResultsResponseDirectory,mStatusUpdater);
			reasonerResultEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}
		
		try {
			ReasonerQueryOutputListEvaluator reasonerOutputEvaluator = new ReasonerQueryOutputListEvaluator(mConfig,evaluationResultsResponseDirectory,mStatusUpdater);
			reasonerOutputEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}
				
		try {
			QueryDifficultyOrderGenerationEvaluator queryDifficultyOrderEvalutor = new QueryDifficultyOrderGenerationEvaluator(mConfig,evaluationResultsResponseDirectory,mStatusUpdater);
			queryDifficultyOrderEvalutor.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}
		
		
		
		
		try {
			ExpectedResultByVerifiedReasonersGenerationEvaluator verifiedReasonerExpectedResultGenerationEvaluator = new ExpectedResultByVerifiedReasonersGenerationEvaluator(mCompetitionHandler.getReasonerList(),mConfig,expectedResultsResponseDirectory+"same-for-all-reasoenrs"+ File.separator);
			verifiedReasonerExpectedResultGenerationEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}	
		
		try {
			ExpectedResultByMajorityVotingGenerationEvaluator majorityVotingExpectedResultGenerationEvaluator = new ExpectedResultByMajorityVotingGenerationEvaluator(mConfig,expectedResultsResponseDirectory+"safe-majority-voting"+ File.separator);
			majorityVotingExpectedResultGenerationEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);			
		} catch (Exception e) {
		}	
		
		try {
			ExpectedResultByMajorityRandomVotingGenerationEvaluator randomMajorityVotingExpectedResultGenerationEvaluator = new ExpectedResultByMajorityRandomVotingGenerationEvaluator(mConfig,expectedResultsResponseDirectory+"random-majority-voting"+ File.separator);
			randomMajorityVotingExpectedResultGenerationEvaluator.evaluateCompetitionResults(mResultStorage, mCompetition);
		} catch (Exception e) {
		}
		
		
		
		mLogger.info("Finished evaluation for competition '{}'.",mCompetition.getCompetitionName());
		
		if (mCallback != null) {
			mCallback.finishedEvaluation();
		}
		
		
	}		

	
}
