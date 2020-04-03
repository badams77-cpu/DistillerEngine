package DistillerEngine;

public class ReportAction {

	public static ReportAction NO_INFO = new ReportAction(0);
	public static ReportAction DOWNLOAD_FAILED = new ReportAction(1);
	public static ReportAction PARSE_FAILED = new ReportAction(2);
	public static ReportAction PARSE_COMPLETED = new ReportAction(3); // For items from the engine thread
	public static ReportAction COMPARE_FAILED = new ReportAction(4);
	public static ReportAction COMPARE_PASSED = new ReportAction(5);
	public static ReportAction COMPARE_MISSING_FILTER = new ReportAction(6);
	
	private int action=0;
	
	public ReportAction(int i){
		action = i;
	}
	
	public boolean equals(ReportAction s){
		return s.action==action;
	}
	
	public String toString(){
		switch (action){
			case 0: return "NO_INFO";
			case 1: return "DOWNLOAD_FAILED";
			case 2: return "PARSE_FAILED";
			case 3: return "PARSE_COMPLETED";
			case 4: return "COMPARE_FAILED";
			case 5: return "COMPARE_PASSED";
			case 6: return "COMPARE_MISSING_FILTER";
			default: return "UNKNOWN_ACTION";
		}
	}
	
}
