package BlogFinder;

import java.io.IOException;
import java.io.Reader;
import java.io.PushbackReader;
import java.util.*;

// A parser that switches between a choice of other parsers when it
// recognises a particular character string

public class SelectParser extends Parser {

	Hashtable<String,Parser> parserForPats;
	Parser activeParser;
	
	public SelectParser(){
		parserForPats = new Hashtable<String,Parser>();
		useReader =true;
	}
	
	public void reset(){
		super.reset();
		activeParser = null;
	}
	
	public Parser getParser(){
		if (activeParser==null){ return this; }
		return activeParser;
	}
	
	
	
	public void configure(String args[], ParserLoader pl){
		if (args.length<2 || (args.length/2)*2!=args.length){
			throw new Error("Bad arguments for Select Parser");
		}
		for(int i=0;i<args.length;){
			String pat = args[i++];
			String mime = args[i++];
			Parser p = pl.getParser(mime);
			parserForPats.put(pat, p);
		}
	}
	
	
	@Override
	void parseAll(Reader reader) throws IOException {
		activeParser=null;
		PushbackReader pbr = new PushbackReader(reader,10240);
		Stack<String> lines = new Stack<String>();
        StringBuffer buf = new StringBuffer();
        boolean hasMore = true;
        int lineCount=20;
        while(activeParser==null && hasMore){
          buf.setLength(0);
          hasMore = readline(pbr,buf);
          String s = buf.toString();
          for(Enumeration<String> e=parserForPats.keys();e.hasMoreElements();){
        	  String pat=e.nextElement();
        	  if (s.contains(pat)){
        		  activeParser=parserForPats.get(pat);
        	  }
          }
          lines.push(s);
          if (lineCount--==0){ pbr.close(); return; }
        }
        if (activeParser==null){ pbr.close(); return; }
        while(!lines.isEmpty()){
        	String s=lines.pop();
//        	System.err.println(s);
        	pbr.unread(s.toCharArray());
        }
 
        activeParser.parseAll(pbr);
        pbr.close();
	}
	
	private boolean readline(Reader r, StringBuffer b) throws IOException {
	  char c[]=new char[1];
	  int ch;
	  while((ch=r.read())!=-1){
		  c[0]=(char) ch;
		  b.append(c);
		  if (ch=='\n') return true;
	  }
	  return false;
	}

}
