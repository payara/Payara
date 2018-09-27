/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
 */

package org.jvnet.hk2.config;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;

/**
 * Transactions is a singleton service that receives transaction notifications and dispatch these
 * notifications asynchronously to listeners.
 *
 * @author Jerome Dochez
 */

@Service
public final class Transactions implements PostConstruct, PreDestroy {

    // each transaction listener has a notification pump.
    private final List<Provider<ListenerNotifier<TransactionListener, ?, Void>>> listeners =
            new ArrayList<Provider<ListenerNotifier<TransactionListener, ?, Void>>>();

    private final Map<Class, Set<ConfigListener>> typeListeners = new HashMap<Class, Set<ConfigListener>>();

    @Inject @Named("transactions-executor") @Optional
    private ExecutorService executor;

    // all configuration listeners are notified though one notifier.
    private final Provider<ConfigListenerNotifier> configListenerNotifier = new Provider<ConfigListenerNotifier>() {

            private final ConfigListenerNotifier configListenerNotifier = new ConfigListenerNotifier();
            private final CountDownLatch initialized = new CountDownLatch(1);

            public ConfigListenerNotifier get() {
                //synchronized(initialized) {
                    if (initialized.getCount()>0) {
                        configListenerNotifier.start();
                        initialized.countDown();
                    }

                return configListenerNotifier;
            //}
        }
    };

    public void postConstruct() {
        if (executor==null) {
            executor = Executors.newCachedThreadPool();
        }
    }

    public void preDestroy() {
       for (Provider<ListenerNotifier<TransactionListener,  ?, Void>> listener : listeners) {
           listener.get().stop();
       }
       configListenerNotifier.get().stop();
       executor.shutdown();
    }

    /**
     * Abstract notification pump, it adds jobs to the queue and process them in the order
     * jobs were added.
     *
     * Jobs are just a wrapper for events of type <U> and a notification mechanism for
     * completion notification
     *
     * @param <T> type of listener interface
     * @param <U> type of events the listener methods are expecting
     * @param <V> return type of the listener interface methods.
     */
    private abstract class Notifier<T, U, V> {
        
        private final BlockingQueue<FutureTask> pendingJobs = new ArrayBlockingQueue<FutureTask>(50);
        private CountDownLatch latch = new CountDownLatch(1);

        /**
         * Creates the task that will notify the listeners of a particular job.
         * @param job contains the specifics of the notification like the events that need to be notified.
         * @return a task that can be run and return an optional value.
         */
        protected abstract FutureTask<V> prepare(final Job<T, U, V> job);

        /**
         * Adds a job to the notification pump. This job will be processed as soon as all other pending
         * jobs have completed.
         *
         * @param job new notification job.
         * @return a future on the return value.
         */
        public Future<V> add(final Job<T, U, V> job) {
                
            // NOTE that this is put() which blocks, *not* add() which will not block and will
            // throw an IllegalStateException if the queue is full.
            if (latch.getCount()==0) {
                throw new RuntimeException("TransactionListener is inactive, yet jobs are published to it");
            }
            try {
                pendingJobs.put(prepare(job));
            } catch (InterruptedException e ) {
                throw new RuntimeException(e);
            }
            return null;
        }
        
        protected void start() {

            executor.submit(new Runnable() {

                public void run() {
                    while (latch.getCount()>0) {
                        try {
                            final FutureTask job = pendingJobs.take();
                            // when listeners start a transaction themselves, several jobs try to get published
                            // simultaneously so we cannot block the pump while delivering the messages. 
                            executor.submit(new Runnable() {
                                public void run() {
                                    job.run();
                                }
                            });
                        }

                        catch (InterruptedException e) {
                            // do anything here?
                        }
                    }
                }
                
            });
        }

        void stop() {
            latch.countDown();
            // last event to force the close
            pendingJobs.add(prepare(new Job<T, U, V>(null, null) {
                public V process(T target) {
                    return null;
                }
            }));
        }
    }

    /**
     * Default listener notification pump. One thread per listener, jobs processed in
     * the order it was received.
     *
     * @param <T> type of listener interface
     * @param <U> type of events the listener methods are expecting
     * @param <V> return type of the listener interface methods.
     */
    private class ListenerNotifier<T,U,V> extends Notifier<T,U,V> {
        
        final T listener;

        public ListenerNotifier(T listener) {
            this.listener = listener;
        }

        protected FutureTask<V> prepare(final Job<T, U, V> job) {
            return new FutureTask<V>(new Callable<V>() {
                    public V call() throws Exception {
                        try {
                            if ( job.mEvents.size() != 0 ) {
                                return job.process(listener);
                           }
                        } finally {
                            job.releaseLatch();
                        }
                        return null;
                    }
                });
        }   
        
    }

    /**
     * Configuration listener notification pump. All Listeners are notified within their own thread, only on thread
     * takes care of the job pump.
     * 
     */
    private class ConfigListenerNotifier extends Notifier<ConfigListener,PropertyChangeEvent,UnprocessedChangeEvents> {

        protected FutureTask<UnprocessedChangeEvents>
            prepare(final Job<ConfigListener, PropertyChangeEvent, UnprocessedChangeEvents> job) {

        // first, calculate the recipients.
        final Set<ConfigListener> configListeners = new HashSet<ConfigListener>();
            if (job.mEvents != null) {
                for (PropertyChangeEvent event : job.mEvents) {
                    final Dom dom = (Dom) ((ConfigView) Proxy.getInvocationHandler(event.getSource())).getMasterView();
                    configListeners.addAll(dom.getListeners());

                    // we also notify the parent.
                    if (dom.parent()!=null) {
                        configListeners.addAll(dom.parent().getListeners());
                    }

                    // and now, notify all listeners for the changed types.
                    Set<ConfigListener> listeners = typeListeners.get(dom.getProxyType());
                    if (listeners!=null) {
                        configListeners.addAll(listeners);
                    }

                    // we need to check if elements are removed to ensure
                    // the typed listeners are notified.
                    if (event.getNewValue()==null) {
                        Object oldValue = event.getOldValue();
                        if (oldValue instanceof ConfigBeanProxy) {
                            Dom domOldValue = Dom.unwrap((ConfigBeanProxy) oldValue);
                            Set<ConfigListener> typedListeners = typeListeners.get(domOldValue.<ConfigBeanProxy>getProxyType());
                            if (typedListeners!=null) {
                                configListeners.addAll(typedListeners);
                            }
                        }
                    }
                }
            }

            return new FutureTask<UnprocessedChangeEvents>(new Callable<UnprocessedChangeEvents>() {
            public UnprocessedChangeEvents call() throws Exception {

                try {
                    // temporary structure to store our future notifications with pointer to the
                    // originator config listener
                    Map<Future<UnprocessedChangeEvents>, ConfigListener> futures = new HashMap<Future<UnprocessedChangeEvents>, ConfigListener>();

                    for (final ConfigListener listener : configListeners) {
                        // each listener is notified in it's own thread.
                        futures.put(executor.submit(new Callable<UnprocessedChangeEvents>() {
                            public UnprocessedChangeEvents call() throws Exception {
                                UnprocessedChangeEvents e = job.process(listener);
                                return e;

                            }
                        }), listener);
                    }
                    List<UnprocessedChangeEvents> unprocessed = new ArrayList<UnprocessedChangeEvents>(futures.size());
                    for (Map.Entry<Future<UnprocessedChangeEvents>, ConfigListener> futureEntry : futures.entrySet()) {
                        Future<UnprocessedChangeEvents> future = futureEntry.getKey();
                        try {
                            UnprocessedChangeEvents result = future.get(200, TimeUnit.SECONDS);
                            if (result!=null && result.getUnprocessed()!=null && result.getUnprocessed().size()>0) {
                                for (UnprocessedChangeEvent event : result.getUnprocessed()) {
                                    Logger.getAnonymousLogger().log(Level.WARNING, "Unprocessed event : " + event);
                                }
                                unprocessed.add(result);
                            }
                        } catch (InterruptedException e) {
                            Logger.getAnonymousLogger().log(Level.SEVERE, "Config Listener notification got interrupted", e);
                        } catch (ExecutionException e) {
                            Logger.getAnonymousLogger().log(Level.SEVERE, "Config Listener notification got interrupted", e);
                        } catch (TimeoutException e) {
                            ConfigListener cl = futureEntry.getValue();
                            Logger.getAnonymousLogger().log(Level.SEVERE, "Config Listener " + cl.getClass() + " notification took too long", e);
                        }
                    }

                    // all notification have been successful, I just need to notify the unprocessed events.
                    // note these events are always synchronous so far.
                    if (!unprocessed.isEmpty()) {
                        Job unprocessedJob = new UnprocessedEventsJob(unprocessed, null);
                        for (Provider<ListenerNotifier<TransactionListener, ?, Void>> listener : Transactions.this.listeners) {
                            listener.get().add(unprocessedJob);
                        }
                    }
                } finally {
                    job.releaseLatch();
                }

                // in theory I should aggregate my unprocessed events but nobody cares.
                return null;
            }
        });
    }

    }


    /**
        A job contains an optional CountdownLatch so that a caller can learn when the
        transaction has "cleared" by blocking until that time.
     */
    private abstract static class Job<T,U,V> {

        private final CountDownLatch mLatch;
        protected final List<U> mEvents;
        
        public Job(List<U> events, final CountDownLatch latch ) {
            mLatch  = latch;
            mEvents = events;
        }
        
        public void waitForLatch() throws InterruptedException {
            if ( mLatch != null ) {
                mLatch.await();
            }
        }
        
        public void releaseLatch() {
            if ( mLatch != null ) {
                mLatch.countDown();
            }
        }
        
        public abstract V process(T target);
    }
    
    private static class TransactionListenerJob extends Job<TransactionListener, PropertyChangeEvent, Void> {

        public TransactionListenerJob(List<PropertyChangeEvent> events, CountDownLatch latch) {
            super(events,  latch);
        }
        
        @Override
        public Void process(TransactionListener listener) {
            try {
                listener.transactionCommited(mEvents);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class UnprocessedEventsJob extends Job<TransactionListener, UnprocessedChangeEvents, Void> {

        public UnprocessedEventsJob(List<UnprocessedChangeEvents> events, CountDownLatch latch) {
            super(events, latch);
        }

        @Override
        public Void process(TransactionListener listener) {
            try {
                listener.unprocessedTransactedEvents(mEvents);
            } catch(Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private class ConfigListenerJob extends Job<ConfigListener, PropertyChangeEvent, UnprocessedChangeEvents> {

        final PropertyChangeEvent[] eventsArray;

        public ConfigListenerJob(List<PropertyChangeEvent> events, CountDownLatch latch) {
            super(events, latch);
            eventsArray = mEvents.toArray(new PropertyChangeEvent[mEvents.size()]);            
        }

        public UnprocessedChangeEvents process(ConfigListener target) {
            return target.changed(eventsArray);
        }
    }

    /**
     * adds a listener for a particular config type
     * @param listenerType the config type
     * @param listener the config listener
     */
    public synchronized void addListenerForType(Class listenerType, ConfigListener listener) {
        Set<ConfigListener> listeners = typeListeners.get(listenerType);
        if (listeners==null) {
            listeners = new HashSet<ConfigListener>();
            typeListeners.put(listenerType, listeners);
        }
        listeners.add(listener);
    }

    /**
     * removes a listener for a particular config type
     *
     * @param listenerType the config type
     * @param listener the config listener
     * @return true if the listener was removed successfully, false otherwise.
     */
    public synchronized boolean removeListenerForType(Class listenerType, ConfigListener listener) {
        Set<ConfigListener> listeners = typeListeners.get(listenerType);
        if (listeners==null) {
            return false;
        }
        return listeners.remove(listener);


    }
    /**
     * add a new listener to all transaction events.
     *
     * @param listener to be added.
     */
    public void addTransactionsListener(final TransactionListener listener) {
        synchronized(listeners) {
            listeners.add(new Provider<ListenerNotifier<TransactionListener, ?, Void>>() {

                final ListenerNotifier<TransactionListener, PropertyChangeEvent, Void> tsListener = new ListenerNotifier<TransactionListener, PropertyChangeEvent, Void>(listener);
                final CountDownLatch initialized = new CountDownLatch(1);

                public ListenerNotifier<TransactionListener, PropertyChangeEvent, Void> get() {
                    //synchronized(initialized) {
                        if (initialized.getCount()>0) {
                            tsListener.start();
                            initialized.countDown();
                        }
                    //}
                    return tsListener;
                }
            });
        }
    }

    /**
     * Removes an existing listener for transaction events
     * @param listener the registered listener
     * @return true if the listener unregistration was successful
     */
    public boolean removeTransactionsListener(TransactionListener listener) {
        synchronized(listeners) {
            for (Provider<ListenerNotifier<TransactionListener, ?, Void>> holder : listeners) {
                ListenerNotifier info = holder.get();
                if (info.listener==listener) {
                    info.stop();
                    return listeners.remove(holder);
                }
            }
        }
        return false;
    }
    
    public List<TransactionListener> currentListeners() {
        synchronized(listeners) {            
            List<TransactionListener> l = new ArrayList<TransactionListener>();
            for (Provider<ListenerNotifier<TransactionListener, ?, Void>> holder : listeners) {
                ListenerNotifier<TransactionListener, ?, Void> info = holder.get();
                l.add(info.listener);
            }
            return l;
        }
    }


    /**
     * Synchronous notification of a new transactional configuration change operation.
     *
     * @param events list of changes 
     */
    void addTransaction( final List<PropertyChangeEvent> events) {
        addTransaction(events, true);
    }
        
    /**
     * Notification of a new transaction completion
     *
     * @param events accumulated list of changes
     * @param waitTillCleared  synchronous semantics; wait until all change events are sent
     */
    @SuppressWarnings("cast")
    void addTransaction(
        final List<PropertyChangeEvent> events,
        final boolean waitTillCleared ) {
        
        final List<ListenerNotifier<TransactionListener, ?, Void>> listInfos = new ArrayList<ListenerNotifier<TransactionListener, ?, Void>>();
        for (Provider<ListenerNotifier<TransactionListener, ?, Void>> holder : listeners) {
            ListenerNotifier<TransactionListener, ?, Void> info = holder.get();
            listInfos.add(info);
        }
        
        // create a CountDownLatch to implement waiting for events to actually be sent
        final Job<TransactionListener, ?, Void> job = new TransactionListenerJob( events,
                                waitTillCleared ? new CountDownLatch(listInfos.size()) : null);
        
        final ConfigListenerJob configJob = new ConfigListenerJob(events,
                waitTillCleared? new CountDownLatch(1):null);
        
        // NOTE that this is put() which blocks, *not* add() which will not block and will
        // throw an IllegalStateException if the queue is full.
        try {
            for (ListenerNotifier listener : listInfos) {
                listener.add(job);
            }

            configListenerNotifier.get().add(configJob);

            job.waitForLatch();
            configJob.waitForLatch();
        } catch (InterruptedException e ) {
            throw new RuntimeException(e);
        }
    }

    public void waitForDrain() {
        // insert a dummy Job and block until is has been processed.  This guarantees
        // that all prior jobs have finished
        addTransaction( new ArrayList<PropertyChangeEvent>(), true );
        // at this point all prior transactions are guaranteed to have cleared
    }    
}






