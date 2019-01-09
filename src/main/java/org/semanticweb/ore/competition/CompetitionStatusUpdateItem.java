package org.semanticweb.ore.competition;

import java.util.HashMap;
import java.util.Vector;

public class CompetitionStatusUpdateItem {
	
	protected CompetitionStatus mStatus = null;
	protected long mCompetitionUpdateID = 0;
	protected long mReasonersUpdateID = 0;
	protected long mEvaluationUpdateID = 0;
	protected int mCompetitionID = 0;
	protected Vector<CompetitionReasonerProgressStatusUpdateItem> mReasonerProgressUpdateItemVector = null;
	protected HashMap<String,CompetitionEvaluationStatusUpdateItem> mEvaluationUpdateItemMap = null; 
		
	public CompetitionStatusUpdateItem(CompetitionStatus status, int competitionID, Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressUpdateItemVector, HashMap<String,CompetitionEvaluationStatusUpdateItem> evaluationUpdateItemMap, long updateCompetitionID, long reasonersUpdateID, long evalutionUpdateID) {
		mStatus = status;
		mCompetitionID = competitionID;
		mCompetitionUpdateID = updateCompetitionID;
		mReasonerProgressUpdateItemVector = reasonerProgressUpdateItemVector;
		mEvaluationUpdateItemMap = evaluationUpdateItemMap;
		mReasonersUpdateID = reasonersUpdateID;
		mEvaluationUpdateID = evalutionUpdateID;
	}
	
	public int getCompetitionID() {
		return mCompetitionID;
	}
	
	
	public Vector<CompetitionReasonerProgressStatusUpdateItem> getReasonerProgressUpdateItemVector() {
		return mReasonerProgressUpdateItemVector;
	}
	
	
	public long getCompetitionUpdateID() {
		return mCompetitionUpdateID;
	}
	

	public long getReasonersUpdateID() {
		return mReasonersUpdateID;
	}
	

	public long getEvaluationUpdateID() {
		return mEvaluationUpdateID;
	}
	
	public boolean isCompetitionUpdated(long competitionUpdateID) {
		if (competitionUpdateID < mCompetitionUpdateID) {
			return true;
		}
		return false;
	}
	
	public boolean isReasonerUpdated(long reasonersUpdateID) {
		if (reasonersUpdateID < mReasonersUpdateID) {
			return true;
		}
		return false;
	}	
	

	public boolean isEvaluationUpdated(long evaluationUpdateID) {
		if (evaluationUpdateID < mEvaluationUpdateID) {
			return true;
		}
		return false;
	}	
	
	public CompetitionStatus getCompetitionStatus() {
		return mStatus;
	}
	
	public HashMap<String,CompetitionEvaluationStatusUpdateItem> getEvaluationMap() {
		return mEvaluationUpdateItemMap;
	}
	
}
