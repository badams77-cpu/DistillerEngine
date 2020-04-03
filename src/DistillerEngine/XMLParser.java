package DistillerEngine;

import java.util.*;
import java.io.*;
import java.text.*;

public class XMLParser extends HyperTextParser implements XMLCallBack {

  public static final String CopyRight =
    "Exclusive Copyright of Barry David Ottley Adams 2008 all Rights Reserved";

  private int  DESCRIPTIONLENGTH = 250; // Length of Description

  public Hashtable fieldParsers;

//  public static Hashtable XMLIndexDefCache;

  StringBuffer description;
  StringBuffer nullDescription;
  String title;

  public XMLIndexDef indexDef;

  public XMLParser() {
    fieldParsers = new Hashtable();
    description = new StringBuffer();
    nullDescription = new StringBuffer();
    title = "";
  }

  public void reset(){
    fieldParsers = new Hashtable();
    description = new StringBuffer();
    nullDescription = new StringBuffer();
    title = "";
    super.reset();
  }

  public String getDescription(){  
    if (description == null){return "";}
    if (description.length()>DESCRIPTIONLENGTH){
      return description.toString().substring(0,DESCRIPTIONLENGTH);
    }
    if (description.length()==0){
      return nullDescription.toString();
    }
    return description.toString();
  }

  public String getTitle(){
    if (title == null){return "";}
    if (title.length() >100){ return title.substring(0,100);}
    return title;
  }

  public Enumeration listFields(){
    return fieldParsers.keys();
  }

  public void configure(String argv[], ParserLoader pl){
    if (argv.length<1){
      throw new Error("XMLParser called without an XMLIndexDef file");
    }
    String xidf = argv[0];
/*
    if (XMLIndexDefCache == null){
      XMLIndexDefCache = new Hashtable();
    }
*/
    XMLIndexDef xid = (XMLIndexDef) ParserDefinitionStore.getDefinition(xidf);
//    XMLIndexDef xid = (XMLIndexDef) XMLIndexDefCache.get(xidf);
    if (xid != null){
      indexDef = xid;
      return;
    }
    xid = new XMLIndexDef();
    try {
      xid.readDef(xidf);
    } catch (IOException e){
       throw new Error("XMLParser could not be configured "+e+" with file "+xidf);
    }
    ParserDefinitionStore.putDefinition(xidf,xid);
    indexDef = xid;
    return;
  }

  public Parser getFieldParser(String field){
    Parser fp = (Parser) fieldParsers.get(field);
    if (fp != null){return fp;}
    return super.getFieldParser(field);
  }

  public Enumeration getLinks(){
    NullParser lp = (NullParser) fieldParsers.get("links");
    if (lp != null){
      return lp.listWords();
    }
    Vector a = new Vector();
    return a.elements();
  }

  public void parseAll(Reader reader) throws IOException {
//   for(Enumeration e= fieldParsers.elements();e.hasMoreElements();){
//      ((Parser) e.nextElement()).reset();
//    }
    reset();
    XMLSaxHandler xhs = new XMLSaxHandler(this);
    xhs.readXML(reader);
  }

  public void parseAll(InputStream is) throws IOException {
//   for(Enumeration e= fieldParsers.elements();e.hasMoreElements();){
//      ((Parser) e.nextElement()).reset();
//    }
    reset();
    XMLSaxHandler xhs = new XMLSaxHandler(this);
    xhs.readXML(is);
  }


  public void addWord(String word,int score){
    words.addElement(word);
    wordScores.addElement(new Integer(score));
    if (description.length() == 0 && nullDescription.length()<DESCRIPTIONLENGTH){
      nullDescription.append(" ");
      nullDescription.append(word);
    }
  }

  public void callBack(String whatisit,String content){
    XMLHowIndex xhi = indexDef.howDoIIndex(whatisit);
    if (xhi == null){
//     System.err.println("Notfound: "+whatisit+" text="+content);
     return;}
    addString(content,xhi);
  }
  
  public void endElement(String a){}

  public void attCallBackPass1(String whatisit, String content, XMLAttributeContext xac){
    XMLHowIndex xhi = indexDef.howDoIIndexAtt(whatisit);
    if (xhi == null){ return; }
    if (!xhi.isRemap){ return; }
    for (Enumeration e = xhi.getRemappedFields(content);e.hasMoreElements();){
      XMLRemap xr = (XMLRemap) e.nextElement();
      xac.setRemap( xr.field, xr.xmlHowIndex);
    }
  }

  public void attCallBackPass2(String whatisit, String content, XMLAttributeContext xac){
    int i = whatisit.lastIndexOf("!");
    if (i>0 && i<whatisit.length()){
      String field = whatisit.substring(i+1);
      XMLHowIndex xhi1 = xac.getRemap(field);
      if (xhi1 != null){
        addString(content,xhi1);
        return;
      }
    }
    XMLHowIndex xhi = indexDef.howDoIIndexAtt(whatisit);
    if (xhi == null){
//    System.err.println("Notfound: "+whatisit+" text="+content);
      return;
    }
    addString(content, xhi);
  }


  public void addString(String s, XMLHowIndex xhi){
//   System.err.println(xhi+" "+s);
    BreakIterator bi = BreakIterator.getWordInstance();
    bi.setText(s);
    boolean links = false;
    boolean base = false;
    boolean isTitle = false;
    int start = bi.first();
    for (int end = bi.next(); end != BreakIterator.DONE; start = end, end = bi.next()) {
      boolean isWord = false;
      for(int x=start; x<end;x++){
        char ch = s.charAt(x);
        if (Character.isLetter(ch) || Character.isDigit(ch)){ isWord = true; }
      }
      if (!isWord){ continue; }
      String word = s.substring(start,end);

      int mainscore = xhi.mainScore;
      if (mainscore != 0){
	      addWord(word,mainscore);
      }
      Hashtable fieldScores = xhi.fieldScores;
      for(Enumeration e=fieldScores.keys();e.hasMoreElements();){
	      String field = (String) e.nextElement();
        if (field.equals("links")){ links=true; continue;}
	if (field.equals("base")){ base=true; continue;}
        if (field.equals("description")){
          if (description.length() !=0){ description.append(" "); }
          description.append(word);
        }
        if (field.equals("title")){ isTitle=true; continue;}
	int score = ((Integer) fieldScores.get(field)).intValue();
        NullParser np = (NullParser) fieldParsers.get(field);
        if (np == null){
          np = new NullParser();
          fieldParsers.put(field,np);
        }
        np.addWord(word,score);
      }
    }
    if (links){
      NullParser np = (NullParser) fieldParsers.get("links");
      if (np == null){
        np = new NullParser();
        fieldParsers.put("links",np);
      }
 	    np.addWord(s,1);
    }
    if (base){
	    HREFbase = s;
    }
    if (isTitle){
      if (!title.equals("")){
        StringBuffer b = new StringBuffer(title);
        b.append(" - ");
        b.append(s);
        title = b.toString();
      } else {
        title = s;
      }
    }

  }

}
