package DistillerEngine;

public class StatusInfo {

	public static StatusInfo FAILED = new StatusInfo(0);
	public static StatusInfo SUCCESS = new StatusInfo(1);
	
	private int succeeded=0;
	
	public StatusInfo(int i){
		succeeded = i;
	}
	
	public boolean equals(StatusInfo s){
		return s.succeeded==succeeded;
	}
	
}
