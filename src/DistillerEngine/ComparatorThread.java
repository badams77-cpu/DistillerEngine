package DistillerEngine;
import java.io.*;
import java.util.*;
import Distiller.Filter;

public class ComparatorThread implements Runnable {

	ParsedBuffer inputData;
	ParsedCache filterData;
	Config conf;
	CorporaNew corpora;
	LoggingThreadGroup ltg;
	Thread myThread;
	Filters filters;
	boolean terminate;
	
	public double scoreLimit = 0.5;
	
	String language = "en"; // Extend to multiple languages soon
	
	public ComparatorThread(LoggingThreadGroup ltg, ParsedBuffer inputData, Config conf){
		this.ltg=ltg;
		this.inputData = inputData;
		this.conf = conf;
		loadCorp(conf.corporaFile);
	}
	
	private void loadCorp(String corporaFile){
		try {
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(corporaFile),2048);
            ObjectInputStream ois = new ObjectInputStream(bis);
            corpora = (CorporaNew) ois.readObject();
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

	public void stop(){
		terminate = true;
	}
	
	public void run(){
		while(!terminate){
			boolean hasItems = false;
			if (inputData.moreItems()){
		        hasItems = true;
				int taskId = inputData.getId();
				Filter filter[] = filters.getFilters(taskId);
//				Logging.info(" found "+filter.length+" filter for task "+taskId);
				boolean haveTried[] = new boolean[filter.length];
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
							float score = compare(inputData.getWords(),filterData.getWords());
	  // Store result
							ReportStore.reportTest(taskId, filter[i].filterid, inputData.getSourceURL(), score);
							if (score>maxScore){maxScore = score; }
							Logging.fine("Score: "+score+" for "+inputData.getSourceURL()+" compare "+furl);
							if (score*filter[i].weighting>scoreLimit){ itemGood = true; nTried=filter.length; break; }
						} else {
							ReportAction ra = ReportStore.getReport(furl, taskId).getAction();
							if (ra.equals(ReportAction.DOWNLOAD_FAILED) || 
									ra.equals(ReportAction.PARSE_FAILED)){
								Logging.warning("Filter "+furl+" missing "+ra);
								haveTried[i]=true;
								missingFilter = true;
							} else {
								Logging.info(" waiting for filter "+i+" "+ra+", tried "+nTried);								
							}
						}

					}
					if (nTried<filter.length){
		// Wait for Filter to download				
						try {
							myThread.sleep(500);
						} catch (InterruptedException e){}
					}
				}
//				Logging.finer(" finished comparing "+inputData.getSourceURL());
				inputData.getItem().setScore(itemGood,maxScore);
				if (itemGood){
					ReportStore.report(inputData.getSourceURL(), inputData.getHead().sourceurl
							, ReportAction.COMPARE_PASSED, taskId, inputData.getItem(),"");

				} else {
					if (!missingFilter){
						ReportStore.report(inputData.getSourceURL(), inputData.getHead().sourceurl
							, ReportAction.COMPARE_FAILED, taskId, inputData.getItem(),"");
					} else {
						ReportStore.report(inputData.getSourceURL(), inputData.getHead().sourceurl
								, ReportAction.COMPARE_FAILED, taskId, inputData.getItem(),"");						
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
				  score1 = ocIn*Math.log(ocFil/ (ocCorp-ocFil));
				} else {
				  score1 = ocIn*Math.log(ocFil/ocCorp);
				}
			}
			score+=(float) score1;
			Logging.finest(word+" "+ocIn+" "+ocFil+" "+ocCorp+": score = "+score1);
		}
		return score;
	}
	
}
