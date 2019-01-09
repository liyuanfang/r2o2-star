package org.semanticweb.ore.querying;

import org.semanticweb.ore.utilities.FilePathString;

public abstract class Query {
	
	protected QueryExpressivity mQueryExpressivity = null;
	protected FilePathString mQuerySourceString = null;
	protected FilePathString mOntologySourceString = null;

	
	public abstract QueryType getQueryType();
	
	
	public FilePathString getQuerySourceString() {
		return mQuerySourceString;
	}
	
	public FilePathString getOntologySourceString() {
		return mOntologySourceString;
	}
	
	public QueryExpressivity getQueryExpressivity() {
		return mQueryExpressivity;
	}
	
	public Query(FilePathString querySourceString, FilePathString ontologySourceString, QueryExpressivity queryExpressivity) {
		mQuerySourceString = querySourceString;
		mOntologySourceString = ontologySourceString;
		mQueryExpressivity = queryExpressivity;
	}

}
