package DistillerEngine;

import Distiller.Filter;
import java.util.*;

// Hold the filters for each id, used by the comparator thread, and loaded by the Engine Thread

public class Filters {

	private Hashtable<Integer,Filter[]> filterHash;
	
	public Filters(){
		filterHash = new Hashtable<Integer,Filter[]>();
	}
	
	public void setFilters(int taskId, Filter[] filters){
		filterHash.put(new Integer(taskId), filters);
	}
	
	public void clearFilter(int taskId){
		Integer i = new Integer(taskId);
		filterHash.remove(i);
	}
	
	public Filter[] getFilters(int taskId){
		Filter fil[] = filterHash.get(new Integer(taskId));
	    if (fil!=null){ return fil; }
	    return new Filter[0];
	}
	
}
