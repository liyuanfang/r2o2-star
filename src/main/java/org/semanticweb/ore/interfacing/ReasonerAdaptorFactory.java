package org.semanticweb.ore.interfacing;

import org.semanticweb.ore.conversion.OntologyFormatRedirector;
import org.semanticweb.ore.querying.Query;

public interface ReasonerAdaptorFactory {
	
	public ReasonerAdaptor getReasonerAdapter(ReasonerDescription reasoner, Query query, String responseDestinationString, OntologyFormatRedirector formatRedirector);

}
