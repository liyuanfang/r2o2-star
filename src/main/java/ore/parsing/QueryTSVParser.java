package ore.parsing;

import ore.configuration.Config;
import ore.querying.Query;
import ore.querying.QueryExpressivity;
import ore.querying.QueryFactory;
import ore.querying.QueryType;
import ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class QueryTSVParser extends TSVParser {

	final private static Logger mLogger = LoggerFactory.getLogger(QueryTSVParser.class);


	private QueryFactory mQueryFactory = null;
	private Query mQuery = null;	
	
	private QueryType mQueryType = null;
	private FilePathString mOntologySourceString = null;
	private FilePathString mEntailmentAxiomsSourceString = null;
	private String mClassString = null;

	private boolean mInDLProfile = false;
	private boolean mInELProfile = false;
	private boolean mInRLProfile = false;
	private boolean mInQLProfile = false;
	private boolean mUsingDatatype = false;
	private boolean mUsingRule = false;	

	@Override
	protected boolean handleParsedValues(String[] values) {
		boolean parsed = false;
		if (values.length >= 2) {
			String valueString1 = values[0].trim();
			String valueString2 = values[1].trim();
			if (valueString1.compareToIgnoreCase("QueryType") == 0) {
				if (valueString2.compareToIgnoreCase("Classification") == 0) {
					mQueryType = QueryType.QUERY_TYPE_CLASSIFICATION;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("Consistency") == 0) {
					mQueryType = QueryType.QUERY_TYPE_CONSISTENCY;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("Realisation") == 0) {
					mQueryType = QueryType.QUERY_TYPE_REALISATION;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("Satisfiability") == 0) {
					mQueryType = QueryType.QUERY_TYPE_SATISFIABILITY;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("Entailment") == 0) {
					mQueryType = QueryType.QUERY_TYPE_ENTAILMENT;
					parsed = true;
				}
			} else if (valueString1.compareToIgnoreCase("OntologySource") == 0) {
				mOntologySourceString = parseTSVFilePathString(values);				
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("SatisfiabilityTestingClass") == 0) {
				mClassString = valueString2;				
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("EntailmentAxiomSource") == 0) {
				mEntailmentAxiomsSourceString = parseTSVFilePathString(values);			
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("UsingRules") == 0) {
				mUsingRule = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("UsingDatatypes") == 0) {
				mUsingDatatype = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("InDLProfile") == 0) {
				mInDLProfile = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("InELProfile") == 0) {
				mInELProfile = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("InRLProfile") == 0) {
				mInRLProfile = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("InQLProfile") == 0) {
				mInQLProfile = parseTSVBoolean(values);
				parsed = true;
			}

		}
		if (!parsed) {
			mLogger.warn("Cannot parse values '{}' for query.",getValuesString(values));
		}		
		return parsed;
	}
	


	@Override
	protected boolean handleStartParsing() {
		mQuery = null;
		return true;
	}

	@Override
	protected boolean handleFinishParsing() {
		if (mQueryFactory != null) {
			QueryExpressivity queryExpressivity = new QueryExpressivity(mInDLProfile, mInELProfile, mInRLProfile, mInQLProfile, mUsingDatatype, mUsingRule);
			if (mQueryType == QueryType.QUERY_TYPE_CLASSIFICATION) {
				mQuery = mQueryFactory.createClassificationQuery(mParsingSourceString,mOntologySourceString, queryExpressivity);
			} else if (mQueryType == QueryType.QUERY_TYPE_CONSISTENCY) {
				mQuery = mQueryFactory.createConsistencyQuery(mParsingSourceString,mOntologySourceString, queryExpressivity);
			} else if (mQueryType == QueryType.QUERY_TYPE_REALISATION) {
				mQuery = mQueryFactory.createRealisationQuery(mParsingSourceString,mOntologySourceString, queryExpressivity);
			} else if (mQueryType == QueryType.QUERY_TYPE_SATISFIABILITY) {
				mQuery = mQueryFactory.createSatisfiabilityQuery(mParsingSourceString,mClassString,mOntologySourceString, queryExpressivity);
			} else if (mQueryType == QueryType.QUERY_TYPE_ENTAILMENT) {
				mQuery = mQueryFactory.createEntailmentQuery(mParsingSourceString,mEntailmentAxiomsSourceString,mOntologySourceString, queryExpressivity);
			}
		}
		return true;
	}
	
	
	public Query parseQuery(InputStream inputStream) throws IOException {
		try {
			parse(inputStream);
		} catch (IOException e) {
			mLogger.warn("Parsing of query failed, got IOException '{}'.",e.getMessage());
			throw e;
		} 
		return mQuery;
	}	
	

	public Query parseQuery(String fileString) throws IOException {
		parse(fileString);
		return mQuery;
	}		
	
	
	public QueryTSVParser(QueryFactory queryFactory, Config config, FilePathString parsingSourceString) {
		super(config,parsingSourceString);
		mQueryFactory = queryFactory;
	}
	


}
