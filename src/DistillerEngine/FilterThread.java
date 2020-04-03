package DistillerEngine;

import java.util.*;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.net.URL;

public class FilterThread implements Runnable {

	public static Config config;
	private boolean terminate;
	private Thread runner;
	private SpiderThread spiderThreads[];
	private int nSpiderThreads;
	private LoggingThreadGroup filterSpiderGroup;
	private LoggingThreadGroup filterReceiverGroup;
	private LoggingThreadGroup filterProcessGroup;
    private ParsedCache filterCache;
    private Stack taskStack;
	private ReceiverThread filterReceiver;
	
	public FilterThread(ParsedCache filterCache, Config conf){
		config = conf;
		this.filterCache = filterCache; // Input Buffer items listing urls of blog items
		spiderThreads = new SpiderThread[nSpiderThreads = conf.engineSpiderThreads];
		taskStack = new Stack();
	}
	
    public void addTask(SpiderTask task){
    	SpiderTask tasks[] = task.splitTask();
    	for(int i=0;i<tasks.length;i++){
    		Logging.fine("Adding filter task "+tasks[i].startURLs[0]);
    		taskStack.add(tasks[i]);
    	}
    }
	
	public void start(){
		filterReceiverGroup = new LoggingThreadGroup("filterReceiverGroup","filterReceiverLog");
		filterSpiderGroup = new LoggingThreadGroup("filterSpiderGroup","filterspiderGroupLog");
		filterProcessGroup = new LoggingThreadGroup("filter process group","filterProcessGroupLog");
		java.util.logging.Handler schedHand= new ConsoleHandler();
		java.util.logging.Handler spidHand = new ConsoleHandler();
		java.util.logging.Handler processHand = new ConsoleHandler();
		try {
			filterReceiverGroup.addHandler(schedHand = new java.util.logging.FileHandler(config.filterReceiverLog,false));
		} catch (Exception e){ config.getLog().print("Problem openning filter scheduler Log"+e); }
		try {
			filterSpiderGroup.addHandler(spidHand = new java.util.logging.FileHandler(config.filterSpiderLog,false));
		} catch (Exception e){ config.getLog().print("Problem openning fitler  spider Log"+e); }
		try {
			filterProcessGroup.addHandler(processHand = new java.util.logging.FileHandler(config.filterProcessLog,false));
		} catch (Exception e){ config.getLog().print("Problem openning filter process log"+e); }
		runner = new Thread(filterProcessGroup,this);
		ParserLoader pl = new ParserLoader(config.parser_list,config.parser_dir);
        filterReceiver = new ReceiverThread(filterReceiverGroup, pl, config);
        filterReceiver.setParsedReceiver(filterCache);          
		schedHand.setLevel(Level.FINEST);
		spidHand.setLevel(Level.FINEST);
		terminate=false;
		runner.start();
		filterReceiver.start(filterReceiverGroup);
	}
	
	public void stop(){
		terminate = true;
		filterReceiver.stop();
	}
	
	public void run(){
		  for(int i=0; i<nSpiderThreads;i++){
			 spiderThreads[i] = new SpiderThread(config,config.getLog(),filterSpiderGroup);
			 filterReceiver.addWatched(spiderThreads[i]);
		  }
		  while(!terminate){
			  boolean moreItems=false;
			  if (moreItems=!taskStack.empty()){
				  SpiderTask task = (SpiderTask) taskStack.pop();
	              if (!task.hasURLs()){ continue; }
	              URL filename = task.startURLs[0];
	              if (filterCache.contains(task.taskId,filename.toString())){ continue; }
	        	  Logging.finer("Trying to Start task Id="+task.taskId+", url"+task.startURLs[0]);
	         	  boolean added=false;
	        	  while(!added){
	        	    for(int sp=0;sp<nSpiderThreads;sp++){
	        		  if (spiderThreads[sp].addSite(task)){
	        			Logging.finest("Started task "+task.taskId+", url"+task.startURLs[0]);
	        		    added=true; break;
	        		  }
	        	    }
	        	  if (!added){
	        		try {
	        		  Thread.sleep(1000); // wait 1 secs for a free spider
	        	    } catch (InterruptedException e){}
	        	  }
	            }				  
			  }
			  if (!moreItems){
	            try {
	        	  Thread.sleep(200); // Wait 1/5 sec for next try
	            } catch (InterruptedException e){}
			  }
		  } 
		}
	
}
