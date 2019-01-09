package org.semanticweb.ore.wep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationStatusUpdateItem;
import org.semanticweb.ore.competition.CompetitionEvaluationType;
import org.semanticweb.ore.competition.CompetitionExecutionState;
import org.semanticweb.ore.competition.CompetitionStatus;
import org.semanticweb.ore.competition.CompetitionStatusUpdateCollectionManager;
import org.semanticweb.ore.competition.CompetitionStatusUpdateItem;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultViewWebPageServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1089732719764432284L;

	final private static Logger mLogger = LoggerFactory.getLogger(ResultViewWebPageServlet.class);
	
	protected String mWebRootDirString = null;
	protected Config mConfig = null;
	protected CompetitionStatusUpdateCollectionManager mStatusUpdateCollector = null;
	

	
	
	public ResultViewWebPageServlet(CompetitionStatusUpdateCollectionManager statusUpdateCollector, Config config) {
		mConfig = config;
		mStatusUpdateCollector = statusUpdateCollector;
		
		mWebRootDirString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestPath = request.getRequestURI();
		
                 
        String disciplineNameString = request.getParameter("disc");
        String evaluationNameString = request.getParameter("eval");
        
        
        CompetitionEvaluationStatus compEvalStat = getCompetitionEvaluationStatus(disciplineNameString,evaluationNameString); 
        if (compEvalStat != null) {  
        	String fileContentString = getMainPageContentString(compEvalStat);
        	if (fileContentString != null) {
	            response.setContentType("text/html");
	            response.setStatus(HttpServletResponse.SC_OK);	       
	            response.getWriter().write(fileContentString);
        	} else {        		
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);	       
        	}
        } else {  
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);	       
        }

    }
	
	
	protected CompetitionEvaluationStatus getCompetitionEvaluationStatus(String disciplineNameString, String evaluationNameString) {
		if (disciplineNameString != null && evaluationNameString != null) {
			for (CompetitionStatusUpdateItem compStatUpItem : mStatusUpdateCollector.getUpdatedCompetitionStatusItemCollection(0)) {
				if (compStatUpItem.getCompetitionStatus().getCompetitionName().compareTo(disciplineNameString) == 0) {	
					HashMap<String,CompetitionEvaluationStatusUpdateItem> evalMap = compStatUpItem.getEvaluationMap();
					if (evalMap != null) {
						for (CompetitionEvaluationStatusUpdateItem evalStatUpItem : evalMap.values()) {
							if (evalStatUpItem.getCompetitionEvaluationStatus().getEvaluationName().compareTo(evaluationNameString) == 0) {
								return evalStatUpItem.getCompetitionEvaluationStatus();
							}
						}
					}
				}
			}
		}
		return null;
	}

	
	protected String loadFileIntoString(String fileString) {	
		String string = null;
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileString));
			StringBuilder sb = new StringBuilder();
	        byte[] b = new byte[4096];
	        for (int i; (i = inputStream.read(b)) != -1;) {
	        	sb.append(new String(b, 0, i));
	        }		        
			inputStream.close();
			
			string = sb.toString();
			
		} catch (Exception e) {
			mLogger.error("Could not access file '{}', got Exception '{}'",fileString,e.getMessage());
		}	
		return string;
	}
	
	
	
	
	protected String getMainPageContentString(CompetitionEvaluationStatus compEvalStat) {
		String mainPageContentString = loadFileIntoString(mWebRootDirString+"results-view.html");
		
		if (mainPageContentString != null) {		
			
			String competitionTablesString = null; 
			CompetitionEvaluationType evalType = compEvalStat.getEvaluationType();
			if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV || evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV) {
				competitionTablesString = createCompetitionResultsTableString(compEvalStat);
			} else if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_HTML) {
				competitionTablesString = createCompetitionResultsHTMLChartString(compEvalStat);
			} else if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_PNG) {
				competitionTablesString = createCompetitionResultsPictureString(compEvalStat);
			}
			if (competitionTablesString != null && !competitionTablesString.isEmpty()) {
				mainPageContentString = mainPageContentString.replace("<div class=\"alert alert-danger\">No connection to evaluation server or no competition loaded.</div>", competitionTablesString);
			}
		}
		
		if (mainPageContentString == null) {
			mainPageContentString = "<html><body>Failed to load page templates</body></html>";
		}
		return mainPageContentString;
		
	}

	


	protected String getPictureDownloadString(CompetitionEvaluationStatus evaluationStatus) {		
		String fileString = URLEncoder.encode(evaluationStatus.getCompetitionName().replace(" ","-")+"-"+evaluationStatus.getEvaluationName().replace(" ","-"));	
		CompetitionEvaluationType evalType = evaluationStatus.getEvaluationType();
		fileString = fileString+"."+evalType.getFileEndString();
		return "download/"+fileString+"?disc="+URLEncoder.encode(evaluationStatus.getCompetitionName())+"&eval="+URLEncoder.encode(evaluationStatus.getEvaluationName())+"&force=false";
	}

	
	

	protected String createCompetitionResultsPictureString(CompetitionEvaluationStatus compEvalStat) {
		StringBuilder sb = new StringBuilder();			
		sb.append("<div class=\"panel panel-black\"><div class=\"panel-heading\"> Showing "+compEvalStat.getEvaluationName()+" for discipline <strong>"+compEvalStat.getCompetitionName()+"</strong></div>");
		
		String pictureSoureString = getPictureDownloadString(compEvalStat);

		String contentDataString = "<img src=\""+pictureSoureString+"\" width=\"100%\" height=\"100%\"/>";
		
    	if (contentDataString != null) {
    		sb.append(contentDataString);    		
    	}

		sb.append("</div>");		
		return sb.toString();
	}	
	
	
	
	protected String createCompetitionResultsHTMLChartString(CompetitionEvaluationStatus compEvalStat) {
		StringBuilder sb = new StringBuilder();			
		sb.append("<div class=\"panel panel-black\"><div class=\"panel-heading\"> Showing "+compEvalStat.getEvaluationName()+" for discipline <strong>"+compEvalStat.getCompetitionName()+"</strong></div>");
		
		String fileNameString = compEvalStat.getEvaluationSourceString();
    	String fileContentString = loadFileIntoString(fileNameString);
    	
    	if (fileContentString != null) {
    		
        	String contentDataString = fileContentString.substring(fileContentString.indexOf("<!-- CHART_DATA_BEGIN -->"), fileContentString.indexOf("<!-- CHART_DATA_END -->"));
    		
        	sb.append(contentDataString);    		
    	} else {
    		sb.append("<br><div class=\"alert alert-danger\"> Loading of results failed. </div>");
    	}

		sb.append("</div>");		
		return sb.toString();
	}	
	
	
	
	
	
	protected String createCompetitionResultsTableString(CompetitionEvaluationStatus compEvalStat) {
		StringBuilder sb = new StringBuilder();			
		sb.append("<div class=\"panel panel-black\"><div class=\"panel-heading\"> Showing "+compEvalStat.getEvaluationName()+" for discipline <strong>"+compEvalStat.getCompetitionName()+"</strong></div>");
		
		String fileNameString = compEvalStat.getEvaluationSourceString();
    	String fileContentString = loadFileIntoString(fileNameString);
    	
    	String columnSplitType = "\t";
    	if (compEvalStat.getEvaluationType() == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV) {
    		columnSplitType = ",";
    	}
    	
    	if (fileContentString != null) {
    		sb.append("<table class=\"table table-striped table-more-condensed table-bordered\">");
    		
    		String[] rowStrings = fileContentString.split("\n");
    		for (String rowString : rowStrings) {
	    		sb.append("<tr>");
	    		String[] columnStrings = rowString.split(columnSplitType); 
	    		for (String columnString : columnStrings) {	  
	    			sb.append("<td>");
	    			sb.append(columnString);
	    			sb.append("</td>");
	    		}
	    		sb.append("</tr>");
    		}
    		sb.append("</table>");
    	} else {
    		sb.append("<br><div class=\"alert alert-danger\"> Loading of results failed. </div>");
    	}

		sb.append("</div>");		
		return sb.toString();
	}
	
	
	
}
