// for a particular XML document, XMLIndexDef produces a data structure
// describe how each element will be indexed.

package DistillerEngine;

import java.io.*;
import java.util.*;

public class XMLIndexDef extends ParserDefinition implements XMLCallBack {

  private Hashtable howToIndexAtt;
  private Hashtable ignoreTags;
  private Hashtable anyorderTags;
  private Hashtable inorderTags;
  private Hashtable anywhereTags;
  private Hashtable entities;

  public XMLIndexDef(){
    howToIndexAtt = new Hashtable();
    ignoreTags = new Hashtable();
    anyorderTags = new Hashtable();
    inorderTags = new Hashtable();
    anywhereTags = new Hashtable();
  }

  public void callBack(String whatisit, String content){
    if (content == null || content.equals("")){ return;}
    try {
      XMLHowIndex xhi = new XMLHowIndex(content);
      Vector tags = tagVec(whatisit);
      String lasttag;
      if (tags.size() != 0){
        lasttag = (String) tags.elementAt(tags.size()-1);
      } else {
        lasttag = "!NeverUseThisTag!";
      }
      if (xhi.tagMatch == XMLHowIndex.TAGANYORDER){
        anyorderTags.put(whatisit,xhi);
      } else if (xhi.tagMatch == XMLHowIndex.TAGANYWHERE){
        anywhereTags.put(lasttag,xhi);
      } else if (xhi.tagMatch == XMLHowIndex.TAGIGNORE){
        ignoreTags.put(lasttag,xhi);
      } else if (xhi.tagMatch == XMLHowIndex.TAGINORDER){
        inorderTags.put(whatisit,xhi);
      }
    } catch (BadIndexingSyntaxException e){
      System.err.println("XMLParser: I don't understand the Index Syntax - "+e.getError());
    }
  }

  public void endElement(String element){}
  
  public void attCallBackPass1(String whatisit, String content, XMLAttributeContext xac){
    if (content == null || content.equals("")){ return;}
    try {
      XMLHowIndex xhi = new XMLHowIndex(content);
      howToIndexAtt.put(whatisit,xhi);
    } catch (BadIndexingSyntaxException e){
      System.err.println("XMLParser: I don't understand the Index Syntax - "+e.getError());
    }
  }

  public void attCallBackPass2(String whatisit, String content, XMLAttributeContext xac){
  }

  public void readDef(String s) throws IOException {
//    FileReader fr = new FileReader(s);
    Reader fr = ParserDefinitionStore.getDefinitionReader(s);
    
    XMLSaxHandler xsh = new XMLSaxHandler(this);
    xsh.readXML(fr);
  }

 public XMLHowIndex howDoIIndexAtt(String s){
   return (XMLHowIndex) howToIndexAtt.get(s);
 }

  public XMLHowIndex howDoIIndex(String s){
    XMLHowIndex temp = (XMLHowIndex) inorderTags.get(s);
    if (temp != null){
      return temp;  // Exact Matches is always first
    }
    Vector tags = tagVec(s);
    Vector matches = new Vector();
    boolean doneSome = false;
    for(Enumeration e=tags.elements();e.hasMoreElements();){
      String t = (String) e.nextElement();
      if ( ignoreTags.get(t) != null){ tags.remove(t);doneSome = true;}
    }
    if (doneSome){
      s = vecTag(tags);
      temp = (XMLHowIndex) inorderTags.get(s);
      if (temp != null){
        matches.addElement(temp);
      }
    }
    for(Enumeration e=tags.elements();e.hasMoreElements();){
      String t = (String) e.nextElement();
      temp = (XMLHowIndex) anywhereTags.get(t);
      if (temp !=null){
        matches.addElement(temp);
      }
    }
    Object tagsArray[] = tags.toArray();
    IntroSort.sort(tagsArray,new StringCompare());
    s = vecTag(tagsArray);
    temp = (XMLHowIndex) anyorderTags.get(s);
    if (temp != null){
      matches.addElement(temp);
    }
    if (matches.size()==0){
      return null;
    }
    Object matchArray[] = matches.toArray();
    IntroSort.sort(matchArray, new XMLHowIndexPriCompare() );
    return (XMLHowIndex) matchArray[0];
  }

  private Vector tagVec(String s){
    Vector tags = new Vector();
    StringTokenizer tok = new StringTokenizer(s,"!");
    while(tok.hasMoreTokens()){
      tags.addElement(tok.nextToken());
    }
    return tags;
  }

  private String vecTag(Vector tags){
    StringBuffer buf = new StringBuffer();
    for(Enumeration e=tags.elements();e.hasMoreElements();){
      if (buf.length()!=0){
        buf.append("!");
      }
      buf.append( (String) e.nextElement());
    }
    return buf.toString();
  }

  private String vecTag(Object tags[]){
    StringBuffer buf = new StringBuffer();
    for(int i=0;i<tags.length;i++){
     if (buf.length()!=0){
        buf.append("!");
      }
      buf.append( (String) tags[i]);
    }
    return buf.toString();
  }

}

class StringCompare implements Compare {

  public int compare( Object a,Object b){
    return ( (String)a).compareTo( (String) b);
  }
}

class XMLHowIndexPriCompare implements Compare {

  public int compare(Object a,Object b){
    int priA = ((XMLHowIndex) a).matchPri;
    int priB = ((XMLHowIndex) b).matchPri;
    if (priA < priB) { return -1;}
    if (priA == priB){ return 0;}
    return 1;
  }

}
