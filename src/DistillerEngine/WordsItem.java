
package DistillerEngine;

import java.io.*;
import java.util.*;

public class WordsItem implements Serializable {

	
	public static final long serialVersionUID = 0xDEADBE0100000011L;

	// Unit to Store words and pairs on disk
	public Hashtable<String,Integer> words;
	public Hashtable<String,Hashtable<String,Integer>> pairs;
    public int wordsCount;
    public int pairsCount;
    
    public WordsItem(){
    	words=new Hashtable<String,Integer>();
    	pairs=new Hashtable<String,Hashtable<String,Integer>>();
        wordsCount=0;
        pairsCount=0;
    }
	
	public WordsItem(ParsedItem pi){
		words= pi.words;
		pairs= pi.pairs;
		wordsCount=0;
		pairsCount=0;
		for(Enumeration<Integer> e=words.elements();e.hasMoreElements();){
			wordsCount+=e.nextElement().intValue();
		}
		for(Enumeration<Hashtable<String,Integer>> e=pairs.elements();e.hasMoreElements();){
			Hashtable<String,Integer> ps=e.nextElement();
			for(Enumeration<Integer> e1=ps.elements(); e1.hasMoreElements();){
				pairsCount+=e1.nextElement().intValue();
			}
		}
	}
	
	public Hashtable<String,Integer> getWords(){ return words; }
	
	public Hashtable<String,Hashtable<String,Integer>> getPairs(){ return pairs; }
	
	public void appendItem(WordsItem wi){
		Hashtable<String,Integer> owords = wi.words;
		Hashtable<String,Hashtable<String,Integer>> opairs= wi.pairs;
		for(Enumeration<String> e=owords.keys();e.hasMoreElements();){
		  	String word = e.nextElement();
		  	Integer oval = owords.get(word);
		  	wordsCount+=oval.intValue();
		  	Integer icnt = words.get(word);
		  	if (icnt!=null){
		  		icnt= new Integer(icnt.intValue()+oval.intValue());
		  		words.put(word,icnt);
		  	} else {
		  		words.put(word, oval);
		  	}
		}
		for(Enumeration<String> e=opairs.keys();e.hasMoreElements();){
			String baseWord = e.nextElement();
			Hashtable<String,Integer> obase = opairs.get(baseWord);
			Hashtable<String,Integer> base = pairs.get(baseWord);
		    if (base==null){
		    	base = new Hashtable<String,Integer>();
		    	pairs.put(baseWord, base);
		    }
			for(Enumeration<String> f=obase.keys();f.hasMoreElements();){
			  	String word = f.nextElement();
			  	Integer oval = obase.get(word);
			  	Integer icnt = base.get(word);
			  	pairsCount+=oval.intValue();
			  	if (icnt!=null){
			  		icnt= new Integer(icnt.intValue()+oval.intValue());
			  		base.put(word,icnt);
			  	} else {
			  		base.put(word, oval);
			  	}
			}
		}
	}

	public static WordsItem getWordsItem(int feedid,byte[] md5) throws IOException {
		Config conf = Config.config;
		String feedname=FeedScheduler.statGetFeedName(feedid);
		String mdname = Distiller.MD5.hexBytes(md5);
		File wfile = new File(conf.downloadTo+File.separator
				+feedname+File.separator+mdname);
		if (!wfile.exists()) throw new IOException("missing cached file: "+wfile);
        FileInputStream fis = new FileInputStream(wfile);
        ObjectInputStream ois = new ObjectInputStream(fis);
        try {
          Object oin = ois.readObject();
          if (! (oin instanceof WordsItem)) throw
          			new IOException("wrong type for"+wfile);
          ois.close();
          fis.close();
          return (WordsItem) oin;
        } catch (ClassNotFoundException e){
          throw new IOException("Wrong class for "+wfile+" "+e);
        }
	}
	
}
