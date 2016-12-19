package ore.conversion;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.interfacing.OntologyFormatType;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import ore.utilities.RelativeFilePathStringType;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.FileDocumentSource;
import org.semanticweb.owlapi.io.OWLFunctionalSyntaxOntologyFormat;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.OWLOntologyMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

public class OntologyFormatDynamicConversionRedirector implements OntologyFormatRedirector {
	
	final private static Logger mLogger = LoggerFactory.getLogger(OntologyFormatDynamicConversionRedirector.class);
	
	private String mConversionRelativeBaseDirectoryString = null;
	private String mConversionAbsoluteBaseDirectoryString = null;
	
	
	protected FilePathString getConversionRedirectionString(FilePathString ontologySourceString, OntologyFormatType ontologyFormat) {
		String redirectSourceString = null;
		String conversionRelativeSourceString = null;
		String conversionBaseSourceString = null;
		if (ontologySourceString.isRelative()) {	
			conversionBaseSourceString = mConversionRelativeBaseDirectoryString;
			redirectSourceString = ontologySourceString.getRelativeFilePathString();
		} else {			
			conversionBaseSourceString = mConversionAbsoluteBaseDirectoryString;
			redirectSourceString = ontologySourceString.getAbsoluteFilePathString();
		}
		File redirectSourceFile = new File(redirectSourceString);
		while (redirectSourceFile != null) {
			String filePartString = redirectSourceFile.getName();
			if (conversionRelativeSourceString == null) {
				conversionRelativeSourceString = filePartString;
			} else {
				conversionRelativeSourceString = filePartString + File.separator + conversionRelativeSourceString;
			}
			redirectSourceFile = redirectSourceFile.getParentFile();
		}
        String formatSuffix = null;
        String formatPrefix = null;
        if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2XML) {
        	formatSuffix = ".owl.xml";
        	formatPrefix = "OWL2XML";
       } else if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2FUNCTIONAL) {
        	formatSuffix = ".owl.fss";
        	formatPrefix = "OWL2FUNCTIONAL";
        } else if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2RDFXML) {
        	formatSuffix = ".owl.rdf.xml";
        	formatPrefix = "OWL2RDFXML";
        }		
		FilePathString redirectionFilePathString = new FilePathString(conversionBaseSourceString,formatPrefix+ File.separator+conversionRelativeSourceString+formatSuffix,RelativeFilePathStringType.RELATIVE_TO_CONVERSIONS_DIRECTORY);
		return redirectionFilePathString;
	}

	@Override
	public FilePathString getOntologySourceStringForFormat(FilePathString ontologySource, OntologyFormatType ontologyFormat) {
		if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_ALL) {
			return ontologySource;
		}
		FilePathString redirectedOntologySource = getConversionRedirectionString(ontologySource,ontologyFormat);
		mLogger.info("Ontology '{}' redirected to '{}'.",ontologySource,redirectedOntologySource);
		if (convertOntologyIntoFormat(ontologySource.getAbsoluteFilePathString(),redirectedOntologySource.getAbsoluteFilePathString(),ontologyFormat)) {
			return redirectedOntologySource;
		} else {
			return ontologySource;
		}
	}
	
	
	protected boolean convertOntologyIntoFormat(String sourceString, String destinationString, OntologyFormatType ontologyFormat) {
		boolean converted = false;
		
		OWLOntologyFormat owlOntologyFormat = null;
        if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2XML) {
        	owlOntologyFormat = new OWLXMLOntologyFormat();
        } else if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2FUNCTIONAL) {
        	owlOntologyFormat = new OWLFunctionalSyntaxOntologyFormat();
        } else if (ontologyFormat == OntologyFormatType.ONTOLOGY_FORMAT_OWL2RDFXML) {
        	owlOntologyFormat = new RDFXMLOntologyFormat();
        }	
        
        

        
        if (new File(destinationString).exists()) {
        	return true;
        }
        
		try {
			mLogger.info("Converting ontology '{}' into ontology format '{}'.",sourceString,owlOntologyFormat);
			FileSystemHandler.ensurePathToFileExists(destinationString);
		
	        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	        
	        OWLOntologyLoaderConfiguration loaderConfig = new OWLOntologyLoaderConfiguration();
	        loaderConfig = loaderConfig.setLoadAnnotationAxioms(false);
	        loaderConfig.setMissingImportHandlingStrategy(MissingImportHandlingStrategy.SILENT);
	    	
	        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new FileDocumentSource(new File(sourceString)),loaderConfig);
	        
	        OWLOntologyMerger ontoMerger = new OWLOntologyMerger(manager);
	        
	        OWLOntologyManager managerMerged = OWLManager.createOWLOntologyManager();
	        
	        OWLOntology mergedOntologie = ontoMerger.createMergedOntology(managerMerged,ontology.getOntologyID().getOntologyIRI());
	        
	        manager.saveOntology(mergedOntologie,owlOntologyFormat,new FileOutputStream(new File(destinationString)));
	        
	        converted = true;
	        mLogger.info("Successfully saved converted ontology at '{}' with format '{}'.",destinationString,owlOntologyFormat);
	        
		} catch (OWLOntologyCreationException e) {
			mLogger.error("Conversion of '{}' to '{}' into format '{}' failed, got OWLOntologyCreationException '{}'.",new Object[]{sourceString,destinationString,owlOntologyFormat,e.getMessage()});
		} catch (OWLOntologyStorageException e) {
			mLogger.error("Conversion of '{}' to '{}' into format '{}' failed, got OWLOntologyStorageException '{}'.",new Object[]{sourceString,destinationString,owlOntologyFormat,e.getMessage()});
		} catch (Exception e) {
			mLogger.error("Conversion of '{}' to '{}' into format '{}' failed, got Exception '{}'.",new Object[]{sourceString,destinationString,owlOntologyFormat,e.getMessage()});
		}
		
		return converted;
	}
	
	
	public OntologyFormatDynamicConversionRedirector(Config config) {
		String conversionBaseDirectory = ConfigDataValueReader.getConfigDataValueString(config, ConfigType.CONFIG_TYPE_CONVERSIONS_DIRECTORY);
		mConversionAbsoluteBaseDirectoryString = conversionBaseDirectory+"absolute"+ File.separator;
		mConversionRelativeBaseDirectoryString = conversionBaseDirectory+"relative"+ File.separator;
	}

}
