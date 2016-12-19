package ore.verification;

import ore.configuration.Config;
import ore.interfacing.ReasonerInterfaceType;
import ore.querying.Query;
import ore.querying.QueryResponse;

public class DefaultQueryResultNormaliserFactory implements	QueryResultNormaliserFactory {
	
	private Config mConfig = null;

	@Override
	public QueryResultNormaliser createQueryResultNormaliser(Query query, QueryResponse queryResponse) {
		if (queryResponse.getUsedInterface() == ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1) {
			return new QueryResultNormaliserOREv1(mConfig);
		} else if (queryResponse.getUsedInterface() == ReasonerInterfaceType.REASONER_INTERFACE_ORE_V2) {
			return new QueryResultNormaliserOREv2(mConfig);
		}
		return null;
	}
	
	
	public DefaultQueryResultNormaliserFactory(Config config) {
		mConfig = config;
	}

}
