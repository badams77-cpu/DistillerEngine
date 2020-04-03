package DistillerEngine;

import java.io.*;
import java.util.logging.*;

public class SavingReceiverThread extends ReceiverThread {

	public String downloadDir="";
	private LoggingThreadGroup ltg;
	private Distiller.MD5 md5;
	
	public SavingReceiverThread(LoggingThreadGroup thg, ParserLoader pl, Config conf){
	  super(thg,pl,conf);
	  downloadDir=conf.downloadTo;
	  ltg=thg;
	  md5 = new Distiller.MD5();
    } 

	
	
	public void saveItem(int taskId,String filename,ParsedItem pi){
	  String blogName = FeedScheduler.statGetFeedName(taskId);
	  String name = Distiller.MD5.hexBytes(md5.MDString8(filename));
	  try {
		  File dir = new File(downloadDir+File.separator+blogName);
		  if (!dir.exists()){
			  dir.mkdirs();
		  }
		  File downloadTo = new File(downloadDir+File.separator+blogName+
				  File.separator+name);
		  FileOutputStream fw= new FileOutputStream(downloadTo);
		  ObjectOutputStream oos = new ObjectOutputStream(fw);
		  WordsItem wi = new WordsItem(pi);
		  System.out.println("Writing cache for "+filename);
		  System.out.println(wi.wordsCount+" words and "+wi.pairsCount+" pairs of words");
		  oos.writeObject(wi);
		  oos.close();
		  fw.close();
	  } catch (Exception e){
			ltg.log(Logging.WARNING , "Failed to save file: "+e);
	  } 
	}
}

 