package DistillerEngine;

import java.util.*;
import java.util.logging.ConsoleHandler;

public class ManagerThread implements Runnable {

// Periodicly runs to check upon the other threads
	
	private Engine engine;
	private Thread runner;
	private BlogThreadDated blogThread;
	private ComparatorDThread compThread;
	private LoggingThreadGroup manGroup;
	private Config config;
	private Hashtable<Integer,Integer> beenMarked;
	private LinkedList<SpiderThread> spiders;
	private LinkedList<String> spiderType;
	private LinkedList<ReceiverThread> receivers;
	private LinkedList<String> recType;
	
	public ManagerThread(Engine eng, Config conf){
	  engine = eng;	
	  config = conf;
	  spiders = new LinkedList<SpiderThread>();
	  receivers = new LinkedList<ReceiverThread>();
	  spiderType = new LinkedList<String>();
	  recType = new LinkedList<String>();
	  beenMarked = new Hashtable<Integer,Integer>();
	}
	
	public void addThreads(Engine eng){
		addSpiders(eng.spiderThreads,"engine");
		addReceiver(eng.receiverThread,"blog queue");
	}
	
	public void start(){
		manGroup = new LoggingThreadGroup("manager","Manager Log",config.managementLevel);
		java.util.logging.Handler manHand = new ConsoleHandler();
		try {
	       manHand = new java.util.logging.FileHandler(config.managementLog,false);
	       manGroup.addHandler(manHand);
	       System.out.println("Starting Management Thread");
		} catch (Exception e){ config.getLog().print("Problem openning process log"+e); }
		runner = new Thread(manGroup,this);
		runner.start();
	}

	public void setCompThread(ComparatorDThread coT){
		compThread = coT;
	}
	
	public void setBlogThread(BlogThreadDated blogT){
		blogThread=blogT;
		compThread=blogThread.comparator;
	    addSpiders(blogThread.spiderThreads,"blog");
	    addReceiver(blogThread.blogReceiver,"comp queue");
	}
	
	private void addSpiders(SpiderThread spid[],String type){
		for(int i=0;i<spid.length;i++){
			spiders.add(spid[i]);
			spiderType.add(type);
		}
	}
	
	private void addReceiver(ReceiverThread rt,String type){
		receivers.add(rt);
		recType.add(type);
	}
	
	public void run(){
		while(true){
		  try {
			Thread.sleep(config.managerRepeatTime);
		  } catch (Exception e){}
		  Set<Integer> jobs = engine.getJobs();
		  long now = System.currentTimeMillis();
		  for(Iterator<Integer> in = jobs.iterator(); in.hasNext();){
			  Integer job = in.next();
			  Long st = engine.getStartTime(job);
			  Date startDate = new Date(st.longValue());
			  if (now-engine.getStartTime(job)>config.slowJobTimeout && beenMarked.get(job)==null){
//				  engine.getScheduler().markTaskDown(job);
//				  beenMarked.put(job,job);
//				  Logging.warning("Slow job, reducing priority on "+engine.getJobName(job)+" feed "+job);
//				  ReportStore.describeJob(job.intValue());

				  Logging.warning("Slow job, halted "+engine.getJobName(job)+" feed "+job);
				  ReportStore.earlyTerminateJob(job.intValue());
				  
			  }
			  Logging.fine("Currently Running job "+engine.getJobName(job)+" feed "+job+" since "+startDate);
		  }
		  Logging.fine("Since last message compared "+compThread.getPagesCompared()+" pages");
		  if (compThread.getPagesNotCompared()!=0){
			  Logging.warning("failed to compared "+compThread.getPagesNotCompared()+" pages");
		  }
		  StringBuffer spidLog=new StringBuffer();
		  int sc =0;
		  int pages=0;
		  Iterator<String> spType = spiderType.iterator();
		  for(Iterator<SpiderThread> it= spiders.iterator();it.hasNext();){
			  SpiderThread spid = it.next();
			  String type = spType.next();
			  int page=spid.getPageCount();
			  pages+=page;
			  spidLog.append("SpiderThread ("+type+") "+(sc++)+": "+page+" pages, @"+spid.getName()+"\n"); 
		  }
		  spidLog.append("\n..Total pages downloaded: "+pages+"\n");
		  Logging.info(spidLog.toString());
		  StringBuffer reciLog = new StringBuffer();
		  int rc = 0;
		  pages = 0;	  
	 	  Iterator<String> reType  = recType.iterator();
		  for(Iterator<ReceiverThread> rt=receivers.iterator(); rt.hasNext();){
			  ReceiverThread rec = rt.next();
			  String type = reType.next();
			  int page = rec.getReceivedCount();
			  int bufsize=0;
			  if (rec.parsedReceiver instanceof ParsedBuffer){
				  ParsedBuffer pb = (ParsedBuffer) rec.parsedReceiver;
				  bufsize = pb.getBufferLength();
			  }
			  pages+=page;
			  reciLog.append("Receiver ("+type+") "+(rc++)+": read "+page+" and holding "+bufsize+" pages\n");
			  reciLog.append("Items are:-\n");
			  if (rec.parsedReceiver instanceof ParsedBuffer){
				  ParsedBuffer pb = (ParsedBuffer) rec.parsedReceiver;
				  Iterator<ParsedItem> ll = pb.fifo.iterator();
				  while(ll.hasNext()){
					  ParsedItem pi = ll.next();
					  reciLog.append("job: "+pi.id+" url: "+pi.sourceurl+"\n");
				  }
			  }			  
		  }
		  Logging.info(reciLog.toString());
		}
		  
	}
	
}
 