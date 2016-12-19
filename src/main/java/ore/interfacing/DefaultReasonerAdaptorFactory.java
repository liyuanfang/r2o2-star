package ore.interfacing;

import ore.conversion.OntologyFormatRedirector;
import ore.querying.Query;

public class DefaultReasonerAdaptorFactory implements ReasonerAdaptorFactory {

	@Override
	public ReasonerAdaptor getReasonerAdapter(ReasonerDescription reasoner, Query query, String responseDestinationString, OntologyFormatRedirector formatRedirector) {
		ReasonerInterfaceType reasonerInterfaceType = reasoner.getReasonerInterface();
		if (reasonerInterfaceType == ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1) {
			return new ReasonerAdaptorOREv1(reasoner,query,responseDestinationString,formatRedirector);
		} else if (reasonerInterfaceType == ReasonerInterfaceType.REASONER_INTERFACE_ORE_V2) {
			return new ReasonerAdaptorOREv2(reasoner,query,responseDestinationString,formatRedirector);
		}
		return null;
	}

}
