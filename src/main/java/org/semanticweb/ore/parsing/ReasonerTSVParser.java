package org.semanticweb.ore.parsing;

import java.io.IOException;
import java.io.InputStream;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.interfacing.ExpressivitySupport;
import org.semanticweb.ore.interfacing.OntologyFormatType;
import org.semanticweb.ore.interfacing.ReasonerDescription;
import org.semanticweb.ore.interfacing.ReasonerDescriptionFactory;
import org.semanticweb.ore.interfacing.ReasonerInterfaceType;
import org.semanticweb.ore.utilities.FilePathString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReasonerTSVParser extends TSVParser {

	final private static Logger mLogger = LoggerFactory.getLogger(ReasonerTSVParser.class);


	private ReasonerDescriptionFactory mReasonerFactory = null;
	private ReasonerInterfaceType mInterfaceType = null;	
	
	private ReasonerDescription mReasonerDescription = null;
	private String mReasonerNameString = null;
	private FilePathString mStartingScript = null;
	private FilePathString mWorkingDirectory = null;	
	private OntologyFormatType mOntologyFormatType = OntologyFormatType.ONTOLOGY_FORMAT_ALL;
	private String mOutputPath = null;
	
	private boolean mDLProfileSupport = false;
	private boolean mELProfileSupport = false;
	private boolean mRLProfileSupport = false;
	private boolean mQLProfileSupport = false;
	private boolean mFULLSupport = false;
	private boolean mDatatypeSupport = false;
	private boolean mRuleSupport = false;
	
	

	@Override
	protected boolean handleParsedValues(String[] values) {
		boolean parsed = false;
		if (values.length >= 2) {
			String valueString1 = values[0].trim();
			String valueString2 = values[1].trim();
			if (valueString1.compareToIgnoreCase("InterfaceType") == 0) {
				if (valueString2.compareToIgnoreCase("OREv1") == 0) {
					mInterfaceType = ReasonerInterfaceType.REASONER_INTERFACE_ORE_V1;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("OREv2") == 0) {
					mInterfaceType = ReasonerInterfaceType.REASONER_INTERFACE_ORE_V2;
					parsed = true;
				}
			} else if (valueString1.compareToIgnoreCase("ReasonerName") == 0) {
				mReasonerNameString = valueString2;
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("StarterScript") == 0) {
				mStartingScript = parseTSVFilePathString(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("WorkingDirectory") == 0) {
				mWorkingDirectory = parseTSVFilePathString(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("OntologyFormatType") == 0) {
				if (valueString2.compareToIgnoreCase("ONTOLOGY_FORMAT_ALL") == 0 || valueString2.compareToIgnoreCase("ALL") == 0 || valueString2.compareToIgnoreCase("ANY") == 0) {
					mOntologyFormatType = OntologyFormatType.ONTOLOGY_FORMAT_ALL;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("ONTOLOGY_FORMAT_OWL2XML") == 0 || valueString2.compareToIgnoreCase("OWL2XML") == 0) {
					mOntologyFormatType = OntologyFormatType.ONTOLOGY_FORMAT_OWL2XML;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("ONTOLOGY_FORMAT_OWL2FUNCTIONAL") == 0 || valueString2.compareToIgnoreCase("OWL2FUNCTIONAL") == 0) {
					mOntologyFormatType = OntologyFormatType.ONTOLOGY_FORMAT_OWL2FUNCTIONAL;
					parsed = true;
				} else if (valueString2.compareToIgnoreCase("ONTOLOGY_FORMAT_OWL2RDF") == 0 || valueString2.compareToIgnoreCase("OWL2RDFXML") == 0 || valueString2.compareToIgnoreCase("RDFXML") == 0) {
					mOntologyFormatType = OntologyFormatType.ONTOLOGY_FORMAT_OWL2RDFXML;
					parsed = true;
				}
			} else if (valueString1.compareToIgnoreCase("DatatypeSupport") == 0) {
				mDatatypeSupport = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("RuleSupport") == 0) {
				mRuleSupport = parseTSVBoolean(values);
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("OutputPathName") == 0) {
				mOutputPath = valueString2;
				parsed = true;
			} else if (valueString1.compareToIgnoreCase("ProfileSupport") == 0) {
				for (int i = 1; i < values.length; ++i) {
					String dataString = values[i].trim();
					if (dataString.equalsIgnoreCase("DL")) {	
						mDLProfileSupport = true;
					} else if (dataString.equalsIgnoreCase("EL")) {	
						mELProfileSupport = true;
					} else if (dataString.equalsIgnoreCase("RL")) {	
						mRLProfileSupport = true;
					} else if (dataString.equalsIgnoreCase("QL")) {	
						mQLProfileSupport = true;
					} else if (dataString.equalsIgnoreCase("FULL")) {	
						mFULLSupport = true;
					}
				}
				parsed = true;
			}
		}
		if (!parsed) {
			mLogger.warn("Cannot parse values '{}' for reasoner.",getValuesString(values));
		}		
		return parsed;
	}
	


	@Override
	protected boolean handleStartParsing() {
		mReasonerDescription = null;
		return true;
	}

	@Override
	protected boolean handleFinishParsing() {
		if (mReasonerFactory != null) {
			boolean parsingIncomplete = false;
			ExpressivitySupport expressivitySupport = new ExpressivitySupport(mDLProfileSupport,mELProfileSupport,mRLProfileSupport,mQLProfileSupport,mDatatypeSupport,mRuleSupport,mFULLSupport);
			if (mOutputPath == null) {
				parsingIncomplete = true;
				mLogger.error("Parsing of OutputPathName failed or missing.");
			}
			if (mInterfaceType == null) {
				parsingIncomplete = true;
				mLogger.error("Parsing of InterfaceType failed or missing.");
			}
			if (mOntologyFormatType == null) {
				mLogger.error("Parsing of OntologyFormatType failed or missing.");
				parsingIncomplete = true;
			}
			if (mReasonerNameString == null) {
				mLogger.error("Parsing of ReasonerName failed or missing.");
				parsingIncomplete = true;
			}
			if (mStartingScript == null) {
				mLogger.error("Parsing of StarterScript failed or missing.");
				parsingIncomplete = true;
			}
			if (mWorkingDirectory == null) {
				mLogger.error("Parsing of WorkingDirectory failed or missing.");
				parsingIncomplete = true;
			}
			if (!parsingIncomplete) {
				mReasonerDescription = mReasonerFactory.createReasonerDescription(mParsingSourceString,mReasonerNameString, mInterfaceType, mStartingScript, mWorkingDirectory, mOntologyFormatType, expressivitySupport, mOutputPath);
			}
		}
		return true;
	}
	
	
	public ReasonerDescription parseReasonerDescription(InputStream inputStream) throws IOException {
		try {
			parse(inputStream);
		} catch (IOException e) {
			mLogger.warn("Parsing failed, got IOException {}.",e.getMessage());
			throw e;
		} 
		return mReasonerDescription;
	}	
	

	public ReasonerDescription parseReasonerDescription(String fileString) throws IOException {
		parse(fileString);
		return mReasonerDescription;
	}		
	
	
	public ReasonerTSVParser(ReasonerDescriptionFactory reasonerFactory, Config config, FilePathString parsingSourceString) {
		super(config,parsingSourceString);
		mReasonerFactory = reasonerFactory;
	}
	


}
