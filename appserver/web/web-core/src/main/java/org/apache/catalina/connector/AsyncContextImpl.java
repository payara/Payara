/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
 */

package org.apache.catalina.connector;

import org.apache.catalina.ContainerEvent;
import org.apache.catalina.LogFacade;
import org.apache.catalina.Globals;
import org.apache.catalina.core.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

class AsyncContextImpl implements AsyncContext {
    // Note...this constant is also defined in org.glassfish.weld.WeldDeployer.  If it changes here it must
    // change there as well.  The reason it is duplicated is so that a dependency from web-core to gf-weld-connector
    // is not necessary.
    private static final String WELD_LISTENER = "org.jboss.weld.servlet.WeldListener";


    /*
     * Event notification types for async mode
     */
    static enum AsyncEventType { COMPLETE, TIMEOUT, ERROR, START_ASYNC }

    private static final Logger log = LogFacade.getLogger();
    private static final ResourceBundle rb = log.getResourceBundle();


    // Default timeout for async operations
    private static final long DEFAULT_ASYNC_TIMEOUT_MILLIS = 30000L;

    // Thread pool for async dispatches
    private static final ExecutorService pool =
        Executors.newCachedThreadPool(new AsyncPoolThreadFactory());

    // The original (unwrapped) request
    private Request origRequest;

    // The possibly wrapped request passed to ServletRequest.startAsync
    private ServletRequest servletRequest;

    // The possibly wrapped response passed to ServletRequest.startAsync
    private ServletResponse servletResponse;

    private boolean isOriginalRequestAndResponse = false;

    private boolean isStartAsyncWithZeroArg = false;

    // defaults to false
    private AtomicBoolean isDispatchInProgress = new AtomicBoolean();

    private ThreadLocal<Boolean> isDispatchInScope = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private AtomicBoolean isOkToConfigure = new AtomicBoolean(true);

    private long asyncTimeoutMillis = DEFAULT_ASYNC_TIMEOUT_MILLIS;

    private final Queue<AsyncListener> listenerQueue = new ConcurrentLinkedQueue<AsyncListener>();

    private final LinkedList<AsyncListenerContext> asyncListenerContexts =
        new LinkedList<AsyncListenerContext>();

    // The number of times this AsyncContext has been reinitialized via a call
    // to ServletRequest#startAsync
    private AtomicInteger startAsyncCounter = new AtomicInteger(0);
    // Has AsyncContext.complete been called?
    private final AtomicBoolean isAsyncCompleteCalled = new AtomicBoolean();
    // Has AsyncContext.complete been really completed?
    private final AtomicBoolean isAsyncCompleted = new AtomicBoolean();
    private volatile boolean delayAsyncDispatchAndComplete = true;

    private ThreadLocal<Boolean> isStartAsyncInScope = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    private Handler handler = null;

    /**
     * Constructor
     *
     * @param origRequest the original (unwrapped) request
     * @param servletRequest the possibly wrapped request passed to
     * ServletRequest.startAsync
     * @param servletResponse the possibly wrapped response passed to
     * ServletRequest.startAsync
     * @param isStartAsyncWithZeroArg true if the zero-arg version of
     * startAsync was called, false otherwise
     */
    AsyncContextImpl(Request origRequest,
                            ServletRequest servletRequest,
                            Response origResponse,
                            ServletResponse servletResponse,
                            boolean isStartAsyncWithZeroArg) {
        this.origRequest = origRequest;
        init(servletRequest, servletResponse, isStartAsyncWithZeroArg);
    }

    @Override
    public ServletRequest getRequest() {
        return servletRequest;
    }

    Request getOriginalRequest() {
        return origRequest;
    }

    @Override
    public ServletResponse getResponse() {
        return servletResponse;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return isOriginalRequestAndResponse;
    }

    @Override
    public void dispatch() {
        ApplicationDispatcher dispatcher =
            (ApplicationDispatcher)getZeroArgDispatcher(
                origRequest, servletRequest, isStartAsyncWithZeroArg);

        dispatch(dispatcher, null, null);
    }

    @Override
    public void dispatch(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Null path");
        }
        ApplicationDispatcher dispatcher = (ApplicationDispatcher)
            servletRequest.getRequestDispatcher(path);

        dispatch(dispatcher, null, path);
    }

    @Override
    public void dispatch(ServletContext context, String path) {
        if (path == null || context == null) {
            throw new IllegalArgumentException("Null context or path");
        }
        ApplicationDispatcher dispatcher = (ApplicationDispatcher)
            context.getRequestDispatcher(path);

        dispatch(dispatcher, context, path);
    }

    private void dispatch(ApplicationDispatcher dispatcher,
            ServletContext context, String path) {

        isDispatchInScope.set(true);
        if (dispatcher != null) {
            if (isDispatchInProgress.compareAndSet(false, true)) {
                if (delayAsyncDispatchAndComplete) {
                    handler = new Handler(this, dispatcher);
                } else {
                    pool.execute(new Handler(this, dispatcher));
                }
            } else {
                String msg = rb.getString(LogFacade.ASYNC_DISPATCH_ALREADY_IN_PROGRESS_EXCEPTION);
                throw new IllegalStateException(msg);
            }
        } else {
            // Should never happen, because any unmapped paths will be
            // mapped to the DefaultServlet
            if (context == null && path == null) {
                log.log(Level.WARNING, LogFacade.UNABLE_DETERMINE_TARGET_OF_DISPATCHER);
            } else if (context == null && path != null) {
                log.log(Level.WARNING, LogFacade.UNABLE_ACQUIRE_REQUEST_DISPATCHER, path);
            } else {
                log.log(Level.WARNING, LogFacade.UNABLE_ACQUIRE_REQUEST_DISPATCHER_IN_SERVLET_CONTEXT,
                        new Object[] {path, context.getContextPath()});
            }
        }
    }

    void invokeDelayDispatch() {
        if (handler != null) {
            pool.execute(handler);
            handler = null;
        }
    }

    boolean isDispatchInScope() {
        return isDispatchInScope.get();
    }
    
    boolean getAndResetDispatchInScope() {
        final boolean flag = isDispatchInScope.get();
        isDispatchInScope.set(Boolean.FALSE);
        return flag;
    }


    boolean isDelayAsyncDispatchAndComplete() {
        return delayAsyncDispatchAndComplete;
    }

    void setDelayAsyncDispatchAndComplete(boolean delayAsync) {
        delayAsyncDispatchAndComplete = delayAsync;
    }

    boolean isAsyncComplete() {
        return isAsyncCompleteCalled.get();
    }

    @Override
    public void complete() {
        tryComplete(true);
    }

    /**
     * Try to complete the AsyncContext, if it hasn't completed yet
     * and if service() thread doesn't rely on request/response existence.
     * @param failIfCompleted 
     */
    void tryComplete(final boolean failIfCompleted) {
        if (isAsyncCompleteCalled.compareAndSet(false, true)) {
            if (delayAsyncDispatchAndComplete) {
                return;
            }
            
            doComplete();
        } else if (failIfCompleted) {
            throw new IllegalStateException(rb.getString(
                    LogFacade.REQUEST_ALREADY_RELEASED_EXCEPTION));
        }
    }

    private void doComplete() {
        if (isAsyncCompleted.compareAndSet(false, true)) {
            origRequest.asyncComplete();
        }
    }
    
    void processAsyncOperations() {
        if (isDispatchInScope()) {
            invokeDelayDispatch();
        } else if (isAsyncComplete()) {
            doComplete();
        }
    }

    
    /**
     * The method is called once service thread finished with the
     * request/response processing and doesn't rely on its existence anymore.
     * 
     * Now it's safe to finish async request/response processing.
     */
    void onExitService() {
        delayAsyncDispatchAndComplete = false;
        processAsyncOperations();
    }
    
    @Override
    public void start(Runnable run) {
        ClassLoader oldCL = null;
        if (Globals.IS_SECURITY_ENABLED) {
            PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
            oldCL = AccessController.doPrivileged(pa);
        } else {
            oldCL = Thread.currentThread().getContextClassLoader();
        }

        try {
            ClassLoader newCL = origRequest.getContext().getLoader().getClassLoader();
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(newCL);
            }

            pool.execute(run);
        } finally {
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(oldCL);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
    }

    @Override
    public void addListener(AsyncListener listener) {
        addListener(listener, this.servletRequest, this.servletResponse);
    }

    @Override
    public void addListener(AsyncListener listener,
                            ServletRequest servletRequest,
                            ServletResponse servletResponse) {
        if (listener == null) {
            throw new IllegalArgumentException("Null listener");
        }

        if (servletRequest == null || servletResponse == null) {
            throw new IllegalArgumentException(
                "Null request, or response");
        }

        if (!isOkToConfigure.get()) {
            String msg = rb.getString(LogFacade.ASYNC_CONTEXT_ADD_LISTENER_EXCEPTION);
            throw new IllegalStateException(msg);
        }

        synchronized(asyncListenerContexts) {
            asyncListenerContexts.add(new AsyncListenerContext(
                listener, servletRequest, servletResponse));
        }
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> clazz)
            throws ServletException {
        T listener = null;
        StandardContext ctx = (StandardContext) origRequest.getContext();
        if (ctx != null) {
            try {
                listener = ctx.createListenerInstance(clazz);
                listenerQueue.add(listener);
            } catch (Throwable t) {
                throw new ServletException(t);
            }
        }
        return listener;
    }

    @Override
    public void setTimeout(long timeout) {
        if (!isOkToConfigure.get()) {
            String msg = rb.getString(LogFacade.ASYNC_CONTEXT_SET_TIMEOUT_EXCEPTION);
            throw new IllegalStateException(msg);
        }
        asyncTimeoutMillis = timeout;
//        origRequest.setAsyncTimeout(timeout);
    }

    @Override
    public long getTimeout() {
        return asyncTimeoutMillis;
    }

    /*
     * Reinitializes this AsyncContext with the given request and response.
     *
     * @param servletRequest the ServletRequest with which to initialize
     * the AsyncContext
     * @param servletResponse the ServletResponse with which to initialize
     * the AsyncContext
     * @param isStartAsyncWithZeroArg true if the zero-arg version of
     * startAsync was called, false otherwise
     */
    void reinitialize(ServletRequest servletRequest,
                      ServletResponse servletResponse,
                      boolean isStartAsyncWithZeroArg) {

        init(servletRequest, servletResponse, isStartAsyncWithZeroArg);
        isDispatchInProgress.set(false);
        setOkToConfigure(true);
        startAsyncCounter.incrementAndGet();
        notifyAsyncListeners(AsyncEventType.START_ASYNC, null);
    }

    /**
     * @return value true if calls to AsyncContext#addListener and
     * AsyncContext#setTimeout will be accepted, and false if these
     * calls will result in an IllegalStateException
     */
    boolean isOkToConfigure() {
        return isOkToConfigure.get();
    }

    /**
     * @param value true if calls to AsyncContext#addListener and
     * AsyncContext#setTimeout will be accepted, and false if these
     * calls will result in an IllegalStateException
     */
    void setOkToConfigure(boolean value) {
        isOkToConfigure.set(value);
    }

    private void init(ServletRequest servletRequest,
            ServletResponse servletResponse, boolean isStartAsyncWithZeroArg) {

        this.servletRequest = servletRequest;
        this.servletResponse = servletResponse;
        // If original or container-wrapped request and response,
        // AsyncContext#hasOriginalRequestAndResponse must return true;
        // false otherwise (i.e., if application-wrapped)
        this.isOriginalRequestAndResponse =
                ((servletRequest instanceof RequestFacade ||
                servletRequest instanceof ApplicationHttpRequest) &&
                (servletResponse instanceof ResponseFacade ||
                servletResponse instanceof ApplicationHttpResponse));

        this.isStartAsyncWithZeroArg = isStartAsyncWithZeroArg;
    }

    /**
     * Determines the dispatcher of a zero-argument async dispatch for the
     * given request.
     *
     * @return the dispatcher of the zero-argument async dispatch
     */
    private RequestDispatcher getZeroArgDispatcher(
            Request origRequest, ServletRequest servletRequest,
            boolean isStartAsyncWithZeroArg) {

        String dispatchTarget = null;
        boolean isNamed = false;
        if ((!isStartAsyncWithZeroArg) &&
                servletRequest instanceof HttpServletRequest) {

            HttpServletRequest req = (HttpServletRequest)servletRequest;
            dispatchTarget = getCombinedPath(req);
        } else {
            DispatchTargetsInfo dtInfo = (DispatchTargetsInfo)origRequest.getAttribute(
                    ApplicationDispatcher.LAST_DISPATCH_REQUEST_PATH_ATTR);
            if (dtInfo != null) {
                dispatchTarget = dtInfo.getLastDispatchTarget();
                isNamed = dtInfo.isLastNamedDispatchTarget();
            }
            if (dispatchTarget == null) {
                dispatchTarget = getCombinedPath(origRequest);
            }
        }

        RequestDispatcher dispatcher = null;
        if (dispatchTarget != null) {
            dispatcher = ((isNamed) ?
                    servletRequest.getServletContext().getNamedDispatcher(dispatchTarget) :
                    servletRequest.getRequestDispatcher(dispatchTarget));
        }

        return dispatcher;
    }

    private String getCombinedPath(HttpServletRequest req) {
        String servletPath = req.getServletPath();
        if (servletPath == null) {
            return null;
        }
        String pathInfo = req.getPathInfo();
        if (pathInfo == null) {
            return servletPath;
        }
        return servletPath + pathInfo;
    }

    static class Handler implements Runnable {

        private final AsyncContextImpl asyncContext;
        private final ApplicationDispatcher dispatcher;

        Handler(AsyncContextImpl asyncContext,
                ApplicationDispatcher dispatcher) {
            this.asyncContext = asyncContext;
            this.dispatcher = dispatcher;
        }

        @Override
        public void run() {
            asyncContext.isStartAsyncInScope.set(Boolean.TRUE);
            Request origRequest = asyncContext.getOriginalRequest();
            origRequest.setAttribute(Globals.DISPATCHER_TYPE_ATTR,
                                     DispatcherType.ASYNC);
            origRequest.setAsyncStarted(false);
            int startAsyncCurrent = asyncContext.startAsyncCounter.get();
            try {
                asyncContext.setDelayAsyncDispatchAndComplete(true);
                dispatcher.dispatch(asyncContext.getRequest(),
                    asyncContext.getResponse(), DispatcherType.ASYNC);

                asyncContext.setDelayAsyncDispatchAndComplete(false);

                asyncContext.processAsyncOperations();

                if ((!asyncContext.isAsyncComplete()) &&
                        asyncContext.startAsyncCounter.compareAndSet(
                        startAsyncCurrent, startAsyncCurrent)) {
                    /*
                     * Close the response after the dispatch target has
                     * completed execution, unless the dispatch target has called
                     * ServletRequest#startAsync, in which case the AsyncContext's
                     * startAsyncCounter will be greater than it was before the
                     * dispatch
                     */
                    asyncContext.complete();
                } else {
                    // Reset async timeout
                    origRequest.setAsyncTimeout(asyncContext.getTimeout());
                }
            } catch (Throwable t) {
                asyncContext.setDelayAsyncDispatchAndComplete(false);
                asyncContext.notifyAsyncListeners(AsyncEventType.ERROR, t);
                origRequest.errorDispatchAndComplete(t);
            } finally {
                asyncContext.isStartAsyncInScope.set(Boolean.FALSE);
            }
        }
    }

    boolean isStartAsyncInScope() {
        return isStartAsyncInScope.get().booleanValue();
    }

    /*
     * Notifies all AsyncListeners of the given async event type
     */
    void notifyAsyncListeners(AsyncEventType asyncEventType, Throwable t) {
        LinkedList<AsyncListenerContext> clone;
        synchronized(asyncListenerContexts) {
            if (asyncListenerContexts.isEmpty()) {
                return;
            }
            clone =
                new LinkedList<AsyncListenerContext>(asyncListenerContexts);
            if (asyncEventType.equals(AsyncEventType.START_ASYNC)) {
                asyncListenerContexts.clear();
            }
        }

        ClassLoader oldCL;
        if (Globals.IS_SECURITY_ENABLED) {
            PrivilegedAction<ClassLoader> pa = new PrivilegedGetTccl();
            oldCL = AccessController.doPrivileged(pa);
        } else {
            oldCL = Thread.currentThread().getContextClassLoader();
        }

        try {
            ClassLoader newCL = origRequest.getContext().getLoader().getClassLoader();
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(newCL);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(newCL);
            }

            ServletRequestListener weldListener = getWeldListener();
            if ( weldListener != null ) {
                // must fire a request initialized so CDI can associate a request with the request and session contexts
                ServletRequestEvent event = new ServletRequestEvent(origRequest.getContext().getServletContext(), origRequest);
                weldListener.requestInitialized(event);
            }

            try {
                for (AsyncListenerContext asyncListenerContext : clone) {
                    AsyncListener asyncListener =
                        asyncListenerContext.getAsyncListener();
                    AsyncEvent asyncEvent = new AsyncEvent(
                        this, asyncListenerContext.getRequest(),
                        asyncListenerContext.getResponse(), t);
                    try {
                        switch (asyncEventType) {
                            case COMPLETE:
                                asyncListener.onComplete(asyncEvent);
                                break;
                            case TIMEOUT:
                                asyncListener.onTimeout(asyncEvent);
                                break;
                            case ERROR:
                                asyncListener.onError(asyncEvent);
                                break;
                            case START_ASYNC:
                                asyncListener.onStartAsync(asyncEvent);
                                break;
                            default: // not possible
                                break;
                        }
                    } catch (Throwable throwable) {
                        log.log(Level.WARNING, LogFacade.ERROR_INVOKE_ASYNCLISTENER,
                                throwable);
                    }
                }
            } finally {
                if ( weldListener != null ) {
                    ServletRequestEvent event = new ServletRequestEvent(origRequest.getContext().getServletContext(), origRequest);
                    weldListener.requestDestroyed(event);
                }
            }

        } finally {
            if (Globals.IS_SECURITY_ENABLED) {
                PrivilegedAction<Void> pa = new PrivilegedSetTccl(oldCL);
                AccessController.doPrivileged(pa);
            } else {
                Thread.currentThread().setContextClassLoader(oldCL);
            }
        }
    }

    private ServletRequestListener getWeldListener() {
        List<EventListener> eventListeners = origRequest.getContext().getApplicationEventListeners();
        if ( eventListeners != null ) {
            for ( EventListener listener : eventListeners ) {
                if ( listener.getClass().getName().equals( WELD_LISTENER ) ) {
                    return (ServletRequestListener) listener;
                }
            }
        }

        return null;
    }

    static ExecutorService getExecutorService() {
        return pool;
    }

    void clear() {
        synchronized(asyncListenerContexts) {
            asyncListenerContexts.clear();
        }

        StandardContext ctx = (StandardContext) origRequest.getContext();
        if (ctx != null) {
            for (AsyncListener l : listenerQueue) {
                ctx.fireContainerEvent(ContainerEvent.PRE_DESTROY, l);
            }
        }
        listenerQueue.clear();
        servletRequest = null;
        servletResponse = null;
        origRequest = null;
    }

    /**
     * Class holding all the information required for invoking an
     * AsyncListener (including the AsyncListener itself).
     */
    private static class AsyncListenerContext {

        private AsyncListener listener;
        private ServletRequest request;
        private ServletResponse response;

        public AsyncListenerContext(AsyncListener listener,
                                    ServletRequest request,
                                    ServletResponse response) {
            this.listener = listener;
            this.request = request;
            this.response = response;
        }

        public AsyncListener getAsyncListener() {
            return listener;
        }

        public ServletRequest getRequest() {
            return request;
        }

        public ServletResponse getResponse() {
            return response;
        }
    }


    private static final class AsyncPoolThreadFactory implements ThreadFactory {

        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
        private final AtomicInteger counter = new AtomicInteger(0);


        // ------------------------------------------ Methods from ThreadFactory


        @Override
        public Thread newThread(Runnable r) {

            final Thread t = defaultFactory.newThread(r);
            t.setName("glassfish-web-async-thread-" + counter.incrementAndGet());
            return t;

        }

    } // END AsyncPoolThreadFactory

    private static class PrivilegedSetTccl implements PrivilegedAction<Void> {

        private ClassLoader cl;

        PrivilegedSetTccl(ClassLoader cl) {
            this.cl = cl;
        }

        @Override
        public Void run() {
            Thread.currentThread().setContextClassLoader(cl);
            return null;
        }
    }

    private static class PrivilegedGetTccl
            implements PrivilegedAction<ClassLoader> {

        @Override
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
