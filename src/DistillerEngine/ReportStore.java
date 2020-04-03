package DistillerEngine;

import java.util.*;

public class ReportStore {

// A singleton object, hold reports for each downloaded item
	
// Also keeps track of how many scheduled items there are for each job
	
	private static ReportStore centralStore = new ReportStore();
	
	private static Integer RUNNING = new Integer(1);
	private static Integer FINISHED = new Integer(2);
	
	private Hashtable<Integer,Vector<ReportTest>> testResults;
	private Hashtable<Integer,Hashtable<String,ReportItem>> reports;
	private Hashtable<Integer,Hashtable<String,Integer>> primaryJobs;
	private Hashtable<Integer,Hashtable<String,Integer>> secondaryJobs;
	
	private ComparatorDThread theComparator; // may need to be hashtable for multiple comparators
	
	private ReportStore(){
		reports = new Hashtable<Integer,Hashtable<String,ReportItem>>();
		primaryJobs = new Hashtable<Integer,Hashtable<String,Integer>>();
		secondaryJobs = new Hashtable<Integer,Hashtable<String,Integer>>();
		testResults = new Hashtable<Integer,Vector<ReportTest>>();
	}

	public static void setComparator(ComparatorDThread comp){
		centralStore.theComparator = comp;
	}
	
	public static void addJobPrimary(int jobNumber,String urls[]){
// return true if new url job added, false if it already exists	
		Integer job = new Integer(jobNumber);
		Hashtable<String,Integer> jobHash;
		boolean exists = false;
		synchronized(centralStore){
			  jobHash = centralStore.primaryJobs.get(job);
			  if (jobHash==null){
				  jobHash = new Hashtable<String,Integer>();
			  }
			  for(int i=0;i<urls.length;i++){
  		          jobHash.put(urls[i], RUNNING);
			  }
			  centralStore.primaryJobs.put(job, jobHash);			
		}
	}
	
	public static void addJobSecondary(int jobNumber,String urls[]){
		// return true if new url job added, false if it already exists	
				Integer job = new Integer(jobNumber);
				Hashtable<String,Integer> jobHash;
				boolean exists = false;
				synchronized(centralStore){
					  jobHash = centralStore.secondaryJobs.get(job);
					  if (jobHash==null){
						  jobHash = new Hashtable<String,Integer>();
					  }
					  for(int i=0;i<urls.length;i++){
		  		          jobHash.put(urls[i], RUNNING);
					  }
					  centralStore.secondaryJobs.put(job, jobHash);			
				}
	}
	
// check if already added, secondaries online
	
	public static boolean isPresent(int jobNumber, String url){
		Integer job = new Integer(jobNumber);
		Hashtable<String,Integer> priHash = centralStore.primaryJobs.get(job);
		if (priHash!=null && priHash.containsKey(url)){ return true; }
		Hashtable<String,Integer> jobHash = centralStore.secondaryJobs.get(job);
		if (jobHash==null){ return false; }
		return jobHash.containsKey(url);
	}	
		
// At present a report is only made at the maximum stage of processing so we can decrement the job count here
	public static void report(String url,String masterUrl,ReportAction action,int jobnumber, ParsedItem item, String why){
		Integer job = new Integer(jobnumber);
		Hashtable<String,ReportItem> repHash;
		synchronized(centralStore){
  		  repHash = centralStore.reports.get(job);
		  if (repHash==null){
			  repHash = new Hashtable<String,ReportItem>();
			  centralStore.reports.put(job, repHash);
		  }
		}
		ReportItem ri = new ReportItem(url,masterUrl,action,jobnumber,item);
		ri.setWhy(why);
		repHash.put(url,ri);
		markDone(jobnumber,url);
	}
	
	public static void report(String url,String masterUrl,ReportAction action,int jobnumber){
		report(url,masterUrl,action,jobnumber,null,"");
	}

	public static void report(String url,String masterUrl,ReportAction action,int jobnumber, String why){
		report(url,masterUrl,action,jobnumber,null,why);
	}	
	
	
	private static void markDone(int jobNumber,String url){
		Integer job = new Integer(jobNumber);
		Hashtable<String,Integer> jobHash;
		boolean exists = false;
		synchronized(centralStore){
			  jobHash = centralStore.primaryJobs.get(job);
			  if (jobHash==null){ return; }
			  if (jobHash.containsKey(url)){
				  jobHash.put(url, FINISHED);
			  }
			  jobHash = centralStore.secondaryJobs.get(job);
			  if (jobHash==null){ return; }
		      if (jobHash.containsKey(url)){ jobHash.put(url, FINISHED); }				  			  
		}
	}
	
	public static Iterator<String> getPrimaryURLs(int jobnumber){
		Integer job = new Integer(jobnumber);
		Hashtable<String,Integer> jobHash;
		boolean exists = false;
	    jobHash = centralStore.primaryJobs.get(job);
	    if (jobHash==null){ return (new ArrayList<String>()).iterator(); }
	    return (new TreeSet<String>(jobHash.keySet())).iterator();
	}
	
	public static Iterator<String> getSecondaryURLs(int jobnumber){
		Integer job = new Integer(jobnumber);
		Hashtable<String,Integer> jobHash;
		boolean exists = false;
	    jobHash = centralStore.secondaryJobs.get(job);
	    if (jobHash==null){ return (new ArrayList<String>()).iterator(); }
	    return (new TreeSet<String>(jobHash.keySet())).iterator();
	}	
	
	public static ReportItem getReport(String url, int jobnumber){
		Integer job = new Integer(jobnumber);
		Hashtable<String,ReportItem> repHash;
		synchronized(centralStore){
			repHash = centralStore.reports.get(job);
			if (repHash==null){ repHash = new Hashtable<String,ReportItem>(); }
		}
		ReportItem act = repHash.get(url);
		if (act==null){ return new ReportItem(url,"",ReportAction.NO_INFO,jobnumber); }
		return act;
	}
	
	public static void reportTest(int jobNumber,int filterNum, String url,float score){
		Vector<ReportTest> repVec;
		synchronized(centralStore){
			repVec = centralStore.testResults.get(jobNumber);
			if (repVec==null){ 
				repVec = new Vector<ReportTest>();
				centralStore.testResults.put(jobNumber,repVec);
			}
		}
		repVec.add(new ReportTest(jobNumber,filterNum,url,score));
	}
	
	public static boolean jobDone(int jobnumber){
		int running = 0;
		Integer job = new Integer(jobnumber);
		Hashtable<String,Integer> jobHash = centralStore.primaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			if (jobHash.get( e.nextElement())!=FINISHED){ running++; }
		  }
		}
		Logging.finer(running+" primary jobs running in job "+jobnumber);
		jobHash = centralStore.secondaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			if (jobHash.get( e.nextElement())!=FINISHED){ running++; }
		  }
		}
		Logging.fine(running+" total jobs running in job "+jobnumber);
		return running==0;
	}
	
	public static void describeJob(int jobnumber){
		int running = 0;
		Integer job = new Integer(jobnumber);
		Hashtable<String,Integer> jobHash = centralStore.primaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			String url = e.nextElement();
			if (jobHash.get( url)!=FINISHED){ 
				running++;
				Logging.info("Primary job "+jobnumber+", still running url: "+url);
			}
		  }
		}
		Logging.finer(running+" primary jobs running in job "+jobnumber);
		jobHash = centralStore.secondaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			String url = e.nextElement();
			if (jobHash.get( url)!=FINISHED){ running++; 
			  Logging.info("Secondary job "+jobnumber+", still running url: "+url);			
			}
		  }
		}
		Logging.fine(running+" total jobs running in job "+jobnumber);
	}	

	public static void earlyTerminateJob(int jobnumber){
		int running = 0;
		Integer job = new Integer(jobnumber);
		Hashtable<String,Integer> jobHash = centralStore.primaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			String url = e.nextElement();
			if (jobHash.get( url)!=FINISHED){ 
				running++;
				Logging.info("Primary job "+jobnumber+", stopping running url: "+url);
				jobHash.put(url, FINISHED);
			}
		  }
		}
		Logging.finer(running+" primary jobs running in job "+jobnumber);
		jobHash = centralStore.secondaryJobs.get(job);
		if (jobHash!=null){
		  for(Enumeration<String> e=jobHash.keys();e.hasMoreElements();){
			String url = e.nextElement();
			if (jobHash.get( url)!=FINISHED){ running++; 
			  Logging.info("Secondary job "+jobnumber+", stopping running url: "+url);
			  jobHash.put(url, FINISHED);
			}
		  }
		}
		Logging.fine(running+" total jobs stopped in job "+jobnumber);
	}		
	
	public static void clearJob(int jobnumber){
		Integer job = new Integer(jobnumber);
		Hashtable<String,ReportItem> repHash;
		synchronized(centralStore){
			centralStore.reports.remove(job);
			centralStore.primaryJobs.remove(job);
			centralStore.secondaryJobs.remove(job);
			centralStore.testResults.remove(job);
			if (centralStore.theComparator!=null){
			  centralStore.theComparator.removeFilters(job);
			}
		}		
	}
	
	public static Distiller.Scores getScores(int task){
		Distiller.Scores scores = new Distiller.Scores();
		Vector<ReportTest> testVec = centralStore.testResults.get(new Integer(task));
		if (testVec==null){ return scores; }
		for(Enumeration<ReportTest> e=testVec.elements();e.hasMoreElements();){
			ReportTest rt = e.nextElement();
			scores.setScore(rt.filterNum, rt.url, rt.score);
		}
		return scores;
	}
	
}
