
package DistillerEngine;

import java.sql.DriverManager;
import java.sql.*;
import java.util.*;

import Distiller.Feed;
import Distiller.Input;
import Distiller.InputReport;
import Distiller.DbBean;
import Distiller.Filter;
import Distiller.Presentation;

public class FeedScheduler extends Scheduler {
	
	private Config conf;
	private List feeds;
	private DbBean dbBean;
	
	public static Hashtable<Integer,String> feedNameCache = 
		new Hashtable<Integer,String>();
	public static Hashtable<Integer,Integer> expireTimeCache =
		new Hashtable<Integer,Integer>();
	
	
	public FeedScheduler(Config conf){
		this.conf=conf;
		dbBean = new DbBean(conf.dbAddress,conf.dbDriver,conf.dbUser,conf.dbPass);
	}
	
	public void readSchedule(){
		feeds = null;
		long now = System.currentTimeMillis();
        try {
        	dbBean.connect();
        	feeds = dbBean.getBeans(Distiller.Feed.class, "select "+Distiller.Feed.fields+" from feeds where " +
        	  "regularality+last_updated<"+now+" and priority>=0 and nextBuild<"+now+" order by priority DESC, last_updated ASC"); 	
        } catch (Exception e){
        	conf.getLog().println("Couldn't Read Schedule from Database "+e);
        	Logging.severe("Couldn't Read Schedule from Database ",e);
        }
        if (feeds.size()>conf.maximumPrimaryTasks){
        	LinkedList<Distiller.Feed> newFeeds = new LinkedList<Distiller.Feed>();
            Iterator<Distiller.Feed> fi = (Iterator<Distiller.Feed>) feeds.iterator();
        	for(int x=0;x<conf.maximumPrimaryTasks && fi.hasNext();){
        		Feed f = fi.next();
        		if (!isRunning(f.getFeedid())){
        			newFeeds.add(f);
        			x++;
        		}
        	}
        	feeds = newFeeds;
        }
//        if (feeds.size()==0){ Logging.fine("no feeds ready"); } // Have a million log messages, no thanks
	}
	
	public void markTaskDown(int feedid){
		markTaskDown(feedid, 1, -1);
	}
	
	public void markTaskOff(int feedid){
		markTaskDown(feedid, -1, 600000);
	}
	
	private void markTaskDown(int feedid,int min, long wait){
		Feed feed;
		try {
			dbBean.connect();
			feed = (Distiller.Feed) dbBean.getBean(Distiller.Feed.class, 
					"select "+Distiller.Feed.fields+" from feeds where feedid="+feedid+";");
			int priority = feed.getPriority();
			if (priority>min){ priority--; } // Won't reduce it enough to stop it running
			if (wait<0){ wait = feed.getRegularality(); }
			long nextBuild=System.currentTimeMillis()+wait; // 10 minutes
			dbBean.updateSQL("update feeds set priority="+priority+", nextBuild="+nextBuild+
					" where feedid="+feedid);
		} catch (Exception e){
			conf.getLog().println("Couldn't read feed for Database"+e);
			Logging.severe("Mark Task Down, couldn't read feed from database",e);
		}
	}
	
	public int howManyTasks(){
	  if (feeds==null){ return 0; }
	  return feeds.size();
	}
	
	public boolean shouldIRun(int i){
		if (feeds==null){ return false; }
		int feednum = ( (Feed) feeds.get(i)).getFeedid();
		return !isRunning(feednum);
	}
	
	public void starting(int i){
		if (feeds==null){ return; }
		int feednum = ( (Feed) feeds.get(i)).getFeedid();
		markStarted(feednum);
	}
	
	public void stoppedRunning(int i){ // i is the Feed number matching the term in the db		
		markStopped(i);
	}
	
	public int updateFeed(int feednum, int numScanned, int numItems){
		int changed = 0;
		try {
			dbBean.connect();
			long time = System.currentTimeMillis();
			changed = dbBean.updateSQL("update feeds set no_scanned="+numScanned+", no_items="+
					numItems+", last_updated="+ time+" where feedId="+feednum+";");
		} catch (Exception e){
			Logging.severe("Database error writing feeds ",e);
			conf.getLog().println("Database writing feeds inputs"+e);			
		}
		return changed;
	}
	
	public SpiderTask getSpiderTask(int listpos){
	    if (feeds== null){ return null; } // Really should throw exception, event shouldn't occur though
		int feednum = ( (Feed) feeds.get(listpos)).getFeedid();
		String feedname = ( (Feed) feeds.get(listpos)).getFeedname();
		List inputs = null;
		String urls[]=new String[0];
		Hashtable<String,String> haveSeen=new Hashtable<String,String>();
    	Vector uv = new Vector();
    	Logging.finest("Listpos "+listpos+" ; Feednum "+feednum +" ; "+" Feedname "+feedname);
		try {
			dbBean.connect();
        	inputs = dbBean.getBeans(Distiller.Input.class, "select "+Distiller.Input.fields+" from inputs where " +
        	  "feedId="+feednum+";");
        	for (int i=0;i<inputs.size();i++){
        	  Input in = (Input) inputs.get(i);
        	  String u = in.getFeed_url();
        	  if (!haveSeen.containsKey(u)){
        	    uv.add(u);
        	    haveSeen.put(u,u);
           	    Logging.finer("Starting download of "+u);
        	  }
        	}
		} catch (Exception e){
			Logging.severe("Database error reading tasks ",e);
			conf.getLog().println("Database error reading tasks"+e);
		}
		urls = new String[uv.size()];
		uv.copyInto(urls);
		return new SpiderTask(urls,feednum);
	}
//	Amber
    public SpiderTask getSpiderTaskForFilters(int pos){
      if (feeds==null){ return null; }
      Feed fd = (Feed) feeds.get(pos);
      int feednum = fd.getFeedid();
      String feedname = fd.getFeedname();
      List filters = null;
      Vector myurls = new Vector();
		try {
			dbBean.connect();
        	filters = dbBean.getBeans(Distiller.Filter.class, "select "+Distiller.Filter.fields+" from filters where " +
        	  "feedId="+feednum+";");
        	for (int i=0;i<filters.size();i++){
        	  Filter fl = (Filter) filters.get(i);
        	  String u = fl.getFilter_url();
        	  myurls.add(u);
           	  Logging.finer("Added list for the downloading of filter pages "+u);
        	}
        	
		} catch (Exception e){
			Logging.severe("Database error reading inputs ",e);
			conf.getLog().println("Database error reading inputs"+e);
		}
		String url[] = new String[myurls.size()];
		myurls.copyInto(url);
		return new SpiderTask(url,feednum);
    }

    public String getFilterType(int pos){
      if (feeds==null){ return ""; }
      Feed fd = (Feed) feeds.get(pos);
      return fd.getFilterType();
    }
    
    
    public Filter[] getFilters(int pos){
        if (feeds==null){ return null; }
        Feed fd = (Feed) feeds.get(pos);
        int feednum = fd.getFeedid();
        String feedname = fd.getFeedname();
        List filters = null;
        Vector<Filter> myFilters = new Vector<Filter>();
  		try {
  			dbBean.connect();
          	filters = dbBean.getBeans(Distiller.Filter.class, "select "+Distiller.Filter.fields+" from filters where " +
          	  "feedId="+feednum+";");
          	for (int i=0;i<filters.size();i++){
          	  Filter fl = (Filter) filters.get(i);
              myFilters.add(fl);
          	}
          	Logging.finest("Found "+myFilters.size()+" filters for feed: "+feedname);
  		} catch (Exception e){
  			Logging.severe("Database error reading filters ",e);
  			conf.getLog().println("Database error reading filters"+e);
  		}
  		Filter fil[] = new Filter[myFilters.size()];
        myFilters.copyInto(fil);
        return fil;
      }
    
      public Presentation getPresentation(int feedid){
    	  Presentation pres =null;
    		try {
      			dbBean.connect();
                pres = (Presentation) dbBean.getBean(Presentation.class,
              			"select "+Distiller.Presentation.fields+" from presentation where " +
              	  "feedid="+feedid+";");     	
      		} catch (Exception e){
      			Logging.severe("Database error reading presentaion ",e);
      			conf.getLog().println("Database error reading presentaion "+e);
      		}
      		return pres;
      }
    
      public String getFeedname(int feedid){
    	  try {
    		  dbBean.connect();
    		  Feed feed = (Feed) dbBean.getBean(Feed.class, "select "+Feed.fields+" from feeds where feedid="
    				  +feedid+";");
    		  String feedName = feed.getFeedname();
    		  feedNameCache.put(new Integer(feedid), feedName);
    		  return feedName;
    	  } catch (Exception e){
      			Logging.severe("Database error reading feedname ",e);
      			conf.getLog().println("Database error reading feedname "+e);
          }
          return "";
      }
      

      public static String statGetFeedName(int feedid){
    	  return feedNameCache.get(new Integer(feedid));
      }
     
      public byte[][] getAccepted(int feedid){
    	  try {
    		  dbBean.connect();
    		  if (dbBean.existsSQL(
    				  "select feedid from arfilters where feedid="+feedid+"\n")){
    			  Object a = dbBean.readJavaObject("select accepted from arfilters where feedid="+feedid);
    		      try {
    		    	  return (byte[][]) a;
    		      } catch (Exception e){
    		    	  return new byte[0][];
    		      }
    		      
    		  } else {
    			  return new byte[0][];
    		  }
    			  
    	  } catch (Exception e){
    		  Logging.severe("Database error reading accepted items",e);
    		  conf.getLog().println("Database error reading Accepted Items"+e);
    	  }
    	  return new byte[0][];
      }

      public byte[][] getRejected(int feedid){
    	  try {
    		  dbBean.connect();
    		  if (dbBean.existsSQL("select feedid from arfilters where feedid="+feedid+";")){
    			  Object a = dbBean.readJavaObject(
    					  "select rejected from arfilters where feedid="+feedid);
    		      try {
    		    	  return (byte[][]) a;
    		      } catch (Exception e){
    		    	  return new byte[0][];
    		      }
    		      
    		  } else {
    			  return new byte[0][];
    		  }
    			  
    	  } catch (Exception e){
    		  Logging.severe("Database error reading accepted items",e);
    		  conf.getLog().println("Database error reading Accepted Items"+e);
    	  }
    	  return new byte[0][];
      }

      public float getARRatio(int feedid){
    	  try {
    		  dbBean.connect();
    		  return dbBean.readFloat("select arratio from arfilters where feedid="+feedid+";");
    	  } catch (Exception e){
    		  Logging.severe("Database error reading accepted items",e);
    		  conf.getLog().println("Database error reading Accepted Items"+e);
    		  return 0.0f;
    	  }
      }
      
      public int getExpireTime(int feedid){
    	try {
    		dbBean.connect();
    		Feed feed = (Feed) dbBean.getBean(Feed.class, "select "+Feed.fields+" from feeds where feedid="
    				+feedid+";");
    		Integer extime = new Integer(feed.getExpiry_time());
    		expireTimeCache.put(new Integer(feedid), extime);
    		return feed.getExpiry_time();
    	} catch (Exception e){
    		Logging.severe("Datebase error reading expire time",e);
    		conf.getLog().println("Database error reading expire time "+e);
    		return Integer.MAX_VALUE;
    	}   	
      }
      
      public static int statGetExpireTime(int feedid){
    	  Integer exT = expireTimeCache.get(new Integer(feedid));
    	  if (exT==null){ return 0; }
    	  return exT.intValue();
      }
      
      public void setScores(Distiller.Scores sc, int feedid){
    	  try {
    		  dbBean.connect();
    		  if (dbBean.existsSQL("select feedid from ScoreTable where feedid="+feedid+";")){
    		     dbBean.writeJavaObject(sc, "update ScoreTable set scores=? where feedid="+feedid);
    		  } else {
    		     dbBean.writeJavaObject(sc, "insert into ScoreTable (feedid,scores) values ("+feedid+",?);");
    		  }
    		  Logging.info("Wrote scores to DB for feedid="+feedid);
    	  } catch (Exception e){
      		Logging.severe("Datebase error seting the scores",e);
    		conf.getLog().println("Database error setting the scores "+e);    		  
    	  }
      }
    
      public void reportOnFeed(int job, Hashtable<String,Integer> sourcedTab, Hashtable<String,Integer> testedTab,
    		  					  Hashtable<String,String> failedTab){
    	try {
    	  dbBean.connect();
    	  dbBean.updateSQL("delete from inputrep where feedid="+job+";");
    	  for(Enumeration<String> e=testedTab.keys(); e.hasMoreElements();){
    		  String url = e.nextElement();
    		  Integer sourceCount = sourcedTab.get(url);
    		  int gooditems = sourceCount==null?0:sourceCount.intValue(); // Sad, but sourced in the engine, counts only the good items
    		  int sourced = testedTab.get(url).intValue(); // Dumb but sourced in the db, counts all the items
    		  String failed = failedTab.get(url);
    		  if (failed==null || failed.equals("")){ failed = "OK"; } // failure message or OK
    		  List oreps = dbBean.getBeans(InputReport.class, "select "+InputReport.fields+
    				  " from inputrep where feedid="+job+" and feed_url=\""+url+"\";");
    		  if (oreps.size()==0){
    			  dbBean.updateSQL(" insert into inputrep (feedid, feed_url, failed, sourced, gooditems) values ("
    					  +job+",\""+url+"\",\""+failed+"\","+sourced+","+gooditems+");" );
    		  } else {
    			  InputReport rep = (Distiller.InputReport) oreps.get(0);
    			  int inrid = rep.getInrid();
    			  dbBean.updateSQL(" update inputrep set failed=\""+failed+"\", sourced="+sourced+
    					  ",gooditems="+gooditems+" where inrid="+inrid+";");
    		  }
    	  }
        } catch (Exception e){
              Logging.severe("Datebase error reporting on feed: "+job,e);
             conf.getLog().println("Database error reporting on feed: "+job+", "+e);    		  
        }
      }
      
      
}



