package DistillerEngine;

import java.util.*;
import java.net.*;

public interface ParsedReceiver {

	public void nextItem();
	
	public void addLink(String link);
	
	public void addLinks(Enumeration<String> links);
	
	public void addWords(Enumeration<String> words, Enumeration<Integer> score);
	
	public void setId(int id);
	
	public void addTitle(String title);
	
	public void addImageURL(String image);
	
	public void addImageWidth(int w);
	
	public void addImageHeight(int h);
	
	public void addDescription(String description);
	
	public void addSourceURL(String filename);
	
	public void setHead(ParsedItem pi);
	
	public void addLanguage(String lang);
	
	public void closeItem();

	public void addEntry(String link, String title, Date date, String summary, String id);

    public void addAuthor(String authorName, String authorEmail);

    public ParsedItem myItem();

}
