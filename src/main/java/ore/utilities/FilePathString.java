package ore.utilities;

public class FilePathString {
	
	private String mAbsoluteFilePathString = null;
	private String mRelativeFilePathString = null;
	private String mRelativeBaseFilePathString = null;
	private RelativeFilePathStringType mRelativeType = null;
	
	public boolean isRelative() {
		if (mRelativeBaseFilePathString != null) {
			return true;			
		} else {
			return false;
		}
	}
	
	public boolean isAbsolute() {
		return !isRelative();
	}
	
	
	public String getRelativeFilePathString() {
		return mRelativeFilePathString;
	}

	public String getPreferedRelativeFilePathString() {
		if (isRelative()) {
			return mRelativeFilePathString;
		} else {
			return mAbsoluteFilePathString;
		}
	}
	
	public RelativeFilePathStringType getRelativeType() {
		return mRelativeType;
	}

	public String getRelativeBaseFilePathString() {
		return mRelativeBaseFilePathString;
	}
	
	
	public String getAbsoluteFilePathString() {
		return mAbsoluteFilePathString;
	}	
	
	public FilePathString(String absoluteFilePathString) {
		mAbsoluteFilePathString = absoluteFilePathString;
	}
	
	public String toString() {
		return mAbsoluteFilePathString;
	}
	
	
	
	public FilePathString(String relativeBaseFilePathString, String relativeFilePathString, RelativeFilePathStringType relativeType) {
		mAbsoluteFilePathString = relativeBaseFilePathString+relativeFilePathString;
		mRelativeBaseFilePathString = relativeBaseFilePathString;
		mRelativeFilePathString = relativeFilePathString;
		mRelativeType = relativeType;
	}	

}
