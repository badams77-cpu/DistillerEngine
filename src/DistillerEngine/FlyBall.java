package DistillerEngine;

import java.net.URL;

public class FlyBall {

// The data caught by the Spider :-)

  public static final String CopyRight =
    "Exclusive Copyright of Barry David Ottley Adams 1998-1999 all Rights Reserved";


  public byte[] inputbuffer;
  public  int bufferLength;
  public String type;
  public long lastModified;
  public URL url;
  public SpiderThread spider;
  public String lang;
  public String langAllow[];
  public String encoding;
  public byte[] bodyDigest;
  public String beginLink;
  public int taskId;

  public FlyBall(byte[] buffer, int length, String type, long lastmod,URL url, SpiderThread spider, String beginLink, String lang,String
encoding, String[] langAllow, int taskId){
    inputbuffer = buffer;
    bufferLength = length;
    this.type = type;
    lastModified = lastmod;
    this.url = url;
    this.spider = spider;
    this.beginLink= beginLink;
    this.lang = lang;
    this.encoding = encoding;
    this.langAllow = langAllow;
    bodyDigest = null;
    this.taskId = taskId;
  }

  public FlyBall(byte[] buffer, int length, String type, long lastmod,URL url, SpiderThread spider,String lang,
	                    byte[] bodyDigest, String beginLink,String encoding, String[] langAllow, int taskId){
    inputbuffer = buffer;
    bufferLength = length;
    this.type = type;
    lastModified = lastmod;
    this.url = url;
    this.spider = spider;
    this.lang = lang;
    this.beginLink = beginLink;
    this.encoding = encoding;
    this.langAllow = langAllow;
    this.bodyDigest = bodyDigest;
    this.taskId = taskId;
  }

  public FlyBall(CachedMetaData cmd,String beginLink, byte[] body, int taskId){
    inputbuffer = body;
    bufferLength = cmd.fileLength;
    type = cmd.contentType;
    lastModified = cmd.lastModified;
    url = cmd.url;
    spider = null;
    lang = cmd.lang;
    this.beginLink = beginLink;
    this.encoding = cmd.encoding;
    this.langAllow = null;
    this.bodyDigest = cmd.bodyDigest;
    this.taskId = taskId;
  }

}