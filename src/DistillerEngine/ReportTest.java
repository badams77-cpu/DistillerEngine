package DistillerEngine;

public class ReportTest {

	int jobNum;
    String url;
	int filterNum;
	float score;
	
	public ReportTest(int jobNum, int filterNum, String url, float score){
		this.jobNum=jobNum;
		this.filterNum=filterNum;
		this.url = url;
		this.score =score;
	}
	
}
