package DistillerEngine;



	import java.util.logging.ConsoleHandler;
	import java.util.logging.Level;
	import java.util.Iterator;
	import java.util.List;
	import java.util.Date;

	public class BlogThreadDated  implements Runnable {

// Plug in replacement for the BlogThread
		// This version only scanned items which have acceptable entry dates within
	    // the time set down, by the Feeds expiry time
		
		public static Config config;
		private boolean terminate;
		private Thread runner;
		public SpiderThread spiderThreads[];
		private int nSpiderThreads;
		private LoggingThreadGroup blogSpiderGroup;
		private LoggingThreadGroup blogReceiverGroup;
		private LoggingThreadGroup processGroup;
		private LoggingThreadGroup blogLogGroup;
	    private ParsedBuffer listBuffer, compareBuffer;
		public ReceiverThread blogReceiver;
		public ComparatorDThread comparator; 
	    
		public BlogThreadDated(LoggingThreadGroup pGroup, ParsedBuffer listBuffer, Config conf){
			config = conf;
			processGroup = pGroup;
			this.listBuffer = listBuffer; // Input Buffer items listing urls of blog items
			compareBuffer = new ParsedBuffer();
			spiderThreads = new SpiderThread[nSpiderThreads = conf.engineSpiderThreads];
		}
		
		public void start(ParsedCache pCache, Filters filters){
			blogReceiverGroup = new LoggingThreadGroup("blog receiver group","blogReceiverLog",config.blogReceiverLevel);
			blogSpiderGroup = new LoggingThreadGroup("blog spider group ","blogGroupLog",config.blogSpiderLevel);
			blogLogGroup = new LoggingThreadGroup("blog group","blog group log", config.blogLogLevel);
			java.util.logging.Handler schedHand= new ConsoleHandler();
			java.util.logging.Handler spidHand = new ConsoleHandler();
			java.util.logging.Handler blogHand = new ConsoleHandler();
			try {
				schedHand = new java.util.logging.FileHandler(config.blogReceiverLog,false);
				blogReceiverGroup.addHandler(schedHand);
			} catch (Exception e){ config.getLog().println("Problem openning scheduler Log: "+e); }
			try {
				spidHand = new java.util.logging.FileHandler(config.blogSpiderLog,false);
				blogSpiderGroup.addHandler(spidHand);
			} catch (Exception e){ config.getLog().println("Problem openning spider Log: "+e); }
			try {
				blogHand = new java.util.logging.FileHandler(config.blogLog,false);
				blogLogGroup.addHandler(blogHand);
			} catch (Exception e){ config.getLog().println("Problem openning Blog Log: "+e); }		
			runner = new Thread(blogLogGroup,this);
			ParserLoader pl = new ParserLoader(config.parser_list,config.parser_dir);
	        blogReceiver = new SavingReceiverThread(blogReceiverGroup, pl, config);
	        blogReceiver.setBufferSize(config.blogSpiderBuffers);
	        comparator = new ComparatorDThread(processGroup, compareBuffer , config);
	        blogReceiver.setParsedReceiver(compareBuffer);          
			terminate=false;
			for(int i=0; i<nSpiderThreads;i++){
			  spiderThreads[i] = new SpiderThread(config,config.getLog(),blogSpiderGroup);
	 	 	  blogReceiver.addWatched(spiderThreads[i]);
			}
			runner.start();
			blogReceiver.start(blogReceiverGroup);
			comparator.start( pCache, filters);
			
		}
		
		public void stop(){
			terminate = true;
			blogReceiver.stop();
		}
		

		
		public void run(){
			  while(!terminate){
				  boolean moreItems=false;
				  if (moreItems=listBuffer.moreItems()){
					  List<String> links = listBuffer.getLinks();
	// Remove any existing copies of the urls				  
					  int id = listBuffer.getId();
					  int extime = FeedScheduler.statGetExpireTime(id);
					  Date minDate = new Date(System.currentTimeMillis()-extime*1000L);
					  ReportStore.report(listBuffer.getSourceURL(), "", ReportAction.PARSE_COMPLETED, id, 
							  listBuffer.getItem(),"");
					  String source = listBuffer.getSourceURL();
					  ParsedItem pi = listBuffer.getItem();
					  for(Iterator<String> i=links.iterator(); i.hasNext(); ){
						  String url = i.next();
						  Date itemDate = pi.getEntryDate(url);
						  if (itemDate!=null && itemDate.before(minDate)){ i.remove(); continue; }
						  if (ReportStore.isPresent(id, url) || url.equals(source) ){
							  i.remove();
						  }
					  }
					  String urls[] = (String[]) links.toArray( new String[0] );
					  SpiderTask task = new SpiderTask(urls,id);
		              if (!task.hasURLs()){ continue; }
		              String turls[] = task.getURLs();
					  for(int i=0;i<turls.length;i++){
						  blogReceiver.setHead(turls[i], listBuffer.outItem);
					  }
		        	  Logging.finer("Starting task "+id);
		        	  // Start each task

		        	  boolean added=false;
		        	  while(!added){
		                for(int sp=0;sp<nSpiderThreads;sp++){
		        	      if (spiderThreads[sp].addSite(task)){
		        	   	    ReportStore.addJobSecondary(id,task.getURLs());  
		        		    Logging.info("Started task: "+id+", "+task.getURLs().length+" items\n"+task);
		        		    added=true; break;
		        		  }
		        	    }
		        	    if (!added){
		        		  try {
		        		    Thread.sleep(2000); // wait 2 secs for a free spider
		        	      } catch (InterruptedException e){}
		        	    }
		              }				  
				  }
				  if (!moreItems){
		            try {
		        	  Thread.sleep(500); // Wait 1/2 sec for next try
		            } catch (InterruptedException e){}
				  }
			  } 
			}
		
	}
	

