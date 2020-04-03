package DistillerEngine;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ParsedCache implements ParsedReceiver, ParsedSource {

	ParsedItem inItem;
	ParsedItem outItem;
	Hashtable<Integer,Hashtable<String,ParsedItem>> cache;
	private Integer currentId;
	private Enumeration<String> urlEnum;
	
// Stores Parsed Items in a cache ready to be retrieved at any later time
// Keyed by there id and source url
// nextItem enumerates over items with the same id
	
	public ParsedCache(){
	    inItem = new ParsedItem();
	    outItem = null;
	    cache = new Hashtable<Integer,Hashtable<String,ParsedItem>>();
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

	public float getScore(){
		return outItem.score;
	}

	public boolean getPassed(){
		return outItem.passed;
	}	
	
	public void addLink(String l){
		inItem.urls.add(l);
	}	
	
	public void addEntry(String link, String title, Date date, String summary, String id){
	    inItem.addEntry(link, title, date, summary, id);
	}

    public void addAuthor(String authorName, String authorEmail){
    	inItem.addAuthor(authorName, authorEmail);
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
		String lastWord="";
		while( words.hasMoreElements() && scores.hasMoreElements()){
		    	String word = words.nextElement().toLowerCase();
		    	if (word.equals(".")) continue;		    	
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


	public void setId(int id) {
		inItem.id = id;
	}	
	
	public void setHead(ParsedItem head){
		inItem.head = head;
	}
	
	public ParsedItem myItem(){ return inItem; }
	
	public void closeItem(){
		Integer id = new Integer(inItem.id);
		Hashtable<String,ParsedItem> hash = cache.get(id);
		if (hash==null){ 
			hash = new Hashtable<String,ParsedItem>(); 
			cache.put(id,hash);
		}
	    hash.put(inItem.sourceurl, inItem);
	    Logging.info("Storing to ParsedCache "+inItem.id+" "+inItem.sourceurl);
	}

//	@Overrides from ParsedSource			
	
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
	
	public ParsedItem getHead(){
		if (outItem==null) return null;
		return outItem.head;
	}
	
	public Hashtable<String,Integer> getWords() {
		if (outItem==null) return null;
        return outItem.words;
	}
	
	public Hashtable<String,Hashtable<String,Integer>> getPairs(){
		if (outItem==null) return null;
		return outItem.pairs;
	}

	public boolean moreItems() {
// Iterates over items with same id
		if (currentId==null || urlEnum==null || !urlEnum.hasMoreElements()){ return false; }
	    String outkey = urlEnum.nextElement();
		outItem = cache.get(currentId).get(outkey);
		return outItem!=null;
	}

	public boolean selectId(int id){
// Selects a particular id and sets up its Enumeration
		currentId = new Integer(id);
		Hashtable<String,ParsedItem> urlHash = cache.get(currentId);
		if (urlHash==null){ currentId=null; return false; }
		urlEnum = urlHash.keys();
		return urlEnum.hasMoreElements();
	}

	public boolean clearId(int id){
//  remove all items with the given id
		Integer idi = new Integer(id);
		return cache.remove(idi)!=null;
	}

	public boolean contains(int id, String url){
	    Integer Id = new Integer(id);
		Hashtable<String,ParsedItem> urlHash = cache.get(Id);
		if (urlHash==null){ return false; }
		return urlHash.containsKey(url);
	}
	
	public boolean selectURL(int id, String url){
	    Integer Id = new Integer(id);
		Hashtable<String,ParsedItem> urlHash = cache.get(Id);
		if (urlHash==null){ return false; }
		if (!urlHash.containsKey(url)){ return false; }
		outItem = urlHash.get(url);
		return true;
	}
	
}

