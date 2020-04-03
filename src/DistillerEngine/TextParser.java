

package DistillerEngine;

import java.io.*;
import java.util.*;

public class TextParser extends Parser {

	
	
  public static final String CopyRight =
    "Exclusive Copyright of Barry David Ottley Adams 1998-1999 all Rights Reserved";

  public TextParser(){
	  super();
	  useReader=true;
  }
  
  void parseAll(Reader f) throws IOException {
    LineNumberReader lnr = new LineNumberReader(f);
    String line;
    String oldLine=null;
    while( (line = lnr.readLine()) != null){
      if (oldLine!=null){ TPaddString(line,1,false);}
      oldLine=line;
    }
    TPaddString(oldLine,1,true);
  }
  
  void TPaddString(String s, int score, boolean sectionEnd){
    StringBuffer wordBuffer = new StringBuffer(20);
    int mode=0;
    char temp[] = new char[1];
    int len = s.length();
    for(int i=0;i<len;i++){
      char x = s.charAt(i);
      temp[0] = x;
      switch (mode){
        case 0:  // Ready for word/number
        case 3:
          if (Character.isLetter(x)){
            wordBuffer  = new StringBuffer(20);
            wordBuffer.append(temp);
            mode = 1;
            break;
          }
          if (Character.isDigit(x)){
            wordBuffer = new StringBuffer(20);
            wordBuffer.append(temp);
            mode = 2;
            break;
          }
          break;
        case 1: // Middle of Word
         if (Character.isLetterOrDigit(x) || x=='`'){
           wordBuffer.append(temp);
           break;
         }
         if (x=='.' || x==';' || x==':'){
           TPaddWord(wordBuffer.toString(),score);
           TPendSentance();
           mode = 3;
           break;
         }
//        End word on all other characters
         TPaddWord(wordBuffer.toString(),score);
         mode = 0;
         break;
       case 2: // Middle of number
         if (Character.isDigit(x)){
           wordBuffer.append(temp);
           break;
         }
         if ( x=='.' || x==','){
           if ((i<len-1) && Character.isDigit(s.charAt(i+1))){
             wordBuffer.append(temp);
             break;
           } else {
             TPaddWord(wordBuffer.toString(),score);
             if (x=='.'){ TPendSentance(); }
             mode = 3;
             break;
           }
         }
         if (x==':' || x==';'){
           TPaddWord(wordBuffer.toString(),score);
           TPendSentance();
           mode = 3;
           break;
         }
         if (Character.isLetter(x)){
           wordBuffer.append(temp);
           mode =1;
           break;
         }
         TPaddWord(wordBuffer.toString(),score);
         mode = 0;
         break;
      default:
        mode = 0;
        break;
//     End Switch
      }
    }
    if (mode==1 || mode==2){
      TPaddWord(wordBuffer.toString(),score);
    }
    if (sectionEnd && mode!=3){
      TPendSentance();
    }
  }

  void TPendSentance(){
    words.addElement(".");
    wordScores.addElement(new Integer(0));
  }

  void TPaddWord(String x, int score){
    if (x.length()==0){ return; }
    if (x.equals(".")){ return; }
    words.addElement(x);
    wordScores.addElement(new Integer(score));
  }


}