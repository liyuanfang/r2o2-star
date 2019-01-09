package org.semanticweb.ore.configuration;

public enum ConfigType {
	
	
	CONFIG_TYPE_EXECUTION_TIMEOUT("org.semanticweb.ore.execution.executiontimeout"),
	CONFIG_TYPE_PROCESSING_TIMEOUT("org.semanticweb.ore.execution.processingtimeout"),
	CONFIG_TYPE_BASE_DIRECTORY("org.semanticweb.ore.directory.base"),
	CONFIG_TYPE_QUERIES_DIRECTORY("org.semanticweb.ore.directory.queries"),
	CONFIG_TYPE_RESPONSES_DIRECTORY("org.semanticweb.ore.directory.responses"),
	CONFIG_TYPE_CONVERSIONS_DIRECTORY("org.semanticweb.ore.directory.conversions"),
	CONFIG_TYPE_EXPECTATIONS_DIRECTORY("org.semanticweb.ore.directory.expectations"),
	CONFIG_TYPE_ONTOLOGIES_DIRECTORY("org.semanticweb.ore.directory.ontologies"),
	CONFIG_TYPE_COMPETITIONS_DIRECTORY("org.semanticweb.ore.directory.competitions"),
	CONFIG_TYPE_TEMPLATES_DIRECTORY("org.semanticweb.ore.directory.templates"),

	CONFIG_TYPE_EXECUTION_MEMORY_LIMIT("org.semanticweb.ore.execution.memorylimit"),

	CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY("org.semanticweb.ore.web.competitionstatusroot"),

	CONFIG_TYPE_RESPONSES_DATE_SUB_DIRECTORY("org.semanticweb.ore.responses.datesubdirectory"),
	CONFIG_TYPE_WRITE_NORMALISED_RESULTS("org.semanticweb.ore.responses.writenormalisedresults"),
	CONFIG_TYPE_SAVE_LOAD_RESULT_HASH_CODES("org.semanticweb.ore.responses.saveloadresulthashcodes"),
	CONFIG_TYPE_SAVE_LOAD_RESULTS_CODES("org.semanticweb.ore.responses.saveloadresults"),
	
	
	CONFIG_TYPE_COMPETITION_EXECUTOR_PER_REASONER("org.semanticweb.ore.competition.executorperreasoner"),
	CONFIG_TYPE_COMPETITION_CONTINUE_EXECUTOR_LOSS("org.semanticweb.ore.competition.continueexecutorloss"),
	CONFIG_TYPE_COMPETITION_INFINITE_REPEAT("org.semanticweb.ore.competition.infiniterepeat"),
	
	
	
	CONFIG_TYPE_EXECUTION_ADD_TIMEOUT_AS_ARGUMENT("org.semanticweb.ore.execution.addtimeoutasargument"),
	CONFIG_TYPE_EXECUTION_ADD_MEMORY_LIMIT_AS_ARGUMENT("org.semanticweb.ore.execution.addmemorylimitasargument"),

	
	
	CONFIG_TYPE_EXECUTION_ONTOLOGY_CONVERTION("org.semanticweb.ore.execution.ontologyconversion"),

	
	
	CONFIG_TYPE_NETWORKING_TERMINATE_AFTER_EXECUTION("org.semanticweb.ore.networking.terminateafterexecution"),
	CONFIG_TYPE_NETWORKING_COMPETITION_SERVER_PORT("org.semanticweb.ore.networking.competitionserverport"),
	CONFIG_TYPE_NETWORKING_STATUS_UPDATE_SERVER_PORT("org.semanticweb.ore.networking.statusupdateserverport"),
	CONFIG_TYPE_NETWORKING_WEB_SERVER_PORT("org.semanticweb.ore.networking.webserverport");
	
	
	private String mConfigTypeString = null;
	
	private ConfigType(String typeString) {
		mConfigTypeString = typeString;
	}
	
	public String getConfigTypeString() {
		return mConfigTypeString;
	}
	

}
