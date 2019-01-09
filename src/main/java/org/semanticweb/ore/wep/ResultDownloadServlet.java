package org.semanticweb.ore.wep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationStatusUpdateItem;
import org.semanticweb.ore.competition.CompetitionExecutionState;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatus;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatusUpdateItem;
import org.semanticweb.ore.competition.CompetitionStatus;
import org.semanticweb.ore.competition.CompetitionStatusUpdateBlockingListner;
import org.semanticweb.ore.competition.CompetitionStatusUpdateCollectionManager;
import org.semanticweb.ore.competition.CompetitionStatusUpdateItem;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultDownloadServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1089732719764432284L;

	final private static Logger mLogger = LoggerFactory.getLogger(ResultDownloadServlet.class);
	
	protected String mWebRootDirString = null;
	protected Config mConfig = null;
	protected CompetitionStatusUpdateCollectionManager mStatusUpdateCollector = null;
	

	
	
	public ResultDownloadServlet(CompetitionStatusUpdateCollectionManager statusUpdateCollector, Config config) {
		mConfig = config;
		mStatusUpdateCollector = statusUpdateCollector;
		
		mWebRootDirString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                 
        String disciplineNameString = request.getParameter("disc");
        String evaluationNameString = request.getParameter("eval");
        String forceDownloadString = request.getParameter("eval");
        
        boolean forceDownload = true;
        if (forceDownloadString.equalsIgnoreCase("false")) {
        	forceDownload = false;
        }
        
        
        String fileNameString = getFileName(disciplineNameString,evaluationNameString);
        if (fileNameString != null) {  
        	byte[] dataArray = loadFile(fileNameString);
        	if (dataArray != null) {
	            response.setStatus(HttpServletResponse.SC_OK);
	            if (forceDownload) {
	            	response.setContentType("application/octet-stream");
	            }
	            response.getOutputStream().write(dataArray);
        	} else {
        		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        	}
        } else {  
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);	       
         }

    }
	
	
	protected String getFileName(String disciplineNameString, String evaluationNameString) {
		if (disciplineNameString != null && evaluationNameString != null) {
			for (CompetitionStatusUpdateItem compStatUpItem : mStatusUpdateCollector.getUpdatedCompetitionStatusItemCollection(0)) {
				if (compStatUpItem.getCompetitionStatus().getCompetitionName().compareTo(disciplineNameString) == 0) {	
					HashMap<String,CompetitionEvaluationStatusUpdateItem> evalMap = compStatUpItem.getEvaluationMap();
					if (evalMap != null) {
						for (CompetitionEvaluationStatusUpdateItem evalStatUpItem : evalMap.values()) {
							if (evalStatUpItem.getCompetitionEvaluationStatus().getEvaluationName().compareTo(evaluationNameString) == 0) {
								return evalStatUpItem.getCompetitionEvaluationStatus().getEvaluationSourceString();
							}
						}
					}
				}
			}
		}
		return null;
	}

	
	protected byte[] loadFile(String fileString) {	
		byte[] data = null;
		try {
			File inputFile = new File(fileString);
			FileInputStream inputStream = new FileInputStream(inputFile);
			data = new byte[(int) inputFile.length()];
			inputStream.read(data);	                
			inputStream.close();
			
			
		} catch (Exception e) {
			mLogger.error("Could not access file '{}', got Exception '{}'",fileString,e.getMessage());
		}	
		return data;
	}
	
	
	
	
	
}
