package org.semanticweb.ore.querying;

public enum QueryType {
	
	QUERY_TYPE_CLASSIFICATION("Classification"),
	QUERY_TYPE_CONSISTENCY("Consistency"),
	QUERY_TYPE_SATISFIABILITY("Satisfiability"),
	QUERY_TYPE_ENTAILMENT("Entailment"),
	QUERY_TYPE_REALISATION("Realisation");
	
	private String mQueryTypeName = null;
	
	public String getQueryTypeName() {
		return mQueryTypeName;
	}
	
	private QueryType(String queryTypeName) {
		mQueryTypeName = queryTypeName;
	}

}
