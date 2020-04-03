package DistillerEngine;

import java.util.*;
import java.util.logging.*;
import Distiller.Filter;
import java.io.*;

public class Engine implements Runnable {

/** Main Class of the Distiller Engine, Starts a cron style daemon that looks for ready
 *  to Download tasks in a Database and activates them.
*/	
	public static Config config;
	private boolean terminate;
	private Thread runner;
	public SpiderThread spiderThreads[];
	private int nSpiderThreads;
	private LoggingThreadGroup spiderGroup;
	private LoggingThreadGroup schedGroup;
	private LoggingThreadGroup processGroup;

	private Hashtable<Integer,Integer> activeJobs;
	private Hashtable<Integer,Long> jobStartTime;
	private Hashtable<Integer,String> jobNames;
	public ReceiverThread receiverThread;
	private BlogThreadDated blogThread;
	private FilterThread filterThread; 
	private FeedScheduler schd;
	private Presenter presenter; //
	private ARFilterMaker arFilterMaker;
	
	private ManagerThread managerThread;
	
	private Filters filters;
	
	private Engine(Config conf){
		config = conf;
		spiderThreads = new SpiderThread[nSpiderThreads = conf.engineSpiderThreads];
		filters = new Filters();
		activeJobs = new Hashtable<Integer,Integer>();
		jobStartTime = new Hashtable<Integer,Long>();
		jobNames = new Hashtable<Integer,String>();
		presenter = new Presenter(conf);
	}
	
	private void start(){
		schedGroup = new LoggingThreadGroup("Scheduler","schedulerLog",config.schedulerLevel);
		spiderGroup = new LoggingThreadGroup("spiderGroup","spiderGroupLog",config.spiderLevel);
		processGroup = new LoggingThreadGroup("processGroup", "processGroupLog",config.processLevel);
        java.util.logging.Handler processHand = new ConsoleHandler();   
		java.util.logging.Handler schedHand= new ConsoleHandler();
		java.util.logging.Handler spidHand = new ConsoleHandler();
		try {
			processHand = new java.util.logging.FileHandler(config.processLog,false);
			processGroup.addHandler( processHand);
		} catch (Exception e){ config.getLog().println("Problem openning process log: "+e); }
		try {
			schedHand = new java.util.logging.FileHandler(config.schedulerLog,false);
			schedGroup.addHandler(schedHand);
		} catch (Exception e){ config.getLog().println("Problem openning scheduler Log: "+e); }
		try {
			spidHand = new java.util.logging.FileHandler(config.spiderLog,false);
			spiderGroup.addHandler(spidHand);
		} catch (Exception e){ config.getLog().println("Problem openning spider Log: "+e); }
		runner = new Thread(schedGroup,this);
		ParserLoader pl  = new ParserLoader(config.parser_list,config.parser_dir);
        receiverThread = new ReceiverThread(processGroup, pl, config); // Allows caching of items
        ParsedBuffer buffy = new ParsedBuffer();
        blogThread = new BlogThreadDated(processGroup, buffy , config);
        receiverThread.setParsedReceiver(buffy);
        receiverThread.setBufferSize(config.blogSpiderBuffers);
		ParsedCache pCache = new ParsedCache();
		filterThread = new FilterThread(pCache, config);
		terminate=false;		
		runner.start();
		receiverThread.start(processGroup);
		blogThread.start(pCache,filters);	
		filterThread.start();
		managerThread = new ManagerThread(this,config);
		managerThread.setBlogThread(blogThread);
		managerThread.addThreads(this);
		managerThread.start();

	}
	
	public void stop(){
		terminate = true;
		receiverThread.stop();
		blogThread.stop();
		filterThread.stop();
	}
	
	@Override
	public void run(){
	  for(int i=0; i<nSpiderThreads;i++){
		 spiderThreads[i] = new SpiderThread(config,config.getLog(),spiderGroup);
		 receiverThread.addWatched(spiderThreads[i]);
	  }
	  schd = new FeedScheduler(config);
	  arFilterMaker = new ARFilterMaker(processGroup,schd,config);	  
	  while(!terminate){
         schd.readSchedule();
         if (schd.howManyTasks()==0){
        	 clean();
         }
         for(int i=0; i<schd.howManyTasks();i++){
        	 if (schd.shouldIRun(i)){
        		 SpiderTask task = schd.getSpiderTask(i);
        		 schd.getExpireTime(task.taskId); // so it is available statically
        		 String fType = schd.getFilterType(i);
        		 if (fType!=null && fType.equalsIgnoreCase("AR")){
        			 arFilterMaker.add(task.taskId);
        		 }
        		 SpiderTask filterTask = schd.getSpiderTaskForFilters(i);
        		 filterThread.addTask(filterTask);
        		 Filter[] fils = schd.getFilters(i);
        		 if (fils.length>0){
          		   filters.setFilters(fils[0].getFeedid(), fils);
        		 } else {
        			 schd.markTaskOff(task.taskId);
        			 continue;
        		 }
        		 if (!task.hasURLs()){ continue; }
        		 Logging.finer("Starting task "+i);
        		 boolean added=false;
        		 while(!added){
        			 for(int sp=0;sp<nSpiderThreads;sp++){
        				 if (spiderThreads[sp].addSite(task)){
        					 ReportStore.addJobPrimary(task.taskId,task.getURLs());
        					 addJob(task.taskId, schd.getFeedname(task.taskId));
        					 Logging.finer("Started task "+i+" "+task);
        					 schd.starting(i);
        					 added=true; break;
        			     }
        			 }
        			 if (!added){
        				 try {
        					 Thread.sleep(10000); // wait 10 secs for a free spider
        				 } catch (InterruptedException e){}
        			 }
        		 }
        	 }
         } // End Scheduled Item Loop
         // Check for finished jobs
         for(Enumeration<Integer> jobs=activeJobs.keys();jobs.hasMoreElements();){
        	 int job = jobs.nextElement().intValue();
        	 if (ReportStore.jobDone(job)){ finishedJob(job); }
         }
         
         try {
        	 Thread.sleep(10000); // Wait 1/2 minutes for next try
         } catch (InterruptedException e){}
	  }
	}

	private void addJob(int i, String jobName){
		Integer job = new Integer(i);
		activeJobs.put(job,job);
		jobStartTime.put(job, new Long(System.currentTimeMillis()));
		jobNames.put(job,jobName);
	}
	
	private void removeJob(int i){
		Integer job = new Integer(i);
		activeJobs.remove(job);
		jobStartTime.remove(job);
		jobNames.remove(job);
	}
	
// For Management thread
	
	public Set<Integer> getJobs(){
		return activeJobs.keySet();
	}
	
	public long getStartTime(Integer job){
		Long st = jobStartTime.get(job);
		if (st==null) return 0;
		return st.longValue();
	}
	
	public String getJobName(Integer job){
		return jobNames.get(job);
	}
	
	public FeedScheduler getScheduler(){
		return schd;
	}
	
	private void finishedJob(int job){
		Logging.info("Finished job "+job);
		ParsedBuffer sources = new ParsedBuffer();
// List Primary Pages, and initialize count
		Hashtable<String,Integer> sourceItems = new Hashtable<String,Integer>();
		Hashtable<String,Integer> totalItems = new Hashtable<String,Integer>();
		Hashtable<String,String> failures = new Hashtable<String,String>();
		for(Iterator<String> i=ReportStore.getPrimaryURLs(job);i.hasNext();){
			String url=i.next();
			sourceItems.put(url,new Integer(0));
			totalItems.put(url, new Integer(0));
			ReportItem ri = ReportStore.getReport(url, job);
			ReportAction ac = ri.getAction();
			if (ac.equals(ReportAction.DOWNLOAD_FAILED) || ac.equals(ReportAction.PARSE_FAILED)){
				String mess = ri.getWhy();
				if (mess!=null){ mess = ac.toString(); }
				failures.put(url,mess);
			} else {
				String action = ac.toString();
				Logging.info("job "+job+" (Pri) "	+" "+action+" "+url);
				sources.addItem(ri.getItem());
			}
		}
// Get minimum date for item expiration
		int ex = schd.getExpireTime(job);
		Date minDate = new Date(System.currentTimeMillis()-ex*1000L);
// List Secondary Pages
		int scanned =0;
		int found = 0;
		ParsedBuffer outputBuf = new ParsedBuffer();
		ParsedBuffer allItems = new ParsedBuffer();
		for(Iterator<String> i=ReportStore.getSecondaryURLs(job);i.hasNext();){
			String url = i.next();
			ReportItem it = ReportStore.getReport(url, job);
			String head = it.getMasterUrl();
			ReportAction act =  it.getAction();
			String action = act.toString();
			if (act.equals(ReportAction.COMPARE_PASSED) || act.equals(ReportAction.COMPARE_FAILED)){
               scanned++;
//  Load the date from the Atom/Rss head page into the current item               
               ParsedItem pi = it.getItem();
               if (pi.head!=null){
            	   Date d = pi.head.getEntryDate(url);
            	   if (d!=null){ pi.date = d; }
               }
               if (pi.date!=null && pi.date.before(minDate)) continue;
                  allItems.addItem(pi);
    		 	  if (head!=null){
    					Integer count = totalItems.get(head);
    					if (count!=null){
    						totalItems.put(head, new Integer(count.intValue()+1));
    					} else { totalItems.put(head, new Integer(1)); }
    				  }             
               
               if (act.equals(ReportAction.COMPARE_FAILED)){ continue; }
               outputBuf.addItem(pi);
               found++;
   		 	  if (head!=null){
				Integer count = sourceItems.get(head);
				if (count!=null){
					sourceItems.put(head, new Integer(count.intValue()+1));
				} else { sourceItems.put(head, new Integer(1)); }
			  }
            }
			Logging.info("job "+job+" (Sec) "+" "+action+" "+url+" , referedby "+head);
		}
//  Remove primary pages which didn't source any good links
		ParsedBuffer goodSources = new ParsedBuffer();
		ParsedBuffer allSources = new ParsedBuffer();
		while(sources.moreItems()){ // moreItems empties the top of the parsedBuffer
			allSources.addItem(sources.getItem());
			String url = sources.getSourceURL();
			Integer count = sourceItems.get(url);
			Logging.info("job "+job+" (Pri) "+url+" sourced "+count+" good links");
			if (count==null || count.intValue()==0){ continue; }
			ParsedItem item = sources.getItem();
			item.count = count.intValue();
			goodSources.addItem(item);
		}
// Sort the output pages by date
		Comparator<ParsedItem> dateOrder = new Comparator<ParsedItem>( ){
			public int compare(ParsedItem p, ParsedItem p1){
				if (p.date==null || p1.date==null ) return 0;
//				System.err.println(p.date+" <=> "+p1.date);
				return p.date.compareTo(p1.date);
			}
		};
		outputBuf.sort(dateOrder);
		allItems.sort(dateOrder);
		Comparator<ParsedItem> countOrder = new Comparator<ParsedItem>() {
			public int compare(ParsedItem p, ParsedItem p1){
		      if (p.count<p1.count){ return -1; }
		      if (p.count>p1.count){ return 1; }
		      return 0;
			}
		};
		goodSources.sort(countOrder);
//	Write the scores object to the DB
		Distiller.Scores sc = ReportStore.getScores(job);
		schd.setScores(sc,job);
// Clean up after the job
		removeJob(job);
		schd.updateFeed(job, scanned, found);
		schd.reportOnFeed(job, sourceItems, totalItems, failures);
		schd.stoppedRunning(job);
		Logging.info("job "+job+" finished: writing output feed");
		ReportStore.clearJob(job);
		ARFilterStore.removeFilter(job);
		Distiller.Presentation pres = schd.getPresentation(job);
		presenter.writeOutput(schd.getFeedname(job),pres,outputBuf,goodSources, Presenter.PASSED_MODE);
		presenter.writeOutput(schd.getFeedname(job),pres,allItems,allSources, Presenter.TOTAL_MODE);		
	}
	
	public void clean(){
	// Memory clean up goes here	
	}
	
	/**
	 * @param args 
	 */
	public static void main(String[] args) {
      if (args.length>=1){
    	  File f = new File(args[0]);
    	  if (f.exists()){
    		  config = new Config(args[0]);
    	  } else {
    		  config = new Config();
    	  }
    	  
      } else {
    	  config = new Config();
      }
      try {
    	  File logfile = new File(config.log);
    	  PrintWriter pr = new PrintWriter(new FileWriter(logfile));
    	  config.setlog(pr);
      } catch (Exception e){
    	  System.err.println("Couldn't open the log file: "+e);
          config.setlog(new PrintWriter(System.err));
      }
      Engine eng = new Engine(config);
      eng.start();
	}

}
