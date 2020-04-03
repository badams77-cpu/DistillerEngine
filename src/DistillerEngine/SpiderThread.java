package DistillerEngine;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

public class SpiderThread implements Runnable {

//  Collects and Buffers files ready to index
  Config conf;
//  MTRemoteIndexer mtri;

  private String agentName = "DistillerEngine 0.1";

  public static final String CopyRight =
    "Exclusive Copyright of Barry David Ottley Adams 2007 all Rights Reserved";


  private int status;
  private int linksdone;
  private boolean running;
  private Thread runnerThread;

  private URL startURL;
  private URL currURL;
  protected String limitPattern;
  protected URL excludePatterns[];
  private HashMap leafMap;

  private Stack toloadStack;
  private Hashtable bodyHashes;
  private Hashtable visited;
  
  private Hashtable redirects;
  
  protected Hashtable depth;

  private Hashtable typeOK;

  private boolean spiderDepthSort;
  private int spiderDepth;
  private int debugLevel;
  private int maxPages = 0;
  private String lang;
  private boolean headerlessOK = false;

  private FlyBall buffers[];
  private String encoding;

  private int bufferpos = 0;
  private boolean useBodyHashing = false;
  private MessageDigest md;

  private Object bufferMonitor;
  private long bufferMemory;

  private PrintWriter log;
  private MimeTypes mimeTypes;

  private long downloadstart=0;
  private URLConnection connection;
  private InputStream is;
  private String threadName;
  private CookieManager cookieManager;
  private String langAllow[];
  private ParserLoader parserLoader;
  private LoggingThreadGroup threadGroup;
  
  private int taskId = 0;
  private int timeout;
  
  private int pageCount = 0;

  public static final int EMPTY = 0;
  public static final int FINISHED = -1;
  public static final int DOWNLOADING = 1;
  public static final int JUSTSTARTING = 2;
  public static final int AWAITINGLINKS = 3;
  public static final int AWAITINGREAD = 4;
  public static final int ERROR = 5;
  public static final int KILLED = 6;



  public SpiderThread(Config conf, PrintWriter log, LoggingThreadGroup threadGroup){
    this.conf = conf;
    this.log = log;
    status = EMPTY;
    running = false;
    int Nbuffers = conf.spiderBuffers;
    buffers = new FlyBall[Nbuffers];
    this.threadGroup = threadGroup;
    bufferMemory = conf.spiderMemory;
    spiderDepth = conf.spiderDepth;
    debugLevel = conf.debugLevel;
    timeout = conf.spiderTimeout;
    typeOK = new Hashtable();
    bufferMonitor = new Object(); // Just used to synchronized blocks
    bufferpos = 0;
    if (!conf.useBodyHashing.equals("")){
      useBodyHashing = true;
      try {
        md = MessageDigest.getInstance("MD5");
      } catch (NoSuchAlgorithmException e){
	Logging.warning(" MD5 Algorithm not found BodyHashing switch off");
        System.err.println(" MD5 Algorithm not found BodyHashing switch off");
	      useBodyHashing = false;
      }
    }
    if (conf.allowHeaderLessHTTP.equalsIgnoreCase("true")){
      headerlessOK = true;
    } else {
      headerlessOK = false;
    }
    if (conf.spiderDepthSort.equalsIgnoreCase("true")){
      spiderDepthSort = true;
    } else {
      spiderDepthSort = false;
    }
    if (!conf.agentName.equals("")){
      agentName = conf.agentName;
    }
    mimeTypes = new MimeTypes(conf.mimetypes,conf.def_type);
    parserLoader = new ParserLoader(conf.parser_list,conf.parser_dir);
    for(Enumeration e = parserLoader.allOKTypes();e.hasMoreElements();){
      String s = (String) e.nextElement();
      typeOK.put(s,s);
    }
    pageCount=0;
  }

  public boolean addSite( URL startURL[], String limitPattern, URL excludePatterns[]){
    return addSite(startURL, limitPattern,excludePatterns, 0,0,"","",null,null, 0);
  }

  public boolean addSite(SpiderTask st,HashMap leafMap){
    return addSite(st.startURLs, st.limitPattern, st.ignorePatterns, st.maxDepth,
      st.maxPages, st.lang, st.charset, leafMap, st.langAllow, st.taskId);
  }

  public boolean addSite(SpiderTask st){
    return addSite(st.startURLs, st.limitPattern, st.ignorePatterns, st.maxDepth,
      st.maxPages, st.lang, st.charset, null, st.langAllow, st.taskId);
  }

  public boolean addSite(URL startURL[], String limitPattern, URL excludePatterns[],
      int spiderDepth, 
      int maxPages, String lang,String charset,HashMap leafMap, String langAllow[], int taskId){
    if (status != EMPTY){ return false;}
    if (running){ return false;}
    this.taskId = taskId;
    this.startURL = startURL[0];
    this.limitPattern = limitPattern;
    this.excludePatterns = excludePatterns; 
    this.lang = lang;
    this.leafMap = leafMap;
    this.langAllow = langAllow;
    if (!charset.equals("")){
      encoding = charset;
    } else {
      encoding = Config.config.charSet;
    }
    if (spiderDepth!=0){
      this.spiderDepth = spiderDepth;
    } else {
      this.spiderDepth = Config.config.spiderDepth;
    }
    this.maxPages = maxPages;
    toloadStack = new Stack();
    for(int i=0;i<startURL.length;i++){
      if (startURL[i]!=null){
        toloadStack.push(startURL[i]);
      }
    }
    bodyHashes = new Hashtable();
    visited = new Hashtable();
    depth = new Hashtable();
    redirects = new Hashtable();
    initializeDepths( startURL);
    downloadstart = 0;
    running = true;
//    runnerThread = new Thread(this,"Spider "+limitPattern);
     runnerThread = new Thread(threadGroup, this, threadName=("Spider "+startURL[0].getHost()) );
    cookieManager = new CookieManager();
    runnerThread.setDaemon(false);
    runnerThread.start();
    Logging.info("Starting Spider at "+startURL[startURL.length-1]);
    System.err.println("Starting Spider at "+startURL[startURL.length-1]);
    status = JUSTSTARTING;
    pageCount++;
    return true;
  }

  public void initializeDepths(URL[] startURL){
    for(int i=0;i<startURL.length;i++){
      depth.put(startURL[i],new Integer(1));
    }
  }

  public int getPageCount(){
	  int temp = pageCount;
	  pageCount=0;
	  return temp;
  }
  
  public int getStatus(){
    return status;
  }

  public boolean isRunning(){
    return running;
  }

  public FlyBall getFlyBall(){
    FlyBall out = null;
    synchronized (bufferMonitor) {
      int i = bufferpos-1;
      if (i>=0){
        bufferpos=i;
        out = buffers[0];
        System.arraycopy(buffers,1,buffers,0,buffers.length-1);
        buffers[buffers.length-1] = null;
      }
    }
    return out;
  }

  private boolean addFlyBall( FlyBall data){
    if (data == null){return true;}
    synchronized (bufferMonitor) {
      int i = bufferpos+1;
      if (i>buffers.length){ return false; }
      buffers[bufferpos] = data;
      bufferpos = i;
    }
    return true;
  }

  private long bufferSize(){
    long size = 0;
    synchronized (bufferMonitor){
      for(int i=0;i<buffers.length;i++){
        FlyBall fb = buffers[i];
        if (fb == null){ break;}
        size += fb.bufferLength;
      }
    }
    return size;
  }


  public void run(){
    int retries = 3;
    linksdone = 0;
    while( running && !toloadStack.empty()){
      while( running && !toloadStack.empty()){  // YES TWICE !
        is = null;
        status = DOWNLOADING;
        long lastModified = 0;
        URL currLink;
        synchronized(toloadStack){
          currLink = (URL) toloadStack.pop();
        }
        Logging.finest("Trying "+currLink);
        if (currLink==null){ break; }
        currURL = currLink;
        String filename = currLink.toExternalForm();
//        We checked the limits before adding the link to the stack
        if (visited.get(filename) != null){
        	ReportStore.report(getBeginURL(filename),"", ReportAction.DOWNLOAD_FAILED, taskId,"Download aborted, visited page");
        	continue;
        }
	    if (leafMap !=null && leafMap.get(filename) != null){
	      visited.put(filename,filename);
	      if (log !=null){ log.println(filename +": not indexed is Leaf page: keeping old version");
	        Logging.finest(filename +": not indexed is Leaf page: keeping old version");
	      }
          System.err.println(filename +": not indexed is Leaf page: keeping old version");
          continue;
        }
        try {
          boolean postmode = false;
	        int pbegin;
          String postdata = null;
          if ( (pbegin = filename.indexOf("!POST!"))>=0){
            currURL = new URL(filename.substring(0,pbegin));
            postdata = filename.substring(pbegin+6);
            postmode = true;
          }
          HttpURLConnection huc = null;
          Logging.finest("opening connection "+currURL);
          connection = currURL.openConnection();
          connection.setConnectTimeout(timeout);
          connection.setReadTimeout(timeout);
          try {
            huc = (HttpURLConnection) connection;
          } catch (ClassCastException e){}
	          if (huc != null){ huc.setFollowRedirects(false); }
          if (postmode && huc != null){
            postMode(huc,currURL, postdata);
          } else {
            connection.setRequestProperty("User-Agent",agentName);
            if (conf.sendCookies){
              String cookies = cookieManager.getCookiesForURL(currURL);
              if (cookies != null){
                connection.setRequestProperty("Cookie",cookies);
              }
            }
            connection.setAllowUserInteraction(false);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(false);
            downloadstart = System.currentTimeMillis();
            Logging.finest(" connect "+currURL);
            connection.connect();
          }
          int length = connection.getContentLength();
          lastModified = connection.getLastModified();
          String type = connection.getContentType();
          int responseCode = 200;
          String cookie = connection.getHeaderField("Set-Cookie");
          if (cookie != null){
            cookieManager.setCookie(cookie);
          }
          boolean didRedirect = false;
          if (huc != null){ responseCode = huc.getResponseCode(); }
//          System.err.println("Response: "+responseCode);
          if (responseCode != 200 && responseCode !=300){
            if (responseCode != -1 || headerlessOK == false){
              if (huc != null && (responseCode == 301 || responseCode == 302 || responseCode == 303 || responseCode==307)){
                String location = huc.getHeaderField("Location");
                Logging.fine("Redirect,  location: "+location);
             //   System.err.println("Redirect,  location: "+location);
                if (log != null){ log.println("Redirect, location: "+location);}
                if (location != null && !location.equals("")){
                  URL addURL = new URL(currURL,location);
//                    if (checkLimit(addURL,limitPattern,excludePatterns)){
                  if (checkQualifiedURL(currURL,addURL)){
                  //    System.err.println("Adding "+addURL);
                	  redirects.put(addURL.toExternalForm(), currURL.toExternalForm());
                	  didRedirect = true;
                      toloadStack.push(addURL);
                  } else {
                	  Logging.info("Aborted Redirect, location: "+addURL);
                	  ReportStore.report(getBeginURL(filename),"",ReportAction.DOWNLOAD_FAILED, taskId,"Download failed, couldn't follow redirect");
                  }
                }
              }
              visited.put(filename,filename);
              if (!didRedirect){
            	  Logging.info(filename+" "+"Failed code "+responseCode);
            	  if (log !=null){ log.println(filename+" "+"Failed code "+responseCode); }
            	  ReportStore.report(getBeginURL(filename),"", ReportAction.DOWNLOAD_FAILED, taskId,"Download failed "+responseCode);
              }
              disconnect(connection);
              continue;
            }
          }
          if (length==0){
            visited.put(filename,filename);
	        Logging.info(filename+" Failed: file is Zero length");
            System.out.println(filename+" Failed: file is Zero length");
	        if (log !=null){ log.println(filename+" Failed: file is Zero length"); }
            disconnect(connection);
            ReportStore.report(getBeginURL(filename),"", ReportAction.DOWNLOAD_FAILED, taskId,"Download failed, zero length");           
            continue;
          }
          if (type == null || conf.useRemoteTypes.equals("false")){
	          String currURLS = currURL.toString();
	          URL currURL1 = currURL;
            int hashpos = currURLS.indexOf('?');
	          if (hashpos >0){
	            currURLS = currURLS.substring(0,hashpos);
	            try {
                currURL1 = new URL(currURLS);
	            } catch (Exception e){}
	          }
            type = getType(currURL1);
          }
	  int t;
	  if ( (t=type.indexOf(';')) >-1){
	    type = type.substring(0,t);
	  }
          Object temp = typeOK.get(type);
          if (temp == null){
            visited.put(filename,filename);
	        Logging.finest("Ignoring "+filename+" because its a "+type);
	        System.err.println("Ignoring "+filename+" because its a "+type);
            ReportStore.report(getBeginURL(filename),"", ReportAction.PARSE_FAILED, taskId,"Unknown file type: "+type);
            disconnect(connection);
            continue;
          }
          int space = (int) ( bufferMemory - bufferSize() );
          is = connection.getInputStream();
          byte[] bhbuffer;
          int pos = 0;
          int read = 0;
          if (length != -1){
            if (length > bufferMemory){
              retries = 0;
              ReportStore.report(getBeginURL(filename),"", ReportAction.DOWNLOAD_FAILED, taskId,"File too big");            
              throw new IOException(" File to large for the Spider - increase Spider Memory");
            }
            waitForSpace(length);
            bhbuffer = new byte[length];
            int remaining = length;
            while( (remaining>0 && (read = is.read(bhbuffer,pos,remaining)) >-1)){
              pos += read;
              remaining -= read;
            }
          } else {
            int blocks = 0;
            int totals = 0;
            Vector buffers = new Vector();
            Vector lengths = new Vector();
            int halfBufferMemory = (int) (bufferMemory/2);
            while( read != -1){
              byte[] tempbuffer = new byte[2048];
              read = is.read(tempbuffer);
              if (read != -1){
                buffers.addElement(tempbuffer);
                lengths.addElement(new Integer(read));
                blocks++;
                totals += read;
                if (totals > halfBufferMemory){
                  retries = 0;           
                  throw new IOException(" File to large for the Spider - increase Spider Memory");
                }
                waitForSpace(totals+2048);
              }
            }
            waitForSpace(totals*2);
            bhbuffer = new byte[totals];
            for(int j=0;j<blocks;j++){
              byte[] tempbuffer = (byte[]) buffers.elementAt(j);
              int len = ( (Integer) lengths.elementAt(j) ).intValue();
              System.arraycopy(tempbuffer,0,bhbuffer,pos,len);
              pos+= len;
            }
          }
          downloadstart = 0;
          disconnect(connection);
          connection = null;
          is.close();
          is = null;
          byte[] digest = null;
          if (useBodyHashing){
            md.reset();
            md.update(bhbuffer,0,pos);
	          digest = md.digest();
	          String digestst = new String(digest,0,digest.length);
   	        if (bodyHashes.get(digestst) != null){
		      Logging.finer("Repeated Page: "+currURL);
              System.err.println("Repeated Page: "+currURL);
	            if (log != null){log.println("Repeated Page: "+currURL);}
	            visited.put(filename,filename);
   	          continue;
  	        }
            bodyHashes.put(digestst,digestst);
          }
          visited.put(filename,filename);
          FlyBall in = new FlyBall(bhbuffer,pos,type,lastModified,currURL,this,lang,digest,
        		  getBeginURL(currLink.toExternalForm()),encoding,langAllow, taskId);
          
	      Logging.finest("Loaded "+currURL);

          while(!addFlyBall(in)){    // WAIT IF BUFFER FULL
            status = AWAITINGREAD;
            try {
              runnerThread.sleep(50); // Wait 1/20 sec
            } catch (InterruptedException e){}
          }
          Logging.finer("Got page "+currURL);
          linksdone++;
          if (--maxPages == 0){ 
	        Logging.info("Spider Hit maximum pages limit, finishing");
            System.err.println("Spider Hit maximum pages limit, finishing");
            running = false; 
          }
        } catch (IOException e){
          disconnect(connection);
          connection = null;
          boolean retry = true;
          if (e instanceof UnknownHostException) retry=false;
          if (retry && retries-- >0){
            toloadStack.push(currLink);
          } else {
	         Logging.warning("Failed to load "+currURL+"\n"+e);
            System.out.println("Failed to load "+currURL+"\n"+e);
	          if (log !=null){ log.println("Failed to load "+currURL+"\n"+e);}
	          ReportStore.report(getBeginURL(filename),"", ReportAction.DOWNLOAD_FAILED, taskId,e.getMessage());
            retries = 3;
          }
        }
        runnerThread.yield();
      }
      if (linksdone>0){
        status = AWAITINGLINKS;
        int timer = 100000;
        while(linksdone>0 && timer>0){
          try {
            runnerThread.sleep(20);
            timer--;
          } catch(InterruptedException e){}
        }
      }
    }
    status = EMPTY;
    running = false;
    runnerThread = null;
  }

  private void disconnect( URLConnection connection){
     try {
       HttpURLConnection huc = (HttpURLConnection) connection;
       huc.disconnect();
     } catch (Exception f){}
  }

  private void postMode (HttpURLConnection con,URL currURL, String data) throws IOException {
    con.setRequestProperty("User-Agent",agentName);
    con.setAllowUserInteraction(true);
    con.setUseCaches(false);
    con.setDoInput(true);
    con.setDoOutput(true);
    if (conf.sendCookies){
      String cookies = cookieManager.getCookiesForURL(currURL);
      if (cookies != null){
        connection.setRequestProperty("Cookie",cookies);
      }
    }
    downloadstart = System.currentTimeMillis();
    con.setRequestProperty("Content-Length", Integer.toString(data.length()));
//    con.connect();  // getOutputSteam does a content
    PrintStream out = new PrintStream(con.getOutputStream());
    out.print(data);
    out.close();
  }

  public String getBeginURL(String s){
// Trace back through the redirect until beginning url found	  
	  String url = s;
	  while(redirects.containsKey(url)){
		  String nurl = (String) redirects.get(url);
		  redirects.remove(url);
		  url = nurl;
	  }
	  // getBeginURL may only be called once per URL, then it removes the trail of URLs
	  // this prevents two different start URL being reported as the same url after a redirect to the same page
	  return url;
  }
  
  public void setFinished(){
    if (running || status !=EMPTY){return;}
    status = FINISHED;
  }

  public void stopSpider(){
    running = false;
  }

  public void waitForSpace(int length){
    int space = (int) ( bufferMemory - bufferSize() );
    if (space < length){
      status = AWAITINGREAD;
      while ( bufferMemory - bufferSize() < length){
        try {
          runnerThread.sleep(20);
        } catch (InterruptedException e){}
      }
    }
  }




  public void addLinks(){
    afterAddLinks();
    linksdone--;
//   The FlyBall consumer must call one of the addLinks() for each FlyBall
//  recieved, otherwise the spider cannot terminate    afterAddLinks();
  }

  public void addLinks(Parser parser,URL currLink){
    if (parser.hypertextparser){
      URL currURL = currLink;
      HyperTextParser htp = (HyperTextParser) parser;
      String targetBase = htp.getTargetBase();
      if (targetBase.equals("_self")){ targetBase = "";}
      String base = htp.getBase();
      URL baseURL = null;
      try {
        if (!base.equals("")){
          baseURL = new URL(currURL,base);
        }
      } catch (MalformedURLException e){}

//   Make the frameContext contain all valid target Names

      boolean isFrameSet = false;
//   End frameContext code

      for (Enumeration en=htp.getLinks();en.hasMoreElements();){
        Object link = en.nextElement();
        String relURL;
        boolean inFrameSet = false;
        if (link instanceof String){
          relURL = (String) link;
        } else {
          continue;
        }
        int hashpos = relURL.indexOf('#');
        if (hashpos == 0){continue;} // String begins with #
        if (hashpos >0){
          relURL = relURL.substring(0,hashpos);
        }
        try {
	  if (relURL.regionMatches(true,0,".http://",0,7)){
            throw new MalformedURLException();
          }
          URL addURL;
          if (baseURL == null ){
            addURL = new URL(currURL,relURL);
          } else {
            addURL = new URL(baseURL,relURL);
          }

// Debug
//          System.err.println("Adding "+relURL+" = "+addURL);
// End Debug


//      Code to deal with change in behaviour of URL("http://site/","../page.html") in 1.4

          String filex = addURL.getFile();
          if (filex.startsWith("/../")){
            addURL = new URL(addURL,filex.substring(3));
          }

//             End 1.4 fix

// !embed! support
          String aUs = addURL.toExternalForm();
          URL addURL1 = addURL;
          if (aUs.endsWith("!embed!")){
            aUs = aUs.substring(0,aUs.length()-7);
              try {
              addURL1 = new URL(aUs);
            } catch (MalformedURLException e){}
          }
//

          htp.addLinkField(addURL1); // Place the full URL into the Parser for indexing in the link: subindex

          if ( checkQualifiedURL(currURL, addURL1) ) {
            toloadStack.push(addURL1);
          }

        } catch (MalformedURLException e){}
      }
    }
    linksdone--;
    afterAddLinks();
  }

  protected void externalAddLink(URL u, int i){
    if (i>spiderDepth){ return; }
    depth.put(u, new Integer(i));
    toloadStack.push(u);    
  }

  protected boolean checkQualifiedURL(URL currURL, URL addURL1){ 
    if (!checkLimit(addURL1,limitPattern,excludePatterns)){
      return false;
    }
    if (visited.get(addURL1)!=null){ return false; } // Redirect to visited page
    return depthGuage(currURL,addURL1);
  }

  protected void  afterAddLinks(){
//  For overriding, everything going AOP
  }


  public long getdownloadtime(){
    if (status != DOWNLOADING){ return 0L;}
    if (downloadstart == 0L){ return 0L;}
    return System.currentTimeMillis() - downloadstart;
  }

/*
  public boolean checkTimeout(){
    long l = getdownloadtime();
    if (l>600000){
      try {
        if (connection != null){
          HttpURLConnection huc = (HttpURLConnection) connection;
	        huc.disconnect();
	        System.err.println("Loading Time out - Disconnecting");
        }
      } catch (Exception e){}
      try {
        if (is != null){
          is.close();
          System.err.println("Loading Time out - closing InputStream");
        }
      } catch (IOException e){}
    }
  }
*/

  public boolean checkTimeout(){
    long l = getdownloadtime();
    if (l>60000){
      if (runnerThread != null && connection != null){
	Logging.warning(getName()+" loading Time out");
        System.err.println(getName()+" loading Time out");
	runnerThread.setPriority(Thread.MIN_PRIORITY);
	running = false;
	status = ERROR;
	runnerThread.interrupt();
	return true;
      }
    }
    return false;
  }

  public String getName(){
    return threadName;
  }

  private String getType(URL url){
    return mimeTypes.getType(url.getFile());
  }

  protected boolean checkLimit(URL url,String limitPattern,URL ignorePatterns[]){
    String urlstring = url.toExternalForm();

    if ( WildCardMatch.wildCardMatchWildEnd(urlstring,limitPattern)){
      if (ignorePatterns!=null){
        for(int i=0;i<ignorePatterns.length;i++){
          String igPat = ignorePatterns[i].toExternalForm();
          if (WildCardMatch.wildCardMatchWildEnd(urlstring,igPat)){ return false; }
        }
      }
      return true;
    } else {
      return false;
    }
  }

  protected boolean depthGuage(URL currURL, URL addURL){
    Integer depInt = (Integer) depth.get(currURL);
    if (depInt == null){ Logging.warning(currURL+" not found in depth list");
      System.err.println(currURL+" not found in depth list"); return false;}
    int d = depInt.intValue();
    if (d >= spiderDepth){ return false;}
    d++;
    Integer addDepth = (Integer) depth.get(addURL);
    if (addDepth == null || addDepth.intValue()>d){
      depth.put(addURL,new Integer(d));
    }
    return true;
  }

  protected int getDepth(URL addURL){
    Integer addDepth = (Integer) depth.get(addURL);
    if (addDepth == null){ return 0; }
    return addDepth.intValue();
  }



}

class ConnectionStoppedException extends IOException {

  public ConnectionStoppedException(){}

}

