package DistillerEngine;

import java.util.*;

public abstract class Scheduler {
	
	private HashSet<Integer> runningTable;
	
	public Scheduler(){
		runningTable = new HashSet<Integer>();
	}
	
	public abstract void readSchedule();

	public abstract SpiderTask getSpiderTask(int i);
	
	public abstract int howManyTasks();

	public abstract boolean shouldIRun(int i);
	
	public abstract void stoppedRunning(int i);

   void markStopped(int i){
		runningTable.remove(new Integer(i));
	}
	
    void markStarted(int i){
		runningTable.add(new Integer(i));
	}
	
    boolean isRunning(int i){
		boolean t= runningTable.contains(new Integer(i));
		if (t){ Logging.finest("Sched: isRunning job:"+i); } else { Logging.finest("Sched: notRunning job:"+i); }
		return t;
	}
}
