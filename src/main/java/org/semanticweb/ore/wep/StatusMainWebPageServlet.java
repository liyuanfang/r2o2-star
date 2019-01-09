package org.semanticweb.ore.wep;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

public class StatusMainWebPageServlet extends HttpServlet {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1089732719764432284L;

	final private static Logger mLogger = LoggerFactory.getLogger(StatusMainWebPageServlet.class);
	
	protected String mWebRootDirString = null;
	protected String mMainPageBaseContentString = null;
	protected boolean mMainPageBaseContentLoaded = false;
	protected Config mConfig = null;
	protected CompetitionStatusUpdateCollectionManager mStatusUpdateCollector = null;
	
	protected int mColumnStackCount = 2;
	protected int mCompetitionCountStackStarting = 1;
	
	protected int mScreenViewMaxCompetitionRowCount = 2;
	
	protected LinkPageMapper mLinkPageMapper = null;
	
	static protected AtomicInteger mStreamViewerCount = new AtomicInteger();
	
	
	protected class LocalUpdateContext {
	
		public boolean mScreenView = false;
		public long mLastUpdateID = 0;
		public HashMap<String,CompetitionStatusUpdateItem> mLastCompetitionStatusItemMap = new HashMap<String,CompetitionStatusUpdateItem>();
		public HashMap<String,CompetitionReasonerProgressStatusUpdateItem> mLastCompetitionReasonerStatusItemMap = new HashMap<String,CompetitionReasonerProgressStatusUpdateItem>();

	}
	
	
	protected enum IDType {
		ID_COMPETITION_OVERVIEW_NAME("con"),
		ID_COMPETITION_OVERVIEW_TIME("cot"),
		ID_COMPETITION_EXECUTION_STATE("st");
		
		private IDType (String shortName) {
			mShortName = shortName;
		}
		
		private String mShortName = null;
		
		public String getShortName() {
			return mShortName;
		}
	}
	

	protected enum AlignType {
		ALIGN_RIGHT,
		ALIGN_LEFT,
		ALIGN_CENTER;
	}
	
	
	protected class CompetitionStatusUpdateSortingItem  implements Comparable<CompetitionStatusUpdateSortingItem> {	
		CompetitionStatusUpdateItem mCompStatusItem = null;
		
		CompetitionStatusUpdateSortingItem(CompetitionStatusUpdateItem compStatusItem) {
			mCompStatusItem = compStatusItem;
		}

		@Override
		public int compareTo(CompetitionStatusUpdateSortingItem o) {
			CompetitionStatus compStatus = mCompStatusItem.getCompetitionStatus();
			CompetitionStatus otherCompStatus = o.mCompStatusItem.getCompetitionStatus();
			if (compStatus.getStartingDate() != null) {
				if (otherCompStatus.getStartingDate() == null) {
					return 1;
				} else {
					if (compStatus.getStartingDate().isBefore(otherCompStatus.getStartingDate())) {
						return -1;
					} else if (compStatus.getStartingDate().isAfter(otherCompStatus.getStartingDate())) {
						return 1;
					}
				}
			}
			if (otherCompStatus.getStartingDate() != null) {
				return -1;
			}
			if (mCompStatusItem.getCompetitionID() < o.mCompStatusItem.getCompetitionID()) {
				return -1;
			} else if (mCompStatusItem.getCompetitionID() > o.mCompStatusItem.getCompetitionID()) {
				return 1;
			}
			return 0;
		}
	}
	
	
	
	protected String getReasonerLinkString(String reasonerName) {
		String reasonerLink = mLinkPageMapper.getReasonerLink(reasonerName);
		if (reasonerLink == null) {
			return reasonerName;
		}
		String reasonerLinkString = "<a class=\"reasoner-link\" href=\""+reasonerLink+"\">"+reasonerName+"</a>";
		return reasonerLinkString;
	}
	

	
	protected String getCompetitionLink1String(String competitionName) {
		String competitionLink = mLinkPageMapper.getCompetitionLink(competitionName);
		if (competitionLink == null) {
			return competitionName;
		}
		String competitionLinkString = "<a class=\"discipline-link-1\" href=\""+competitionLink+"\">"+competitionName+"</a>";
		return competitionLinkString;
	}

	protected String getCompetitionLink2String(String competitionName) {
		String competitionLink = mLinkPageMapper.getCompetitionLink(competitionName);
		if (competitionLink == null) {
			return competitionName;
		}
		String competitionLinkString = "<a class=\"discipline-link-2\" href=\""+competitionLink+"\">"+competitionName+"</a>";
		return competitionLinkString;
	}
	
	public StatusMainWebPageServlet(CompetitionStatusUpdateCollectionManager statusUpdateCollector, LinkPageMapper linkPageMapper, Config config) {
		mConfig = config;
		mStatusUpdateCollector = statusUpdateCollector;
		mLinkPageMapper = linkPageMapper;
		
		mWebRootDirString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
	}

	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LocalUpdateContext updateContext = new LocalUpdateContext();
		mStreamViewerCount.incrementAndGet();
		
		
	    String requestPath = request.getRequestURI();
	    String screenParamString = request.getParameter("screen");
	    if (requestPath.compareTo("/liveScreen.html") == 0 || screenParamString != null && screenParamString.equalsIgnoreCase("true")) {
	    	updateContext.mScreenView = true;	    	
	    }
		
		if (requestPath.compareTo("/") == 0 || requestPath.compareTo("/live.html") == 0 || requestPath.compareTo("/liveScreen.html") == 0) {
			
	        response.setContentType("text/html");
	        response.setStatus(HttpServletResponse.SC_OK);
	        
	        String fileString = requestPath.replace("/", "");
	        if (fileString.isEmpty()) {
	        	fileString = "live.html";
	        }
	        
	        String pageContentString = getMainPageContentString(fileString,updateContext);
	        
	        response.getWriter().println(pageContentString);
		} else {
			


	        response.setContentType("text/html");
	        response.setStatus(HttpServletResponse.SC_OK);
			
	        CompetitionStatusUpdateBlockingListner updateListner = new CompetitionStatusUpdateBlockingListner();	        
        	mLogger.info("Starting streaming to client '{}', currently {} viewers.",request.getRemoteAddr().toString(),mStreamViewerCount.get());

	        
	        try {
		        mStatusUpdateCollector.addUpdateListner(updateListner);
		        boolean allCompFinished = false;
		        while (!allCompFinished || updateContext.mLastUpdateID != mStatusUpdateCollector.getCurrentUpdateID()) {
		        	long newUpdateID = mStatusUpdateCollector.getCurrentUpdateID();
		        	Collection<CompetitionStatusUpdateItem> updateItemCollection = mStatusUpdateCollector.getUpdatedCompetitionStatusItemCollection(updateContext.mLastUpdateID);
		        	
					String streamContentString = "";
					if (hasToUpdateTableStructure(updateItemCollection,updateContext)) {
						createLastUpdateData(updateItemCollection,updateContext);
						streamContentString = getUpdateInnerHTMLCommand("base",createCompetitionTablesString(updateContext.mLastCompetitionStatusItemMap.values(),updateContext));
						streamContentString = streamContentString+createCompetitionOverlayString(updateContext.mLastCompetitionStatusItemMap.values(),updateContext);
					} else {
						streamContentString = createCompetitionsStreamContentString(updateItemCollection,updateContext);
						createLastUpdateData(updateItemCollection,updateContext);
					}
					
			        response.getWriter().print(streamContentString);
			        response.getWriter().print(createViewerCountStreamContentString());
			        response.getWriter().flush();
			        
			        
			        updateContext.mLastUpdateID = newUpdateID;
			        if (!allCompFinished) {
			        	updateListner.waitUpdated(180000);
//			        	allCompFinished = mStatusUpdateCollector.allCompetitionsFinished();
			        }
		        }
	        } catch (Exception e) {
	        	mLogger.error("Failed to stream results to client '{}', got Exception '{}'",request.getRemoteAddr().toString(),e.getMessage());
	        	e.printStackTrace();
	        } finally {
	        	mStatusUpdateCollector.removeUpdateListner(updateListner);
	        }
	        
	        mLogger.info("Stopping streaming to client '{}', {} viewers remaining.",request.getRemoteAddr().toString(),mStreamViewerCount.get()-1);

		}
//		} else {
//			String fileString = loadFileIntoString(mWebRootDirString+"."+requestPath);
//			if (fileString != null) {				
//		        response.setContentType("text/html");
//		        response.setStatus(HttpServletResponse.SC_OK);
//		        response.getWriter().println(fileString);
//			} else {
//				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//			}
//		}

		mStreamViewerCount.decrementAndGet();
    }
	
	
	protected void createLastUpdateData(Collection<CompetitionStatusUpdateItem> updateItemCollection, LocalUpdateContext updateContext) {		
		for (CompetitionStatusUpdateItem compStatusItem : updateItemCollection) {
			updateContext.mLastCompetitionStatusItemMap.put(compStatusItem.getCompetitionStatus().getCompetitionSourceString(), compStatusItem);
			Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressStatusItemVector = compStatusItem.getReasonerProgressUpdateItemVector();
			if (reasonerProgressStatusItemVector != null) {
				for (CompetitionReasonerProgressStatusUpdateItem reasonerUpdateItem : reasonerProgressStatusItemVector) {
					if (reasonerUpdateItem != null && reasonerUpdateItem.isUpdated(updateContext.mLastUpdateID)) {
						updateContext.mLastCompetitionReasonerStatusItemMap.put(compStatusItem.getCompetitionStatus().getCompetitionSourceString()+":"+reasonerUpdateItem.getCompetitionReasonerProgressStatus().getReasonerSourceString(),reasonerUpdateItem);
					}
				}
			}
		}
	}
	
	
	
	protected boolean hasToUpdateTableStructure(Collection<CompetitionStatusUpdateItem> updateItemCollection, LocalUpdateContext updateContext) {
		for (CompetitionStatusUpdateItem compStatusItem : updateItemCollection) {
			CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
			CompetitionStatusUpdateItem lastCompStatusItem = updateContext.mLastCompetitionStatusItemMap.get(compStatus.getCompetitionSourceString());
			if (lastCompStatusItem == null) {		
				return true;
			} else {				
				CompetitionStatus lastCompStatus = lastCompStatusItem.getCompetitionStatus();
				if (lastCompStatus.getExecutionState() != compStatus.getExecutionState()) {
					return true;
				}
			}
		}
		return false;
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
	
	
	
	
	protected String getMainPageContentString(String fileString, LocalUpdateContext updateContext) {
		String mainPageContentString = loadFileIntoString(mWebRootDirString+fileString);
		
		if (mainPageContentString != null) {		
			
		   	Collection<CompetitionStatusUpdateItem> updateItemCollection = mStatusUpdateCollector.getUpdatedCompetitionStatusItemCollection(updateContext.mLastUpdateID);
			
			String competitionTablesString = createCompetitionTablesString(updateItemCollection,updateContext);
			if (competitionTablesString != null && !competitionTablesString.isEmpty()) {
				mainPageContentString = mainPageContentString.replace("<div class=\"alert alert-danger\">No connection to evaluation server or no competition loaded.</div>", competitionTablesString);
			}
		}
		
		if (mainPageContentString == null) {
			mainPageContentString = "<html><body>Failed to load page templates</body></html>";
		}
		
		
		return mainPageContentString;
		
	}
	
	
	
	
	protected String createCompetitionOverviewExecutionStatusClassString(CompetitionStatusUpdateItem compStatusItem) {
		String statusString = "";
		CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
		if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_RUNNING) {
			statusString = "success";
		} else if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
			statusString = "black";
		}
		return statusString;
	}
	

	protected String fillUpWithZeros(String string, int requiredLength) {
		while (string.length() < requiredLength) {
			string = "0"+string;
		}
		return string;
	}
		
	
	protected String createCompetitionOverviewTimeString(CompetitionStatusUpdateItem compStatusItem) {
		String timeStartingString = "??";
		String timeEndingString = "??";
		CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
		DateTime startingDateTime = compStatus.getStartingDate();
		if (startingDateTime != null) {
			DateTime dtUTC = startingDateTime.withZone(DateTimeZone.forID("Europe/Vienna"));
			timeStartingString = fillUpWithZeros(String.valueOf(dtUTC.getHourOfDay()),2)+":"+fillUpWithZeros(String.valueOf(dtUTC.getMinuteOfHour()),2);			
		}
		DateTime endingDateTime = compStatus.getEndingDate();
		if (endingDateTime != null) {
			DateTime dtUTC = endingDateTime.withZone(DateTimeZone.forID("Europe/Vienna"));
			timeEndingString = fillUpWithZeros(String.valueOf(dtUTC.getHourOfDay()),2)+":"+fillUpWithZeros(String.valueOf(dtUTC.getMinuteOfHour()),2);
		}
		
		String statusString = timeStartingString+" - "+timeEndingString+" (CEST)";
		return statusString;
	}
	
	
	protected String createCompetitionOverviewString(Collection<CompetitionStatusUpdateItem> updateItemCollection) {
		StringBuilder sb = new StringBuilder();
		
		

		ArrayList<CompetitionStatusUpdateSortingItem> sortingList = new ArrayList<CompetitionStatusUpdateSortingItem>();
		for (CompetitionStatusUpdateItem compStatusItem : updateItemCollection) {
			sortingList.add(new CompetitionStatusUpdateSortingItem(compStatusItem));
		}
		Collections.sort(sortingList);
		
		
		
		
		
		
		
		sb.append("<div class=\"table-responsive\"><table class=\"table table-striped table-more-condensed table-bordered\"><colgroup><col width=\"80\"></colgroup><tr><td style=\"text-align: center\"><strong>Discipline</strong>:</td>");
		for (CompetitionStatusUpdateSortingItem compSortingItem : sortingList) {
			CompetitionStatusUpdateItem compStatusItem = compSortingItem.mCompStatusItem;
			CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
			int compID = compStatusItem.getCompetitionID();			
			String executionStatusClassString = "class=\""+createCompetitionOverviewExecutionStatusClassString(compStatusItem)+"\"";			
			String competitionString = "<td style=\"text-align: center\" "+executionStatusClassString+" id=\""+ getIDString(compID,IDType.ID_COMPETITION_OVERVIEW_NAME.getShortName()) +"\"><small>"+getCompetitionLink1String(compStatus.getCompetitionName())+"</small></td>";
			sb.append(competitionString);
		}
		sb.append("</tr><tr><td style=\"text-align: center\"><strong>Schedule</strong>:</td> ");
		for (CompetitionStatusUpdateSortingItem compSortingItem : sortingList) {
			CompetitionStatusUpdateItem compStatusItem = compSortingItem.mCompStatusItem;
			int compID = compStatusItem.getCompetitionID();			
			String executionStatusClassString = "class=\""+createCompetitionOverviewExecutionStatusClassString(compStatusItem)+"\"";			
			String competitionTimeString = createCompetitionOverviewTimeString(compStatusItem);
			String competitionString = "<td style=\"text-align: center\" "+executionStatusClassString+" id=\""+ getIDString(compID,IDType.ID_COMPETITION_OVERVIEW_TIME.getShortName()) +"\"><small>"+competitionTimeString+"</small></td>";
			sb.append(competitionString);
		}
		sb.append("</tr></table></div> ");
		
		String competitionTablesString = sb.toString();
		return competitionTablesString;
	}
	
	
	
	
	

	protected String createCompetitionsStreamContentString(Collection<CompetitionStatusUpdateItem> updateItemCollection, LocalUpdateContext updateContext) {	
		String competitionStreamContentString = null;
		StringBuilder sb = new StringBuilder();
		for (CompetitionStatusUpdateItem compStatusItem : updateItemCollection) {
			int compID = compStatusItem.getCompetitionID();
			Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressStatusItemVector = compStatusItem.getReasonerProgressUpdateItemVector();
			String competitionTableString = createCompetitionStreamContentString(compID,compStatusItem,reasonerProgressStatusItemVector,updateContext);
			sb.append(competitionTableString);
		}
		competitionStreamContentString = sb.toString();
		return competitionStreamContentString;
	}
	
	
	

	
	
	protected String createViewerCountStreamContentString() {
		int viewerCount = mStreamViewerCount.get();
		String viewerCountStreamCo = getUpdateInnerHTMLCommand("viewer-count",String.valueOf(viewerCount));
		return viewerCountStreamCo;
	}
	
	
	

	protected String createCompetitionStreamContentString(int competitionID, CompetitionStatusUpdateItem compStatusItem, Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressStatusItemVector, LocalUpdateContext updateContext) {
		String tableString = null;
		StringBuilder sb = new StringBuilder();
		
		if (compStatusItem.isCompetitionUpdated(updateContext.mLastUpdateID)) {
			String compExecStateStreamContentString = createCompetitionExecutionStateString(competitionID,compStatusItem);
			sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,IDType.ID_COMPETITION_EXECUTION_STATE.getShortName()),compExecStateStreamContentString));
			String compOverviewExecStateStreamContentString = createCompetitionOverviewExecutionStatusClassString(compStatusItem);		
			sb.append(getUpdateClassCommand(getIDString(competitionID,IDType.ID_COMPETITION_OVERVIEW_NAME.getShortName()),compOverviewExecStateStreamContentString));
			sb.append(getUpdateClassCommand(getIDString(competitionID,IDType.ID_COMPETITION_OVERVIEW_TIME.getShortName()),compOverviewExecStateStreamContentString));
		}
		
		for (int i = 0; i < compStatusItem.getCompetitionStatus().getReasonerCount(); ++i) {
			CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem = reasonerProgressStatusItemVector.get(i);
			if (competitionReasonerProgressStatusUpdateItem != null) {
				if (competitionReasonerProgressStatusUpdateItem.isUpdated(updateContext.mLastUpdateID)) {
					String reasonerStreamContentString = createCompetitionReasonerProgressStreamContentString(competitionID,i,compStatusItem,competitionReasonerProgressStatusUpdateItem,updateContext);
					sb.append(reasonerStreamContentString);
				}
			}
		}
		tableString = sb.toString();
		return tableString;
	}	
	
		

	
	
	protected String createCompetitionReasonerProgressStreamContentString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem, LocalUpdateContext updateContext) {
		String contentString = null;
		StringBuilder sb = new StringBuilder();
		int nextColumn = 0;
		String rankString = "?";
		String reasonerName = "?????";
		String correctlyProccessedTimeString = "?.?? s";
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			rankString = String.valueOf(reasonerProgressStatus.getReasonerRank());
			reasonerName = getReasonerLinkString(reasonerProgressStatus.getReasonerName());
			String correctlyProccessedTime = String.format(Locale.ENGLISH,"%1$,.1f", reasonerProgressStatus.getCorrectlyProccessedTime()/(double)1000);
			correctlyProccessedTimeString = "<span class=\"time-success\">"+correctlyProccessedTime+"</span> s";
		}
		sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,nextColumn++),rankString));
		sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,nextColumn++),reasonerName));		
		sb.append(createCompetitionReasonerProgressCommandString(getIDString(competitionID,reasonerID,nextColumn++),competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem,updateContext));
		//sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,nextColumn++),createCompetitionReasonerProgressString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem)));
		sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,"ss"),createCompetitionReasonerScoreStreamString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem)));
		nextColumn++;
		sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,"se"),createCompetitionReasonerErrorStreamString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem)));
		nextColumn++;
		sb.append(getUpdateInnerHTMLCommand(getIDString(competitionID,reasonerID,nextColumn++),correctlyProccessedTimeString));
		sb.append(createCompetitionReasonerProgressStreamBlinkString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem,updateContext));
		contentString = sb.toString();
		return contentString;
	}
	
	


	protected String createCompetitionReasonerErrorStreamString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem) {
		int incompletelyProcessedCount = 0;
		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			incompletelyProcessedCount = (reasonerProgressStatus.getTotalProcessedCount()-reasonerProgressStatus.getCorrectlyProcessedCount());
		}
		
		return String.valueOf(incompletelyProcessedCount);
	}		
	
	

	protected String createCompetitionReasonerScoreStreamString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem) {
		int correctlyProcessedCount = 0;		
		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			correctlyProcessedCount = reasonerProgressStatus.getCorrectlyProcessedCount();
		}
		
		return String.valueOf(correctlyProcessedCount);
	}		
	
	
		
	
	
	
	protected String createCompetitionReasonerProgressStreamBlinkString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem, LocalUpdateContext updateContext) {
		boolean successBlinkProgress = false;
		boolean failBlinkProgress = false;
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();

			CompetitionReasonerProgressStatusUpdateItem lastCompetitionReasonerProgressStatusUpdateItem = updateContext.mLastCompetitionReasonerStatusItemMap.get(compStatusItem.getCompetitionStatus().getCompetitionSourceString()+":"+reasonerProgressStatus.getReasonerSourceString());
			if (lastCompetitionReasonerProgressStatusUpdateItem == null) {
				successBlinkProgress = true;
			} else {
				if (lastCompetitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getCorrectlyProcessedCount() < competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getCorrectlyProcessedCount()) {
					successBlinkProgress = true;
				} else if (lastCompetitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getTotalProcessedCount() < competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getTotalProcessedCount()) {
					failBlinkProgress = true;
				}				
			}
		}
		
		if (successBlinkProgress || failBlinkProgress) {			
			String classString = null;
			if (successBlinkProgress) {
				classString = "success-blink";
			} else {
				classString = "fail-blink";
			}
			return getUpdateBlinkCommand(getIDString(competitionID,reasonerID),classString);
		} else {
			return "";
		}
	}
	
	

	
	protected String getShowOverlayCountdownCommand(String showInnerHTML, long time, int countdown, String counterIDString) {
		return "SOC\t"+time+"\t"+countdown+"\t"+counterIDString+"\t"+showInnerHTML+"\n";
	}	
	
	protected String getUpdateInnerHTMLCommand(String idString, String newInnerHTML) {
		return "UIH\t"+idString+"\t"+newInnerHTML+"\n";
	}	

	protected String getUpdateStyleWidthCommand(String idString, String styleWidth) {
		return "USW\t"+idString+"\t"+styleWidth+"\n";
	}	
	

	protected String getUpdateClassCommand(String idString, String classString) {
		return "UCL\t"+idString+"\t"+classString+"\n";
	}	
	

	protected String getUpdateBlinkCommand(String idString, String classString) {
		return "UBA\t"+idString+"\t"+classString+"\n";
	}		
	
	
	
	
	protected String createCompetitionOverlayString(Collection<CompetitionStatusUpdateItem> updateItemCollection, LocalUpdateContext updateContext) {
		
		for (CompetitionStatusUpdateItem comStatUpItem : updateItemCollection) {
			CompetitionStatus compStatus = comStatUpItem.getCompetitionStatus();
			if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_PREPARATION) {
				
				long executionTime = compStatus.getExecutionTime();
				if (executionTime < -1000) {
					DateTime statusTimeStamp = compStatus.getTimeStamp();
					DateTime currentDateTime = new DateTime(DateTimeZone.UTC);
					if (currentDateTime.isAfter(statusTimeStamp)) {
						long timeStampDiff = currentDateTime.getMillis() - statusTimeStamp.getMillis();
						long newExecutionTime = timeStampDiff + executionTime;
						if (newExecutionTime < -3000) {
							long remainingPreparationTime = -newExecutionTime;
							String counterIDString = "overlay-counter";
							StringBuilder sb = new StringBuilder();
//							sb.append("<br><br><br><br><div class=\"row\"><div class=\"col-md-6\">");
//							sb.append("<img src=\"imgs/ore-logo2014.png\" width=\"300\" height=\"187\"/>");
//							sb.append("<h1>Live Competition</h1>");							
//							sb.append("</div><div class=\"col-md-6\">");	
//							sb.append("<br><h3>Start of discipline</h3><h2><strong>");
//							sb.append(getCompetitionLink1String(compStatus.getCompetitionName()));
//							sb.append("</strong></h2><h3>in</h3>");
//							sb.append("<h1><span id=\""+counterIDString+"\">"+remainingPreparationTime/1000+"</span></h1>");
//							sb.append("</div></div>");
							
							sb.append("<br><div class=\"row\"><div class=\"col-md-6\"><img src=\"imgs/ore-logo2014.png\" width=\"300\" height=\"187\"/></div>");
							sb.append("<div class=\"col-md-6\"><br><h4>Sponsored by:</h4><img src=\"imgs/B2i.png\" width=\"180\" height=\"120\"/><img src=\"imgs/dl-small.png\" width=\"120\" height=\"100\"/></div></div>");
							
							sb.append("<br><div class=\"row\"><div class=\"col-md-12\"><h3>Start of discipline <strong>");
							sb.append(getCompetitionLink1String(compStatus.getCompetitionName()));
							sb.append("</strong> in: </h3><h1><span id=\""+counterIDString+"\" style=\"font-size: 2.0em; text-color: #d9534f;\">"+remainingPreparationTime/1000+"</span></h1>");
							sb.append("</div></div>");
							
							String showOverlayCountdownCommandString = getShowOverlayCountdownCommand(sb.toString(),remainingPreparationTime-1000,(int)remainingPreparationTime/1000,counterIDString);
							return showOverlayCountdownCommandString;
						}
					}
				}
			}
		}
		return "";
	}

	
	
	protected String createCompetitionTablesString(Collection<CompetitionStatusUpdateItem> updateItemCollection, LocalUpdateContext updateContext) {
		StringBuilder sb = new StringBuilder();
		
		boolean appendStartingSoon = false;
		boolean appendLogos = false;
		
		if (updateItemCollection.size() >= 1) {
			sb.append("<div>");
	 		String competitionOverviewString = createCompetitionOverviewString(updateItemCollection);
			sb.append(competitionOverviewString);
			
			boolean allWaiting = true;
			ArrayList<CompetitionStatusUpdateSortingItem> sortingList = new ArrayList<CompetitionStatusUpdateSortingItem>();
			for (CompetitionStatusUpdateItem compStatusItem : updateItemCollection) {
				CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
				boolean showCompInGrid = true;
				if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_WAITING) {
					showCompInGrid = false;					
				}
				if (showCompInGrid) {
					allWaiting = false;
					sortingList.add(new CompetitionStatusUpdateSortingItem(compStatusItem));
				}
			}
			if (allWaiting) {
				appendStartingSoon = true;
			}
			Collections.sort(sortingList);
			Collections.reverse(sortingList);			
			
			ArrayList<ArrayList<CompetitionStatusUpdateItem>> stackingCompStatusUpItemsList = new ArrayList<ArrayList<CompetitionStatusUpdateItem>>(); 
			
			int columnStackCount = mColumnStackCount;
			if (updateContext.mScreenView && columnStackCount < 3 && sortingList.size() >= 5) {
				columnStackCount = 3;
			}
			if (updateContext.mScreenView && sortingList.size() >= 2) {
				sb.append("<br>");
			}
			
			ListIterator<CompetitionStatusUpdateSortingItem> compStatusSortItemIt = sortingList.listIterator();
			if (compStatusSortItemIt.hasNext()) {
				CompetitionStatusUpdateSortingItem compStatusUpSortItem = compStatusSortItemIt.next();
				ArrayList<CompetitionStatusUpdateItem> nextStackingList = new ArrayList<CompetitionStatusUpdateItem>();
				nextStackingList.add(compStatusUpSortItem.mCompStatusItem);
				stackingCompStatusUpItemsList.add(nextStackingList);
				
				if (columnStackCount == 2 && compStatusSortItemIt.hasNext() && sortingList.size() > 2 && sortingList.size() % 2 == 0) {				
					CompetitionStatusUpdateSortingItem addCompStatusUpSortItem = compStatusSortItemIt.next();
					nextStackingList.add(addCompStatusUpSortItem.mCompStatusItem);
				}
				
				if (updateContext.mScreenView && columnStackCount == 3) {
					if (compStatusSortItemIt.hasNext() && sortingList.size() > 4) {				
						CompetitionStatusUpdateSortingItem addCompStatusUpSortItem = compStatusSortItemIt.next();
						nextStackingList.add(addCompStatusUpSortItem.mCompStatusItem);
					}
					if (compStatusSortItemIt.hasNext() && sortingList.size() > 5) {				
						CompetitionStatusUpdateSortingItem addCompStatusUpSortItem = compStatusSortItemIt.next();
						nextStackingList.add(addCompStatusUpSortItem.mCompStatusItem);
					}
				}
			}

	
			
			while (compStatusSortItemIt.hasNext()) {
				ArrayList<CompetitionStatusUpdateItem> nextStackingList = new ArrayList<CompetitionStatusUpdateItem>();
				int stackingNumber = 0;
				while (compStatusSortItemIt.hasNext() && stackingNumber++ < columnStackCount) {					
					CompetitionStatusUpdateItem compStatUpItem = compStatusSortItemIt.next().mCompStatusItem;
					nextStackingList.add(compStatUpItem);
				}
//				while (nextStackingList.size() < mColumnStackCount) {			
//					nextStackingList.add(null);
//				}
				stackingCompStatusUpItemsList.add(nextStackingList);
			}
			
			
			
			String stackingStartString =  "<div class=\"row\">";
			String startNextStackingString = "<div class=\"col-md-12\">";

			String endNextStackingString = "</div>";
			
				
			String endStackingString = "</div>";
			
			String spaceString = "";		
			
			if (stackingCompStatusUpItemsList.size() <= 1) {				
				spaceString = "<br>";
			} else if (stackingCompStatusUpItemsList.size() <= 2) {		
				if (!updateContext.mScreenView) {
					spaceString = "<br>";
				}
			}
			
			int rowNumber = 0;
			
			Iterator<ArrayList<CompetitionStatusUpdateItem>> stackingompStatusUpItemsIt = stackingCompStatusUpItemsList.listIterator();
			while (stackingompStatusUpItemsIt.hasNext()) {
				++rowNumber;
				ArrayList<CompetitionStatusUpdateItem> nextStackingList = stackingompStatusUpItemsIt.next();
			
				if (rowNumber <= mScreenViewMaxCompetitionRowCount || !updateContext.mScreenView) {
					
					int stackingCount = nextStackingList.size();		
					
					startNextStackingString = "<div class=\"col-md-12\">";	
					if (stackingCount >= 2) {
						startNextStackingString = "<div class=\"col-md-6\">";		
					}
					if (stackingCount >= 3) {			
						startNextStackingString = "<div class=\"col-md-4\">";
					}
					if (stackingCount >= 4) {			
						startNextStackingString = "<div class=\"col-md-3\">";
					}
					
					sb.append(spaceString);
					sb.append(stackingStartString);
					
					int reasonerRowCount = 0;
					
					Iterator<CompetitionStatusUpdateItem> compStatUpItemReasonerCountCheckIt = nextStackingList.listIterator();
					while (compStatUpItemReasonerCountCheckIt.hasNext()) {
						CompetitionStatusUpdateItem compStatUpItem = compStatUpItemReasonerCountCheckIt.next();
						if (compStatUpItem != null) {
							if (compStatUpItem.getCompetitionStatus().getReasonerCount() > reasonerRowCount) {
								reasonerRowCount = compStatUpItem.getCompetitionStatus().getReasonerCount();
							}
						}
					}
					
					int condensedLevel = stackingCount;
					if (updateContext.mScreenView && rowNumber > 1 && condensedLevel <= 1) {		
						condensedLevel = 2;
					}
					
					
					Iterator<CompetitionStatusUpdateItem> compStatUpItemIt = nextStackingList.listIterator();
					while (compStatUpItemIt.hasNext()) {
						CompetitionStatusUpdateItem compStatUpItem = compStatUpItemIt.next();
						if (compStatUpItem != null) {
							int rowFillUpCount = reasonerRowCount - compStatUpItem.getCompetitionStatus().getReasonerCount();
							if (rowFillUpCount < 0) {
								rowFillUpCount = 0;
							}
							sb.append(startNextStackingString);
							
							int compID = compStatUpItem.getCompetitionID();
							Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressStatusItemVector = compStatUpItem.getReasonerProgressUpdateItemVector();
							String competitionTableString = createCompetitionTableString(compID,compStatUpItem,reasonerProgressStatusItemVector,condensedLevel,rowFillUpCount);
							sb.append(competitionTableString);
							
							sb.append(endNextStackingString);
						}
					}
					
					
					sb.append(endStackingString);
				}
			}
			
			if (rowNumber <= 1) {	
				appendLogos = true;
			}
			

			sb.append("</div>");
		}
		
		
		if (appendLogos) {
			sb.append("<br>");
			sb.append("<div class=\"row\" style=\"text-align:center; opacity: 1.0;\"><div class=\"col-md-2\"></div><div class=\"col-md-4\"><img src=\"imgs/ore-logo2014.png\" width=\"300\" height=\"187\"/></div>");
			sb.append("<div class=\"col-md-4\"><br><br><br><h4>Sponsored by:</h4><img src=\"imgs/B2i.png\" width=\"140\" height=\"85\"/><img src=\"imgs/dl-small.png\" width=\"85\" height=\"70\"/></div><div class=\"col-md-2\"></div>");
			if (appendStartingSoon) {
				sb.append("<div class=\"col-md-12\"><br><h2>Live Competition is starting soon...</h2></div>");
			}
			sb.append("</div>");
		}
		
		String competitionTablesString = sb.toString();
		return competitionTablesString;
	}
	
	
	protected String getIDString(int competitionID, int rowID, int columnID) {
		return "t"+competitionID+"_r"+rowID+"_c"+columnID;
	}
	
	protected String getIDString(int competitionID, int rowID) {
		return "t"+competitionID+"_r"+rowID;
	}
		

	protected String getIDString(int competitionID, int rowID, String specialName) {
		return "t"+competitionID+"_r"+rowID+"_s"+specialName;
	}
		
	
	protected String getIDString(int competitionID, String specialName) {
		return "t"+competitionID+"_s"+specialName;
	}	
	
	protected String getIDString(int competitionID) {
		return "t"+competitionID;
	}	
	
	
	protected String createCompetitionExecutionStateString(int competitionID, CompetitionStatusUpdateItem compStatusItem) {
		CompetitionStatus compStatus = compStatusItem.getCompetitionStatus();
		String execState = "(queued)";
		if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_FINISHED) {
			execState = "(finished)";
		} else if (compStatus.getExecutionState() == CompetitionExecutionState.COMPETITION_EXECUTION_STATE_RUNNING) {
			execState = "("+compStatus.getExecutionHandlerCount()+" running)";
		}
		return execState;
	}
	
	
	protected String createCompetitionTableString(int competitionID, CompetitionStatusUpdateItem compStatusItem, Vector<CompetitionReasonerProgressStatusUpdateItem> reasonerProgressStatusItemVecto, int condensedLevel, int rowFillUpCount) {
		String tableString = null;
		StringBuilder sb = new StringBuilder();
		CompetitionStatus competitionStatus = compStatusItem.getCompetitionStatus();
		if (condensedLevel >= 2) {
			sb.append("<small>");
		}
		
		int missingRowCount = rowFillUpCount;
		
		
		String titleHeightString = "100%";
		String titleStatusWidthString = "80";
		String titleWidthString = "*";
		boolean twoRowHeader = false;
		if (missingRowCount > 1 && condensedLevel >= 2) {
			titleHeightString = "37px";
			//titleWidthString = "120";
			titleStatusWidthString = "*";
			twoRowHeader = true;
			--missingRowCount;
		}		
		
		
		sb.append("<div class=\"panel panel-black\"> <div class=\"panel-heading\">");
		sb.append("<table width=\"100%\"><colgroup><col width=\""+titleWidthString+"\"><col width=\""+titleStatusWidthString+"\"></colgroup> <tr height=\""+titleHeightString+"\"> <td>");
		sb.append("Discipline: <span id=\""+getIDString(competitionID,"cn")+"\">");
		if (twoRowHeader) {
			sb.append("<br>");
		}
		sb.append("<strong>"+getCompetitionLink2String(competitionStatus.getCompetitionName())+"</strong>");
		sb.append("</span></td><td style=\"text-align: right\" id=\""+getIDString(competitionID,"st")+"\">");
		sb.append(createCompetitionExecutionStateString(competitionID,compStatusItem));
		sb.append("</td></tr></table></div>");
		
		if (missingRowCount > 1) {
			sb.append("<div style=\"height:20px; overflow:hidden;\"> </div>");
			--missingRowCount;
		}

		if (condensedLevel >= 3) {
			sb.append("<table id=\""+getIDString(competitionID)+"\" class=\"table table-striped table-very-condensed table-bordered\"><colgroup>");
	
			sb.append("<col width=\"10\"><col width=\"*\"><col width=\"68\"><col width=\"10\"><col width=\"65\"></colgroup>");		
			sb.append("<thead><tr></tr>"+
					"<th onmouseover=\"TagToTip('tt-reasoner')\" onmouseout=\"UnTip()\"><u>Reasoner</u>&nbsp;</th>"+
					"<th onmouseover=\"TagToTip('tt-progress')\" onmouseout=\"UnTip()\">&nbsp;<u>Progress</u></th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-score')\" onmouseout=\"UnTip()\"><u>Score</u>&nbsp;</th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-error')\" onmouseout=\"UnTip()\" class=\"score-error\">!&nbsp;</th>"+
					"<th style=\"text-align:right\"  onmouseover=\"TagToTip('tt-time')\" onmouseout=\"UnTip()\"><u>Time</u>&nbsp;</th></thead><tbody>");
		} else if (condensedLevel >= 2) {
			sb.append("<table id=\""+getIDString(competitionID)+"\" class=\"table table-striped table-very-condensed table-bordered\"><colgroup>");
			sb.append("<col width=\"10\"><col width=\"10\"><col width=\"*\"><col width=\"72\"><col width=\"30\"><col width=\"65\"></colgroup>");		
			sb.append("<thead><tr></tr>"+
					"<th style=\"text-align:center\" onmouseover=\"TagToTip('tt-rank')\" onmouseout=\"UnTip()\"><u>Rank</u>&nbsp;</th>"+
					"<th onmouseover=\"TagToTip('tt-reasoner')\" onmouseout=\"UnTip()\"><u>Reasoner</u>&nbsp;</th>"+
					"<th onmouseover=\"TagToTip('tt-progress')\" onmouseout=\"UnTip()\">&nbsp;<u>Progress</u></th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-score')\" onmouseout=\"UnTip()\"><u>Score</u>&nbsp;</th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-error')\" onmouseout=\"UnTip()\" class=\"score-error\">!&nbsp;</th>"+
					"<th style=\"text-align:right\"  onmouseover=\"TagToTip('tt-time')\" onmouseout=\"UnTip()\"><u>Time</u>&nbsp;</th></thead><tbody>");
		} else {
			sb.append("<table id=\""+getIDString(competitionID)+"\" class=\"table table-striped table-more-condensed table-bordered\"><colgroup>");
			sb.append("<col width=\"10\"><col width=\"10\"><col width=\"*\"><col width=\"82\"><col width=\"30\"><col width=\"75\"></colgroup>");		
			sb.append("<thead><tr></tr>"+
					"<th style=\"text-align:center\" onmouseover=\"TagToTip('tt-rank')\" onmouseout=\"UnTip()\"><u>Rank</u>&nbsp;</th>"+
					"<th onmouseover=\"TagToTip('tt-reasoner')\" onmouseout=\"UnTip()\"><u>Reasoner</u>&nbsp;</th>"+
					"<th onmouseover=\"TagToTip('tt-progress')\" onmouseout=\"UnTip()\">&nbsp;<u>Progress</u></th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-score')\" onmouseout=\"UnTip()\"><u>Score</u>&nbsp;</th>"+
					"<th style=\"text-align:right\" onmouseover=\"TagToTip('tt-error')\" onmouseout=\"UnTip()\" class=\"score-error\">!&nbsp;</th>"+
					"<th style=\"text-align:right\"  onmouseover=\"TagToTip('tt-time')\" onmouseout=\"UnTip()\"><u>Time</u>&nbsp;</th></thead><tbody>");
		}
		
		
//		"<th onmouseover=\"TagToTip('tt-rank')\" onmouseout=\"UnTip()\"><span class=\"th-ra\">Rank</span></th>"+
//		"<th onmouseover=\"TagToTip('tt-reasoner')\" onmouseout=\"UnTip()\">&nbsp;<span class=\"th-re\">Reasoner</span>&nbsp;</th>"+
//		"<th onmouseover=\"TagToTip('tt-progress')\" onmouseout=\"UnTip()\">&nbsp;<span class=\"th-pr\">Progress</span></th>"+
//		"<th onmouseover=\"TagToTip('tt-score')\" onmouseout=\"UnTip()\"><span class=\"th-sc\">Score</span>&nbsp;</th>"+
//		"<th onmouseover=\"TagToTip('tt-error')\" onmouseout=\"UnTip()\"><span class=\"th-er\">!</span>&nbsp;</th>"+
//		"<th onmouseover=\"TagToTip('tt-time')\" onmouseout=\"UnTip()\"><span class=\"th-ti\">Time</span>&nbsp;</th></thead><tbody>");

		
//		sb.append("<thead><tr><td colspan=\"5\"><h3><span id=\""+getIDString(competitionID,"cn")+"\">");
//		sb.append("Discipline: "+competitionStatus.getCompetitionName());
//		sb.append("</span><h3></td></tr>  <th>Rank</th>  <th>Reasoner</th>  <th>Progress</th>  <th style=\"text-align:right\">Score</th>  <th style=\"text-align:right\">Time</th>  </thead><tbody>");
		
		for (int i = 0; i < competitionStatus.getReasonerCount(); ++i) {	
			sb.append(createCompetitionReasonerProgressRowString(competitionID,i,compStatusItem,reasonerProgressStatusItemVecto.get(i),condensedLevel));
		}
//		while (missingRowCount > 0) {
//			sb.append("<tr><td>&nbsp;</td><td></td><td></td><td></td><td></td>");
//			if (condensedLevel < 3) {
//				sb.append("<td></td>");
//			}
//			sb.append("</tr>");
//			--missingRowCount;
//		}
		if (missingRowCount > 0) {
			sb.append("<tr></tr>");			
		}
		sb.append("</tbody></table>");
		
		if (missingRowCount > 0) {
			while (missingRowCount > 0) {
				sb.append("<div style=\"height:20px; overflow:hidden;\"> </div>");
				--missingRowCount;
			}
		}
		sb.append("</div>");
		
		if (condensedLevel >= 2) {
			sb.append("</small>");
		}
		tableString = sb.toString();
		return tableString;
	}
	
	
	protected String createCompetitionReasonerProgressRowString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem, int condensedLevel) {
		String rowString = null;
		StringBuilder sb = new StringBuilder();
		sb.append("<tr id=\""+getIDString(competitionID,reasonerID)+"\">");
		int nextColumn = 0;
		String rankString = "?";
		String reasonerName = "?????";
		String correctlyProccessedTimeString = "?.?? s";		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			rankString = String.valueOf(reasonerProgressStatus.getReasonerRank());
			reasonerName = getReasonerLinkString(reasonerProgressStatus.getReasonerName());
			String correctlyProccessedTime = null;
			if (condensedLevel < 3) {
				correctlyProccessedTime = String.format(Locale.ENGLISH,"%1$,.1f", reasonerProgressStatus.getCorrectlyProccessedTime()/(double)1000);
			} else {
				correctlyProccessedTime = String.format(Locale.ENGLISH,"%1$,.1f", reasonerProgressStatus.getCorrectlyProccessedTime()/(double)1000);
			}
			correctlyProccessedTimeString = "<span class=\"time-success\">"+correctlyProccessedTime+"</span> s";
		}
		if (condensedLevel < 3) {
			sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),rankString,AlignType.ALIGN_CENTER));
		} else {
			nextColumn++;
		}
		sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),reasonerName,AlignType.ALIGN_LEFT));
		sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),createCompetitionReasonerProgressString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem),AlignType.ALIGN_LEFT));
		sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),createCompetitionReasonerScoreString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem),AlignType.ALIGN_RIGHT));
		sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),createCompetitionReasonerErrorString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem),AlignType.ALIGN_RIGHT));
		sb.append(createColumnString(getIDString(competitionID,reasonerID,nextColumn++),correctlyProccessedTimeString,AlignType.ALIGN_RIGHT));
		sb.append("</tr>");
		rowString = sb.toString();
		return rowString;
	}	
	
	
	


	protected String createCompetitionReasonerProgressCommandString(String idString, int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem, LocalUpdateContext updateContext) {
		boolean onlyUpdateWidth = true;
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			if (reasonerProgressStatus.getTotalProcessedCount()+reasonerProgressStatus.getOutOfTimeCount() >= compStatusItem.getCompetitionStatus().getQueryCount()) {		
				onlyUpdateWidth = false;
			} else {
				CompetitionReasonerProgressStatusUpdateItem lastCompetitionReasonerProgressStatusUpdateItem = updateContext.mLastCompetitionReasonerStatusItemMap.get(compStatusItem.getCompetitionStatus().getCompetitionSourceString()+":"+reasonerProgressStatus.getReasonerSourceString());
				if (lastCompetitionReasonerProgressStatusUpdateItem == null) {
					onlyUpdateWidth = false;
				} else {
					if (lastCompetitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getTotalProcessedCount() <= 0) {
						onlyUpdateWidth = false;
					} else if (lastCompetitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus().getReasonerPosition() != reasonerProgressStatus.getReasonerPosition()) {
						onlyUpdateWidth = false;
					}
				}
			}
		} else {
			onlyUpdateWidth = false;
		}
		
		if (onlyUpdateWidth) {
			double correctlyProcessedPercent = 0;		
			double incompletelyProcessedPercent = 0;
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			correctlyProcessedPercent = reasonerProgressStatus.getCorrectlyProcessedCount()*100./compStatusItem.getCompetitionStatus().getQueryCount();
			incompletelyProcessedPercent = (reasonerProgressStatus.getTotalProcessedCount()-reasonerProgressStatus.getCorrectlyProcessedCount())*100./compStatusItem.getCompetitionStatus().getQueryCount();
			return getUpdateStyleWidthCommand(getIDString(competitionID,reasonerID,"sb"),String.valueOf(correctlyProcessedPercent)+"%")+getUpdateStyleWidthCommand(getIDString(competitionID,reasonerID,"fb"),String.valueOf(incompletelyProcessedPercent)+"%");
		} else {
			return getUpdateInnerHTMLCommand(idString,createCompetitionReasonerProgressString(competitionID,reasonerID,compStatusItem,competitionReasonerProgressStatusUpdateItem));
		}
	}		
	

	protected String createCompetitionReasonerProgressString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem) {
		double correctlyProcessedPercent = 0;		
		double incompletelyProcessedPercent = 0;
		double outOfTimePercent = 0;
		String progressBarString0 = "<div class=\"progress progress-striped active\">";
		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			correctlyProcessedPercent = reasonerProgressStatus.getCorrectlyProcessedCount()*100./compStatusItem.getCompetitionStatus().getQueryCount();
			incompletelyProcessedPercent = (reasonerProgressStatus.getTotalProcessedCount()-reasonerProgressStatus.getCorrectlyProcessedCount())*100./compStatusItem.getCompetitionStatus().getQueryCount();
			outOfTimePercent = reasonerProgressStatus.getOutOfTimeCount()*100./compStatusItem.getCompetitionStatus().getQueryCount();
			if (reasonerProgressStatus.getTotalProcessedCount()+reasonerProgressStatus.getOutOfTimeCount() >= compStatusItem.getCompetitionStatus().getQueryCount()) {		
				progressBarString0 = "<div class=\"progress\">";
			}
		}
		
		String progressBarString1 = "<div id=\""+getIDString(competitionID,reasonerID,"sb")+"\" class=\"progress-bar progress-bar-success\" style=\"width:"+correctlyProcessedPercent+"%\"></div>";
		String progressBarString2 = "<div id=\""+getIDString(competitionID,reasonerID,"fb")+"\" class=\"progress-bar progress-bar-danger\" style=\"width:"+ incompletelyProcessedPercent+"%\"></div>";
		String progressBarString3 = "";
		if (outOfTimePercent > 0) {
			progressBarString3 = "<div id=\""+getIDString(competitionID,reasonerID,"ob")+"\" class=\"progress-bar progress-bar-black\" style=\"width:"+ outOfTimePercent+"%\"></div></div>";
		}
		return progressBarString0+progressBarString1+progressBarString2+progressBarString3+"</div>";
	}	
	

	protected String createCompetitionReasonerScoreString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem) {
		int correctlyProcessedCount = 0;		
		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			correctlyProcessedCount = reasonerProgressStatus.getCorrectlyProcessedCount();
		}
		
		return "<span class=\"score-success\" id=\""+getIDString(competitionID,reasonerID,"ss")+"\">"+correctlyProcessedCount+"</span> / "+compStatusItem.getCompetitionStatus().getQueryCount();
	}		
	
	
	


	protected String createCompetitionReasonerErrorString(int competitionID, int reasonerID, CompetitionStatusUpdateItem compStatusItem, CompetitionReasonerProgressStatusUpdateItem competitionReasonerProgressStatusUpdateItem) {
		int incompletelyProcessedCount = 0;
		
		if (competitionReasonerProgressStatusUpdateItem != null) {
			CompetitionReasonerProgressStatus reasonerProgressStatus = competitionReasonerProgressStatusUpdateItem.getCompetitionReasonerProgressStatus();
			incompletelyProcessedCount = (reasonerProgressStatus.getTotalProcessedCount()-reasonerProgressStatus.getCorrectlyProcessedCount());
		}
		
		return "<span class=\"score-error\" id=\""+getIDString(competitionID,reasonerID,"se")+"\">"+incompletelyProcessedCount+"</span>";
	}		
	
	
	protected String createColumnString(String idString, String columnText, AlignType alignType) {
		String alignString = "";
		if (alignType == AlignType.ALIGN_RIGHT) {
			alignString = " align=\"right\" ";
		} else if (alignType == AlignType.ALIGN_CENTER) {
			alignString = " align=\"center\" ";
		} else if (alignType == AlignType.ALIGN_LEFT) {
			alignString = " align=\"left\" ";
		}
		String columnString = "<td id=\""+idString+"\""+alignString+">"+columnText+"</td>";
		return columnString;
	}
	
	
}
