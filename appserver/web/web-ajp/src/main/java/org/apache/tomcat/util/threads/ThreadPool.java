/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.threads;

import org.apache.tomcat.util.res.StringManager;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread pool that is trying to copy the apache process management.
 *
 * @author Gal Shachor
 */
public class ThreadPool  {

    private static Logger log = Logger.getLogger(ThreadPool.class.getName());

    private static final StringManager sm =
        StringManager.getManager("org.apache.tomcat.util.threads.res");

    private static boolean logfull=true;

    /*
     * Default values ...
     */
    public static final int MAX_THREADS = 200;
    public static final int MAX_THREADS_MIN = 10;
    public static final int MAX_SPARE_THREADS = 50;
    public static final int MIN_SPARE_THREADS = 4;
    public static final int WORK_WAIT_TIMEOUT = 60*1000;

    /*
     * Where the threads are held.
     */
    protected ControlRunnable[] pool = null;

    /*
     * A monitor thread that monitors the pool for idel threads.
     */
    protected MonitorRunnable monitor;


    /*
     * Max number of threads that you can open in the pool.
     */
    protected int maxThreads;

    /*
     * Min number of idel threads that you can leave in the pool.
     */
    protected int minSpareThreads;

    /*
     * Max number of idel threads that you can leave in the pool.
     */
    protected int maxSpareThreads;

    /*
     * Number of threads in the pool.
     */
    protected int currentThreadCount;

    /*
     * Number of busy threads in the pool.
     */
    protected int currentThreadsBusy;

    /*
     * Flag that the pool should terminate all the threads and stop.
     */
    protected boolean stopThePool;

    /* Flag to control if the main thread is 'daemon' */
    protected boolean isDaemon=true;

    /** The threads that are part of the pool.
     * Key is Thread, value is the ControlRunnable
     */
    protected Hashtable<Thread, ControlRunnable> threads =
            new Hashtable<Thread, ControlRunnable>();

    protected Vector<ThreadPoolListener> listeners =
            new Vector<ThreadPoolListener>();

    /** Name of the threadpool
     */
    protected String name = "TP";

    /**
     * Sequence.
     */
    protected int sequence = 1;


    /**
     * Constructor.
     */    
    public ThreadPool() {
        maxThreads = MAX_THREADS;
        maxSpareThreads = MAX_SPARE_THREADS;
        minSpareThreads = MIN_SPARE_THREADS;
        currentThreadCount = 0;
        currentThreadsBusy = 0;
        stopThePool = false;
    }


    /** Create a ThreadPool instance.
     *
     * @param jmx True if you want a pool with JMX support. A regular pool
     *  will be returned if JMX or the modeler are not available.
     *
     * @return ThreadPool instance. If JMX support is requested, you need to
     *   call register() in order to set a name.
     */
    public static ThreadPool createThreadPool(boolean jmx) {
//        if( jmx ) {
//            try {
//                Class.forName( "org.apache.tomcat.util.modeler.Registry");
//                Class tpc=Class.forName( "org.apache.tomcat.util.threads.ThreadPoolMX");
//                ThreadPool res=(ThreadPool)tpc.newInstance();
//                return res;
//            } catch( Exception ex ) {
//            }
//        }
        return new ThreadPool();
    }

    public synchronized void start() {
	stopThePool=false;
        currentThreadCount  = 0;
        currentThreadsBusy  = 0;

        adjustLimits();

        pool = new ControlRunnable[maxThreads];

        openThreads(minSpareThreads);
        monitor = new MonitorRunnable(this);
        if (maxSpareThreads < maxThreads) {
            monitor = new MonitorRunnable(this);
        }
    }

    public MonitorRunnable getMonitor() {
        return monitor;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMinSpareThreads(int minSpareThreads) {
        this.minSpareThreads = minSpareThreads;
    }

    public int getMinSpareThreads() {
        return minSpareThreads;
    }

    public void setMaxSpareThreads(int maxSpareThreads) {
        this.maxSpareThreads = maxSpareThreads;
    }

    public int getMaxSpareThreads() {
        return maxSpareThreads;
    }

    public int getCurrentThreadCount() {
        return currentThreadCount;
    }

    public int getCurrentThreadsBusy() {
        return currentThreadsBusy;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public static int getDebug() {
        return 0;
    }

    /** The default is true - the created threads will be
     *  in daemon mode. If set to false, the control thread
     *  will not be daemon - and will keep the process alive.
     */
    public void setDaemon( boolean b ) {
        isDaemon=b;
    }
    
    public boolean getDaemon() {
        return isDaemon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getSequence() {
        return sequence++;
    }

    public void addThread( Thread t, ControlRunnable cr ) {
        threads.put( t, cr );
        for( int i=0; i<listeners.size(); i++ ) {
            ThreadPoolListener tpl=(ThreadPoolListener)listeners.elementAt(i);
            tpl.threadStart(this, t);
        }
    }

    public void removeThread( Thread t ) {
        threads.remove(t);
        for( int i=0; i<listeners.size(); i++ ) {
            ThreadPoolListener tpl=(ThreadPoolListener)listeners.elementAt(i);
            tpl.threadEnd(this, t);
        }
    }

    public void addThreadPoolListener( ThreadPoolListener tpl ) {
        listeners.addElement( tpl );
    }

    public Enumeration getThreads(){
        return threads.keys();
    }

    public void run(Runnable r) {
        ControlRunnable c = findControlRunnable();
        c.runIt(r);
    }    
    
    //
    // You may wonder what you see here ... basically I am trying
    // to maintain a stack of threads. This way locality in time
    // is kept and there is a better chance to find residues of the
    // thread in memory next time it runs.
    //

    /**
     * Executes a given Runnable on a thread in the pool, block if needed.
     */
    public void runIt(ThreadPoolRunnable r) {
        if(null == r) {
            throw new NullPointerException();
        }

        ControlRunnable c = findControlRunnable();
        c.runIt(r);
    }

    private ControlRunnable findControlRunnable() {
        ControlRunnable c=null;

        if ( stopThePool ) {
            throw new IllegalStateException();
        }

        // Obtain a free thread from the pool.
        synchronized(this) {

            while (currentThreadsBusy == currentThreadCount) {
                 // All threads are busy
                if (currentThreadCount < maxThreads) {
                    // Not all threads were open,
                    // Open new threads up to the max number of idel threads
                    int toOpen = currentThreadCount + minSpareThreads;
                    openThreads(toOpen);
                } else {
                    logFull(log, currentThreadCount, maxThreads);
                    // Wait for a thread to become idel.
                    try {
                        this.wait();
                    }
                    // was just catch Throwable -- but no other
                    // exceptions can be thrown by wait, right?
                    // So we catch and ignore this one, since
                    // it'll never actually happen, since nowhere
                    // do we say pool.interrupt().
                    catch(InterruptedException e) {
                        log.log(Level.SEVERE, "Unexpected exception", e);
                    }
		    if( log.isLoggable(Level.FINE) ) {
			log.fine("Finished waiting: CTC="+currentThreadCount +
				 ", CTB=" + currentThreadsBusy);
                    }
                    // Pool was stopped. Get away of the pool.
                    if( stopThePool) {
                        break;
                    }
                }
            }
            // Pool was stopped. Get away of the pool.
            if(0 == currentThreadCount || stopThePool) {
                throw new IllegalStateException();
            }
                    
            // If we are here it means that there is a free thread. Take it.
            int pos = currentThreadCount - currentThreadsBusy - 1;
            c = pool[pos];
            pool[pos] = null;
            currentThreadsBusy++;

        }
        return c;
    }

    private static void logFull(Logger loghelper, int currentThreadCount,
                                int maxThreads) {
	if( logfull ) {
            log.severe(sm.getString("threadpool.busy",
                                    Integer.valueOf(currentThreadCount),
                                    Integer.valueOf(maxThreads)));
            logfull=false;
        } else if( log.isLoggable(Level.FINE) ) {
            log.fine("All threads are busy " + currentThreadCount + " " +
                     maxThreads );
        }
    }

    /**
     * Stop the thread pool
     */
    public synchronized void shutdown() {
        if(!stopThePool) {
            stopThePool = true;
            monitor.terminate();
            monitor = null;
            for(int i = 0 ; i < currentThreadCount - currentThreadsBusy; i++) {
                try {
                    pool[i].terminate();
                } catch(Throwable t) {
                    /*
		     * Do nothing... The show must go on, we are shutting
		     * down the pool and nothing should stop that.
		     */
		    log.log(Level.SEVERE,
                            "Ignored exception while shutting down thread pool",
                            t);
                }
            }
            currentThreadsBusy = currentThreadCount = 0;
            pool = null;
            notifyAll();
        }
    }

    /**
     * Called by the monitor thread to harvest idle threads.
     */
    protected synchronized void checkSpareControllers() {

        if(stopThePool) {
            return;
        }

        if((currentThreadCount - currentThreadsBusy) > maxSpareThreads) {
            int toFree = currentThreadCount -
                         currentThreadsBusy -
                         maxSpareThreads;

            for(int i = 0 ; i < toFree ; i++) {
                ControlRunnable c = pool[currentThreadCount - currentThreadsBusy - 1];
                c.terminate();
                pool[currentThreadCount - currentThreadsBusy - 1] = null;
                currentThreadCount --;
            }

        }

    }

    /**
     * Returns the thread to the pool.
     * Called by threads as they are becoming idel.
     */
    protected synchronized void returnController(ControlRunnable c) {

        if(0 == currentThreadCount || stopThePool) {
            c.terminate();
            return;
        }

        // atomic
        currentThreadsBusy--;

        pool[currentThreadCount - currentThreadsBusy - 1] = c;
        notify();
    }

    /**
     * Inform the pool that the specific thread finish.
     *
     * Called by the ControlRunnable.run() when the runnable
     * throws an exception.
     */
    protected synchronized void notifyThreadEnd(ControlRunnable c) {
        currentThreadsBusy--;
        currentThreadCount --;
        notify();
    }


    /*
     * Checks for problematic configuration and fix it.
     * The fix provides reasonable settings for a single CPU
     * with medium load.
     */
    protected void adjustLimits() {
        if(maxThreads <= 0) {
            maxThreads = MAX_THREADS;
        } else if (maxThreads < MAX_THREADS_MIN) {
            log.warning(sm.getString("threadpool.max_threads_too_low",
                                     Integer.valueOf(maxThreads),
                                     Integer.valueOf(MAX_THREADS_MIN)));
            maxThreads = MAX_THREADS_MIN;
        }

        if(maxSpareThreads >= maxThreads) {
            maxSpareThreads = maxThreads;
        }

        if(maxSpareThreads <= 0) {
            if(1 == maxThreads) {
                maxSpareThreads = 1;
            } else {
                maxSpareThreads = maxThreads/2;
            }
        }

        if(minSpareThreads >  maxSpareThreads) {
            minSpareThreads =  maxSpareThreads;
        }

        if(minSpareThreads <= 0) {
            if(1 == maxSpareThreads) {
                minSpareThreads = 1;
            } else {
                minSpareThreads = maxSpareThreads/2;
            }
        }
    }

    /** Create missing threads.
     *
     * @param toOpen Total number of threads we'll have open
     */
    protected void openThreads(int toOpen) {

        if(toOpen > maxThreads) {
            toOpen = maxThreads;
        }

        for(int i = currentThreadCount ; i < toOpen ; i++) {
            pool[i - currentThreadsBusy] = new ControlRunnable(this);
        }

        currentThreadCount = toOpen;
    }

    /** 
     * Periodically execute an action - cleanup in this case
     */
    public static class MonitorRunnable implements Runnable {
        ThreadPool p;
        Thread     t;
        int interval=WORK_WAIT_TIMEOUT;
        boolean    shouldTerminate;

        MonitorRunnable(ThreadPool p) {
            this.p=p;
            this.start();
        }

        public void start() {
            shouldTerminate = false;
            t = new Thread(this);
            t.setDaemon(p.getDaemon() );
	    t.setName(p.getName() + "-Monitor");
            t.start();
        }

        public void setInterval(int i ) {
            this.interval=i;
        }

        public void run() {
            while(true) {
                try {

                    // Sleep for a while.
                    synchronized(this) {
                        this.wait(WORK_WAIT_TIMEOUT);
                    }

                    // Check if should terminate.
                    // termination happens when the pool is shutting down.
                    if(shouldTerminate) {
                        break;
                    }

                    // Harvest idle threads.
                    p.checkSpareControllers();

                } catch(Throwable t) {
		    ThreadPool.log.log(Level.SEVERE, "Unexpected exception",
                                       t);
                }
            }
        }

        public void stop() {
            this.terminate();
        }

	/** Stop the monitor
	 */
        public synchronized void terminate() {
            shouldTerminate = true;
            this.notify();
        }
    }

    /**
     * A Thread object that executes various actions ( ThreadPoolRunnable )
     *  under control of ThreadPool
     */
    public static class ControlRunnable implements Runnable {
        /**
	 * ThreadPool where this thread will be returned
	 */
        private ThreadPool p;

	/**
	 * The thread that executes the actions
	 */
        private ThreadWithAttributes     t;

	/**
	 * The method that is executed in this thread
	 */
        
        private ThreadPoolRunnable   toRun;
        private Runnable toRunRunnable;

	/**
	 * Stop this thread
	 */
	private boolean    shouldTerminate;

	/**
	 * Activate the execution of the action
	 */
        private boolean    shouldRun;

	/**
	 * Per thread data - can be used only if all actions are
	 *  of the same type.
	 *  A better mechanism is possible ( that would allow association of
	 *  thread data with action type ), but right now it's enough.
	 */
	private boolean noThData;

	/**
	 * Start a new thread, with no method in it
	 */
        ControlRunnable(ThreadPool p) {
            toRun = null;
            shouldTerminate = false;
            shouldRun = false;
            this.p = p;
            t = new ThreadWithAttributes(p, this);
            t.setDaemon(true);
            t.setName(p.getName() + "-Processor" + p.getSequence());
            p.addThread( t, this );
	    noThData=true;
            t.start();
        }

        public void run() {
            boolean _shouldRun = false;
            boolean _shouldTerminate = false; 
            ThreadPoolRunnable _toRun = null;
          try {
            while(true) {
                try {
                    /* Wait for work. */
                    synchronized(this) {
                        while (!shouldRun && !shouldTerminate) {
                            this.wait();
                        }
                        _shouldRun = shouldRun;
                        _shouldTerminate = shouldTerminate;
                        _toRun = toRun;
                    }

                    if( _shouldTerminate ) {
                            if( p.log.isLoggable(Level.FINE))
                                p.log.fine( "Terminate");
                            break;
                    }

                    /* Check if should execute a runnable.  */
                    try {
                        if(noThData) {
                            if( _toRun != null ) {
                                Object thData[]=_toRun.getInitData();
                                t.setThreadData(p, thData);
                                if(p.log.isLoggable(Level.FINE))
                                    p.log.fine( "Getting new thread data");
                            }
                            noThData = false;
                        }

                        if(_shouldRun) {
			    if( _toRun != null ) { 
                                _toRun.runIt(t.getThreadData(p));
                            } else if( toRunRunnable != null ) { 
                                toRunRunnable.run();
                            } else {
                                if( p.log.isLoggable(Level.FINE))
                                    p.log.fine( "No toRun ???");
                            }
                        }
                    } catch(Throwable t) {
			p.log.severe(sm.getString("threadpool.thread_error",
                                                  t, toRun.toString()));
                       /*
                        * The runnable throw an exception (can be even a ThreadDeath),
                        * signalling that the thread die.
                        *
			* The meaning is that we should release the thread from
			* the pool.
			*/
                        shouldTerminate = true;
                        shouldRun = false;
                        p.notifyThreadEnd(this);
                    } finally {
                        if(_shouldRun) {
                            shouldRun = false;
                            /*
			     * Notify the pool that the thread is now idle.
                             */
                            p.returnController(this);
                        }
                    }

                    /*
		     * Check if should terminate.
		     * termination happens when the pool is shutting down.
		     */
                    if(_shouldTerminate) {
                        break;
                    }
                } catch(InterruptedException ie) { /* for the wait operation */
		    // can never happen, since we don't call interrupt
    		    p.log.log(Level.SEVERE, "Unexpected exception", ie);
                }
            }
          } finally {
              p.removeThread(Thread.currentThread());
          }
        }

        /** Run a task
         *
         * @param toRun
         */
        public synchronized void runIt(Runnable toRun) {
	    this.toRunRunnable = toRun;
	    // Do not re-init, the whole idea is to run init only once per
	    // thread - the pool is supposed to run a single task, that is
	    // initialized once.
            // noThData = true;
            shouldRun = true;
            this.notify();
        }

        /** Run a task
         *
         * @param toRun
         */
        public synchronized void runIt(ThreadPoolRunnable toRun) {
	    this.toRun = toRun;
	    // Do not re-init, the whole idea is to run init only once per
	    // thread - the pool is supposed to run a single task, that is
	    // initialized once.
            // noThData = true;
            shouldRun = true;
            this.notify();
        }

        public void stop() {
            this.terminate();
        }

        public void kill() {
            t.stop();
        }

        public synchronized void terminate() {
            shouldTerminate = true;
            this.notify();
        }
    }

    /** Debug display of the stage of each thread. The return is html style,
     * for display in the console ( it can be easily parsed too )
     *
     * @return thread status as String
     */
    public String threadStatusString() {
        StringBuilder sb=new StringBuilder();
        Iterator it=threads.keySet().iterator();
        sb.append("<ul>");
        while( it.hasNext()) {
            sb.append("<li>");
            ThreadWithAttributes twa=(ThreadWithAttributes)
                    it.next();
            sb.append(twa.getCurrentStage(this) ).append(" ");
            sb.append( twa.getParam(this));
            sb.append( "</li>\n");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    /** Return an array with the status of each thread. The status
     * indicates the current request processing stage ( for tomcat ) or
     * whatever the thread is doing ( if the application using TP provide
     * this info )
     *
     * @return array with the status of each thread
     */
    public String[] getThreadStatus() {
        String status[]=new String[ threads.size()];
        Iterator it=threads.keySet().iterator();
        for( int i=0; ( i<status.length && it.hasNext()); i++ ) {
            ThreadWithAttributes twa=(ThreadWithAttributes)
                    it.next();
            status[i]=twa.getCurrentStage(this);
        }
        return status;
    }

    /** Return an array with the current "param" ( XXX better name ? )
     * of each thread. This is typically the last request.
     *
     * @return array with the current param of each thread
     */
    public String[] getThreadParam() {
        String status[]=new String[ threads.size()];
        Iterator it=threads.keySet().iterator();
        for( int i=0; ( i<status.length && it.hasNext()); i++ ) {
            ThreadWithAttributes twa=(ThreadWithAttributes)
                    it.next();
            Object o=twa.getParam(this);
            status[i]=(o==null)? null : o.toString();
        }
        return status;
    }
    
    /** Interface to allow applications to be notified when
     * a threads are created and stopped.
     */
    public static interface ThreadPoolListener {
        public void threadStart( ThreadPool tp, Thread t);

        public void threadEnd( ThreadPool tp, Thread t);
    }
}
