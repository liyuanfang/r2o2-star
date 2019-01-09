package org.semanticweb.ore.networking;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.networking.events.DelayExecutionTaskProviderEvent;
import org.semanticweb.ore.networking.events.NewSocketConnectionEvent;
import org.semanticweb.ore.networking.events.RemoveExecutionHandlerEvent;
import org.semanticweb.ore.networking.events.RequestExecutionTaskProviderEvent;
import org.semanticweb.ore.networking.events.ScheduleExecutionTaskProviderEvent;
import org.semanticweb.ore.threading.Event;
import org.semanticweb.ore.threading.EventThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ServerExecutionManager extends EventThread implements ExecutionTaskScheduler {
	
	final private static Logger mLogger = LoggerFactory.getLogger(ServerExecutionManager.class);
	
	protected ConnectionHandler mConnectionHandler = null;
	protected boolean mCommunicationError = false;
	protected int mListenPort = 0;
	protected ServerSocket mListenSocket = null;
	protected Config mConfig = null;
	protected int mClientNumber = 0;
	
	protected ArrayList<ExecutionTaskHandlerItem> mHandlerSet = new ArrayList<ExecutionTaskHandlerItem>();
	protected HashMap<ExecutionTaskHandler,ExecutionTaskHandlerItem> mHandlerItemMap = new HashMap<ExecutionTaskHandler,ExecutionTaskHandlerItem>();
	
	protected HashSet<ExecutionTaskHandlerItem> mScheduledHandlerSet = new HashSet<ExecutionTaskHandlerItem>();
	protected HashSet<ExecutionTaskHandlerItem> mAvailableHandlerSet = new HashSet<ExecutionTaskHandlerItem>();
	
	
	
	protected ArrayList<ExecutionTaskProviderItem> mProviderList = new ArrayList<ExecutionTaskProviderItem>();
	protected HashMap<ExecutionTaskProvider,ExecutionTaskProviderItem> mProviderItemMap = new HashMap<ExecutionTaskProvider,ExecutionTaskProviderItem>();

	
	protected HashSet<ExecutionTaskProviderItem> mProcessingProviderSet = new HashSet<ExecutionTaskProviderItem>();
	protected HashSet<ExecutionTaskProviderItem> mPendingProviderSet = new HashSet<ExecutionTaskProviderItem>();
	protected HashSet<ExecutionTaskProviderItem> mDelayedProviderSet = new HashSet<ExecutionTaskProviderItem>();
	protected ArrayList<ExecutionTaskProviderItem> mPendingProviderList = new ArrayList<ExecutionTaskProviderItem>();
	protected ArrayList<ExecutionTaskProviderItem> mProcessedProviderList = new ArrayList<ExecutionTaskProviderItem>();
	

	protected ArrayList<ExecutionTaskProviderItem> mPreparingProviderList = new ArrayList<ExecutionTaskProviderItem>();
	protected HashSet<ExecutionTaskProviderItem> mCurrentPreparingProviderSet = new HashSet<ExecutionTaskProviderItem>();
	protected boolean mProviderPreparing = true;
	protected long mProviderPreparingTime = 30000;
	
	
	
	protected enum ExecutionTaskHandlerSchedulingState {
		STATE_SCHEDULED,
		STATE_AVAILABLE;
	}

	protected class ExecutionTaskHandlerItem {
		protected ExecutionTaskHandler mExecutionHandler = null;
		protected ExecutionTaskProviderItem mAssociatedExecutionProvider = null;		
		protected ExecutionTaskHandlerSchedulingState mState = ExecutionTaskHandlerSchedulingState.STATE_AVAILABLE;
		
		protected String mAddress = null;
		protected int mClientNumber = 0;
		
		public ExecutionTaskHandlerItem(ExecutionTaskHandler executionHandler, int number, String address) {
			mExecutionHandler = executionHandler;	
			mClientNumber = number;
			mAddress = address;
		}
		
		public ExecutionTaskHandler getExecutionHandler() {
			return mExecutionHandler;
		}
		
		public void setExecutionTaskProvider(ExecutionTaskProviderItem associatedExecutionProvider) {
			mAssociatedExecutionProvider = associatedExecutionProvider;
		}
		
		public ExecutionTaskProviderItem getAssociatedExecutionProvider() {
			return mAssociatedExecutionProvider;
		}
		
		public ExecutionTaskHandlerSchedulingState getState() {
			return mState;
		}
		
		public void setState(ExecutionTaskHandlerSchedulingState state) {
			mState = state;
		}
		
		public String toString() {
			return "Client-"+mClientNumber+mAddress;
		}
	}
	
	
	
	
	
	
	protected enum ExecutionTaskProviderSchedulingState {
		STATE_PENDING,
		STATE_PROCESSING,		
		STATE_PROCESSED;
	}

	protected class ExecutionTaskProviderItem {
		protected ExecutionTaskProvider mExecutionProvider = null;
		protected ExecutionTaskProviderSchedulingState mState = ExecutionTaskProviderSchedulingState.STATE_PENDING;	
		protected ProcessingRequirements mProcessingRequirement = null;
		
		protected HashSet<ExecutionTaskHandlerItem> mAssociatedHandlerSet = new HashSet<ExecutionTaskHandlerItem>();
		
		public ExecutionTaskProviderItem(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirement) {
			mExecutionProvider = executionProvider;
			mProcessingRequirement = processingRequirement;
		}
		
		public ExecutionTaskProvider getProvider() {
			return mExecutionProvider;
		}
		
		public ProcessingRequirements getProcessingRequirement() {
			return mProcessingRequirement;
		}
		
		public void addExecutionTaskHandler(ExecutionTaskHandlerItem handler) {
			mAssociatedHandlerSet.add(handler);
		}
		
		public void removeExecutionTaskHandler(ExecutionTaskHandlerItem handler) {
			mAssociatedHandlerSet.remove(handler);
		}
		
		public ExecutionTaskProviderSchedulingState getState() {
			return mState;
		}
		
		public void setState(ExecutionTaskProviderSchedulingState state) {
			mState = state;
		}
		
		public boolean canAddTaskExecutionHandler() {
			return mProcessingRequirement.canAddExecutionTaskHandler(mAssociatedHandlerSet.size()+1);
		}
		
		public boolean canInitProcessing(int taskExecutionHandlerCount) {
			return mProcessingRequirement.canInitProcessing(taskExecutionHandlerCount);
		}

		public boolean hasAssociatedTaskExecutionHandlers() {
			return mAssociatedHandlerSet.size() > 0;
		}
		
		public boolean canProvideMoreExecutionTask() {
			return mProcessingRequirement.canProvideMoreExecutionTask();
		}
		
		public String toString() {
			return mExecutionProvider.getProviderName();
		}
		
	}	
	
	
	
	
	
	
	
	
	
		
	public ServerExecutionManager(int port, Config config) {		
		mConfig = config;
		mListenPort = port;
		startThread();
	}	
	
	
	protected void threadStart() {
		super.threadStart();
		
		try {			
			mListenSocket = new ServerSocket(mListenPort);
			mLogger.info("Created server socket to listen on port {}",mListenPort);
		} catch (IOException e) {	
			mCommunicationError = true;
			mLogger.error("Failed to create server socket to listen on port '{}', got IOException '{}'.",mListenPort,e.getMessage());
		}
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {		
				
				while (!mCommunicationError) {
					try {
						Socket socket = mListenSocket.accept();
						postEvent(new NewSocketConnectionEvent(socket));
					} catch (IOException e) {	
						mCommunicationError = true;
						mLogger.error("Failed to create server socket to listen on port '{}', got IOException '{}'.",mListenPort,e.getMessage());
					}
					
				}
				
			}
			
		}).start();
	}	

	protected void threadFinish() {	
		super.threadFinish();
	}	
	
	
	
	
	
	protected boolean processEvent(Event e) {
		if (super.processEvent(e)) {
			return true;
		} else {
			if (e instanceof NewSocketConnectionEvent) {
				NewSocketConnectionEvent nsce = (NewSocketConnectionEvent)e;
				Socket socket = nsce.getSocket();			
				String remoteAddressString = socket.getRemoteSocketAddress().toString();
				ExecutionTaskClientHandler clientHandler = new ExecutionTaskClientHandler(socket,this,mConfig);
				ExecutionTaskHandlerItem handlerItem = new ExecutionTaskHandlerItem(clientHandler,mClientNumber,remoteAddressString);
				mLogger.info("New network client connected from '{}', identifying client as '{}'.",remoteAddressString,handlerItem.toString());
				mHandlerSet.add(handlerItem);
				mHandlerItemMap.put(clientHandler,handlerItem);
				mAvailableHandlerSet.add(handlerItem);
				++mClientNumber;
				scheduleExecution();
				return true;
			} else if (e instanceof ScheduleExecutionTaskProviderEvent) {
				ScheduleExecutionTaskProviderEvent setpe = (ScheduleExecutionTaskProviderEvent)e;
				final ExecutionTaskProvider executionProvider = setpe.getExecutionTaskProvider();
				final ProcessingRequirements processingRequirements = setpe.getProcessingRequirements();
				ExecutionTaskProviderItem providerItem = new ExecutionTaskProviderItem(executionProvider,processingRequirements);
				mProviderItemMap.put(executionProvider,providerItem);
				

				boolean delayedExecution = false;
				DateTime startingDate = processingRequirements.getDesiredStartingDate();
				if (startingDate != null) {
					DateTime currentDate = new DateTime();
					if (currentDate.isBefore(startingDate)) {
						long millisecondsUntilStart = startingDate.getMillis()-currentDate.getMillis();
						if (mProviderPreparing) {
							millisecondsUntilStart = millisecondsUntilStart-mProviderPreparingTime;
						}
						final long finalMillisecondsUntilStart = millisecondsUntilStart;
						if (millisecondsUntilStart > 0) {
							mDelayedProviderSet.add(providerItem);
							delayedExecution = true;
							
							mLogger.info("Delaying processing of competition '{}' for {} milliseconds.",executionProvider.getProviderName(),finalMillisecondsUntilStart);

							
							new Thread(new Runnable() {		
								
								@Override
								public void run() {
									try {
										Thread.sleep(finalMillisecondsUntilStart);
										postEvent(new DelayExecutionTaskProviderEvent(executionProvider,processingRequirements,mProviderPreparing));
									} catch (Exception e) {										
									}
								}
									

							}).start();
						}
					}
				}
				
				if (!delayedExecution) {
					if (mProviderPreparing) { 
						mLogger.info("Preparing processing of competition '{}'.",executionProvider.getProviderName());
						mPreparingProviderList.add(providerItem);
					} else {
						mLogger.info("Sheduling processing of competition '{}'.",executionProvider.getProviderName());
						mPendingProviderSet.add(providerItem);
						mPendingProviderList.add(providerItem);
					}
					scheduleExecution();
				}
				return true;
			} else if (e instanceof DelayExecutionTaskProviderEvent) {
				DelayExecutionTaskProviderEvent detpe = (DelayExecutionTaskProviderEvent)e;
				ExecutionTaskProvider executionProvider = detpe.getExecutionTaskProvider();				
				ExecutionTaskProviderItem providerItem = mProviderItemMap.get(executionProvider);
				
				mDelayedProviderSet.remove(providerItem);
				boolean preparing = detpe.getPreparing();
				if (preparing) {					
					mLogger.info("Preparing processing of competition '{}'.",executionProvider.getProviderName());
					mPreparingProviderList.add(providerItem);
				} else {
					mLogger.info("Sheduling processing of competition '{}'.",executionProvider.getProviderName());
					mPendingProviderSet.add(providerItem);
					mPendingProviderList.add(providerItem);
					mCurrentPreparingProviderSet.remove(providerItem);
				}
				scheduleExecution();
				
			} else if (e instanceof RequestExecutionTaskProviderEvent) {
				RequestExecutionTaskProviderEvent retpe = (RequestExecutionTaskProviderEvent)e;
				ExecutionTaskHandler executionHandler = retpe.getExecutionTaskHandler();
				ExecutionTaskHandlerItem handlerItem = mHandlerItemMap.get(executionHandler);
				if (handlerItem != null) {					
					changeHandlerItemState(handlerItem, ExecutionTaskHandlerSchedulingState.STATE_AVAILABLE);
					deassociateHandlerFromProvider(handlerItem);
				}
				scheduleExecution();
				return true;
			} else if (e instanceof RemoveExecutionHandlerEvent) {
				RemoveExecutionHandlerEvent csce = (RemoveExecutionHandlerEvent)e;
				ExecutionTaskHandler handler = csce.getExecutionTaskHandler();
				ExecutionTaskHandlerItem handlerItem = mHandlerItemMap.get(handler);
				deassociateHandlerFromProvider(handlerItem);
				mHandlerSet.remove(handlerItem);
				mHandlerItemMap.remove(handler);
				mAvailableHandlerSet.remove(handlerItem);
				scheduleExecution();
				return true;
			}
		}
		return false;
	}


	
	public void scheduleExecution() {
		
		List<ExecutionTaskHandlerItem> delayedHanlderAssociationList = new ArrayList<ExecutionTaskHandlerItem>();
		
		Iterator<ExecutionTaskProviderItem> processingProviderIt = mProcessingProviderSet.iterator();
		while (processingProviderIt.hasNext()) {
			ExecutionTaskProviderItem providerItem = processingProviderIt.next();
			if (!providerItem.hasAssociatedTaskExecutionHandlers()) {
				if (providerItem.canProvideMoreExecutionTask()) {
					changeProviderItemState(providerItem, ExecutionTaskProviderSchedulingState.STATE_PENDING);
				} else {
					changeProviderItemState(providerItem, ExecutionTaskProviderSchedulingState.STATE_PROCESSED);
				}
				processingProviderIt = mProcessingProviderSet.iterator();
			}
		}		
		
		if (mCurrentPreparingProviderSet.isEmpty() || !mProviderPreparing) {
			Iterator<ExecutionTaskProviderItem> processingProviderAddHandlerIt = mProcessingProviderSet.iterator();
			while (processingProviderAddHandlerIt.hasNext() && !mAvailableHandlerSet.isEmpty()) {
				ExecutionTaskProviderItem providerItem = processingProviderAddHandlerIt.next();
				if (providerItem.canAddTaskExecutionHandler()) {
					ExecutionTaskHandlerItem handlerItem = mAvailableHandlerSet.iterator().next();				
					associateHandlerToProvider(handlerItem,providerItem,delayedHanlderAssociationList);
				}
			}
		}
		
		
		if (mCurrentPreparingProviderSet.isEmpty() || !mProviderPreparing) {
			boolean continueSchedulingNewProviderItems = !mAvailableHandlerSet.isEmpty();
			Iterator<ExecutionTaskProviderItem> pendingProviderIt = mPendingProviderList.iterator();
			while (pendingProviderIt.hasNext() && continueSchedulingNewProviderItems) {
				ExecutionTaskProviderItem providerItem = pendingProviderIt.next();			
				int availableHandlerCount = mAvailableHandlerSet.size();
				
				if (providerItem.canInitProcessing(availableHandlerCount)) {
					while (providerItem.canAddTaskExecutionHandler() && !mAvailableHandlerSet.isEmpty()) {
						ExecutionTaskHandlerItem handlerItem = mAvailableHandlerSet.iterator().next();
						associateHandlerToProvider(handlerItem,providerItem,delayedHanlderAssociationList);
					}
					pendingProviderIt.remove();
				} else {
					continueSchedulingNewProviderItems = false;
				}			
			}
		} 
		if (!mPreparingProviderList.isEmpty() && mCurrentPreparingProviderSet.isEmpty() && mPendingProviderList.isEmpty()) {	
			int availableHandlerCount = mAvailableHandlerSet.size();
			Iterator<ExecutionTaskProviderItem> preparingProviderIt = mPreparingProviderList.iterator();
			if (preparingProviderIt.hasNext()) {
				final ExecutionTaskProviderItem providerItem = preparingProviderIt.next();				
				if (providerItem.canInitProcessing(availableHandlerCount)) {	
					preparingProviderIt.remove();
					mCurrentPreparingProviderSet.add(providerItem);
					
					DateTime plannedExecutionTime = new DateTime(DateTimeZone.UTC);
					providerItem.getProvider().postExecutionPreparation(plannedExecutionTime.plus(mProviderPreparingTime));
					
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(mProviderPreparingTime);
								postEvent(new DelayExecutionTaskProviderEvent(providerItem.getProvider(),providerItem.getProcessingRequirement(),false));
							} catch (Exception e) {
							}
						}		
					}).start();

				}
			}
		}
		
		for (ExecutionTaskHandlerItem handlerItem : delayedHanlderAssociationList) {			
			handlerItem.getExecutionHandler().postAssociatedExecutionTaskProvider(handlerItem.getAssociatedExecutionProvider().getProvider());
		}
		
	}
	
	
	
	protected void associateHandlerToProvider(ExecutionTaskHandlerItem handlerItem, ExecutionTaskProviderItem providerItem, List<ExecutionTaskHandlerItem> delayedHanlderAssociationList) {
		providerItem.addExecutionTaskHandler(handlerItem);
		handlerItem.setExecutionTaskProvider(providerItem);
		changeHandlerItemState(handlerItem, ExecutionTaskHandlerSchedulingState.STATE_SCHEDULED);
		changeProviderItemState(providerItem, ExecutionTaskProviderSchedulingState.STATE_PROCESSING);		
		providerItem.getProvider().postExecutionTaskHandlerAddition(handlerItem.getExecutionHandler());
		mLogger.info("Associate execution handler '{}' to  competition '{}'.",handlerItem,providerItem);
		if (delayedHanlderAssociationList != null) {
			delayedHanlderAssociationList.add(handlerItem);
		} else {
			handlerItem.getExecutionHandler().postAssociatedExecutionTaskProvider(providerItem.getProvider());
		}
	}
	
	
	protected void deassociateHandlerFromProvider(ExecutionTaskHandlerItem handlerItem) {
		ExecutionTaskProviderItem providerItem = handlerItem.getAssociatedExecutionProvider();
		if (providerItem != null) {
			deassociateHandlerFromProvider(handlerItem,providerItem);
			providerItem.getProvider().postExecutionTaskHandlerRemovement(handlerItem.getExecutionHandler());
			mLogger.info("Deassociate execution handler '{}' from competition '{}'.",handlerItem,providerItem);
		}
	}

	protected void deassociateHandlerFromProvider(ExecutionTaskHandlerItem handlerItem, ExecutionTaskProviderItem providerItem) {
		providerItem.removeExecutionTaskHandler(handlerItem);
		handlerItem.setExecutionTaskProvider(null);
		changeHandlerItemState(handlerItem, ExecutionTaskHandlerSchedulingState.STATE_AVAILABLE);
	}


	@Override
	public void postProviderRequest(ExecutionTaskHandler executionHandler) {
		postEvent(new RequestExecutionTaskProviderEvent(executionHandler));
	}


	@Override
	public void postSchedulingRequest(ExecutionTaskProvider executionProvider, ProcessingRequirements processingRequirements) {
		postEvent(new ScheduleExecutionTaskProviderEvent(executionProvider,processingRequirements));
	}



	
	
	protected void changeProviderItemState(ExecutionTaskProviderItem providerItem, ExecutionTaskProviderSchedulingState state) {
		ExecutionTaskProviderSchedulingState prevState = providerItem.getState();
		if (prevState != state) {
			if (prevState == ExecutionTaskProviderSchedulingState.STATE_PENDING) {	
				mPendingProviderSet.remove(providerItem);
			} else if (prevState == ExecutionTaskProviderSchedulingState.STATE_PROCESSING) {	
				mProcessingProviderSet.remove(providerItem);
			}
			
			if (state == ExecutionTaskProviderSchedulingState.STATE_PENDING) {	
				mPendingProviderSet.add(providerItem);
				mPendingProviderList.add(0, providerItem);
			} else if (state == ExecutionTaskProviderSchedulingState.STATE_PROCESSING) {	
				mProcessingProviderSet.add(providerItem);				
			} else if (state == ExecutionTaskProviderSchedulingState.STATE_PROCESSED) {	
				mProcessedProviderList.add(providerItem);
			}
			providerItem.setState(state);
		}
	}
	
	
	
	protected void changeHandlerItemState(ExecutionTaskHandlerItem handlerItem, ExecutionTaskHandlerSchedulingState state) {
		ExecutionTaskHandlerSchedulingState prevState = handlerItem.getState();
		if (prevState != state) {			
			if (prevState == ExecutionTaskHandlerSchedulingState.STATE_SCHEDULED) {	
				mScheduledHandlerSet.remove(handlerItem);
			} else if (prevState == ExecutionTaskHandlerSchedulingState.STATE_AVAILABLE) {	
				mAvailableHandlerSet.remove(handlerItem);
			}
			
			if (state == ExecutionTaskHandlerSchedulingState.STATE_AVAILABLE) {	
				mAvailableHandlerSet.add(handlerItem);
			} else if (state == ExecutionTaskHandlerSchedulingState.STATE_SCHEDULED) {	
				mScheduledHandlerSet.add(handlerItem);				
			}
			handlerItem.setState(state);
		}
	}


	@Override
	public void postRemoveHandler(ExecutionTaskHandler executionHandler) {
		postEvent(new RemoveExecutionHandlerEvent(executionHandler));
	}
	
}
