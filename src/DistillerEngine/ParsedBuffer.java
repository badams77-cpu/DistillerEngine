package DistillerEngine;

import java.net.*;
import java.io.*;
import java.util.*;

public class ParsedBuffer implements ParsedReceiver, ParsedSource {


	/* A Parsed Buffer is a fifo buffer for data from a ReceiverThread to be read by any class implementing
	 *  ParsedSource
	 */
	
	
		ParsedItem inItem;
		ParsedItem outItem;
		LinkedList<ParsedItem> fifo;
		
		public ParsedBuffer(){
		    inItem = new ParsedItem();
		    outItem = null;
		    fifo = new LinkedList<ParsedItem>();
		}
		
		public ParsedBuffer copyTo(){
			ParsedBuffer out = new ParsedBuffer();
			out.fifo.addAll(fifo);
			return out;
		}

		public int getBufferLength(){
			return fifo.size();
		}
		
		// Overrides from ParsedReceiver
		
		public void addDescription(String description) {
			inItem.description = description;
		}

		
		public void addLanguage(String lang) {
			inItem.language = lang;
		}

		public void addLinks(Enumeration<String> e){
			while( e.hasMoreElements()){
	            inItem.urls.add(e.nextElement());
			}
		}
		
		public void addLink(String l){
			inItem.urls.add(l);
		}

		public void addSourceURL(String filename) {
			inItem.sourceurl = filename;
		}

		public void addTitle(String title) {
			inItem.title = title;
		}
		
		public void addImageURL(String imageURL){
			inItem.imageurl = imageURL;
		}
		
		public void addImageHeight(int h){ inItem.imageHeight=h; }
		
		public void addImageWidth(int w){ inItem.imageWidth=w; }

		public void addWords(Enumeration<String> words, Enumeration<Integer> scores) {
			String lastWord = "";
			while( words.hasMoreElements() && scores.hasMoreElements()){
			    	String word = words.nextElement().toLowerCase();
			    	if (word.equals(".")){ lastWord=""; continue; }
			    	Integer sc = scores.nextElement();
			    	Integer os = (Integer) inItem.words.get(word);
			    	if (os==null){ 
			    		inItem.words.put(word,sc);
			    	} else {
			    		inItem.words.put(word,  new Integer(os.intValue()+sc.intValue()) );
			    	}
			    	if (!lastWord.equals("")){
			    		Hashtable<String,Integer> px = inItem.pairs.get(lastWord);
			    		if (px==null){ 
			    			px = new Hashtable<String,Integer>();
			    			inItem.pairs.put(lastWord,px);
			    		}
			            os = px.get(word);
			            if (os==null){ 
			            	px.put(word, new Integer(1));
			            } else {
			            	px.put(word, new Integer(os.intValue()+1));
			            }
			    	}
			    	lastWord = word;
			}
		}

		public void nextItem() {
	        inItem = new ParsedItem();
		}

        public ParsedItem myItem(){ return inItem; }
		
		public void setId(int id) {
			inItem.id = id;
		}
		
		public void setHead(ParsedItem pi){ inItem.head = pi; }
		
		public void closeItem(){
			synchronized(fifo){
		      fifo.addFirst(inItem);
			}
			inItem = null;
		}

//		@Overrides from ParsedSource			
		
		public String getDescription() {
			if (outItem==null) return "";
			return outItem.description;
		}

		public int getId() {
            if (outItem==null) return ParsedItem.EMPTY;
			return outItem.id;
		}

		public String getLanguage() {
			if (outItem==null) return "";
			return outItem.language;
		}

		public List getLinks() {
			if (outItem==null) return null;
            return outItem.urls;
		}
		
		public float getScore(){
			return outItem.score;
		}

		public boolean getPassed(){
			return outItem.passed;
		}
		
		public String getSourceURL() {
			if (outItem==null) return null;
			return outItem.sourceurl;
		}
		
		public String getTitle() {
			if (outItem==null) return null;
			return outItem.title;
		}

		public String getImageURL(){
			if (outItem==null) return null;
			return outItem.imageurl;
		}

		public int getImageHeight(){
			if (outItem==null) return 0;
			return outItem.imageHeight;
		}
		
		public int getImageWidth(){
			if (outItem==null) return 0;
			return outItem.imageWidth;
		}
		
		public int getCount(){
			if (outItem==null) return 0;
			return outItem.count;
		}

		public Hashtable<String,Integer> getWords() {
			if (outItem==null) return null;
	        return outItem.words;
		}

		public Hashtable<String,Hashtable<String,Integer>> getPairs(){
			if (outItem==null) return null;
			return outItem.pairs;
		}
		
		public ParsedItem getHead(){
			if (outItem==null) return null;
			return outItem.head;
		}
		
		public boolean moreItems() {
			synchronized(fifo){
  			  outItem = fifo.pollLast();
			}
			return outItem!=null;
		}

		ParsedItem getItem(){
			return outItem;
		}
		
		void addItem(ParsedItem pi){
			fifo.add(pi);
		}

		public void addEntry(String link, String title, Date date, String summary, String id){
		    inItem.addEntry(link, title, date, summary, id );
		}

        public void addAuthor(String authorName, String authorEmail){
        	inItem.addAuthor(authorName, authorEmail);
        }		
		
        public void sort( Comparator<ParsedItem> comp){
        	Collections.sort(fifo,comp);
        }
        
        public void dumpTitles(PrintStream pr){
        	for(Iterator<ParsedItem> ip=fifo.iterator();ip.hasNext();){
        		ParsedItem pi = ip.next();
        		pr.println(pi.sourceurl+"\n"+pi.title);
        	}
        }
        
	}	
