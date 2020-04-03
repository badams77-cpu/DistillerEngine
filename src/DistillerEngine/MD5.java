

package DistillerEngine;

import java.io.*;

// The MD5 RFC C code translated to Java by Barry Adams

  public class MD5 {

  private int buffer[];
  private static int T[] = {
    0,
    0xd76aa478,
    0xe8c7b756,
    0x242070db,
    0xc1bdceee,
    0xf57c0faf,
    0x4787c62a,
    0xa8304613,
    0xfd469501, // 8
    0x698098d8,
    0x8b44f7af,
    0xffff5bb1,
    0x895cd7be,
    0x6b901122,
    0xfd987193,
    0xa679438e,
    0x49b40821, // 16
    0xf61e2562,
    0xc040b340,
    0x265e5a51,
    0xe9b6c7aa,
    0xd62f105d,
    0x2441453,
    0xd8a1e681,
    0xe7d3fbc8, // 24
    0x21e1cde6,
    0xc33707d6,
    0xf4d50d87,
    0x455a14ed,
    0xa9e3e905,
    0xfcefa3f8,
    0x676f02d9,
    0x8d2a4c8a, // 32
    0xfffa3942,
    0x8771f681,
    0x6d9d6122,
    0xfde5380c,
    0xa4beea44,
    0x4bdecfa9,
    0xf6bb4b60,
    0xbebfbc70, // 40
    0x289b7ec6,
    0xeaa127fa,
    0xd4ef3085,
    0x4881d05,
    0xd9d4d039,
    0xe6db99e5,
    0x1fa27cf8,
    0xc4ac5665, // 48
    0xf4292244,
    0x432aff97,
    0xab9423a7,
    0xfc93a039,
    0x655b59c3,
    0x8f0ccc92,
    0xffeff47d,
    0x85845dd1, // 56
    0x6fa87e4f,
    0xfe2ce6e0,
    0xa3014314,
    0x4e0811a1,
    0xf7537e82,
    0xbd3af235,
    0x2ad7d2bb,
    0xeb86d391, //64
  };

  public MD5() {
  }

  public byte[] MDString8(String s){
    byte buf[] = new byte[s.length()];
    for(int i=0;i<s.length();i++){
      buf[i] = (byte) (s.charAt(i) &0xff);
    }
    return MDBytes(buf);
  }
  
  public String MDString(String s){
	    byte buf[] = new byte[s.length()];
	    for(int i=0;i<s.length();i++){
	      buf[i] = (byte) (s.charAt(i) &0xff);
	    }
	    return hexString(MDBytes(buf));	  
  }

  private void MDString_test(String s){
    System.err.print(" MD5(\""+s+"\") = ");
    byte MD5hash[] = MDString8(s);
    outHexBytes( MD5hash,System.err);
  }

  public static void outHexBytes( byte[] data, PrintStream p ){
    StringBuffer temp = new StringBuffer("  ");
    for(int i=0;i<data.length;i++){
      byte a = data[i];
      char b = (char) (a&0xf);
      if (b <10){
        b = (char) (b + '0');
      } else {
        b = (char) (b + 'a'-10);
      }
     char c = (char) ((a&0xf0) >>4);
      if (c <10){
        c = (char) (c + '0');
      } else {
        c = (char) (c + 'a'-10);
      }
      temp.setCharAt(0,c);
      temp.setCharAt(1,b);
      p.print(temp);
    }
    p.print("\n");
  }

  public static String hexString( byte[] data ){
	  StringBuffer outBuf = new StringBuffer();
	    StringBuffer temp = new StringBuffer("  ");
	    for(int i=0;i<data.length;i++){
	      byte a = data[i];
	      char b = (char) (a&0xf);
	      if (b <10){
	        b = (char) (b + '0');
	      } else {
	        b = (char) (b + 'a'-10);
	      }
	     char c = (char) ((a&0xf0) >>4);
	      if (c <10){
	        c = (char) (c + '0');
	      } else {
	        c = (char) (c + 'a'-10);
	      }
	      temp.setCharAt(0,c);
	      temp.setCharAt(1,b);
	      outBuf.append(temp);
	    }
	    return outBuf.toString();
	  }  
  
  public byte[] MDBytes( byte input[]){
    int length = input.length;
    int appendlength = 56-(length %64);
    if (appendlength <= 0){
      appendlength = 64+appendlength;
    }
    byte[] padded = new byte[length+appendlength+8];
    for(int i=0;i<length;i++){
      padded[i] = input[i];
    }
    padded[length] = (byte) 128;
    for(int i=length+1;i<length+appendlength;i++){
      padded[i]=0;
    }
    long lnx = ((long) length) * 8L;
    for(int i=length+appendlength;i<length+appendlength+8;i++){
      padded[i] = (byte) (lnx & 0xFFL);
      lnx >>= 8;
    }
    int wlen = (length+appendlength+8)/4;
    int words[] = new int[wlen];
    for(int i=0;i<wlen;i++){
      words[i] = (padded[4*i]&0xFF) | (padded[4*i+1]&0xFF)<<8 |
        (padded[4*i+2]&0xFF)<<16 | ((padded[4*i+3]&0xFF)<< 24);
    }
//    when byte is casted to an int here we don`t want
//    sign extention.
    long a = 0x67452301;
    long b = 0xefcdab89;
    long c = 0x98badcfe;
    long d = 0x10325476;
    long aa,bb,cc,dd;
    buffer = new int[16];
    for (int i=0;i<wlen/16;i++){
      aa = a; bb = b; cc = c; dd = d;
      for(int j=0;j<16;j++){
        buffer[j] = words[i*16+j];
      }

      a = round1(a,b,c,d,0,7,1);
      d = round1(d,a,b,c,1,12,2);
      c = round1(c,d,a,b,2,17,3);
      b = round1(b,c,d,a,3,22,4);
      a = round1(a,b,c,d,4,7,5);
      d = round1(d,a,b,c,5,12,6);
      c = round1(c,d,a,b,6,17,7);
      b = round1(b,c,d,a,7,22,8);
      a = round1(a,b,c,d,8,7,9);
      d = round1(d,a,b,c,9,12,10);
      c = round1(c,d,a,b,10,17,11);
      b = round1(b,c,d,a,11,22,12);
      a = round1(a,b,c,d,12,7,13);
      d = round1(d,a,b,c,13,12,14);
      c = round1(c,d,a,b,14,17,15);
      b = round1(b,c,d,a,15,22,16);

      a = round2(a,b,c,d,1,5,17);
      d = round2(d,a,b,c,6,9,18);
      c = round2(c,d,a,b,11,14,19);
      b = round2(b,c,d,a,0,20,20);
      a = round2(a,b,c,d,5,5,21);
      d = round2(d,a,b,c,10,9,22);
      c = round2(c,d,a,b,15,14,23);
      b = round2(b,c,d,a,4,20,24);
      a = round2(a,b,c,d,9,5,25);
      d = round2(d,a,b,c,14,9,26);
      c = round2(c,d,a,b,3,14,27);
      b = round2(b,c,d,a,8,20,28);
      a = round2(a,b,c,d,13,5,29);
      d = round2(d,a,b,c,2,9,30);
      c = round2(c,d,a,b,7,14,31);
      b = round2(b,c,d,a,12,20,32);

      a = round3(a,b,c,d,5,4,33);
      d = round3(d,a,b,c,8,11,34);
      c = round3(c,d,a,b,11,16,35);
      b = round3(b,c,d,a,14,23,36);
      a = round3(a,b,c,d,1,4,37);
      d = round3(d,a,b,c,4,11,38);
      c = round3(c,d,a,b,7,16,39);
      b = round3(b,c,d,a,10,23,40);
      a = round3(a,b,c,d,13,4,41);
      d = round3(d,a,b,c,0,11,42);
      c = round3(c,d,a,b,3,16,43);
      b = round3(b,c,d,a,6,23,44);
      a = round3(a,b,c,d,9,4,45);
      d = round3(d,a,b,c,12,11,46);
      c = round3(c,d,a,b,15,16,47);
      b = round3(b,c,d,a,2,23,48);

      a = round4(a,b,c,d,0,6,49);
      d = round4(d,a,b,c,7,10,50);
      c = round4(c,d,a,b,14,15,51);
      b = round4(b,c,d,a,5,21,52);
      a = round4(a,b,c,d,12,6,53);
      d = round4(d,a,b,c,3,10,54);
      c = round4(c,d,a,b,10,15,55);
      b = round4(b,c,d,a,1,21,56);
      a = round4(a,b,c,d,8,6,57);
      d = round4(d,a,b,c,15,10,58);
      c = round4(c,d,a,b,6,15,59);
      b = round4(b,c,d,a,13,21,60);
      a = round4(a,b,c,d,4,6,61);
      d = round4(d,a,b,c,11,10,62);
      c = round4(c,d,a,b,2,15,63);
      b = round4(b,c,d,a,9,21,64);

      a = (a+aa);
      b = (b+bb);
      c = (c+cc);
      d = (d+dd);
    }

    byte ret[] = new byte[16];
    for(int i=0;i<4;i++){
      ret[i] = (byte) (a&0xff);
      a >>= 8;
    }
    for(int i=4;i<8;i++){
      ret[i] = (byte) (b&0xff);
      b >>= 8;
    }
    for(int i=8;i<12;i++){
      ret[i] = (byte) (c&0xff);
      c >>= 8;
    }
    for(int i=12;i<16;i++){
      ret[i] = (byte) (d&0xff);
      d >>= 8;
    }
    return ret;
  }

  private long round1(long aa,long bb,long cc,long dd,int k, int s,int i){
    int a = (int) (aa & 0xffffffffL);
    int b = (int) (bb & 0xffffffffL);
    int c = (int) (cc & 0xffffffffL);
    int d = (int) (dd & 0xffffffffL);
    return (bb+rot( aa + F(b,c,d) + buffer[k]+T[i], s)) &0xffffffffL;
  }

  private long round2(long aa,long bb,long cc,long dd,int k, int s,int i){
    int a = (int) (aa & 0xffffffffL);
    int b = (int) (bb & 0xffffffffL);
    int c = (int) (cc & 0xffffffffL);
    int d = (int) (dd & 0xffffffffL);
    return (bb+rot( aa + G(b,c,d) + buffer[k]+T[i], s)) &0xffffffffL;
  }

  private long round3(long aa,long bb,long cc,long dd,int k, int s,int i){
    int a = (int) (aa & 0xffffffffL);
    int b = (int) (bb & 0xffffffffL);
    int c = (int) (cc & 0xffffffffL);
    int d = (int) (dd & 0xffffffffL);
    return (bb+rot( aa + H(b,c,d) + buffer[k]+T[i], s)) &0xffffffffL;
  }

  private long round4(long aa,long bb,long cc,long dd,int k, int s,int i){
    int a = (int) (aa & 0xffffffffL);
    int b = (int) (bb & 0xffffffffL);
    int c = (int) (cc & 0xffffffffL);
    int d = (int) (dd & 0xffffffffL);
    return (bb+rot( aa + I(b,c,d) + buffer[k]+T[i], s)) &0xffffffffL;
  }


  private int F(int x,int y,int z){
    return x&y | (~x)&z;
  }

  private int G(int x,int y,int z){
    return x&z | (~z)&y;
  }

  private int H(int x,int y,int z){
    return x^y^z;
  }

  private int I(int x,int y,int z){
    return y^(x | ~z);
  }

  private long rot(long a, int b){
    return  ((a << b) & 0xffffffffL) | ( (a & 0xffffffffL) >>> (32-b));
  }

  public static void main(String argv[]){
    MD5 md5 = new MD5();
    md5.MDString_test("");
    md5.MDString_test("a");
    md5.MDString_test("abc");
    md5.MDString_test("message digest");
    md5.MDString_test("abcdefghijklmnopqrstuvwxyz");
    md5.MDString_test("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    md5.MDString_test("12345678901234567890123456789012345678901234567890123456789012345678901234567890");
  }

}

