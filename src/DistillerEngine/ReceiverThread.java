package DistillerEngine;

import java.io.*;
import java.util.*;
import java.net.URL;


public class ReceiverThread implements Runnable {

  private boolean notStopped = false;	
  private int  maxParseBufferLength = 0;
  private int receivedCount = 0;
  
  
  ParsedReceiver parsedReceiver;
  ParserLoader parserLoader;
  Config conf;
  
  private Hashtable<String,ParsedItem> headHash; // sometimes used to get the head items for a particular url
  private Hashtable<String,Integer> headCount; // counts number of insertion of same item to headHash
  
  public List<SpiderThread> watchList = new LinkedList<SpiderThread>();
  
  private Thread runner;
  
  public ReceiverThread(ThreadGroup ptg, ParserLoader pl, Config conf){
    this.parserLoader = pl;
    this.conf = conf;
    headHash = new Hashtable<String,ParsedItem>();
    headCount = new Hashtable<String,Integer>();
    maxParseBufferLength = conf.engineSpiderBuffers;
  }
  
  public void setBufferSize(int size){
	  maxParseBufferLength=size;
  }
  
  public void stop(){notStopped = false;}
  
  public void start(ThreadGroup ptg){
	runner = new Thread(ptg,this,"A ReceiverThread");
    notStopped = true;
    runner.start();
  }
  
  public void addWatched(SpiderThread st){
	watchList.add(st);  
  }
  
  public void setParsedReceiver(ParsedReceiver pr){
	parsedReceiver = pr;
  }

  public void setHead(String url, ParsedItem pi){
	  // Upgraded to cope with multiple feeds using the same page.
	  synchronized(headCount){
		  headHash.put(url,pi);
		  Integer in = headCount.get(url);
		  if (in==null){ headCount.put(url,new Integer(1)); } else {
			  headCount.put(url, new Integer(1+in.intValue()));
		  }
	  }
	  
  }
  
  public void removeHead(String url){
	  // Upgraded to cope with multiple feeds using the same page.
	  int n=0;
	  synchronized(headCount){
		  Integer in = headCount.get(url);
		  if (in==null){ return; }
		  n = in.intValue()-1;
		  if (n<1){ headCount.remove(url); } else {
			  headCount.put(url, new Integer(n));
		  }
	  }
  }
  
  public int getReceivedCount(){
	  int temp = receivedCount;
	  receivedCount=0;
	  return temp;
  }
  
  public void run(){
	while(notStopped){  
	  for(int i=0;i<watchList.size();i++){
        SpiderThread st = (SpiderThread) watchList.get(i);
        FlyBall fb = null;
	    if ((fb=st.getFlyBall())==null){continue; }
	    parse(fb);
	    receivedCount++;
	  }	
	  try {
        Thread.sleep(100);
	  } catch (InterruptedException e){}
	}
  }

  public void parse(FlyBall fly){
      String encoding = fly.encoding;
      String type = fly.type;
      StringTokenizer tok = new StringTokenizer(type,";");
      if (tok.countTokens()>1){
        type = tok.nextToken();
        String var = tok.nextToken("=");
        if (var.length()<1){ return; }
        if (var.charAt(0)==';'){ var = var.substring(1); }
        int j=0;
        for(int i=0; i<var.length();i++){
          if (var.charAt(i)==' '){ j++;} else { break;}
        }
        var = var.substring(j);
        if (var.equalsIgnoreCase("charset")){
          encoding = tok.nextToken();
        }
      }
      Parser parser = parserLoader.getParser(type);
      String linkname = fly.beginLink;
      if (parser == null){
    	Logging.info("No parser for type "+type+" at href="+fly.beginLink);
        ReportStore.report(fly.beginLink,"", ReportAction.PARSE_FAILED, fly.taskId," Unknown file type");
        fly.spider.addLinks();
        return;
      } else {
        String filename = fly.url.toExternalForm();
//        System.err.println("Parsing "+filename);
        conf.configureParser(parser,filename);
        parser.reset();
        parser.setEncoding(encoding);
        Thread.yield();
        Logging.fine("Starting Parsing "+filename+" with "+parser.getClass().getName());
        if (parser.useReader){
          Reader reader;
          try {
            reader = new BufferedReader(new InputStreamReader(
              new ByteArrayInputStream(fly.inputbuffer,0,fly.bufferLength),
                encoding),1024);
          } catch (UnsupportedEncodingException e){
            reader = new BufferedReader(new InputStreamReader(
              new ByteArrayInputStream(fly.inputbuffer,0,fly.bufferLength)),1024);
          }
          try {
            parser.open(reader);
          } catch (Exception e){
            ReportStore.report(linkname,"", ReportAction.PARSE_FAILED, fly.taskId, "Couldn't parse the file");
            System.err.println( e.toString()+" parsing "+filename);
	        Logging.warning(e.toString()+" parsing "+filename, e);
          }
        } else {
          InputStream is = new OpenByteArrayInputStream(fly.inputbuffer,0,fly.bufferLength);
          try {
            parser.open(is);
          } catch (Exception e){
              ReportStore.report(linkname,"", ReportAction.PARSE_FAILED, fly.taskId,"Couldn't parse the file");
        	  Logging.warning(e.toString()+" parsing "+filename, e);
            }
          }
        }
        if (parser instanceof SelectParser){ parser=((SelectParser) parser).getParser(); }
        Thread.yield();
        String filename = fly.url.toExternalForm();
      	parser.setLanguageCode(fly.lang);
        parser.langAllow = fly.langAllow;
        parser.setFrameInfo(fly);
        fly.spider.addLinks();
	      long lastModified = fly.lastModified;
	      lastModified = 3600*(lastModified /3600); // To nearest hour
        parser.setLastModified(fly.lastModified);
/*
 *   Replace URL was present in RemoteSearch, would break the recognising finished jobs in BlogDistiller
 * 
 *         Parser replaceURLParser = parser.getFieldParser("replaceURL");
        if (replaceURLParser!=null && replaceURLParser.numberOfWords()>0){
          String s  =  (String) replaceURLParser.words.elementAt(0);
          try {
            java.net.URL u = new java.net.URL(s);
            filename = u.toExternalForm();
          } catch (java.net.MalformedURLException e){
            Logging.warning("Could not change filename to "+s+": bad URL");
          }
        }
*/
  
  
//        if (parser.shouldIndex()){

          if (parser instanceof SectioningParser){
            SectioningParser sParser = (SectioningParser) parser;
            try {
              for(sParser.firstSection(); sParser.hasMoreSections(); sParser.nextSection()){
                String sectionName = sParser.getSectionName();
                addParsedFile(filename,linkname+"#"+sectionName, fly.taskId, fly.lang,parser);
              }
            } catch (NoMoreSectionsException e ){}
          } else {
            addParsedFile(filename,linkname,fly.taskId,fly.lang,parser);
          }
          Logging.fine("Finished parsing "+filename);
//        } else {
//          Logging.fine("Skipping (noindex) "+filename); 
//        }
        return;  
    }
   
// Filename, is the URL (as a string) of the current document
// While linkname is the orignal URL that caused it to be loaded, equal if no redirects

    public void addParsedFile(String filename, String linkname, int id,String lang, Parser parser){
      if (parsedReceiver==null){ Logging.warning("Parsed Receiver not set"); return; }
      if (filename.equals(linkname)){ Logging.info("Adding parsed file "+linkname+" via "+parser.getClass()); } else {
    	  Logging.info("Adding(redirected) parsed file "+filename+" from "+linkname+" via "+parser.getClass());
      }
      URL orig = null;
      try {
        orig = new URL(filename);
      } catch (Exception e){
    	  Logging.warning("Trying to add invalid URL "+filename);
    	  return;
      }
      if (parsedReceiver instanceof ParsedBuffer){
    	  
    	  int len = ((ParsedBuffer) parsedReceiver).getBufferLength();
    	  if (len>maxParseBufferLength){
    		 while(len>maxParseBufferLength){
    			 try {
    			    Thread.sleep(500); // Wait 1/2 sec for space
    			    Logging.info("Waiting for ParsedBuffer space "+filename);
    			 } catch (Exception e){}
    			 len = ((ParsedBuffer) parsedReceiver).getBufferLength();
    		 }
    	  }
      }
      parsedReceiver.nextItem();
      parsedReceiver.addSourceURL(linkname);
      ParsedItem pi = headHash.get(linkname);
      if (pi!=null){ parsedReceiver.setHead(pi); removeHead(linkname); }
      parsedReceiver.setId(id);
      parsedReceiver.addWords(parser.listWords(),parser.listScores());
      parsedReceiver.addLanguage(lang);
      parsedReceiver.addTitle(parser.getTitle());
      String desc;
      parsedReceiver.addDescription(desc=parser.getDescription());
//      Logging.finest(filename+": Description= "+desc); 
//  HTML, etc parsers        
      if (parser instanceof HyperTextParser){
    	Enumeration linke= ((HyperTextParser) parser).getLinks();
    	Vector<String> linkv = new Vector<String>();
    	while(linke.hasMoreElements()){
    		String lnk = (String) linke.nextElement();
    		if (lnk.startsWith("javascript")){ continue; }
      	  try {
    		  URL link = new URL(orig,lnk);
    		  linkv.add(link.toExternalForm());
    	  } catch (Exception e){
    		  Logging.finest("Couldn't add url "+lnk+" "+e);
    	  }    		
    	}
    	parsedReceiver.addLinks(linkv.elements());
//        parsedReceiver.addLinks( ((HyperTextParser) parser).getLinks());
      } else {
    	parsedReceiver.addLinks( (new Vector()).elements()); 
      }
      if (parser instanceof ImageParser){
    	  ImageParser img = (ImageParser) parser;
    	  String src = img.getImageURL();
    	  try {
    		  URL imgu = new URL(orig,src);
    		  src = imgu.toExternalForm();
    	  } catch (Exception e){}
    	  if (img.getImageHeight()*img.getImageWidth()>10000){
    		  parsedReceiver.addImageHeight(img.getImageHeight());
    		  parsedReceiver.addImageWidth(img.getImageWidth());
    		  parsedReceiver.addImageURL(src);
    	  }
      }
//   Feed Parser      
      if (parser instanceof FeedParser){
    	  FeedParser fp = (FeedParser) parser;
    	  Iterator<String> links = fp.elinks.iterator();
    	  Iterator<String> ids = fp.eids.iterator();
    	  Iterator<String> titles = fp.etitles.iterator();
    	  Iterator<String> summary = fp.esummary.iterator();
    	  Iterator<Date> edates = fp.edates.iterator();
    	  while(links.hasNext()){
    		  String link = links.next();
        	  try {
        		  URL imgu = new URL(orig,link);
        		  link = imgu.toExternalForm();
        	  } catch (Exception e){}
    		  parsedReceiver.addEntry(link, titles.next(), edates.next(), 
    				  summary.next(), ids.next());
    	  }
    	  parsedReceiver.addAuthor(fp.author, fp.authorEmail);
      }
    saveItem(id,filename,parsedReceiver.myItem());
    parsedReceiver.closeItem();
  }

  public void saveItem(int feedid,String url,ParsedItem pi){
  	// For overloading 
  }

    

}

