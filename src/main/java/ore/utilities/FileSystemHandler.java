package ore.utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class FileSystemHandler {


	public static Collection<String> collectSubdirectories(String directoryString) {
		return collectSubdirectories(Collections.singleton(directoryString));
	}
	
	public static Collection<String> collectSubdirectories(Collection<String> directoryStrings) {
		ArrayList<String> directoryStringList = new ArrayList<String>();
		for (String directoryString : directoryStrings) {
			File directoryFile = new File(directoryString);
			if (!directoryFile.isDirectory()) {
				directoryFile = directoryFile.getParentFile();
			}
			if (directoryFile != null && directoryFile.isDirectory()) {
				directoryStringList.add(directoryFile.getPath());
				File[] subfiles = directoryFile.listFiles();
				for (File subfile : subfiles) {
					if (subfile.isDirectory()) {						
						directoryStringList.addAll(collectSubdirectories(subfile.getPath()));
					}
				}
			}
		}
		return directoryStringList;
	}
	
	
	
	public static Collection<String> collectFilesInSubdirectories(String directoryString) {
		return collectFilesInSubdirectories(Collections.singleton(directoryString));
	}
	
	public static Collection<String> collectFilesInSubdirectories(Collection<String> directoryStrings) {
		ArrayList<String> fileStringList = new ArrayList<String>();
		for (String directoryString : directoryStrings) {
			File directoryFile = new File(directoryString);
			if (!directoryFile.isDirectory()) {
				directoryFile = directoryFile.getParentFile();
			}
			if (directoryFile != null) {
				File[] subfiles = directoryFile.listFiles();
				for (File subfile : subfiles) {
					if (subfile.isDirectory()) {						
						fileStringList.addAll(collectFilesInSubdirectories(subfile.getPath()));
					} else {
						fileStringList.add(subfile.getPath());
					}
				}
			}
		}
		return fileStringList;
	}	
	
	
	public static Collection<String> collectRelativeFilesInSubdirectories(String directoryString) {
		return collectRelativeFilesInSubdirectories(Collections.singleton(directoryString),"");
	}
	
	public static Collection<String> collectRelativeFilesInSubdirectories(Collection<String> directoryStrings) {
		return collectRelativeFilesInSubdirectories(directoryStrings,"");
	}
	

	public static Collection<String> collectRelativeFilesInSubdirectories(String directoryString, String relativeSubdirectoryString) {
		return collectRelativeFilesInSubdirectories(Collections.singleton(directoryString),relativeSubdirectoryString);
	}
	
	public static Collection<String> collectRelativeFilesInSubdirectories(Collection<String> directoryStrings, String relativeSubdirectoryString) {
		ArrayList<String> fileStringList = new ArrayList<String>();
		for (String directoryString : directoryStrings) {
			File directoryFile = new File(directoryString);
			if (!directoryFile.isDirectory()) {
				directoryFile = directoryFile.getParentFile();
			}
			if (directoryFile != null) {
				File[] subfiles = directoryFile.listFiles();
				for (File subfile : subfiles) {
					if (subfile.isDirectory()) {
						fileStringList.addAll(collectRelativeFilesInSubdirectories(subfile.getPath(),relativeSubdirectoryString+subfile.getName()+ File.separator));
					} else {
						fileStringList.add(relativeSubdirectoryString+subfile.getName());
					}
				}
			}
		}
		return fileStringList;
	}
	
	
	public static boolean isFileStringRelative(String fileString, String baseDirectoryString) {
		File file = new File(fileString);
		File baseDirectory = new File(baseDirectoryString);
		String absoluteFileString = file.getAbsolutePath();
		String absoluteBaseDirectoryString = baseDirectory.getAbsolutePath();
		if (absoluteFileString.startsWith(absoluteBaseDirectoryString)) {			
			return true;
		}
		return false;
	}	
	
	public static String getRelativeFileString(String fileString, String baseDirectoryString) {
		File file = new File(fileString);
		File baseDirectory = new File(baseDirectoryString);
		String absoluteFileString = file.getAbsolutePath();
		String absoluteBaseDirectoryString = baseDirectory.getAbsolutePath();
		if (absoluteFileString.startsWith(absoluteBaseDirectoryString)) {
			String relativeFileString = absoluteFileString.substring(absoluteBaseDirectoryString.length());
			if (file.isDirectory()) {
				relativeFileString = relativeFileString+ File.separator;
			}
			return relativeFileString;
		}
		return fileString;
	}
	
	
	public static boolean ensurePathToFileExists(FilePathString filePathString) {
		return ensurePathToFileExists(filePathString.getAbsoluteFilePathString());
	}

	public static boolean nonEmptyFileExists(FilePathString filePathString) {	
		return nonEmptyFileExists(filePathString.getAbsoluteFilePathString());
	}

	public static boolean nonEmptyFileExists(String fileString) {
		File file = new File(fileString);
		if (file.isFile() && file.canRead() && file.exists() && file.length() > 0) {
			return true;
		}
		return false;
	}
	
	public static boolean ensurePathToFileExists(String fileString) {
		File file = new File(fileString);
		if (file.isDirectory()) {
			return false;
		} else {
			File parentFile = file.getParentFile();
			if (parentFile != null) {
				return parentFile.mkdirs();
			}
			return false;
		}
	}
	
	
	
	public static String relativeAbsoluteResolvedFileString(FilePathString filePathString) {
		
		if (filePathString.isRelative()) {
			return "relative" + File.separator + filePathString.getRelativeFilePathString();
		} else {
			String redirectSourceString = null;
			File redirectSourceFile = new File(filePathString.getAbsoluteFilePathString());
			while (redirectSourceFile != null) {
				String filePartString = redirectSourceFile.getName();
				if (redirectSourceString == null) {
					redirectSourceString = filePartString;
				} else {
					redirectSourceString = filePartString + File.separator + redirectSourceString;
				}
				redirectSourceFile = redirectSourceFile.getParentFile();
			}
			return "absolute" + File.separator + redirectSourceString;
		}
		
	}

}
