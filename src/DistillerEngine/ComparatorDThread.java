package DistillerEngine;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import Distiller.Filter;

public class ComparatorDThread implements Runnable {

// Drop in replacement for the ComparatorThread utilising pairs in its score	
	
	ParsedBuffer inputData;
	ParsedCache filterData;
	Config conf;
	CorporaDouble corpora;
	LoggingThreadGroup ltg;
	Thread myThread;
	Filters filters;
	boolean terminate;
	int pagesCompared;
	int pagesNotCompared;
	MD5 md5;
	
	public double scoreLimit = 0.5;
	
	String language = "en"; // Extend to multiple languages soon
	
	public ComparatorDThread(LoggingThreadGroup ltg, ParsedBuffer inputData, Config conf){
		this.ltg=ltg;
		this.inputData = inputData;
		this.conf = conf;
		loadCorp(conf.corporaFile);
		md5 = new MD5();
		ReportStore.setComparator(this);		
	}
	
	private void loadCorp(String corporaFile){
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(corporaFile),2048);
            ObjectInputStream ois = new ObjectInputStream(bis);
            corpora = (CorporaDouble) ois.readObject();
		} catch(Exception e){
			System.err.println("Failed loading the corpora "+corporaFile);
			e.printStackTrace(System.err);
			throw new Error("Can't start ComporatorThread, no corpora");
		}
	}
	
	public void start(ParsedCache pCache, Filters filters){
		filterData = pCache;
		this.filters = filters;
		myThread = new Thread(ltg,this);
		terminate = false;
		myThread.start();
	}

	public void removeFilters(int taskId){
		filters.clearFilter(taskId);
        filterData.clearId(taskId);
	}
	
	public void stop(){
		terminate = true;
	}
	
	public int getPagesCompared(){
		int temp= pagesCompared;
		pagesCompared=0;
		return temp;
	}
	
	public int getPagesNotCompared(){
		int temp =  pagesNotCompared;
		pagesNotCompared =0;
		return temp;
	}
	
	public void run(){
		while(!terminate){
			boolean hasItems = false;
			if (inputData.moreItems()){
		        hasItems = true;
				int taskId = inputData.getId();
				if (ARFilterStore.hasFilter(taskId)){
					ARCompare(inputData,taskId);
				} else {
                  Filter filter[] = filters.getFilters(taskId);
//				Logging.info(" found "+filter.length+" filters for task "+taskId);
				  boolean haveTried[] = new boolean[filter.length];
				  long start=System.currentTimeMillis();
		          int nTried = 0;
			      for(int i=0;i<filter.length;i++){
				    haveTried[i] = false;
			      }
		  		  boolean itemGood = false;
				  boolean missingFilter = false;
				  float maxScore =0;
				  while( nTried<filter.length){
		// Loop over filters, may need to wait for some to have loaded	
					for(int i=0;i<filter.length;i++){
						if (haveTried[i]){ continue; }
						String furl = filter[i].getFilter_url();
						if (filterData.selectURL(taskId, furl)){
							nTried++;
							haveTried[i]=true;
							float score = 0.0f;
							float sScore = compare(inputData.getWords(),filterData.getWords());
							float dScore= comparePair(inputData.getPairs(), filterData.getPairs());
							score = sScore+2.0f*dScore;
	  // Store result
							ReportStore.reportTest(taskId, filter[i].filterid, inputData.getSourceURL(), score);
							if (score>maxScore){maxScore = score; }
							Logging.fine("Score: "+score+" for "+inputData.getSourceURL()+" compare "+furl);
							Logging.fine("Score,Single: "+sScore+" for "+inputData.getSourceURL()+" compare "+furl);
							Logging.fine("Score,Pairs: "+dScore+" for "+inputData.getSourceURL()+" compare "+furl);
							if (score*filter[i].weighting>scoreLimit){ itemGood = true; nTried=filter.length; break; }
							pagesCompared++;
						} else {
							ReportAction ra = ReportStore.getReport(furl, taskId).getAction();
							if (ra.equals(ReportAction.DOWNLOAD_FAILED) || 
									ra.equals(ReportAction.PARSE_FAILED)){
								Logging.warning("Filter "+furl+" missing "+ra);
								nTried++;
								haveTried[i]=true;
								missingFilter = true;
								pagesNotCompared++;
							} else {
								Logging.info(" waiting for filter "+i+" "+ra+", tried "+nTried);
								if (System.currentTimeMillis()-start>180000){
									Logging.warning("Giving up on filter page "+furl);
									haveTried[i]=true;
									nTried++;
								}
							}
						}
                        
					}
					if (nTried<filter.length && !missingFilter){
		// Wait for Filter to download				
						try {
							Thread.sleep(500);
						} catch (InterruptedException e){}
					}
				}
//				Logging.finer(" finished comparing "+inputData.getSourceURL());
				try { // in case of null pointers
					inputData.getItem().setScore(itemGood,maxScore);
					String sourcedby = "";
					ParsedItem head = inputData.getHead();
					if (head!=null){ sourcedby=head.sourceurl; } else { 
						System.err.println("Missing head for "+inputData.getSourceURL());
						Logging.warning("Missing head for "+inputData.getSourceURL());
					}
					if (itemGood){
						ReportStore.report(inputData.getSourceURL(), sourcedby
							, ReportAction.COMPARE_PASSED, taskId, inputData.getItem().trimData(),"");

					} else {
						if (!missingFilter){
							ReportStore.report(inputData.getSourceURL(), sourcedby
							, ReportAction.COMPARE_FAILED, taskId, inputData.getItem().trimData(),"");
						} else {
							ReportStore.report(inputData.getSourceURL(), sourcedby
								, ReportAction.COMPARE_FAILED, taskId, inputData.getItem().trimData(),"");						
						}
					}
				} catch (NullPointerException e){
					Logging.severe("NullPointerException in Comparator Thread",e);
					String headUrl = "No head";
					String why="unknown";
					if (inputData.getHead()!=null){ headUrl =  inputData.getHead().sourceurl;} else {  why="no head";}
					ParsedItem item = inputData.getItem();
					if (item != null){
						item.trimData();
						ReportStore.report(inputData.getSourceURL(), headUrl, ReportAction.COMPARE_FAILED, taskId,item,why);
					} else {
						ReportStore.report(inputData.getSourceURL(), headUrl, ReportAction.COMPARE_FAILED, taskId, "No Parsed Item");
					}
				} catch (Exception e){
					Logging.severe("Exception in Comparator Thread: "+e,e);					
				}
				try {
					ParsedItem item =inputData.getItem();
					item.trimData();
				} catch (Exception e){
					Logging.severe("Exception in Comparator Thread, while cleaning item: "+e,e);
				}
			  }
			}
			if (!hasItems){
				try {
				  myThread.sleep(500);
				} catch (InterruptedException e){}
			}
		}
	}
	
	public float compare(Hashtable<String,Integer> inputWords, Hashtable<String,Integer> filterWords){
		float score = 0.0f;
		Enumeration<String> words = inputWords.keys();
		if (inputWords.size()>filterWords.size()){ words = filterWords.keys(); }
		String word = null;
		int inputWordsSize = 0;
		for(Enumeration<String> eWords=inputWords.keys(); eWords.hasMoreElements(); 
		     inputWordsSize+=inputWords.get(eWords.nextElement()).intValue());
		int filterWordsSize = 0;
		for(Enumeration<String> eWords=filterWords.keys(); eWords.hasMoreElements(); 
	      filterWordsSize+=filterWords.get(eWords.nextElement()).intValue());		
		if (inputWordsSize==0 || filterWordsSize==0){ return 0.0f; }
		CorporaOccurances occs = corpora.getOccurances(language);
		double logMax = -Math.log(corpora.getTotal(language)); // Maximum occurance boost
		int wordCount=0;
		while(words.hasMoreElements()){
			word = words.nextElement();
			Integer occIn = inputWords.get(word);
			Integer occFil = filterWords.get(word);
//			Logging.finest(word+" "+occIn+" "+occFil);
			if (occIn==null || occFil==null){ continue; }
			float ocIn = occIn.floatValue()/inputWordsSize;
			float ocFil = occFil.floatValue()/filterWordsSize;
			if (ocFil==0.0) continue; 
			wordCount++;
			float ocCorp = occs.getOccurance(word);
	// Total score is the score for scalar product of the occurance between the test and the filter
	// multipled by the log factor for the word to be in the filter but not the general corpora
			double score1=0;
			if (ocCorp==0.0){
				score1 = ocIn*(Math.log(ocFil)-logMax);
			} else {
				if (ocFil<ocCorp){
//				  score1 = ocIn*Math.log(ocFil/ (ocCorp-ocFil));
				   score1 = ocIn*Math.log(ocFil/ocCorp);
				} else {
				  score1 = ocIn*Math.log(ocFil/ocCorp);
				}
			}
			score+=(float) score1;
//			Logging.finest("Word: "+word+" "+ocIn+" "+ocFil+" "+ocCorp+": score = "+score1);
		}
		return score;
	}
	
	public float comparePair(Hashtable<String,Hashtable<String,Integer>> inputPairs,
				Hashtable<String,Hashtable<String,Integer>> filterPairs){
		float score = 0.0f;
		Enumeration<String> words = inputPairs.keys();
		if (inputPairs.size()>filterPairs.size()){ words = filterPairs.keys(); }
		String word = null;
		int inputPairsSize = 0;
		for(Enumeration<String> eWords=inputPairs.keys(); eWords.hasMoreElements(); ){
			Hashtable<String,Integer> inSec = inputPairs.get(eWords.nextElement());
			for(Enumeration<String> eSec=inSec.keys(); eSec.hasMoreElements();
		     inputPairsSize+=inSec.get(eSec.nextElement()).intValue());
		}
		int filterPairsSize = 0;
		for(Enumeration<String> eWords=filterPairs.keys(); eWords.hasMoreElements(); ){
			Hashtable<String,Integer> inSec = filterPairs.get(eWords.nextElement());
			for(Enumeration<String> eSec=inSec.keys(); eSec.hasMoreElements();
		     	filterPairsSize+=inSec.get(eSec.nextElement()).intValue());
		}	
		if (inputPairsSize==0 || filterPairsSize==0){ return 0.0f; }
		CorporaOccurancesPairs occs = corpora.getOccurancesPairs(language);
		double logMax = -Math.log(corpora.getTotal(language)); // Maximum occurance boost
		int wordCount=0;
		while(words.hasMoreElements()){
			word = words.nextElement();
			Hashtable<String,Integer> shashIn = inputPairs.get(word);
			Hashtable<String,Integer> shashFil = filterPairs.get(word);
//			Logging.finest(word+" "+occIn+" "+occFil);
			if (shashIn==null || shashFil==null){ continue; }
			Enumeration<String> secWord = shashIn.keys();
			while(secWord.hasMoreElements()){
				String sWord = secWord.nextElement();
				Integer ioccIn = shashIn.get(sWord);
				Integer ioccFil = shashFil.get(sWord);
				if (ioccFil==null) continue;
				float ocIn = ioccIn.floatValue()/inputPairsSize;
				float ocFil = ioccFil.floatValue()/filterPairsSize;
				if (ocFil==0.0) continue; 
				wordCount++;
				float ocCorp = occs.getOccurancePairs(word,sWord);
	// Total score is the score for scalar product of the occurance between the test and the filter
	// multipled by the log factor for the word to be in the filter but not the general corpora
				double score1=0;
				if (ocCorp==0.0){ // If not in corpora we assume its due to being common words, so no score
//					score1 = ocIn*(Math.log(ocFil)-logMax);
				} else {
					if (ocFil<ocCorp){
//						score1 = ocIn*Math.log(ocFil/ (ocCorp-ocFil));
						score1 = ocIn*Math.log(ocFil/ocCorp);
					} else {
						score1 = ocIn*Math.log(ocFil/ocCorp);
					}
				}
				score+=(float) score1;
//				Logging.finest("Pair: "+word+" "+sWord+" :"+ocIn+" "+ocFil+" "+ocCorp+": score = "+score1);
			}
		}
		return score;
	}
	
	public boolean ARCompare(ParsedBuffer inputData, int feedid){
		int secs=0;
		while(!ARFilterStore.filterReady(feedid) && secs<60){
			try {
			Thread.sleep(1000);
			} catch (Exception e){}
			secs++;
		}
		if (secs>59){ Logging.warning("Waited over 1 minute for ARFilter for job "+feedid); return false; }
		Logging.info("comparing with hash of "+inputData.getSourceURL());
		byte[] mdURL = md5.MDString8(inputData.getSourceURL());
		String sourcedby = "";
		ParsedItem head = inputData.getHead();
		if (head!=null){ sourcedby=head.sourceurl; } else { 
			System.err.println("Missing head for "+inputData.getSourceURL());
			Logging.warning("Missing head for "+inputData.getSourceURL());
		}		
		if (ARFilterStore.preAccepted(feedid, mdURL)){
			    Logging.fine("job "+feedid+" items was listed to accept");
				ReportStore.report(inputData.getSourceURL(), sourcedby
					, ReportAction.COMPARE_PASSED, feedid, inputData.getItem().trimData(),"");
				return true;
		}
		if (ARFilterStore.preRejected(feedid, mdURL)){
			Logging.fine("job "+feedid+" items was listed to reject");
			ReportStore.report(inputData.getSourceURL(), sourcedby
				, ReportAction.COMPARE_FAILED, feedid, inputData.getItem().trimData(),"");
			return true;
	    }
		float bestAcc=-1000, bestRej=-1000;		
		Vector<WordsItem> acc = ARFilterStore.getAcceptedWordsItem(feedid);
		Vector<WordsItem> rej = ARFilterStore.getRejectedWordsItem(feedid);
		if (acc==null){
			Logging.warning("No Accepted Items List for job:"+feedid);
		} else {
	  	  for(Enumeration<WordsItem> e=acc.elements(); e.hasMoreElements(); ){
			WordsItem wi = (WordsItem) e.nextElement();
			float sScore = compare(inputData.getWords(),wi.getWords());
			float dScore= comparePair(inputData.getPairs(), wi.getPairs());
			float score = sScore+2.0f*dScore;			
            if (score>bestAcc){ bestAcc=score; }
		  }
		}
		if (rej==null){
			Logging.warning("No Rejected Items List for job:"+feedid);
		} else {
		  for(Enumeration<WordsItem> e=rej.elements(); e.hasMoreElements(); ){
			WordsItem wi = (WordsItem) e.nextElement();
			float sScore = compare(inputData.getWords(),wi.getWords());
			float dScore= comparePair(inputData.getPairs(), wi.getPairs());
			float score = sScore+2.0f*dScore;			
            if (score>bestRej){ bestRej=score; }
		  }
		}
		float score = bestAcc-bestRej;
		float arratio = ARFilterStore.getARRattio(feedid);
		float ave = arratio*(bestAcc+bestRej)/4.0f;
		if (score-ave>0.0f){
			Logging.fine("job "+feedid+" best accept match scored "+bestAcc+" reject scored "+bestRej+": accepting");
				ReportStore.report(inputData.getSourceURL(), sourcedby
					, ReportAction.COMPARE_PASSED, feedid, inputData.getItem().trimData(),"");
				return true;	
		} else {
			Logging.fine("job "+feedid+" best accept match scored "+bestAcc+" reject scored "+bestRej+": rejecting");
			ReportStore.report(inputData.getSourceURL(), sourcedby
					, ReportAction.COMPARE_FAILED, feedid, inputData.getItem().trimData(),"");
				return true;			
		}
	}
	
}
