package org.semanticweb.ore.wep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.semanticweb.ore.configuration.Config;
import org.semanticweb.ore.configuration.ConfigDataValueReader;
import org.semanticweb.ore.configuration.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LinkPageMapper {
	
	final private static Logger mLogger = LoggerFactory.getLogger(LinkPageMapper.class);

	protected Config mConfig = null;
	
	protected HashMap<String,String> mReasonerLinkMap = new HashMap<String,String>();
	protected HashMap<String,String> mCompetitionLinkMap = new HashMap<String,String>();
	
	protected String mWebRootDirString = null;
	
	public LinkPageMapper(Config config) {
		mConfig = config;
		
		mWebRootDirString = ConfigDataValueReader.getConfigDataValueString(mConfig, ConfigType.CONFIG_TYPE_WEB_COMPETITION_STATUS_ROOT_DIRECTORY);
		
		loadStringLinkFile(mWebRootDirString+"reasonerLinkMapping.txt",mReasonerLinkMap);
		loadStringLinkFile(mWebRootDirString+"competitionLinkMapping.txt",mCompetitionLinkMap);
	}
	
	
	public void loadStringLinkFile(String fileString, HashMap<String,String> stringLinkMap) {	
		try {
			FileInputStream inputStream = new FileInputStream(new File(fileString));
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			String line = null;
			while ((line = reader.readLine()) != null) {	
				String[] strings = line.split("\t");
				if (strings.length >= 2) {
					if (!strings[0].startsWith("#")) {
						stringLinkMap.put(strings[0], strings[1]);
					}
				}
			}
			reader.close();
		} catch (Exception e) {
			mLogger.error("Failed to load '{}'",e.getMessage());
		}
	}
	
	
	public String getReasonerLink(String reasonerName) {
		String reasonerLink = mReasonerLinkMap.get(reasonerName);
		return reasonerLink;
	}
	

	public String getCompetitionLink(String competitionName) {
		String competitionLink = mCompetitionLinkMap.get(competitionName);
		return competitionLink;
	}
	
}
