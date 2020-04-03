package DistillerEngine;

import java.net.URL;
import java.util.*;

public interface ParsedSource {

	// skip to the next item
	public boolean moreItems();
	
	public List<String> getLinks();
	
	public Hashtable<String,Integer> getWords();
	
	public Hashtable<String,Hashtable<String,Integer>> getPairs();
	
	public int getId();
	
	public String getTitle();
	
	public String getDescription();
	
	public String getSourceURL();
	
	public String getLanguage();
	
	public String getImageURL();
	
	public int getImageHeight();
	
	public int getImageWidth();
	
	public ParsedItem getHead();
	
	public float getScore();

	public boolean getPassed();
	

}
