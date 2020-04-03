package DistillerEngine;

import java.net.URL;
import java.io.PrintStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;

public class ParsedItem {
		
	    public static int EMPTY = -1;
	
		LinkedList<String> urls;
		Hashtable<String,Date> urlDates;
		Hashtable<String, String> urlSummaries;
		Hashtable<String, String> urlTitles;
		Hashtable<String, String> urlIds;
		Hashtable<String,Integer> words;
		Hashtable<String,Hashtable<String,Integer>> pairs;

		String author;
		String authorEmail;
		String title;
		String description;
		String sourceurl;
		String language;
		String imageurl;
		int imageHeight;
		int imageWidth;
		int id;
		Date date;
		int count;
		float score;
		boolean passed;
		ParsedItem head; // optional link to master Item
		
		public ParsedItem(){
			urls = new LinkedList<String>();
		    words = new Hashtable<String,Integer>();
		    urlDates = new Hashtable<String, Date>();
		    urlTitles = new Hashtable<String, String>();
		    urlSummaries = new Hashtable<String, String>();
		    urlIds = new Hashtable<String,String>();
		    pairs = new Hashtable<String,Hashtable<String,Integer>>();
		    date = new Date(0);
		    title = description = sourceurl = language = "";
		    author=authorEmail = imageurl= "";
		    id = EMPTY;
		}
		
		public ParsedItem trimData(){
			words = new Hashtable<String,Integer>();
			pairs = new Hashtable<String,Hashtable<String,Integer>>();
			return this;
		}
		
		public void addEntry(String link, String title, Date date, String summary, String id){
		    urls.add(link);
		    urlDates.put(link, date);
		    urlSummaries.put(link, summary);
		    urlTitles.put(link, title);
		    urlIds.put(link, id);
//		    Logging.info("Adding entry: "+link+"\n"+title+"\n"+summary);		    
		}

		
        public void addAuthor(String authorName, String authorEmail){
            author=authorName;
            this.authorEmail = authorEmail;
        }
        
        public Date getEntryDate(String url){
          	return urlDates.get(url);
        }	
        
        public String getEntryTitle(String url){
        	String tit = urlTitles.get(url);
        	if (tit==null){ return ""; }
        	return tit;
        }

        
        public String getEntrySummary(String url){
        	String s = urlSummaries.get(url);
        	if (s==null){ return ""; }
        	return s;
        }        

        public String getEntryId(String url){
        	String s = urlIds.get(url);
        	if (s==null){ return ""; }
        	return s;
        }
        
        public void setScore(boolean passed, float score){
        	this.passed = passed;
        	this.score = score;
        }
       
        public void dump(PrintStream ps){
          ps.println("Item from: "+sourceurl);
          ps.println("Id: "+id);
          ps.println("Author: "+author);
          ps.println("AuthorEmail: "+authorEmail);
          ps.println("Image: "+imageurl);
          ps.println("Image Width * Height "+imageWidth+" * "+imageHeight);
          ps.println("Title: "+title);
          ps.println("Description:\n"+description);
          ps.println("\nNwords: "+words.size());
          ps.println("Npairs: "+pairs.size());
          ps.println("Nurls: "+urls.size());
        	
        }
        
}
