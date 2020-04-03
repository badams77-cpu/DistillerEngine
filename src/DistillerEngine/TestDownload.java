package DistillerEngine;
import java.util.logging.*;
import java.net.*;
import java.io.*;

public class TestDownload {

	public static void main(String argv[]){
		if (argv.length<2){
			System.err.println("Usage: TestDownload configfile url");
		    return;
		}
		LoggingThreadGroup group = new LoggingThreadGroup("All","All","All");		
		Config conf = new Config(argv[0]);
		PrintWriter log = new PrintWriter(System.err);
		URL url[] = new URL[1];
		try {
			url[0] = new URL(argv[1]);
		} catch (Exception e){ e.printStackTrace(System.err); }
		
		group.addHandler(new ConsoleHandler());
		SpiderThread spid = new SpiderThread(conf,log,group);
		ParserLoader pl  = new ParserLoader(conf.parser_list,conf.parser_dir);
		ReceiverThread rec = new ReceiverThread(group,pl,conf);
		ParsedBuffer buf = new ParsedBuffer();
		spid.addSite(url,"http", new URL[0]);
		rec.addWatched(spid);
		rec.setParsedReceiver(buf);
		rec.start(group);
		while(!buf.moreItems()){
			try {
				Thread.sleep(500);
			} catch (Exception e){}
		}
		buf.getItem().dump(System.err);
		rec.stop();
	}
}
