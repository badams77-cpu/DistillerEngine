package DistillerEngine;

import java.util.*;
import java.io.IOException;

// Makes an Accept Reject Filter for a class

public class ARFilterMaker implements Runnable {

	private static ARFilterMaker singleton;
	
	FeedScheduler feedSched;
	Config conf;
	LoggingThreadGroup ltg;
	private Thread arthread;
	private boolean running;
	
	private LinkedList<Integer> filo;
	
	public ARFilterMaker(LoggingThreadGroup lg,FeedScheduler fs,Config conf){
		this.conf=conf;
		ltg= lg;
		feedSched=fs;
		filo = new LinkedList<Integer>();
		if (singleton==null){
			singleton=this;
			running=false;
		}
	}
	
	public static ARFilterMaker getMaker(){ return singleton; }
	
	public void add(int feedid){
		synchronized(filo){
		  filo.addFirst(new Integer(feedid));
		  ARFilterStore.setMaking(feedid);
		}
		  String  fname = feedSched.getFeedname(feedid); // Needed to allow cached name lookups later
		  Logging.fine("Started make ARFilter for job:"+feedid+", "+fname);
		  if (running) return;
		  running=true;
		  arthread = new Thread(ltg,this);
		  arthread.start();
	}
	
	public void run(){
		while(running){
		  int feedid=-1;
		  Integer feedInt = null;
	      synchronized(filo){
		    feedInt=filo.pollLast();
	      }
		  if (feedInt==null){ 
		      try {
		         arthread.sleep(200);
		      } catch (InterruptedException e){}
		      continue; // thread loop
		  } else {
		    feedid = feedInt.intValue();
	      }
	      byte[][] accepted=null,rejected=null;
	      try {
	        accepted = feedSched.getAccepted(feedid);
	        rejected = feedSched.getRejected(feedid);
	        ARFilterStore.setARRatio(feedid, feedSched.getARRatio(feedid));
	      } catch (Exception e){
	    	  Logging.severe( 
	    			  "Error get accept/reject items for feedid="+feedid+";\n",e);
	        continue; // thread loop
	      }
// Accepted
	      WordsItem toAccept = new WordsItem();
	      for(int i=0;i<accepted.length;i++){
	    	  byte[] md5=accepted[i];
	    	  ARFilterStore.setItemGood(feedid, md5);
	    	  try {
	    		  ARFilterStore.addAcceptedWords(feedid, WordsItem.getWordsItem(feedid, md5));
	    	  } catch (Exception e){
	    		  Logging.warning("ARFilterMaker Failed reading item "+md5+
	    				  " in feedid="+feedid,e);
	    	  }
	      }
	      WordsItem toReject = new WordsItem();
	      for(int i=0;i<rejected.length;i++){
	    	  byte[] md5=rejected[i];
	    	  ARFilterStore.setItemBad(feedid,md5);
	    	  try {
	    	      ARFilterStore.addRejectedWords(feedid, WordsItem.getWordsItem(feedid, md5));
	    	  } catch (Exception e){
	    		  Logging.warning("ARFitlerMaker failed reading item "+md5+
	    				  " in feedid="+feedid,e);
	    	  }
	      }

	      ARFilterStore.setReady(feedid);
		} // thread loop
		
	}
	
}

