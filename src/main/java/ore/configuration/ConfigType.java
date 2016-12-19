package ore.configuration;

public enum ConfigType {
	
	
	CONFIG_TYPE_EXECUTION_TIMEOUT("ore.execution.executiontimeout"),
	CONFIG_TYPE_PROCESSING_TIMEOUT("ore.execution.processingtimeout"),
	CONFIG_TYPE_BASE_DIRECTORY("ore.directory.base"),
	CONFIG_TYPE_QUERIES_DIRECTORY("ore.directory.queries"),
	CONFIG_TYPE_RESPONSES_DIRECTORY("ore.directory.responses"),
	CONFIG_TYPE_CONVERSIONS_DIRECTORY("ore.directory.conversions"),
	CONFIG_TYPE_EXPECTATIONS_DIRECTORY("ore.directory.expectations"),
	CONFIG_TYPE_ONTOLOGIES_DIRECTORY("ore.directory.ontologies"),
	CONFIG_TYPE_COMPETITIONS_DIRECTORY("ore.directory.competitions"),
	CONFIG_TYPE_TEMPLATES_DIRECTORY("ore.directory.templates"),

	CONFIG_TYPE_EXECUTION_MEMORY_LIMIT("ore.execution.memorylimit"),

	CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY("ore.web.competitionstatusroot"),

	CONFIG_TYPE_RESPONSES_DATE_SUB_DIRECTORY("ore.responses.datesubdirectory"),
	CONFIG_TYPE_WRITE_NORMALISED_RESULTS("ore.responses.writenormalisedresults"),
	CONFIG_TYPE_SAVE_LOAD_RESULT_HASH_CODES("ore.responses.saveloadresulthashcodes"),
	CONFIG_TYPE_SAVE_LOAD_RESULTS_CODES("ore.responses.saveloadresults"),
	
	
	CONFIG_TYPE_COMPETITION_EXECUTOR_PER_REASONER("ore.competition.executorperreasoner"),
	CONFIG_TYPE_COMPETITION_CONTINUE_EXECUTOR_LOSS("ore.competition.continueexecutorloss"),
	CONFIG_TYPE_COMPETITION_INFINITE_REPEAT("ore.competition.infiniterepeat"),
	
	
	
	CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT("ore.execution.addtimeoutasargument"),
	CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT("ore.execution.addmemorylimitasargument"),

	
	
	CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION("ore.execution.ontologyconversion"),

	
	
	CONFIG_TYPE_NETWORKING_TERMINATE_AFTER_EXECUTION("ore.networking.terminateafterexecution"),
	CONFIG_TYPE_NETWORKING_COMPETITION_SERVER_PORT("ore.networking.competitionserverport"),
	CONFIG_TYPE_NETWORKING_STATUS_UPDATE_SERVER_PORT("ore.networking.statusupdateserverport"),
	CONFIG_TYPE_NETWORKING_WEB_SERVER_PORT("ore.networking.webserverport");
	
	
	private String mConfigTypeString = null;
	
	private ConfigType(String typeString) {
		mConfigTypeString = typeString;
	}
	
	public String getConfigTypeString() {
		return mConfigTypeString;
	}
	

}
