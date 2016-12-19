package ore.interfacing;

import ore.conversion.OntologyFormatRedirector;
import ore.querying.Query;

public interface ReasonerAdaptorFactory {
	
	public ReasonerAdaptor getReasonerAdapter(ReasonerDescription reasoner, Query query, String responseDestinationString, OntologyFormatRedirector formatRedirector);

}
