package ore.verification;

import ore.querying.QueryResultData;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

public class OntologyPackedQueryResultData implements QueryResultData {
	
	private OWLOntology mOntology = null;
	private int mResultHashCode = 0;

	@Override
	public int getResultHashCode() {
		return mResultHashCode;
	}
	
	
	public OntologyPackedQueryResultData(OWLOntology ontology) {
		mOntology = ontology;
		mResultHashCode = mOntology.hashCode();
		for (OWLAxiom axiom : mOntology.getAxioms()) {
			// inefficient, but axiom.hashCode() does not always result in the same hash codes!
			mResultHashCode += axiom.toString().hashCode();
		}
	}

}
