package org.semanticweb.ore.wep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.semanticweb.ore.competition.CompetitionEvaluationStatus;
import org.semanticweb.ore.competition.CompetitionEvaluationStatusUpdateItem;
import org.semanticweb.ore.competition.CompetitionEvaluationType;
import org.semanticweb.ore.competition.CompetitionExecutionState;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatus;
import org.semanticweb.ore.competition.CompetitionReasonerProgressStatusUpdateItem;
import org.semanticweb.ore.competition.CompetitionStatus;
import org.semanticweb.ore.competition.CompetitionStatusUpdateCollectionManager;
import org.semanticweb.ore.competition.CompetitionStatusUpdateItem;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultMainWebPageServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1089732719764432284L;

	final private static Logger mLogger = LoggerFactory.getLogger(ResultMainWebPageServlet.class);
	
	protected String mWebRootDirString = null;
	protected Config mConfig = null;
	protected CompetitionStatusUpdateCollectionManager mStatusUpdateCollector = null;
	
	protected LinkPageMapper mLinkPageMapper = null;

	
	
	public ResultMainWebPageServlet(CompetitionStatusUpdateCollectionManager statusUpdateCollector, LinkPageMapper linkPageMapper, Config config) {
		mConfig = config;
		mStatusUpdateCollector = statusUpdateCollector;
		mLinkPageMapper = linkPageMapper;
		
		mWebRootDirString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
	}

	

	protected String getReasonerLinkString(String reasonerName) {
		String reasonerLink = mLinkPageMapper.getReasonerLink(reasonerName);
		if (reasonerLink == null) {
			return reasonerName;
		}
		String reasonerLinkString = "<a class=\"reasoner-link\" href=\""+reasonerLink+"\">"+reasonerName+"</a>";
		return reasonerLinkString;
	}
	

	
	protected String getCompetitionLink2String(String competitionName) {
		String competitionLink = mLinkPageMapper.getCompetitionLink(competitionName);
		if (competitionLink == null) {
			return competitionName;
		}
		String competitionLinkString = "<a class=\"discipline-link-2\" href=\""+competitionLink+"\">"+competitionName+"</a>";
		return competitionLinkString;
	}	
	
	

	
	protected String getCompetitionLink1String(String competitionName) {
		String competitionLink = mLinkPageMapper.getCompetitionLink(competitionName);
		if (competitionLink == null) {
			return competitionName;
		}
		String competitionLinkString = "<a class=\"discipline-link-1\" href=\""+competitionLink+"\">"+competitionName+"</a>";
		return competitionLinkString;
	}	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String requestPath = request.getRequestURI();
			
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);	        
        
        String pageContentString = getMainPageContentString();
        
        response.getWriter().println(pageContentString);
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
		}	
		return string;
	}
	
	
	
	
	protected String getMainPageContentString() {
		String mainPageContentString = loadFileIntoString(mWebRootDirString+"results.html");
		
		if (mainPageContentString != null) {		
			
		   	Collection<CompetitionStatusUpdateItem> updateItemCollection = mStatusUpdateCollector.getUpdatedCompetitionStatusItemCollection(0);
			
			String competitionTablesString = createCompetitionResultsTablesString(updateItemCollection);
			if (competitionTablesString != null && !competitionTablesString.isEmpty()) {
				mainPageContentString = mainPageContentString.replace("<div class=\"alert alert-danger\">No connection to evaluation server or no competition loaded.</div>", competitionTablesString);
			}
		}
		
		if (mainPageContentString == null) {
			mainPageContentString = "<html><body>Failed to load page templates</body></html>";
		}
		
		
		return mainPageContentString;
		
	}

	
	protected String getResultViewString(CompetitionEvaluationStatus evaluationStatus) {
		return "results/view?disc="+URLEncoder.encode(evaluationStatus.getCompetitionName())+"&eval="+URLEncoder.encode(evaluationStatus.getEvaluationName());
	}
	
	

	protected String getResultDownloadString(CompetitionEvaluationStatus evaluationStatus) {		
		String fileString = URLEncoder.encode(evaluationStatus.getCompetitionName().replace(" ","-")+"-"+evaluationStatus.getEvaluationName().replace(" ","-"));	
		CompetitionEvaluationType evalType = evaluationStatus.getEvaluationType();
		fileString = fileString+"."+evalType.getFileEndString();
		return "results/download/"+fileString+"?disc="+URLEncoder.encode(evaluationStatus.getCompetitionName())+"&eval="+URLEncoder.encode(evaluationStatus.getEvaluationName())+"&force=true";
	}

	
	

	protected String getResultTypeString(CompetitionEvaluationStatus evaluationStatus) {
		String labelTypeString = "?";
		String labelClassString = "label-default";
		CompetitionEvaluationType evalType = evaluationStatus.getEvaluationType();
		if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_CSV) {	
			//labelClassString = "label-csv";
			labelTypeString = "CSV";
		} else if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_TABLE_TSV) {	
			//labelClassString = "label-tsv";
			labelTypeString = "TSV";
		} else if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_PNG) {	
			//labelClassString = "label-png";
			labelTypeString = "PNG";
		} else if (evalType == CompetitionEvaluationType.COMPETITION_EVALUTION_TYPE_CHART_HTML) {	
			//labelClassString = "label-html";
			labelTypeString = "HTML";
		}
		String labelString = "<span class=\"label "+labelClassString+"\">"+labelTypeString+"</span>";	
		return labelString;
	}
	
	
	
	protected String createCompetitionResultsTablesString(Collection<CompetitionStatusUpdateItem> updateItemCollection) {
		StringBuilder sb = new StringBuilder();
		
		for (CompetitionStatusUpdateItem compStatUpdateItem : updateItemCollection) {			
			CompetitionStatus compStatus = compStatUpdateItem.getCompetitionStatus();
			sb.append("<div class=\"panel panel-black\"><div class=\"panel-heading\"> Results for discipline <strong>"+getCompetitionLink2String(compStatus.getCompetitionName())+"</strong></div><div class=\"panel-body\">");
			if (compStatus.getExecutionState() != CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
				sb.append("<br><div class=\"alert alert-danger\"> Results are available as soon as all computations for the discipline <strong>"+getCompetitionLink1String(compStatus.getCompetitionName())+"</strong> are completed. </div>");
			} else {
				sb.append("<div class=\"row\">");
				
				sb.append("<div class=\"col-md-4\">");
				sb.append("<h4>Final Ranking:</h4>");
				Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerStatUpItemVector = compStatUpdateItem.getReasonerProgressUpdateItemVector();
				if (reasonerStatUpItemVector != null) {		
					for (int i = 0; i < reasonerStatUpItemVector.size(); ++i) {
						CompetitionReasonerProgressStatusUpdateItem reasonerStatUpItem = reasonerStatUpItemVector.get(i);
						CompetitionReasonerProgressStatus reasonerStatus = reasonerStatUpItem.getCompetitionReasonerProgressStatus();
						int rank = reasonerStatus.getReasonerRank();
						String rankString = String.valueOf(rank);
						if (rank < 10 && reasonerStatUpItemVector.size() >= 10) {
							rankString = "&nbsp;&nbsp;"+rankString;
						}
						String reasonerName = reasonerStatus.getReasonerName();
						if (rank == 1) {
							sb.append("<h3><span class=\"label label-success\">&nbsp;"+rankString+".&nbsp;</span>&nbsp;&nbsp;<strong>"+getReasonerLinkString(reasonerName)+"</strong></h3>");
						} else if (rank <= 3) {
							sb.append("<h4>&nbsp;<span class=\"label label-primary\">&nbsp;"+rankString+".&nbsp;</span>&nbsp;&nbsp;&nbsp;<strong>"+getReasonerLinkString(reasonerName)+"</strong></h4>");
						} else {
							sb.append("<h4>&nbsp;<span class=\"label label-default\">&nbsp;"+rankString+".&nbsp;</span>&nbsp;&nbsp;&nbsp;"+getReasonerLinkString(reasonerName)+"</h4>");
						}
					}
				}
				
				sb.append("</div><div class=\"col-md-8\">");	
				sb.append("<div><h4>Detailed Results:<h4></div>");
				HashMap<String,CompetitionEvaluationStatusUpdateItem> evalMap = compStatUpdateItem.getEvaluationMap();
				if (evalMap == null || evalMap.isEmpty()) {
					sb.append("<br><div class=\"alert alert-danger\"> Result details are not yet available.</div>");
				} else {
					sb.append("<ul>");

					
					TreeMap<String,CompetitionEvaluationStatusUpdateItem> sortedevalStatUpdateItemMap = new TreeMap<String,CompetitionEvaluationStatusUpdateItem>(); 
					for (CompetitionEvaluationStatusUpdateItem evalStatUpdateItem : evalMap.values()) {
						sortedevalStatUpdateItemMap.put(evalStatUpdateItem.getCompetitionEvaluationStatus().getEvaluationSourceString(), evalStatUpdateItem);
					}
					
					for (CompetitionEvaluationStatusUpdateItem evalStatUpdateItem : sortedevalStatUpdateItemMap.values()) {
						CompetitionEvaluationStatus evalStatus = evalStatUpdateItem.getCompetitionEvaluationStatus();
						sb.append("<li>"+getResultTypeString(evalStatus)+" <a href=\""+getResultViewString(evalStatus)+"\">"+evalStatus.getEvaluationName()+"</a> (<a href=\""+getResultDownloadString(evalStatus)+"\">Download</a>)</li>");
					}
					sb.append("</ul>");
				}

				sb.append("</div></div>");
			}
			sb.append("</div></div>");
		}
		return sb.toString();
	}
	
	
	
}
