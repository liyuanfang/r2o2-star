package ore.querying;

public class QueryExpressivity {

	private boolean mDLProfile = false;
	private boolean mELProfile = false;
	private boolean mRLProfile = false;
	private boolean mQLProfile = false;
	private boolean mUsingDatatype = false;
	private boolean mUsingRule = false;
	
	public boolean isInDLProfile() {
		return mDLProfile;
	}
	
	public boolean isInELProfile() {
		return mELProfile;
	}
	
	public boolean isInRLProfile() {
		return mRLProfile;
	}
	
	public boolean isInQLProfile() {
		return mQLProfile;
	}
	
	public boolean isUsingDatatypes() {
		return mUsingDatatype;
	}
	
	public boolean isUsingRules() {
		return mUsingRule;
	}	
	
	public QueryExpressivity(boolean inDLProfile, boolean inELProfile, boolean inRLProfile, boolean inQLProfile, boolean usingDatatypes, boolean usingRules) {
		mDLProfile = inDLProfile;
		mELProfile = inELProfile;
		mRLProfile = inRLProfile;
		mQLProfile = inQLProfile;
		mUsingDatatype = usingDatatypes;
		mUsingRule = usingRules;
	}
	
}
