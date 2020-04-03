package DistillerEngine;


import java.io.*;
import java.util.*;

public class CorporaNew extends Corpora implements Serializable {

	final static long serialVersionUID = 0xBABEBABE00000002L;
	
	public Hashtable<String,Hashtable<String,Integer>> langWords;
	public Hashtable<String,Long> totals;
	
	public CorporaNew(){
		langWords = new Hashtable<String,Hashtable<String,Integer>>();
		totals = new Hashtable<String,Long>();
	}
	
	public long getTotal(String lang){
		Long ix= totals.get(lang);
		if (ix==null) return 0L;
		return ix.longValue();
	}
	
	public CorporaOccurances getOccurances(String lang){
		final Long total = totals.get(lang);
		final Hashtable<String,Integer> hash = langWords.get(lang);
		return new CorporaOccurances(){
			
			float tot = total.longValue();
			Hashtable<String,Integer> values = hash;
			
			public float getOccurance(String word){
				Integer val = values.get(word);
				if (val ==null){ return 0.0f; }
				return val.floatValue()/tot;
			}
		};
	}
	
	void cul(){
//	     Remove all elements with occurance=1
			System.out.println("Culling the corpora");
			for(Enumeration<String> langE=totals.keys();langE.hasMoreElements(); ){
				String lang = langE.nextElement();
				long total = totals.get(lang).longValue();
				System.err.println(lang+" started with "+total+" occurances");
				Hashtable<String,Integer> scores = langWords.get(lang);
				for(Iterator<String> wordE=scores.keySet().iterator();wordE.hasNext();){
					String word = wordE.next();
					int n = scores.get(word).intValue();
					if (n==1){
						wordE.remove(); total--;
					}
				}
				System.out.println(lang+" now has "+total+"  occurances");
				totals.put(lang, new Long(total));
			}
		}
	
	private void addParsedFile(Parser parser, String lang){
		Long tot = totals.get(lang);
		if (tot==null){ tot = new Long(0); }
		long total = tot.longValue();
		Hashtable<String,Integer> scores = langWords.get(lang);
		if (scores==null){ scores = new Hashtable<String,Integer>(); }
		for(Enumeration words=parser.listWords(); words.hasMoreElements(); ){
			String word = ((String) words.nextElement()).toLowerCase();
			if (word.equals(".")) continue;
			total++;
			Integer sc = scores.get(word);
			if (sc==null){ sc= new Integer(1); } else {
				sc = new Integer(1+sc.intValue());
			}
			scores.put(word,sc);
		}
		langWords.put(lang,scores);
		totals.put(lang,new Long(total));
	}
	
	private static OpenByteArrayInputStream readFile(File f) throws IOException {
		int len = (int) f.length();
		byte a[] = new byte[len];
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		for(int i=0;i<len;){
			i+= bis.read(a,i,len-i);
		}
		bis.close();
		return new OpenByteArrayInputStream(a);
	}
	
	public static void main(String argv[]){
		if (argv.length<3){
			System.err.println("Usage: Corpora configfile languageDirectory output file");
			System.exit(255);
		}
		Config conf = new Config(argv[0]);
		ParserLoader pl = new ParserLoader(conf.parser_list,conf.parser_dir);
		MimeTypes mt = new MimeTypes(conf.mimetypes,conf.def_type);
		CorporaNew corp = new CorporaNew();
		int randmax =0;
		if (argv.length>3){
			randmax = Integer.parseInt(argv[3]);
			System.err.println(" Indexing 1 out of each "+randmax+" files");
		}
		Random rand = new Random();
		try {
			File dir = new File(argv[1]);
			if (!dir.isDirectory()){
				System.err.println(argv[1]+" should be a directory");
				System.exit(255);
			}
			File dirs[] = dir.listFiles();
			for(int i=0;i<dirs.length;i++){
				String lang = dirs[i].getName();
				File parts[] = dirs[i].listFiles();
				for(int j=0;j<parts.length;j++){
					if ( rand.nextInt(randmax)!=0) continue;
					File file = parts[j];
					try {
						String mimetype = mt.getType(file.getName());
						Parser parser = pl.getParser(mimetype);
						parser.reset();
						OpenByteArrayInputStream obis = readFile(file);
						parser.parseAll(obis);
						corp.addParsedFile(parser,lang);
						System.out.println("Done "+file);
					} catch (Exception e){
						System.err.println("Parsed Failed for file "+file);
						e.printStackTrace(System.err);
					}
				}
			}
		} catch (Exception e){
			System.err.println("Corpora building failed "+e);
			System.exit(255);
		}
		corp.cul();
		try {
		  File out = new File(argv[2]);
	      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out));
	      oos.writeObject(corp);
	      oos.close();
		} catch (Exception e){
			System.err.println("Failed writing the Corpora "+e);
		}
	}
	
}

