package DistillerEngine;
import Distiller.Presentation;
import java.io.*;
import java.text.*;
import java.util.*;
// SAX imports for the XSLT transform
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

public class Presenter {

	public static int PASSED_MODE=0;
	public static int TOTAL_MODE=1;

	public static String SAXParserName = "javax.xml.parsers.SAXParser";
	
	
	Config conf;
	MD5 md5;
	
	public Presenter(Config conf){
		this.conf = conf;
		md5 = new MD5();
	}
	
	public void writeOutput(String feedname, Presentation pres, ParsedBuffer items, ParsedBuffer sources, int mode){
		File root = new File(conf.outputDir);
		if (!root.isDirectory()){
			Logging.severe("cannot find output directory" +
					conf.outputDir);
			return;
		}
		File outdir = new File(root,feedname);
		if (!outdir.exists()){
			if (!outdir.mkdir()){
				Logging.severe("couldn't make directory "+outdir.getAbsolutePath());
			}
		}
		File atomOut = new File(outdir,"atom.xml");
		if (mode==TOTAL_MODE){ atomOut = new File(outdir,"checked.xml"); }
		ParsedBuffer items1 = items.copyTo();
		ParsedBuffer sources1 = sources.copyTo();
		try {
		  PrintWriter pw = new PrintWriter(atomOut,conf.outputCharSet);	
		  writeAtom( pw, pres, items1, sources1,mode,feedname);
		  pw.close();
		} catch (IOException e){
			Logging.warning("Failed writing atom output to "+atomOut.getAbsolutePath()+": "+e);
		}
		if (mode!=TOTAL_MODE){
// RSS outputted only for passed items
			File rssOut = new File(outdir,"feed.rss");
	 	    try {
			  PrintWriter pw = new PrintWriter(rssOut, conf.outputCharSet);
			  writeRSS(pw, pres, items, sources,feedname);
			  pw.close();
		    } catch (IOException e){
		      Logging.warning("Failed writing rss output to "+rssOut.getAbsolutePath(),e);	
		    }
		}
		File xslt = new File(conf.defaultAtomXSLT);
		try {
			File skinned = new File(conf.xsltPrefix+pres.getSkin()+conf.xsltSuffix);
			if (skinned.exists()){ xslt = skinned; } else {
				if (pres.getSkin()!=null && pres.getSkin().equals("")){
					Logging.warning("Missing skin "+skinned);
				}
			}
		} catch (Exception e){
		}
		File override = new File(outdir,"atom-html.xslt");
		File htmlOut = new File(outdir,"feed.html");
		if (override.exists()) xslt = override;
		if (mode==TOTAL_MODE){
			xslt = new File(conf.checkedAtomXSLT);
			htmlOut = new File(outdir,"checked.html");
		}
		if (xslt.isFile()){
			try {
			  transform(atomOut,htmlOut, xslt);
			  File lastWritten = new File(conf.lastWrittenFile);
			  PrintWriter pw = new PrintWriter(lastWritten);
			  pw.print(htmlOut);
			  pw.close();
			} catch (SAXException e){
			  Logging.warning("Failed transforming to html "+atomOut.getAbsolutePath(),e);
			} catch (IOException e){
			  Logging.warning("Failed writing html output "+htmlOut.getAbsolutePath(),e);
			}
			if (!conf.postOutputScript.equals("") && mode!=TOTAL_MODE){
				try {
				  String cmd[]= new String[2];
				  cmd[0]=conf.postOutputScript;
				  cmd[1]=htmlOut.getAbsolutePath();
				  Process p = Runtime.getRuntime().exec(cmd);
				  InputStream stderr = p.getErrorStream();
				  InputStream stdout = p.getInputStream();
				  String line;
				  BufferedReader brCleanUp = 
				        new BufferedReader (new InputStreamReader (stdout));
				      while ((line = brCleanUp.readLine ()) != null) {
				        //System.out.println ("[Stdout] " + line);
				      }
				      brCleanUp.close();
				      
				      // clean up if any output in stderr
				      brCleanUp = 
				        new BufferedReader (new InputStreamReader (stderr));
				      while ((line = brCleanUp.readLine ()) != null) {
				        Logging.fine("Script STDERR: "+line);
				      }
				      brCleanUp.close();

				} catch (IOException e){
					Logging.warning("HTML Post processing script failed",e);
				}
			}
		}
	}
	

	
	void writeRSS( PrintWriter pw, Presentation pres, ParsedBuffer items, ParsedBuffer sources, String feedname){
		pw.println("<?xml version=\"1.0\" encoding=\""+conf.outputCharSet+"\"?>");
		pw.println("<rss version=\"2.0\">"); 
		pw.println("<channel>");
		pw.println("<title>"+XMLEscape(pres.getTitle())+"</title>");
		pw.println("<link rel=\"alternate\" type=\"application/atom+xml\" title=\""+pres.getTitle()+
				"\" href=\""+conf.outputAddress+"/"+XMLEscape(feedname)+"/atom.xml"+"\"/>");
		pw.println("<link rel=\"self\" type=\"application/atom+rss\" title=\""+pres.getTitle()+
				"\" href=\""+conf.outputAddress+"/"+XMLEscape(feedname)+"/feed.rss"+"\"/>");	
		pw.println("<description>"+XMLEscape(pres.getDescription())+"</description>");
	    DateFormat df= new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z");
	    String now = df.format(new Date());
//	    pw.println("<language>en-us</language");
		pw.println("<pubDate>"+now+"</pubDate>");
		pw.println("<lastBuildDate>"+now+"</lastBuildDate>");
		pw.println("<generator>FeedDistiller</generator>");
		pw.println("<managingEditor>"+XMLEscape(pres.getAuthormail())+"</managingEditor>");
		pw.println("<ttl>"+60+"</ttl>");
		while(items.moreItems()){
  		  pw.println("");
  		  pw.println("<item>");
			ParsedItem head = items.getHead();
			String url = items.getSourceURL();
			pw.println("<link>"+XMLEscape(url)+"</link>");
			String date = "";
			String title = items.getTitle();
			String desc =  items.getDescription();
			String id = "";
			if (head!=null){
			  Date dd = head.getEntryDate(url);
			  if (dd!=null) date = df.format(dd);
			  String desc1 = head.getEntrySummary(url);
			  if (!desc1.equals("") && (desc.length()==0 ||desc1.length()<desc.length())) desc=desc1;
			  id = head.getEntryId(url);
			  String title1 = head.getEntryTitle(url);
			  if (!title1.equals("")){ title = title1; }
			}
			title = stripTags(title);
			desc = stripTags(desc);
		  pw.println("<title>"+XMLEscape(title)+"</title>");
  		  pw.println("<description>"+XMLEscape(desc)+"</description>");
  		  pw.println("<pubDate>"+date+"</pubDate>");
  		  if (head!=null) pw.println("<source url=\""+XMLEscape(head.sourceurl)+"\">"+
  				  XMLEscape(head.title)+"</source>");
  		  pw.println("<guid>"+getID(url)+"</guid>");
  		  pw.println("</item>");
		}
		pw.println("</channel>");
		pw.println("</rss>");
	}
		
	void writeAtom( PrintWriter pw, Presentation pres, ParsedBuffer items, ParsedBuffer sources, int mode, String feedname){
		pw.println("<?xml version=\"1.0\" encoding=\""+conf.outputCharSet+"\"?>");
		pw.println("<feed xmlns=\"http://www.w3.org/2005/Atom\">");
		pw.println("<title>"+XMLEscape(pres.getTitle())+"</title>");
		pw.println("<link rel=\"self\" type=\"application/atom+xml\" title=\""+pres.getTitle()+
				"\" href=\""+conf.outputAddress+"/"+XMLEscape(feedname)+"/atom.xml"+"\"/>");
		pw.println("<link rel=\"alternate\" type=\"application/atom+rss\" title=\""+pres.getTitle()+
				"\" href=\""+conf.outputAddress+"/"+XMLEscape(feedname)+"/feed.rss"+"\"/>");		
		pw.println("<subtitle>"+XMLEscape(pres.getDescription())+"</subtitle>");
		pw.println("<feedid>"+pres.getFeedid()+"</feedid>");
		pw.println("<feedname>"+feedname+"</feedname>");
		DateFormat df= new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date d = new Date();
		pw.println("<updated>"+df.format(d)+"</updated>");
		pw.println("<author>");
		pw.println("<name>"+XMLEscape(pres.getAuthor())+"</name>");
		pw.println("<email>"+XMLEscape(pres.getAuthormail())+"</email>");
		pw.println("</author>");
		int count=0;
		while(items.moreItems()){
			pw.println();
			pw.println("<entry>");
			ParsedItem head = items.getHead();
			if (head!=null){
  			  pw.println("<from href=\""+XMLEscape(head.sourceurl)+"\">"+XMLEscape(head.title)+"</from>");
			}
			String url = items.getSourceURL();
			String date = "";
			String desc = items.getDescription();
			String id = "";
			String title = items.getTitle();
			if (head!=null){
			  Date dd = head.getEntryDate(url);
			  if (dd!=null) date = df.format(dd);
			  String title1 = head.getEntryTitle(url);
			  if (title1!=""){ title=title1; }
			  String desc1 = head.getEntrySummary(url);
			  if (!desc1.equals("") && (desc.length()==0 ||desc1.length()<desc.length())) desc=desc1;
			  id = head.getEntryId(url);
			}
			title = stripTags(title);
			desc = stripTags(desc);
			pw.println("<title>"+XMLEscape(title)+"</title>");
			pw.println("<link href=\""+XMLEscape(url)+"\"/>");
			if (mode==TOTAL_MODE){
				pw.println("<scored>"+items.getScore()+"</scored>");
				pw.println("<passed>"+items.getPassed()+"</passed>");
			}
			pw.println("<updated>"+date+"</updated>"); // Need to add date
			if (items.getImageURL()!=null && !items.getImageURL().equals("")){
			  try {
				int height=conf.outputImageWidth*items.getImageHeight()/items.getImageWidth();
				pw.println("<image href=\""+XMLEscape(items.getImageURL())+"\" width=\""+conf.outputImageWidth+
						"\" height=\""+height+"\"/>");
			  } catch (Exception e){}
			}
			pw.println("<summary>"+XMLEscape(desc)+"</summary>");
			
			if (!id.equals("")) pw.println("<id>"+getID(url)+"</id>");
			pw.println("</entry>");
			count++;
		}
		if (count==0){
			pw.println("<entry><title>Content to come</title>");
			pw.println("<updated>"+df.format(new Date())+"</updated>");
			pw.println("<summary> No content at the moment, check back later, perphaps the" +
					" feed owner should add more feeds, or check the reference pages</summary>");
			pw.println("</entry>");
		}
		while(sources.moreItems()){
		  pw.println();
		  pw.println("<source>");
		  pw.println("<title>"+XMLEscape(sources.getTitle())+"</title>");
		  pw.println("<link href=\""+XMLEscape(sources.getSourceURL())+"\"/>");
		  pw.println("<count>"+sources.getCount()+"</count>"); // Add count to the sources	  
		  pw.println("</source>");
		}
		pw.println();
		pw.println("</feed>");
	}
// Transform the atom output to html using SAX and XSLT
    public void transform(File sourceFile,File desFile, File xslt) throws IOException,SAXException {
    	
        StreamSource source = new StreamSource(sourceFile);
        StreamResult result = new StreamResult(desFile);
        StreamSource style = new StreamSource(xslt);
        TransformerFactory transFact = TransformerFactory.newInstance();
        try {
          Transformer trans = transFact.newTransformer(style);
          trans.transform(source,result);
        } catch (TransformerException e){
          Logging.warning("Error transforming "+sourceFile.getAbsolutePath()+" to html ",e);
        }     
    }

    private static String XMLEscape(String s){
       StringBuffer buffy = new StringBuffer(s.length());
       for(int i=0;i<s.length();i++){
    	   Character c = s.charAt(i);
    	   switch(c){
    	     case '"':
    	    	 buffy.append("&quot;"); break;
    	     case '&':
//    	    	 if (i+9<s.length() ) System.err.print(s.substring(i+1,i+9)+":\n");
    	    	 if (i+9<s.length() && s.substring(i+1,i+9).equals("amp;amp;")){
    	    		 buffy.append("&amp;"); i+=8; break; 
    	    	 } else if (i+10<s.length() && s.substring(i+1,i+10).equals("amp;quot;")){
    	    		 buffy.append("&quot;"); i+=9; break;
    	    	 } else if (i+5<s.length() && s.substring(i+1,i+5).equals("amp;")){
    	    		 buffy.append("&amp;"); i+=4; break;
    	    	 }
    	         int j=s.indexOf(";",i+1);
//    	         System.err.println(j);
    	         if (j-i<1 || j-i>8){
    	        	 buffy.append("&amp;"); break;
    	         }
    	    	 String ent = s.substring(i+1,j);
    	    	 char rep[] = replaceEntity(ent);
    	    	 if (rep!=null){
    	    		 buffy.append(rep); i=j+1; break;
    	    	 } else {
    	    		 buffy.append("&amp;"); break;
    	    	 }
    	     case '<':
    	    	 buffy.append("&lt;"); break;
    	     case '>':
    	    	 buffy.append("&gt;"); break;
    	     case '\'':
    	    	 buffy.append("&apos;"); break;
    	     default:
    	    	 buffy.append(c);
    	   };
       }
       return buffy.toString();
    }
    
    private static char[] replaceEntity(String s){
      char c[] = new char[1];
//      System.err.println(s);
      if (s.equals("amp")){ c = "&amp;".toCharArray(); }
      if (s.equals("laquo")){ c[0]=171; return c; }
      if (s.equals("raquo")){ c[0]=187; return c; }
      if (s.equals("nbsp")){ c[0]=160; return c; }
      if (s.equals("rsquo")){ c[0]=8217; return c; }
      if (s.equals("lsquo")){ c[0]=8216; return c; }
      return null;
    }
    
    private String stripTags(String s){
    	boolean output = true;
    	StringBuffer buffy = new StringBuffer(s.length());
    	for(int i=0;i<s.length();i++){
    		Character c = s.charAt(i);
    		switch(c){
    			case '<':
    				output=false; break;
    			case '>':
    				output=true; break;
    			default:
    				if (output) buffy.append(c);
    		};
    	}
    	return buffy.toString();
    }
    
    private String getID(String s){
    	String id= md5.MDString(s);
    	Logging.info("Presentation hash of "+s+": "+id);
    	return id; // Use MD5 of the URL as the ID
    }
    
    public static void main(String args[]){
    	if (args.length<2){
    		System.out.println("Usage: Presenter configfile feedname");
    		return;
    	}
       String conf_file = args[0];
       String feedname = args[1];
       Config conf = new Config(conf_file);
       Presenter pres = new Presenter(conf);
		File root = new File(conf.outputDir);
    	if (!root.isDirectory()){
		   System.err.println("cannot find output directory" +
				conf.outputDir);
		   return;
 	    }
	    File outdir = new File(root,feedname);
	    if (!outdir.exists()){
		   if (!outdir.mkdir()){
			  System.err.println("couldn't make directory "+outdir.getAbsolutePath());
		   }
	    }
	    File atomOut = new File(outdir,"atom.xml");
	    File xslt = new File(conf.defaultAtomXSLT);
	    File override = new File(outdir,"atom-html.xslt");
	    File htmlOut = new File(outdir,"feed.html");
	    if (override.exists()) xslt = override;
	    if (xslt.isFile()){
		  try {
		    pres.transform(atomOut,htmlOut, xslt);
		  } catch (SAXException e){
		    System.err.println("Failed transforming to html "+atomOut.getAbsolutePath()+"\n"+e);
		  } catch (IOException e){
		    System.err.println("Failed writing html output "+htmlOut.getAbsolutePath()+"\n"+e);
		  }
		}
    }
    
    public static void main1(String ar[]){
    	System.err.println(XMLEscape(" - &laquo; &nbsp; &raquo;  &amp;  &amp;amp; "));
    }
    
}
