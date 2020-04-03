package DistillerEngine;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Vector;

public class ARFilterStore {

// Singleton repositry for ARFilters for any
// active classes
    private static ARFilterStore store = new ARFilterStore();	

	private Hashtable<Integer,Boolean> hasFilter;
	private Hashtable<Integer,Boolean> filterReady;
	private Hashtable<Integer, Float> arRatios;
	private Hashtable<Integer,Vector<WordsItem>> accepted;
	private Hashtable<Integer,Vector<WordsItem>> rejected;
	private Hashtable<Integer,Hashtable<ByteArrayWrapper,Boolean>> itemScore;
	
	public ARFilterStore(){
		hasFilter = new Hashtable<Integer,Boolean>();
		filterReady = new Hashtable<Integer,Boolean>();
		accepted = new Hashtable<Integer,Vector<WordsItem>>();
		rejected = new Hashtable<Integer,Vector<WordsItem>>();
		arRatios = new Hashtable<Integer,Float>();
		itemScore = new Hashtable<Integer,
			Hashtable<ByteArrayWrapper,Boolean>>();
	}

	public static boolean hasFilter(int feedid){
		Boolean bl = store.hasFilter.get(new Integer(feedid));
		if (bl==null) return false;
		return bl.booleanValue();
	}
	
	public static boolean filterReady(int feedid){
		Boolean bl = store.filterReady.get(new Integer(feedid));
		if (bl==null) return false;
		return bl.booleanValue();
	}
	
	public static void setMaking(int feedid){
		Integer in = new Integer(feedid);
		store.hasFilter.put(in, Boolean.TRUE);
		store.filterReady.put(in, Boolean.FALSE);
	}
	
	public static void setReady(int feedid){
		Integer in = new Integer(feedid);
		store.hasFilter.put(in, Boolean.TRUE);
		store.filterReady.put(in, Boolean.TRUE);
	}
	
	private static Hashtable<ByteArrayWrapper,Boolean> getItemHash(int feedid){
		Integer in = new Integer(feedid);
		Hashtable<ByteArrayWrapper,Boolean> retHash = store.itemScore.get(in);
		if (retHash!=null){ return retHash; }
		retHash = new Hashtable<ByteArrayWrapper,Boolean>();
		store.itemScore.put(in,retHash);
		return retHash;
	}
	
	public static void setARRatio(int feedid, float f){
		store.arRatios.put(new Integer(feedid), new Float(f));
	}
	
	public static float getARRattio(int feedid){
		Float fl = store.arRatios.get(new Integer(feedid));
		if (fl!=null) return fl.floatValue();
		return 0.0f;
	}
	
	public static void setItemGood(int feedid, byte[] md5){
		Hashtable<ByteArrayWrapper,Boolean> items = getItemHash(feedid);
//		Logging.info("putting hash "+MD5.hexString(md5)+" as good");
		items.put(new ByteArrayWrapper(md5), Boolean.TRUE);
	}
	
	public static void setItemBad(int feedid, byte[] md5){
		Hashtable<ByteArrayWrapper,Boolean> items = getItemHash(feedid);
//		Logging.info("putting hash "+MD5.hexString(md5)+" as bad");
		items.put(new ByteArrayWrapper(md5), Boolean.FALSE);
	}
	
	public static boolean preRejected(int feedid, byte[] md5){
		Hashtable<ByteArrayWrapper,Boolean> items = getItemHash(feedid);
		Boolean score = items.get(new ByteArrayWrapper(md5));
		if (score==null){ 
//			Logging.info("Items held "+items.size()+" items but not:"+MD5.hexString(md5));
			return false; }
		return !score.booleanValue();
	}
	
	public static boolean preAccepted(int feedid, byte[] md5){
		Hashtable<ByteArrayWrapper,Boolean> items = getItemHash(feedid);
		Boolean score = items.get(new ByteArrayWrapper(md5));
		if (score==null){
//			Logging.info("Items held "+items.size()+" items but not:"+MD5.hexString(md5));			
			return false; }
		return score.booleanValue();
	}
	
	public static void addAcceptedWords(int feedid, WordsItem wi){
	  Integer iF = new Integer(feedid);
	  Vector<WordsItem> acVec = store.accepted.get(iF);
	  if (acVec==null){ 
		  acVec = new Vector<WordsItem>();
	      store.accepted.put(new Integer(feedid), acVec);
	  }
	  acVec.add(wi);
	}
	
	public static void addRejectedWords(int feedid, WordsItem wi){
		  Integer iF = new Integer(feedid);
		  Vector<WordsItem> reVec = store.rejected.get(iF);
		  if (reVec==null){ 
			  reVec = new Vector<WordsItem>();
		      store.rejected.put(new Integer(feedid), reVec);
		  }
		  reVec.add(wi);
	}
	
	public static Vector<WordsItem> getAcceptedWordsItem(int feedid){
		return store.accepted.get(new Integer(feedid));
	}
	
	public static Vector<WordsItem> getRejectedWordsItem(int feedid){
		return store.rejected.get(new Integer(feedid));
	}
	
	public static void removeFilter(int feedid){
		Integer iId = new Integer(feedid);
		store.accepted.remove(iId);
		store.rejected.remove(iId);
		store.itemScore.remove(iId);
		store.hasFilter.remove(iId);
		store.filterReady.remove(iId);
	}
	
}

class ByteArrayWrapper {

	byte[] array;
	
	ByteArrayWrapper(byte[] arr){ array=arr; }
	
	public boolean equals(Object a){
		if (!(a instanceof ByteArrayWrapper)) return false;
		byte[] arrayB = ((ByteArrayWrapper) a).array;
		return Arrays.equals(array, arrayB);
	}

	public int hashCode(){
		return Arrays.hashCode(array);
	}
	
}

	