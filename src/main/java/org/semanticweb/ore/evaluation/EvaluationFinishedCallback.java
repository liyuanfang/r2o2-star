package org.semanticweb.ore.evaluation;

import org.semanticweb.ore.threading.Callback;

public interface EvaluationFinishedCallback extends Callback {
	
	public void finishedEvaluation();

}
