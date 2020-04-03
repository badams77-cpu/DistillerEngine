
package DistillerEngine;

import java.io.*;
import java.net.*;
import java.util.*;

public class SpiderTask {

  public URL[] startURLs;
  public URL[] ignorePatterns;
  public InetAddress ip;
  public int port;
  public String limitPattern;
  public String password;
  public String username;
  public String lang;
  public String charset;
  public String langAllow[];
  public int maxDepth;
  public int maxPages;
  public int taskId;

  public SpiderTask(){
  }

  public SpiderTask(String[] startURLs, String limitPattern,String ignorePatterns[], int maxDepth, int maxPages)
     throws MalformedURLException {
   
    this.startURLs = new URL[startURLs.length];
    this.ignorePatterns = new URL[ignorePatterns.length];
    for(int i=0; i<startURLs.length; i++){
      this.startURLs[i] = new URL(startURLs[i]);
    }
    for(int i=0; i<ignorePatterns.length; i++){
      this.ignorePatterns[i] = new URL(ignorePatterns[i]);
    }
    this.limitPattern = limitPattern;
    this.maxDepth = maxDepth;
    this.maxPages = maxPages;
    password = username = lang = charset = "";
    langAllow = null;
    taskId=-1;
  }

  public SpiderTask(String[] urls, int taskId){
	  Vector<URL> urlsVec = new Vector<URL>();
	  Hashtable<URL,URL> seenHash = new Hashtable<URL,URL>();
	  Hashtable<String, String> seenString = new Hashtable<String,String>();
	  for(int i=0;i<urls.length;i++){
		  String s = urls[i];
		  if (s.endsWith("/")){ s=s.substring(0,s.length()-1); }
		  if (seenString.contains(s)){ continue; }
		  seenString.put(s,s); // This protects against http://fd.com and http://fd.com/ both being added
		  try {
			  if (!checkProto(urls[i])){ continue; }
			  URL u = new URL(urls[i]);
			  
		//	  Logging.finest("Adding url "+u);
			  if (!seenHash.contains(u)){
			    urlsVec.add(u);
			    seenHash.put(u,u);
			  } 
		  } catch (MalformedURLException e){
			ReportStore.report(urls[i], "", ReportAction.DOWNLOAD_FAILED, taskId, "Bad URL");
			Logging.info("Bad url "+urls[i]);
		  }
	  }
	  this.startURLs = new URL[urlsVec.size()];
	  urlsVec.copyInto(this.startURLs);
	  limitPattern = "";
	  this.maxDepth=3;
	  this.maxPages = urlsVec.size();
	  this.taskId=  taskId;
	    password = username = lang = charset = "";
  }

  public SpiderTask[] splitTask(){
	  SpiderTask[] tasks = new SpiderTask[startURLs.length];
	  for(int i=0;i<startURLs.length;i++){
		  tasks[i]= new SpiderTask();
		  tasks[i].startURLs = new URL[1];
		  tasks[i].startURLs[0] = startURLs[i];
		  Logging.fine("split task "+startURLs[i]);
		  tasks[i].limitPattern = "";
		  tasks[i].maxDepth = 3;
		  tasks[i].maxPages = 1;
		  tasks[i].taskId = taskId;
		  tasks[i].password = "";
		  tasks[i].username = "";
		  tasks[i].lang = "";
		  tasks[i].charset = "";
	  }
	  return tasks;
  }
  
  public boolean hasURLs(){
	if (startURLs==null){ return false; }
	return startURLs.length>0;  
  }
  
  public String[] getURLs(){
	  Vector stringVec = new Vector<String>();
	  for(int i=0;i<startURLs.length;i++){
		  stringVec.add(startURLs[i].toExternalForm());
	  }
	  String[] out = new String[stringVec.size()];
	  stringVec.copyInto(out);
	  return out;
  }
  
  public String toString(){
	StringBuffer sb = new StringBuffer();
	for(int i=0;i<startURLs.length;i++){
		if (i!=0){ sb.append("\n"); }
		sb.append(startURLs[i]);
	}
	return sb.toString();
  }
  
  public boolean checkProto(String u){
// 	  Ignore protocol if return false 
	  if (u.length()>7 && u.substring(0,7).toLowerCase().equals("mailto:")){ return false; }
	  return true;
  }

}