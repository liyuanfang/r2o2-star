package org.semanticweb.ore.interfacing;

import org.semanticweb.ore.querying.QueryExpressivity;

public class ExpressivitySupport {

	private boolean mDLProfileSupport = false;
	private boolean mELProfileSupport = false;
	private boolean mRLProfileSupport = false;
	private boolean mQLProfileSupport = false;
	private boolean mDatatypeSupport = false;
	private boolean mRuleSupport = false;
	private boolean mFULLSupport = false;
	
	public boolean isDLProfileSupported() {
		return mDLProfileSupport;
	}
	
	public boolean isELProfileSupport() {
		return mELProfileSupport;
	}
	
	public boolean isRLProfileSupport() {
		return mRLProfileSupport;
	}
	
	public boolean isQLProfileSupport() {
		return mQLProfileSupport;
	}
	
	public boolean isDatatypeSupport() {
		return mDatatypeSupport;
	}
	
	public boolean isRuleSupport() {
		return mRuleSupport;
	}
	
	public boolean isFULLSupport() {
		return mFULLSupport;
	}
	
	public boolean supportsQuery(QueryExpressivity queryExpressivity) {
		if (!mRuleSupport && queryExpressivity.isUsingRules()) {
			return false;
		}
		if (!mDatatypeSupport && queryExpressivity.isUsingDatatypes()) {
			return false;
		}
		if (mFULLSupport) {
			return true;
		}
		if (mDLProfileSupport && queryExpressivity.isInDLProfile()) {
			return true;
		}
		if (mELProfileSupport && queryExpressivity.isInELProfile()) {
			return true;
		}
		if (mRLProfileSupport && queryExpressivity.isInRLProfile()) {
			return true;
		}
		if (mQLProfileSupport && queryExpressivity.isInQLProfile()) {
			return true;
		}
		return false;				
	}
	
	public ExpressivitySupport(boolean supportedDLProfile, boolean supportedELProfile, boolean supportedRLProfile, boolean supportedQLProfile, boolean datatypesSupported, boolean rulesSupported, boolean fullSupported) {
		mDLProfileSupport = supportedDLProfile;
		mELProfileSupport = supportedELProfile;
		mRLProfileSupport = supportedRLProfile;
		mQLProfileSupport = supportedQLProfile;
		mDatatypeSupport = datatypesSupported;
		mRuleSupport = rulesSupported;
		mFULLSupport = fullSupported;
	}
	

}
