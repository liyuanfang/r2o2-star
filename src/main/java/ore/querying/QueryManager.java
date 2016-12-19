package ore.querying;

import ore.configuration.Config;
import ore.configuration.ConfigDataValueReader;
import ore.configuration.ConfigType;
import ore.parsing.QueryTSVParser;
import ore.utilities.FilePathString;
import ore.utilities.FileSystemHandler;
import ore.utilities.RelativeFilePathStringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class QueryManager {
	
	final private static Logger mLogger = LoggerFactory.getLogger(QueryManager.class);
	
	protected Config mConfig = null;
	protected HashMap<String,Query> mStringQueryMap = new HashMap<String,Query>();
	protected Collection<Query> mDefaultLoadQueries = null;
	
	public QueryManager(Config config) {
		mConfig = config;
	}
	

	public List<Query> loadFilterSortDefaultQueries(String queryFilePathFilterString, FilePathString sortingQueryFilePathString) {
		Collection<Query> queries = loadDefaultQueries();
		if (queryFilePathFilterString != null) {
			queries = filterQueries(queries, queryFilePathFilterString);
		}
		return sortQueries(queries, sortingQueryFilePathString);
	}
	
	
	public List<Query> loadFilterSortQueries(String absoluteQueryFileBaseString, String queryFilePathFilterString, FilePathString sortingQueryFilePathString) {
		Collection<Query> queries = loadQueries(absoluteQueryFileBaseString);
		if (queryFilePathFilterString != null) {
			queries = filterQueries(queries, queryFilePathFilterString);
		}
		return sortQueries(queries, sortingQueryFilePathString);
	}	

	
	public Collection<Query> loadDefaultQueries() {
		if (mDefaultLoadQueries == null) {
			String queriesString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
			mDefaultLoadQueries = loadQueries(queriesString);
		}
		return mDefaultLoadQueries;
	}

	public Collection<Query> loadQueries(String absoluteQueryFileBaseString) {
		HashSet<Query> querySet = new HashSet<Query>();
		
		Collection<String> queryFileStringCollection = FileSystemHandler.collectRelativeFilesInSubdirectories(absoluteQueryFileBaseString);
		
		for (String relativeQueryFileString : queryFileStringCollection) {
			
			String completeQueryFileString = absoluteQueryFileBaseString + relativeQueryFileString;
			
			Query query = mStringQueryMap.get(completeQueryFileString);
			
			
			if (query == null) {
				FilePathString queryFileString = new FilePathString(absoluteQueryFileBaseString,relativeQueryFileString,RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
				
				DefaultQueryFactory queryFactory = new DefaultQueryFactory(); 
				QueryTSVParser queryTSVParser = new QueryTSVParser(queryFactory, mConfig, queryFileString);
				
				try {
					query = queryTSVParser.parseQuery(completeQueryFileString);
					mStringQueryMap.put(completeQueryFileString, query);
				} catch (IOException e) {
					mLogger.error("Loading query from '{}' failed, got IOException '{}'.",completeQueryFileString,e.getMessage());
				}
			}
			
			if (query != null) {
				querySet.add(query);
			}
		}
		return querySet;
	}
	
	
	public Query loadQuery(String queryFileString) {
		Query query = null;
		String queriesBaseString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_QUERIES_DIRECTORY);
		FilePathString queryFilePathString = new FilePathString(queriesBaseString,queryFileString,RelativeFilePathStringType.RELATIVE_TO_QUERIES_DIRECTORY);
		query = loadQuery(queryFilePathString);
		if (query == null) {
			queryFilePathString = new FilePathString(queryFileString);
			query = loadQuery(queryFilePathString);
		}
		return query;
	}
	
	
	public Query loadQuery(FilePathString queryFilePathString) {

		String absoluteQueryFileString = queryFilePathString.getAbsoluteFilePathString();
		Query query = mStringQueryMap.get(absoluteQueryFileString);
				
		DefaultQueryFactory queryFactory = new DefaultQueryFactory(); 
		QueryTSVParser queryTSVParser = new QueryTSVParser(queryFactory, mConfig, queryFilePathString);
		
		try {
			query = queryTSVParser.parseQuery(absoluteQueryFileString);
			mStringQueryMap.put(absoluteQueryFileString, query);
		} catch (IOException e) {
			mLogger.error("Loading query from '{}' failed, got IOException '{}'.",absoluteQueryFileString,e.getMessage());
		}		
		return query;
	}
	
	
	public Collection<Query> filterQueries(Collection<Query> queryCollection, String queryFilePathFilterString) {
		if (queryFilePathFilterString != null) {
			HashSet<Query> filteredQuerySet = new HashSet<Query>();
			for (Query query : queryCollection) {
				String queryPathString = query.getQuerySourceString().getAbsoluteFilePathString().replace("\\", "/");
				if (queryPathString.contains(queryFilePathFilterString)) {
					filteredQuerySet.add(query);
				}
			}
			return filteredQuerySet;
		} else {
			return queryCollection;
		}		
	}
	
	
	
	

	
	public List<Query> sortQueries(Collection<Query> queryCollection, FilePathString sortingQueryFilePathString) {
		
		HashMap<String,Integer> directMatchIndexMap = new HashMap<String,Integer>();
		ArrayList<String> matchIndexList = new ArrayList<String>();
		boolean matchSorting = false;		
		int indexCount = 0;
		
		if (sortingQueryFilePathString != null) {
			
			try {
				FileInputStream inputStream = new FileInputStream(new File(sortingQueryFilePathString.getAbsoluteFilePathString()));
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line != null && !line.isEmpty()) {
						String matchText = line.trim();
						matchText = matchText.replace("\\","/");
						directMatchIndexMap.put(matchText, indexCount);
						matchIndexList.add(matchText);
						matchSorting = true;
						indexCount++;
					}
				}
				reader.close();				
				inputStream.close();
			} catch (IOException e) {
				mLogger.warn("Parsing of '{}' failed.",sortingQueryFilePathString);
			}			
			
		}
		
		ArrayList<Query> list = new ArrayList<Query>();
		if (matchSorting) {
			HashMap<Integer,ArrayList<Query>> indexQueryListHash = new HashMap<Integer,ArrayList<Query>>();
			for (Query query : queryCollection) {
				int index = 0;
				boolean foundIndexMatch = false;
				if (query.getQuerySourceString().isRelative()) { 
					String relativeQuerySourceString = query.getQuerySourceString().getRelativeFilePathString();
					relativeQuerySourceString = relativeQuerySourceString.replace("\\","/");
					if (directMatchIndexMap.containsKey(relativeQuerySourceString)) {
						index = directMatchIndexMap.get(relativeQuerySourceString);
						foundIndexMatch = true;
					}
				}
				String absoluteQuerySourceString = query.getQuerySourceString().getAbsoluteFilePathString();
				absoluteQuerySourceString = absoluteQuerySourceString.replace("\\","/");
				if (!foundIndexMatch) {					
					if (directMatchIndexMap.containsKey(absoluteQuerySourceString)) {
						index = directMatchIndexMap.get(absoluteQuerySourceString);
						foundIndexMatch = true;
					}
				}				
				if (!foundIndexMatch) {
					int nextIndex = 0;
					Iterator<String> matchInIt = matchIndexList.iterator();
					if (matchInIt.hasNext() && !foundIndexMatch) {
						String matchString = matchInIt.next();
						if (absoluteQuerySourceString.contains(matchString)) {		
							foundIndexMatch = true;
							index = nextIndex;
						}
						nextIndex++;
					}
				}
				if (!foundIndexMatch) {
					index = indexCount;
					foundIndexMatch = true;
				}
				
				ArrayList<Query> queryList = indexQueryListHash.get(index);
				if (queryList == null) {
					queryList = new ArrayList<Query>();
					indexQueryListHash.put(index,queryList);
				}
				queryList.add(query);					
				
			}
			
			for (int i = 0; i <= indexCount; ++i) {
				ArrayList<Query> queryList = indexQueryListHash.get(i);
				if (queryList != null) {	
					list.addAll(queryList);
				}
			}
					
		} else {
			list.addAll(queryCollection);
		}
		return list;
	}

	
	
	
}
