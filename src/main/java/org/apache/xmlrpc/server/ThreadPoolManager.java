package org.apache.xmlrpc.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class to manage thread resources throughout the application. Singleton
 * Creates a series of cached thread pools that represent named resources.
 * The named resource can be managed as a group of threads
 * @author jg
 *
 */
public class ThreadPoolManager {
	int threadNum = 0;
    PoolThreadFactory dtf ;//= new PoolThreadFactory();
    private static Map<String, ExecutorService> executor = new HashMap<String, ExecutorService>();// = Executors.newCachedThreadPool(dtf);

	public static ThreadPoolManager threadPoolManager = null;
	private ThreadPoolManager() { }
	
	public static ThreadPoolManager getInstance() {
		if( threadPoolManager == null ) {
			threadPoolManager = new ThreadPoolManager();
			// set up pool for system processes
			executor.put("SYSTEM", Executors.newCachedThreadPool(getInstance().new PoolThreadFactory("SYSTEM")));
		}
		return threadPoolManager;
	}
	/**
	 * Create an array of Executors that manage a cached thread pool for
	 * reading topics. One thread pool per topic to notify listeners of data ready
	 * @param threadGroupNames The topics for which thread groups are established
	 */
	public static void init(String[] threadGroupNames) {
		for(String tgn : threadGroupNames) {
			executor.put(tgn, Executors.newCachedThreadPool(getInstance().new PoolThreadFactory(tgn)));
		}
	}
	
	public void waitGroup(String group) {
		try {
			ExecutorService w = executor.get(group);
			synchronized(w) {
				w.wait();
			}
		} catch (InterruptedException e) {
		}
	}
	
	public void waitGroup(String group, long millis) {
		try {
			ExecutorService w = executor.get(group);
			synchronized(w) {
				w.wait(millis);
			}
		} catch (InterruptedException e) {
		}
	}

	public void shutdownGroup(String group) throws InterruptedException {
		ExecutorService w = executor.get(group);
		List<?> tasks = w.shutdownNow();
		if (!w.awaitTermination(60, TimeUnit.SECONDS))
		     System.err.println("Thread Pool for group "+group+" did not terminate");
	}
	
	public void notifyGroup(String group) {
			ExecutorService w = executor.get(group);
			synchronized(w) {
				w.notifyAll();
			}
	}
	
	public int countActiveThreads(String group) {
		ExecutorService w = executor.get(group);
		synchronized(w) {
			return ((ThreadPoolExecutor)w).getActiveCount();
		}
	}
	/**
	 * @param group
	 * @return
	 */
	public ThreadFactory getThreadFactory(String group) {
		ExecutorService w = executor.get(group);
		synchronized(w) {
			return ((ThreadPoolExecutor)w).getThreadFactory();
		}
	}
	
	public ThreadGroup getThreadGroup(String group) {
		ExecutorService w = executor.get(group);
		synchronized(w) {
			return((PoolThreadFactory)((ThreadPoolExecutor)w).getThreadFactory()).getThreadGroup();
		}
	}
	
	public void spin(Runnable r, ThreadGroup group) {
	    executor.get(group.getName()).execute(r);
	}
	
	public void spin(Runnable r, String group) {
	    executor.get(group).execute(r);
	}
	/**
	 * Shut down all threads in all groups
	 */
	public void shutdown() {
		Collection<ExecutorService> ex = executor.values();
		for(ExecutorService e : ex) {
			List<Runnable> spun = e.shutdownNow();
			for(Runnable rs : spun) {
				System.out.println("Marked for Termination:"+rs.toString()+" "+e.toString());
			}
		}
	}
	
	class PoolThreadFactory implements ThreadFactory {
		ThreadGroup threadGroup;
	
		public PoolThreadFactory(String threadGroupName) {
			threadGroup = new ThreadGroup(threadGroupName);
		}	
		public ThreadGroup getThreadGroup() { return threadGroup; }		
	    public Thread newThread(Runnable r) {
	        Thread thread = new Thread(threadGroup, r, threadGroup.getName()+(++threadNum));
	        //thread.setDaemon(true);
	        return thread;
	    }
	}
}
