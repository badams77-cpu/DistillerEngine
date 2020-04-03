package DistillerEngine;

/*
 *  Used to hold a log of information about a particular page of url
 *  e.g. Download sucessful, failed
 * 
 */

public class ReportItem {

	private String url;
	private String masterUrl;
	private ReportAction action;
	private int jobnumber;
	private ParsedItem item;
	private String whyFailed;
	
	public ReportItem(String url,String masterUrl,ReportAction action,int jobnumber){
		this.url = url;
		this.masterUrl = masterUrl;
		this.action = action;
		this.jobnumber = jobnumber;
		this.item = null;
	}

	public ReportItem(String url,String masterUrl,ReportAction action,int jobnumber, ParsedItem ri){
		this.url = url;
		this.masterUrl = masterUrl;
		this.action = action;
		this.jobnumber = jobnumber;
		this.item=ri;
	}	
	
	public String getWhy(){ return whyFailed; }
	public void setWhy(String whyFailed){ this.whyFailed = whyFailed; }
	
	public ReportAction getAction(){
		return action;
	}
	
	public String getMasterUrl(){ return masterUrl; }

	ParsedItem getItem(){
		return item;
	}
	
}
