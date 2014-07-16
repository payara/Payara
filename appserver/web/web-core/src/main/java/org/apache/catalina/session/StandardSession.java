/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.session;

import com.sun.enterprise.spi.io.BaseIndirectlySerializable;
import org.apache.catalina.*;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.security.SecurityUtil;
import org.apache.catalina.util.Enumerator;
import org.apache.tomcat.util.security.PrivilegedSetTccl;
import org.glassfish.logging.annotation.LogMessageInfo;

import javax.servlet.ServletContext;
import javax.servlet.http.*;
import java.io.*;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
//end HERCULES:add



/**
 * Standard implementation of the <b>Session</b> interface.  This object is
 * serializable, so that it can be stored in persistent storage or transferred
 * to a different JVM for distributable session support.
 * <p>
 * <b>IMPLEMENTATION NOTE</b>:  An instance of this class represents both the
 * internal (Session) and application level (HttpSession) view of the session.
 * However, because the class itself is not declared public, Java logic outside
 * of the <code>org.apache.catalina.session</code> package cannot cast an
 * HttpSession view of this instance back to a Session view.
 * <p>
 * <b>IMPLEMENTATION NOTE</b>:  If you add fields to this class, you must
 * make sure that you carry them over in the read/writeObject methods so
 * that this class is properly serialized.
 *
 * @author Craig R. McClanahan
 * @author Sean Legassick
 * @author <a href="mailto:jon@latchkey.com">Jon S. Stevens</a>
 * @version $Revision: 1.33 $ $Date: 2007/03/12 21:41:52 $
 */

public class StandardSession
    implements HttpSession, Session, Serializable {

    private static final Logger log = StandardServer.log;

    private static final ResourceBundle rb = StandardServer.log.getResourceBundle();

    @LogMessageInfo(
            message = "Session id change event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_ID_CHANGE_EVENT_LISTENER_EXCEPTION = "AS-WEB-CORE-00389";

    @LogMessageInfo(
            message = "Session event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_EVENT_LISTENER_EXCEPTION = "AS-WEB-CORE-00390";

    @LogMessageInfo(
            message = "Session already invalidated",
            level = "WARNING"
    )
    public static final String SESSION_INVALIDATED_EXCEPTION = "AS-WEB-CORE-00391";

    @LogMessageInfo(
            message = "Session attribute event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_ATTRIBUTE_EVENT_LISTENER_EXCEPTION = "AS-WEB-CORE-00392";

    @LogMessageInfo(
            message = "setAttribute: name parameter cannot be null",
            level = "WARNING"
    )
    public static final String NAME_PARAMETER_CANNOT_BE_NULL_EXCEPTION = "AS-WEB-CORE-00393";

    @LogMessageInfo(
            message = "Session binding event listener threw exception",
            level = "WARNING"
    )
    public static final String SESSION_BINDING_EVENT_LISTENER_EXCEPTION = "AS-WEB-CORE-00394";

    @LogMessageInfo(
            message = " Cannot serialize session attribute {0} for session {1}",
            level = "WARNING"
    )
    public static final String CANNOT_SERIALIZE_SESSION_EXCEPTION = "AS-WEB-CORE-00395";
    // ----------------------------------------------------------- Constructors

    /**
     * Construct a new Session associated with the specified Manager.
     *
     * @param manager The manager with which this Session is associated
     */
    public StandardSession(Manager manager) {

        super();
        setManager(manager);
        if (manager instanceof ManagerBase) {
            this.debug = ((ManagerBase) manager).getDebug();
        }
    }


    // ----------------------------------------------------- Class Variables


    /**
     * Type array.
     */
    protected static final String EMPTY_ARRAY[] = new String[0];

    /**
     * The dummy attribute value serialized when a NotSerializableException is
     * encountered in <code>writeObject()</code>.
     */
    protected static final String NOT_SERIALIZED =
        "___NOT_SERIALIZABLE_EXCEPTION___";

    //HERCULES:add
    /**
     * The string used in the name for setAttribute and removeAttribute
     * to signify on-demand sync
     */    
    protected static final String SYNC_STRING = "com.sun.sync";
    //end HERCULES:add

    /**
     * The method signature for the <code>fireContainerEvent</code> method.
     */
    static final Class<?> containerEventTypes[] =
        { String.class, Object.class };

    /**
     * Descriptive information describing this Session implementation.
     */
    protected static final String info = "StandardSession/1.0";

    /**
     * Set of attribute names which are not allowed to be persisted.
     */
    private static final String[] excludedAttributes = {
        Globals.SUBJECT_ATTR
    };

    /**
     * The HTTP session context associated with this session.
     */
    protected static volatile HttpSessionContext sessionContext = null;

    /**
     * Used for serialized format versioning.
     * 1 = first version where this is being tracked.
     *
     * NOTE: You must increment this version whenever any changes are made
     * to the serialized representation of this class between releases
     */
    private static final Short SERIALIZED_FORM_VERSION = Short.valueOf("1");


    // ----------------------------------------------------- Instance Variables

    /**
     * The session identifier of the parent SipApplicationSession, if any
     */
    private String sipAppSessionId = null;

    /**
     * The BEKEY of this session, or <tt>null</tt>.
     *
     * <p>The BEKEY is used by the Converged Loadbalancer (CLB) in DCR mode
     * for loadbalancing purposes, and supplied to the web container in the
     * form of a request header.
     *
     * <p>See https://sailfin.dev.java.net/issues/show_bug.cgi?id=1647
     * for additional details
     */
    private String beKey;

    /**
     * The collection of user data attributes associated with this Session.
     */
    protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();    

    /**
     * The authentication type used to authenticate our cached Principal,
     * if any.  NOTE:  This value is not included in the serialized
     * version of this object.
     */
    protected transient String authType = null;

    /**
     * The <code>java.lang.Method</code> for the
     * <code>fireContainerEvent()</code> method of the
     * <code>org.apache.catalina.core.StandardContext</code> method,
     * if our Context implementation is of this class.  This value is
     * computed dynamically the first time it is needed, or after
     * a session reload (since it is declared transient).
     */
    protected transient Method containerEventMethod = null;

    /**
     * The time this session was created, in milliseconds since midnight,
     * January 1, 1970 GMT.
     */
    protected long creationTime = 0L;

    /**
     * The debugging detail level for this component.  NOTE:  This value
     * is not included in the serialized version of this object.
     */
    protected transient int debug = 0;

    /**
     * We are currently processing a session expiration, so bypass
     * certain IllegalStateException tests.  NOTE:  This value is not
     * included in the serialized version of this object.
     */
    protected transient boolean expiring = false;

    /**
     * The facade associated with this session.  NOTE:  This value is not
     * included in the serialized version of this object.
     */
    protected transient StandardSessionFacade facade = null;

    /**
     * The session identifier of this Session.
     */
    protected String id = null;

    /**
     * The last accessed time for this Session.
     */
    protected long lastAccessedTime = creationTime;

    /**
     * The session event listeners for this Session.
     */
    protected transient ArrayList<SessionListener> listeners =
        new ArrayList<SessionListener>();

    /**
     * The Manager with which this Session is associated.
     */
    protected transient Manager manager = null;

    /**
     * The context with which this Session is associated.
     */
    protected transient StandardContext context = null;

    /**
     * The maximum time interval, in seconds, between client requests before
     * the servlet container may invalidate this session.  A negative time
     * indicates that the session should never time out.
     */
    protected int maxInactiveInterval = -1;

    /**
     * Flag indicating whether this session is new or not.
     */
    protected boolean isNew = false;

    /**
     * Flag indicating whether this session is valid or not.
     */
    protected boolean isValid = false;

    /**
     * Internal notes associated with this session by Catalina components
     * and event listeners.  <b>IMPLEMENTATION NOTE:</b> This object is
     * <em>not</em> saved and restored across session serializations!
     */
    protected transient Map<String, Object> notes = new Hashtable<String, Object>();

    /**
     * The authenticated Principal associated with this session, if any.
     // START SJSWS 6371339
     // * <b>IMPLEMENTATION NOTE:</b>  This object is <i>not</i> saved and
     // * restored across session serializations!
     // END SJSWS 6371339
     */
    protected transient Principal principal = null;

    /**
     * The current accessed time for this session.
     */
    protected long thisAccessedTime = creationTime;

    /**
     * The session version, incremented and used by in-memory-replicating
     * session managers
     */
    protected AtomicLong version = new AtomicLong(-1);

    /**
     * single sign on id. It is null if there is no SSO.
     */
    protected String ssoId = null;

    /**
     * single sign on version.
     */
    protected volatile long ssoVersion = 0L;


    // ----------------------------------------------------- Session Properties


    /**
     * Return the authentication type used to authenticate our cached
     * Principal, if any.
     */
    public String getAuthType() {

        return (this.authType);

    }


    /**
     * Set the authentication type used to authenticate our cached
     * Principal, if any.
     *
     * @param authType The new cached authentication type
     */
    public void setAuthType(String authType) {

        this.authType = authType;
    }


    /**
     * Set the creation time for this session.  This method is called by the
     * Manager when an existing Session instance is reused.
     *
     * @param time The new creation time
     */
    public void setCreationTime(long time) {

        this.creationTime = time;
        this.lastAccessedTime = time;
        this.thisAccessedTime = time;

    }


    /**
     * Return the session identifier for this session.
     */
    public String getId() {

        return getIdInternal();

    }


    /**
     * Return the session identifier for this session.
     */
    public String getIdInternal() {

        return (this.id);

    }


    /**
     * Set the session identifier for this session.
     *
     * @param id The new session identifier
     */
    public void setId(String id) {

        if ((this.id != null) && (manager != null))
            manager.remove(this);

        String oldId = this.id;
        this.id = id;

        if (manager != null)
            manager.add(this);

        
        // Notify interested session event listeners
        if (oldId == null) { // the session is just created
            tellNew();
        } else { // change session id
            HttpSessionEvent event = null;
            List<EventListener> listeners = context.getApplicationEventListeners();
            if (listeners.isEmpty()) {
                return;
            }
            Iterator<EventListener> iter = listeners.iterator();
            while (iter.hasNext()) {
                EventListener eventListener = iter.next();
                if (!(eventListener instanceof HttpSessionIdListener)) {
                    continue;
                }
                HttpSessionIdListener listener = (HttpSessionIdListener)eventListener;
                try {
                    fireContainerEvent(context,
                                       "beforeSessionIdChanged",
                                       listener);
                    if (event == null) {
                        event = new HttpSessionEvent(getSession());
                    }
                    listener.sessionIdChanged(event, oldId);
                    fireContainerEvent(context,
                                       "afterSessionIdChanged",
                                       listener);
                } catch (Throwable t) {
                    try {
                        fireContainerEvent(context,
                                           "afterSessionIdChanged",
                                           listener);
                    } catch (Exception e) {
                        // Ignore
                    }
                    log(rb.getString(SESSION_ID_CHANGE_EVENT_LISTENER_EXCEPTION), t);
                }
            }
        }
    }


/**
     * Sets the BEKEY for this session
     *
     * <p>The BEKEY is used by the Converged Loadbalancer (CLB) in DCR mode
     * for loadbalancing purposes, and supplied to the web container in the
     * form of a request header.
     *
     * @param beKey the BEKEY for this session, or <tt>null</tt> if not
     * present
     */
    public void setBeKey(String beKey) {
        this.beKey = beKey;
    }


    /**
     * Gets the BEKEY of this session
     *
     * @return the BEKEY of this session, or <tt>null</tt> if not present
     */
    public String getBeKey() {
        return beKey;
    }


    /**
     * Sets the id of the SipApplicationSession that is the parent of this
     * StandardSession.
     *
     * @param id SipApplicationSession id
     */
    public void setSipApplicationSessionId(String id) {
        sipAppSessionId = id;
    }


    /**
     * Gets the id of the SipApplicationSession that is the parent of this
     * StandardSession.
     *
     * @return The SipApplicationSession id, or null if this
     * StandardSession does not have any SipApplicationSession parent
     */
    public String getSipApplicationSessionId() {
        return sipAppSessionId;
    }


    /**
     * Inform the listeners about the new session.
     *
     */
    public void tellNew() {

        // Notify interested session event listeners
        fireSessionEvent(Session.SESSION_CREATED_EVENT, null);

        HttpSessionEvent event = new HttpSessionEvent(getSession());

        // Notify interested application event listeners
        for (HttpSessionListener listener : context.getSessionListeners()) {
            try {
                fireContainerEvent(context, "beforeSessionCreated",
                                   listener);
                listener.sessionCreated(event);
                fireContainerEvent(context, "afterSessionCreated",
                                   listener);
            } catch (Throwable t) {
                try {
                    fireContainerEvent(context, "afterSessionCreated",
                                       listener);
                } catch (Exception e) {
                    // Ignore
                }
                log(rb.getString(SESSION_EVENT_LISTENER_EXCEPTION), t);
            }
        }
    }


    /**
     * Return descriptive information about this Session implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return (this.info);

    }


    /**
     * Return the last time the client sent a request associated with this
     * session, as the number of milliseconds since midnight, January 1, 1970
     * GMT.  Actions that your application takes, such as getting or setting
     * a value associated with the session, do not affect the access time.
     */
    public long getLastAccessedTime() {
        if ( !isValid() ) {
            throw new IllegalStateException
                ("getLastAccessedTime: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));
        }
        return (this.lastAccessedTime);

    }
    

    // START SJSAS 6470831
    /**
     * Same as getLastAccessedTime(), except that there is no call to 
     * isValid(), which may expire the session and cause any subsequent
     * session access to throw an IllegalStateException.
     */
    public long getLastAccessedTimeInternal() {
        return this.lastAccessedTime;
    }
    // END SJSAS 6470831

    
    /**
     * Set the last time the client sent a request associated with this
     * session, as the number of milliseconds since midnight, January 1, 1970
     * GMT.  Actions that your application takes, such as getting or setting
     * a value associated with the session, do not affect the access time.
     * HERCULES: added method
     */	
    public void setLastAccessedTime(long lastAcessedTime) {
        this.lastAccessedTime = lastAcessedTime;
    }    


    /**
     * Return the Manager within which this Session is valid.
     */
    public Manager getManager() {

        return (this.manager);

    }


    /**
     * Set the Manager within which this Session is valid.
     *
     * @param manager The new Manager
     */
    public void setManager(Manager manager) {
        this.manager = manager;
        context = (StandardContext) manager.getContainer();
    }


    /**
     * Return the maximum time interval, in seconds, between client requests
     * before the servlet container will invalidate the session.  A negative
     * time indicates that the session should never time out.
     */
    public int getMaxInactiveInterval() {

        return (this.maxInactiveInterval);

    }


    /**
     * Set the maximum time interval, in seconds, between client requests
     * before the servlet container will invalidate the session.  A negative
     * time indicates that the session should never time out.
     *
     * @param interval The new maximum interval
     */
    public void setMaxInactiveInterval(int interval) {

        this.maxInactiveInterval = interval;
        if (isValid && interval == 0) {
            expire();
        }

    }


    /**
     * Set the <code>isNew</code> flag for this session.
     *
     * @param isNew The new value for the <code>isNew</code> flag
     */
    public void setNew(boolean isNew) {

        this.isNew = isNew;

    }


    /**
     * Return the authenticated Principal that is associated with this Session.
     * This provides an <code>Authenticator</code> with a means to cache a
     * previously authenticated Principal, and avoid potentially expensive
     * <code>Realm.authenticate()</code> calls on every request.  If there
     * is no current associated Principal, return <code>null</code>.
     */
    public Principal getPrincipal() {

        return (this.principal);

    }


    /**
     * Set the authenticated Principal that is associated with this Session.
     * This provides an <code>Authenticator</code> with a means to cache a
     * previously authenticated Principal, and avoid potentially expensive
     * <code>Realm.authenticate()</code> calls on every request.
     *
     * @param principal The new Principal, or <code>null</code> if none
     */
    public void setPrincipal(Principal principal) {

        this.principal = principal;
    }


    /**
     * Return the <code>HttpSession</code> for which this object
     * is the facade.
     */
    public HttpSession getSession() {

        if (facade == null){
            if (SecurityUtil.isPackageProtectionEnabled()){
                final StandardSession fsession = this;
                facade = AccessController.doPrivileged(
                        new PrivilegedAction<StandardSessionFacade>(){
                    public StandardSessionFacade run(){
                        return new StandardSessionFacade(fsession);
                    }
                });
            } else {
                facade = new StandardSessionFacade(this);
            }
        }
        return (facade);

    }


    /**
     * Return the <code>isValid</code> flag for this session.
     */
    public boolean isValid() {

        if (this.expiring){
            return true;
        }

        if (!this.isValid ) {
            return false;
        }

        if (isForegroundLocked()) {
            return true;
        }

        /* SJSAS 6329289
        if (maxInactiveInterval >= 0) { 
            long timeNow = System.currentTimeMillis();
            int timeIdle = (int) ((timeNow - thisAccessedTime) / 1000L);
            if (timeIdle >= maxInactiveInterval) {
                expire(true);
            }
        }
        */
        // START SJSAS 6329289
        if (hasExpired()) {
            expire(true);
        }
        // END SJSAS 6329289

        return (this.isValid);
    }

    // START CR 6363689
    public boolean getIsValid() {
        return this.isValid; 
    }    
    // END CR 6363689    

    /**
     * Set the <code>isValid</code> flag for this session.
     *
     * @param isValid The new value for the <code>isValid</code> flag
     */
    public void setValid(boolean isValid) {

        this.isValid = isValid;
        //SJSAS 6406580 START
        if (!isValid && (getManager() instanceof PersistentManagerBase)) {
            ((PersistentManagerBase) getManager()).addToInvalidatedSessions(this.id);            
        }
        //SJSAS 6406580 END        
    }


    // ------------------------------------------------- Session Public Methods


    /**
     * Update the accessed time information for this session.  This method
     * should be called by the context when a request comes in for a particular
     * session, even if the application does not reference it.
     */
    public void access() {
        this.lastAccessedTime = this.thisAccessedTime;
        this.thisAccessedTime = System.currentTimeMillis();

        evaluateIfValid();
    }


    /**
     * End the access.
     */
    public void endAccess() {
        isNew = false;
    }


    /**
     * Add a session event listener to this component.
     */
    public void addSessionListener(SessionListener listener) {

        synchronized (listeners) {
            listeners.add(listener);
        }

    }


    /**
     * Perform the internal processing required to invalidate this session,
     * without triggering an exception if the session has already expired.
     */
    public void expire() {

        expire(true);

    }
    
    /**
     * Perform the internal processing required to invalidate this session,
     * without triggering an exception if the session has already expired.
     *
     * @param notify Should we notify listeners about the demise of
     *  this session?
     */
    public void expire(boolean notify) {
        expire(notify, true);
    }
    
    /**
     * Perform the internal processing required to invalidate this session,
     * without triggering an exception if the session has already expired.
     *
     * @param notify Should we notify listeners about the demise of
     *  this session?
     * @param persistentRemove should we call store to remove the session
     *  if available
     */
    public void expire(boolean notify, boolean persistentRemove) {

        // Mark this session as "being expired" if needed
        if (expiring)
            return;

        synchronized (this) {

            if (manager == null)
                return;

            expiring = true;
        
            // Notify interested application event listeners
            // FIXME - Assumes we call listeners in reverse order

            // The call to expire() may not have been triggered by the webapp.
            // Make sure the webapp's class loader is set when calling the
            // listeners
            ClassLoader oldTccl = null;
            if (context.getLoader() != null &&
                    context.getLoader().getClassLoader() != null) {
                oldTccl = Thread.currentThread().getContextClassLoader();
                if (Globals.IS_SECURITY_ENABLED) {
                    PrivilegedAction<Void> pa = new PrivilegedSetTccl(
                            context.getLoader().getClassLoader());
                    AccessController.doPrivileged(pa);
                } else {
                    Thread.currentThread().setContextClassLoader(
                            context.getLoader().getClassLoader());
                }
            }
            try {
                List<HttpSessionListener> listeners = context.getSessionListeners();
                if (notify && !listeners.isEmpty()) {
                    HttpSessionEvent event = new HttpSessionEvent(getSession());
                    int len = listeners.size();
                    for (int i = 0; i < len; i++) {
                        // Invoke in reverse order of declaration
                        HttpSessionListener listener = listeners.get((len - 1) - i);
                        try {
                            fireContainerEvent(context,
                                               "beforeSessionDestroyed",
                                               listener);
                            listener.sessionDestroyed(event);
                            fireContainerEvent(context,
                                               "afterSessionDestroyed",
                                               listener);
                        } catch (Throwable t) {
                            try {
                                fireContainerEvent(context,
                                                   "afterSessionDestroyed",
                                                   listener);
                            } catch (Exception e) {
                                // Ignore
                            }
                            // FIXME - should we do anything besides log these?
                            log(rb.getString(SESSION_EVENT_LISTENER_EXCEPTION), t);
                        }
                    }
                }
            } finally {
                if (oldTccl != null) {
                    if (Globals.IS_SECURITY_ENABLED) {
                        PrivilegedAction<Void> pa =
                            new PrivilegedSetTccl(oldTccl);
                        AccessController.doPrivileged(pa);
                    } else {
                        Thread.currentThread().setContextClassLoader(oldTccl);
                    }
                }
            }

            setValid(false);

            /*
             * Compute how long this session has been alive, and update
             * session manager's related properties accordingly
             */
            long timeNow = System.currentTimeMillis();
            int timeAlive = (int) ((timeNow - creationTime)/1000);
            synchronized (manager) {
                if (timeAlive > manager.getSessionMaxAliveTimeSeconds()) {
                    manager.setSessionMaxAliveTimeSeconds(timeAlive);
                }
                int numExpired = manager.getExpiredSessions();
                numExpired++;
                manager.setExpiredSessions(numExpired);
                int average = manager.getSessionAverageAliveTimeSeconds();
                average = ((average * (numExpired-1)) + timeAlive)/numExpired;
                manager.setSessionAverageAliveTimeSeconds(average);
            }
            
            // Remove this session from our manager's active sessions
            if(persistentRemove) {
                manager.remove(this);
            } else {
                if(manager instanceof PersistentManagerBase) {
                    ((PersistentManagerBase)manager).remove(this, false);
                }
            }            

            /*
             * Mark session as expired *before* removing its attributes, so
             * that its HttpSessionBindingListener objects will get an
             * IllegalStateException when accessing the session attributes
             * from within their valueUnbound() method
             */ 
            expiring = false;

            // Unbind any objects associated with this session
            String keys[] = keys();
            for (int i = 0; i < keys.length; i++)
                removeAttribute(keys[i], notify, false);

            // Notify interested session event listeners
            if (notify) {
                context.sessionExpiredEvent(this);
                fireSessionEvent(Session.SESSION_DESTROYED_EVENT, null);
            }

        }

    }    
    
    /**
     * Perform the internal processing required to passivate
     * this session.
     */
    public void passivate() {

        context.sessionPassivatedStartEvent(this);

        try {
            // Notify ActivationListeners
            HttpSessionEvent event = null;
            String keys[] = keys();
            for (int i = 0; i < keys.length; i++) {
                Object attribute = getAttributeInternal(keys[i]);
                if (attribute instanceof HttpSessionActivationListener) {
                    if (event == null)
                        event = new HttpSessionEvent(getSession());
                    // FIXME: Should we catch throwables?
                    ((HttpSessionActivationListener)attribute).sessionWillPassivate(event);
                }
            }
        } finally {
            context.sessionPassivatedEndEvent(this);
        }
    }


    /**
     * Perform internal processing required to activate this
     * session.
     */
    public void activate() {

        context.sessionActivatedStartEvent(this);

        try {
            // Notify ActivationListeners
            HttpSessionEvent event = null;
            String keys[] = keys();
            for (int i = 0; i < keys.length; i++) {
                Object attribute = getAttributeInternal(keys[i]);
                if (attribute instanceof HttpSessionActivationListener) {
                    if (event == null)
                        event = new HttpSessionEvent(getSession());
                    // FIXME: Should we catch throwables?
                    ((HttpSessionActivationListener)attribute).sessionDidActivate(event);
                }
            }
        } finally {
            context.sessionActivatedEndEvent(this);
        }
    }


    /**
     * Return the object bound with the specified name to the internal notes
     * for this session, or <code>null</code> if no such binding exists.
     *
     * @param name Name of the note to be returned
     */
    public Object getNote(String name) {
        return (notes.get(name));
    }


    /**
     * Return an Iterator containing the String names of all notes bindings
     * that exist for this session.
     */
    public Iterator<String> getNoteNames() {
        return (notes.keySet().iterator());
    }


    /**
     * Release all object references, and initialize instance variables, in
     * preparation for reuse of this object.
     */
    public void recycle() {

        // Reset the instance variables associated with this Session
        attributes.clear();
        setAuthType(null);
        creationTime = 0L;
        expiring = false;
        id = null;
        lastAccessedTime = 0L;
        maxInactiveInterval = -1;
        notes.clear();
        setPrincipal(null);
        isNew = false;
        isValid = false;

        listeners.clear();

        manager = null;

    }


    /**
     * Remove any object bound to the specified name in the internal notes
     * for this session.
     *
     * @param name Name of the note to be removed
     */
    public void removeNote(String name) {
        notes.remove(name);
    }


    /**
     * Remove a session event listener from this component.
     */
    public void removeSessionListener(SessionListener listener) {

        synchronized (listeners) {
            listeners.remove(listener);
        }

    }


    /**
     * Bind an object to a specified name in the internal notes associated
     * with this session, replacing any existing binding for this name.
     *
     * @param name Name to which the object should be bound
     * @param value Object to be bound to the specified name
     */
    public void setNote(String name, Object value) {
        notes.put(name, value);
    }


    // START SJSAS 6329289
    /**
     * Checks whether this Session has expired.
     *
     * @return true if this Session has expired, false otherwise
     */
    public boolean hasExpired() {

        if (maxInactiveInterval >= 0
                && (System.currentTimeMillis() - thisAccessedTime >=
                    maxInactiveInterval * 1000L)) {
            return true;
        } else {
            return false;
        }
    }
    // END SJSAS 6329289


    /** 
     * Increments the version number
     */
    public long incrementVersion() {
        return version.incrementAndGet();
    } 

    
    /** 
     * Gets the version number
     */    
    public long getVersion() {
        return version.get();
    }
    

    /** 
     * Sets the version number
     */    
    public void setVersion(long value) {
        version.set(value);
    }    


    /**
     * Return the single sign on id.
     * It is null if there is no SSO.
     */
    public String getSsoId() {
        return ssoId;
    }


    /**
     * Set the single sign on id.
     */
    public void setSsoId(String ssoId) {
        this.ssoId = ssoId;
    }


    /**
     * Return the single sign on version.
     */
    public long getSsoVersion() {
        return ssoVersion;
    }


    /**
     * Set the single sign on version.
     */
    public void setSsoVersion(long value) {
        ssoVersion = value;
    }


    /**
     * Return a string representation of this object.
     */
    public String toString() {

        // STARTS S1AS
        /*
        StringBuilder sb = new StringBuilder();
        sb.append("StandardSession[");
        sb.append(id);
        sb.append("]");
        return (sb.toString());
        */
        // END S1AS
        // START S1AS
        StringBuilder sb = null;

        if(!this.isValid) {
            sb = new StringBuilder();
        } else {
            sb = new StringBuilder(1000);
        }

        sb.append("StandardSession[");
        sb.append(id);
        sb.append("]");
        
        if (this.isValid) {
            Enumeration<String> attrNamesEnum = getAttributeNames();
            while(attrNamesEnum.hasMoreElements()) {
                String nextAttrName = attrNamesEnum.nextElement();
                Object nextAttrValue = getAttribute(nextAttrName);
                sb.append("\n");
                sb.append("attrName = " + nextAttrName);
                sb.append(" : attrValue = " + nextAttrValue);
            }
        }

        return sb.toString();
        // END S1AS
    }


    // ------------------------------------------------ Session Package Methods


    /**
     * Creates a StandardSession instance from the given ObjectInputStream,
     * and returns it.
     *
     * If ObjectInputStream does not contain a serialized StandardSession
     * (or one of its subclasses), this method will create an empty session
     * and populate it with the serialized data (this is for backwards
     * compatibility).
     *
     * @param ois The ObjectInputStream from which to read the serialized
     *        session data
     * @param manager The session manager from which to create an empty
     *        session if needed
     *
     * @return The restored session
     *
     * @exception ClassNotFoundException If the class for an object being
     *            restored cannot be found.
     * @exception IOException if I/O errors occur
     */
    static StandardSession deserialize(ObjectInputStream ois,
                                       Manager manager)
            throws ClassNotFoundException, IOException {

        StandardSession result = null;

        Object obj = ois.readObject();
        if (obj instanceof StandardSession) {
            // New format following standard serialization
            result = (StandardSession) obj;
        } else {
            // Old format, obj is an instance of Long and contains the
            // session's creation time
            result = (StandardSession) manager.createEmptySession();
            result.setCreationTime(((Long) obj).longValue());
            result.readRemainingObject(ois);
        }

        return result;
    }


    // ------------------------------------------------- HttpSession Properties


    /**
     * Return the time when this session was created, in milliseconds since
     * midnight, January 1, 1970 GMT.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public long getCreationTime() {

        if (!isValid())
            throw new IllegalStateException
                ("getCreationTime: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));

        return (this.creationTime);

    }


    /**
     * Return the ServletContext to which this session belongs.
     */
    public ServletContext getServletContext() {

        if (manager == null)
            return (null);
        if (context == null)
            return (null);
        else
            return (context.getServletContext());

    }


    /**
     * Return the session context with which this session is associated.
     *
     * @deprecated As of Version 2.1, this method is deprecated and has no
     *  replacement.  It will be removed in a future version of the
     *  Java Servlet API.
     */
    public HttpSessionContext getSessionContext() {
        if (sessionContext == null)
            sessionContext = new StandardSessionContext();
        return (sessionContext);

    }


    // ----------------------------------------------HttpSession Public Methods


    /**
     * Return the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound with that name.
     *
     * @param name Name of the attribute to be returned
     *                                                                                   * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public Object getAttribute(String name) {

        if (!isValid())
            throw new IllegalStateException
                ("getAttribute: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));

        if (name == null) return null;

        return (attributes.get(name));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Return an <code>Enumeration</code> of <code>String</code> objects
     * containing the names of the objects bound to this session.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public Enumeration<String> getAttributeNames() {

        if (!isValid())
            throw new IllegalStateException
                ("getAttributeNames: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));


        return (new Enumerator<String>(attributes.keySet(), true));

    }


    /**
     * Return the object bound with the specified name in this session, or
     * <code>null</code> if no object is bound with that name.
     *
     * @param name Name of the value to be returned
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>getAttribute()</code>
     */
    public Object getValue(String name) {

        return (getAttribute(name));

    }


    /**
     * Return the set of names of objects bound to this session.  If there
     * are no such objects, a zero-length array is returned.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>getAttributeNames()</code>
     */
    public String[] getValueNames() {

        if (!isValid())
            throw new IllegalStateException
                ("getValueNames: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));

        return (keys());

    }
    
    
// ------------------------session locking --HERCULES:add-------------------
    
    /**
     * get this session locked for foreground
     * if the session is found to be presently background
     * locked; retry logic in a time-decay polling loop
     * waits for background lock to clear
     * after 6 attempts (12.6 seconds) it unlocks the
     * session and acquires the foreground lock
     */         
    protected boolean getSessionLockForForeground() {
        boolean result = false;
        StandardSession sess = (StandardSession) this;       
        //now lock the session
        //System.out.println("IN LOCK_SESSION_FOR_FOREGROUND: sess =" + sess);        
        long pollTime = 200L;
        int tryNumber = 0;
        int numTries = 7;
        boolean keepTrying = true;
        boolean lockResult = false;
        //System.out.println("locking session: sess =" + sess);
        //try to lock up to numTries (i.e. 7) times
        //poll and wait starting with 200 ms
        while(keepTrying) {
            lockResult = sess.lockForeground();
            if(lockResult) {
                keepTrying = false;
                result = true;
                break;
            }
            tryNumber++;
            if(tryNumber < (numTries - 1) ) {
                pollTime = pollTime * 2L;
            } else {
                //unlock the background so we can take over
                //FIXME: need to log warning for this situation
                sess.unlockBackground();
            }              
        }
        //System.out.println("finished locking session: sess =" + sess);
        //System.out.println("LOCK = " + sess.getSessionLock());
        return result;
    } 
     
    /**
     * return whether this session is currently foreground locked
     */    
    public boolean isForegroundLocked() {
        //in this case we are not using locks
        //so just return false
        if(_sessionLock == null)
            return false;        
        synchronized(this) {
            return _sessionLock.isForegroundLocked();
        } 
    }    
    
    /**
     * lock the session for foreground
     * returns true if successful; false if unsuccessful
     */       
    public boolean lockBackground() {
        //in this case we are not using locks
        //so just return true
        if(_sessionLock == null)
            return true;
        synchronized(this) {
            return _sessionLock.lockBackground();
        }
    }
    
    /**
     * lock the session for background
     * returns true if successful; false if unsuccessful
     */     
    public boolean lockForeground() {
        //in this case we are not using locks
        //so just return true
        if(_sessionLock == null)
            return true;
        synchronized(this) {
            return _sessionLock.lockForeground();
        }
    }
    
    /**
     * unlock the session completely
     * irregardless of whether it was foreground or background locked
     */     
    public void unlockForegroundCompletely() {
        //in this case we are not using locks
        //so just return true
        if(_sessionLock == null)
            return;
        synchronized(this) {
            _sessionLock.unlockForegroundCompletely();
        }
    }
    
    /**
     * unlock the session from foreground
     */      
    public void unlockForeground() {
        //in this case we are not using locks
        //so just return true
        if(_sessionLock == null)
            return;
        synchronized(this) {
            _sessionLock.unlockForeground();
        }
    } 
    
    /**
     * unlock the session from background
     */     
    public void unlockBackground() {
        //in this case we are not using locks
        //so just return true
        if(_sessionLock == null)
            return;
        synchronized(this) {
            _sessionLock.unlockBackground();
        }
    }    

    /**
     * return the Session lock
     */     
    public SessionLock getSessionLock() {
        return _sessionLock;
    }    
    
    /**
     * set the Session lock
     * @param sessionLock
     */     
    public void setSessionLock(SessionLock sessionLock) {
        _sessionLock = sessionLock;
    }
    
    /**
     * @return true if this session has been locked by any
     * out-of-band (i.e., non-http) request, false otherwise
     */      
    public synchronized boolean hasNonHttpLockOccurred() {
        //in this case we are not using locks
        //so just return false
        if(_sessionLock == null)
            return false;
        return _sessionLock.hasNonHttpLockOccurred();
    }

    protected transient SessionLock _sessionLock = new SessionLock();

// ------------------------end session locking ---HERCULES:add--------        
    


    /**
     * Invalidates this session and unbinds any objects bound to it.
     *
     * @exception IllegalStateException if this method is called on
     *  an invalidated session
     * HERCULES:modified method
     */
    public void invalidate() {

        if (!isValid)
            throw new IllegalStateException
                ("invalidate: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));
        //make sure foreground locked first
        if(!this.isForegroundLocked()) {
            this.getSessionLockForForeground();
        }
        // Cause this session to expire
        try {
            expire();
        } finally {
            this.unlockForeground();
        }

    } 


    /**
     * Return <code>true</code> if the client does not yet know about the
     * session, or if the client chooses not to join the session.  For
     * example, if the server used only cookie-based sessions, and the client
     * has disabled the use of cookies, then a session would be new on each
     * request.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public boolean isNew() {

        if (!isValid())
            throw new IllegalStateException
                ("isNew: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));

        return (this.isNew);

    }


    /**
     * Bind an object to this session, using the specified name.  If an object
     * of the same name is already bound to this session, the object is
     * replaced.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueBound()</code> on the object.
     *
     * @param name Name to which the object is bound, cannot be null
     * @param value Object to be bound, cannot be null
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>setAttribute()</code>
     */
    public void putValue(String name, Object value) {

        setAttribute(name, value);

    }


    /**
     * Remove the object bound with the specified name from this session.  If
     * the session does not have an object bound with this name, this method
     * does nothing.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueUnbound()</code> on the object.
     *
     * @param name Name of the object to remove from this session.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public void removeAttribute(String name) {

        removeAttribute(name, true, true);

    }


    /**
     * Remove the object bound with the specified name from this session.  If
     * the session does not have an object bound with this name, this method
     * does nothing.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueUnbound()</code> on the object.
     *
     * @param name Name of the object to remove from this session.
     * @param notify Should we notify interested listeners that this
     *  attribute is being removed?
     * @param checkValid Indicates whether IllegalStateException must be
     * thrown if session has already been invalidated 
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public void removeAttribute(String name, boolean notify, 
                                boolean checkValid) {

        if (name == null) return;

        // Validate our current state
        if (!isValid() && checkValid)
            throw new IllegalStateException
                ("removeAttribute: " + rb.getString(SESSION_INVALIDATED_EXCEPTION));

        // Remove this attribute from our collection
        Object value = attributes.remove(name);

        // Do we need to do valueUnbound() and attributeRemoved() notification?
        if (!notify || (value == null)) {
            return;
        }

        // Call the valueUnbound() method if necessary
        HttpSessionBindingEvent event = null;
        if (value instanceof HttpSessionBindingListener) {
            event = new HttpSessionBindingEvent(getSession(), name, value);
            try {
                context.fireContainerEvent(
                    ContainerEvent.BEFORE_SESSION_VALUE_UNBOUND, null);
                ((HttpSessionBindingListener) value).valueUnbound(event);
                context.fireContainerEvent(
                    ContainerEvent.AFTER_SESSION_VALUE_UNBOUND, null);
            } catch (Throwable t) {
                // Log exception
                context.fireContainerEvent(
                    ContainerEvent.AFTER_SESSION_VALUE_UNBOUND, null);
            }
        }
        
        // Notify special event listeners on removeAttribute
        //HERCULES:add
        // fire container event        
        context.fireContainerEvent("sessionRemoveAttributeCalled", event);
        // fire sync container event if name equals SYNC_STRING
        if (SYNC_STRING.equals(name)) {
            context.fireContainerEvent("sessionSync",  (new HttpSessionBindingEvent(getSession(), name)));
        }         
        //END HERCULES:add         

        // Notify interested application event listeners
        List<EventListener> listeners = context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }
        Iterator<EventListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            EventListener eventListener = iter.next();
            if (!(eventListener instanceof HttpSessionAttributeListener)) {
                continue;
            }
            HttpSessionAttributeListener listener =
                (HttpSessionAttributeListener) eventListener;
            try {
                fireContainerEvent(context,
                                   "beforeSessionAttributeRemoved",
                                   listener);
                if (event == null) {
                    event = new HttpSessionBindingEvent(getSession(), name, value);
                }
                listener.attributeRemoved(event);
                fireContainerEvent(context,
                                   "afterSessionAttributeRemoved",
                                   listener);
            } catch (Throwable t) {
                try {
                    fireContainerEvent(context,
                                       "afterSessionAttributeRemoved",
                                       listener);
                } catch (Exception e) {
                    // Ignore
                }
                log(rb.getString(SESSION_ATTRIBUTE_EVENT_LISTENER_EXCEPTION), t);
            }
        }

    }


    /**
     * Remove the object bound with the specified name from this session.  If
     * the session does not have an object bound with this name, this method
     * does nothing.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueUnbound()</code> on the object.
     *
     * @param name Name of the object to remove from this session.
     *
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     *
     * @deprecated As of Version 2.2, this method is replaced by
     *  <code>removeAttribute()</code>
     */
    public void removeValue(String name) {

        removeAttribute(name);

    }


    /**
     * Bind an object to this session, using the specified name.  If an object
     * of the same name is already bound to this session, the object is
     * replaced.
     * <p>
     * After this method executes, and if the object implements
     * <code>HttpSessionBindingListener</code>, the container calls
     * <code>valueBound()</code> on the object.
     *
     * @param name Name to which the object is bound, cannot be null
     * @param value Object to be bound, cannot be null
     *
     * @exception IllegalArgumentException if an attempt is made to add a
     *  non-serializable object in an environment marked distributable.
     * @exception IllegalStateException if this method is called on an
     *  invalidated session
     */
    public void setAttribute(String name, Object value) {

        // Name cannot be null
        if (name == null)
            throw new IllegalArgumentException
                (rb.getString(NAME_PARAMETER_CANNOT_BE_NULL_EXCEPTION));

        // Null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        // Validate our current state
        if (!isValid()) {
            throw new IllegalStateException
                ("setAttribute: "+ rb.getString(SESSION_INVALIDATED_EXCEPTION));
        }

        if (manager != null) {
            manager.checkSessionAttribute(name, value);
        }

        // Construct an event with the new value
        HttpSessionBindingEvent event = null;

        // Call the valueBound() method if necessary
        if (value instanceof HttpSessionBindingListener) {
            event = new HttpSessionBindingEvent(getSession(), name, value);
            try {
                ((HttpSessionBindingListener) value).valueBound(event);
            } catch (Throwable t){
                log(rb.getString(SESSION_BINDING_EVENT_LISTENER_EXCEPTION), t);
            }
        }

        // Replace or add this attribute
        Object unbound = attributes.put(name, value);

        // Call the valueUnbound() method if necessary
        if ((unbound != null) &&
            (unbound instanceof HttpSessionBindingListener)) {
            try {
                context.fireContainerEvent(
                    ContainerEvent.BEFORE_SESSION_VALUE_UNBOUND, null);
                ((HttpSessionBindingListener) unbound).valueUnbound
                    (new HttpSessionBindingEvent(getSession(), name));
                context.fireContainerEvent(
                    ContainerEvent.AFTER_SESSION_VALUE_UNBOUND, null);
            } catch (Throwable t) {
                context.fireContainerEvent(
                    ContainerEvent.AFTER_SESSION_VALUE_UNBOUND, null);
                log(rb.getString(SESSION_BINDING_EVENT_LISTENER_EXCEPTION), t);
            }
        }
        
        //HERCULES:add
        // fire sync container event if name equals SYNC_STRING
        if (SYNC_STRING.equals(name)) {
            context.fireContainerEvent("sessionSync",
                new HttpSessionBindingEvent(getSession(), name));
        }
        //end HERCULES:add

        // Notify interested application event listeners
        List<EventListener> listeners = context.getApplicationEventListeners();
        if (listeners.isEmpty()) {
            return;
        }
        Iterator<EventListener> iter = listeners.iterator();
        while (iter.hasNext()) {
            EventListener eventListener = iter.next();
            if (!(eventListener instanceof HttpSessionAttributeListener)) {
                continue;
            }
            HttpSessionAttributeListener listener =
                (HttpSessionAttributeListener) eventListener;
            try {
                if (unbound != null) {
                    fireContainerEvent(context,
                                       "beforeSessionAttributeReplaced",
                                       listener);
                    if (event == null) {
                        event = new HttpSessionBindingEvent
                            (getSession(), name, unbound);
                    }
                    listener.attributeReplaced(event);
                    fireContainerEvent(context,
                                       "afterSessionAttributeReplaced",
                                       listener);
                } else {
                    fireContainerEvent(context,
                                       "beforeSessionAttributeAdded",
                                       listener);
                    if (event == null) {
                        event = new HttpSessionBindingEvent(
                                        getSession(), name, value);
                    }
                    listener.attributeAdded(event);
                    fireContainerEvent(context,
                                       "afterSessionAttributeAdded",
                                       listener);
                }
            } catch (Throwable t) {
                try {
                    if (unbound != null) {
                        fireContainerEvent(context,
                                           "afterSessionAttributeReplaced",
                                           listener);
                    } else {
                        fireContainerEvent(context,
                                           "afterSessionAttributeAdded",
                                           listener);
                    }
                } catch (Exception e) {
                    // Ignore
                }
                log(rb.getString(SESSION_ATTRIBUTE_EVENT_LISTENER_EXCEPTION), t);
            }
        }

    }


    // ------------------------------------------ HttpSession Protected Methods


    /**
     * Read a serialized version of this session object from the specified
     * object input stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The reference to the owning Manager
     * is not restored by this method, and must be set explicitly.
     *
     * @param stream The input stream to read from
     *
     * @exception ClassNotFoundException if an unknown class is specified
     * @exception IOException if an input/output error occurs
     */
    private void readObject(ObjectInputStream stream)
        throws ClassNotFoundException, IOException {

        if (listeners == null) {
            listeners = new ArrayList<SessionListener>();
        }
        if (notes == null) {
            notes = new Hashtable<String, Object>();
        }

        // Deserialize the scalar instance variables (except Manager)
        authType = null;        // Transient only

        /*
         * The stream starts with a Long, which indicates the session's
         * creation time. This Long may optionally be preceded by a Short,
         * which indicates the session's serializedFormVersion.
         */
        Object obj = stream.readObject();
        short readSerializedFormVersion = 0;
        if (obj instanceof Short) {
            readSerializedFormVersion = ((Short) obj).shortValue();
            creationTime = ((Long) stream.readObject()).longValue();
        } else {
           creationTime = ((Long) obj).longValue();
        }

        readRemainingObject(stream);

        /*
         * Any additional fields that are to be included in the serialized
         * representation of this class MUST be written to the end of the
         * stream (in writeObject), and must be read back in HERE, i.e.,
         * AFTER readRemainingObject (which is shared by the code that reads
         * in sessions that were serialized using an earlier, proprietary
         * format) has returned.
         */
        sipAppSessionId = (String) stream.readObject();

        switch (readSerializedFormVersion) {
        case 0:
            // Do nothing
            break;
        case 1:
            beKey = (String) stream.readObject();
            break;
        default:
            throw new IOException("Unable to deserialize into "
                    + getClass().getName()
                    + " due to unknown serializedFormVersion of "
                    + readSerializedFormVersion);
        }
    }


    /**
     * Reads the serialized session data from the given ObjectInputStream,
     * with the assumption that the session's creation time, which appears
     * first in the serialized data, has already been consumed.
     *
     * @param stream The ObjectInputStream from which to read the serialized
     *        session data
     *
     * @exception ClassNotFoundException If the class for an object being
     *            restored cannot be found.
     * @exception IOException if I/O errors occur
     */
    private void readRemainingObject(ObjectInputStream stream)
            throws ClassNotFoundException, IOException {

        version = new AtomicLong();

        lastAccessedTime = ((Long) stream.readObject()).longValue();
        maxInactiveInterval = ((Integer) stream.readObject()).intValue();
        isNew = ((Boolean) stream.readObject()).booleanValue();
        isValid = ((Boolean) stream.readObject()).booleanValue();
        thisAccessedTime = ((Long) stream.readObject()).longValue();
        /* SJSWS 6371339
        principal = null;        // Transient only
        //        setId((String) stream.readObject());
        id = (String) stream.readObject();
        */
        // START SJSWS 6371339
        // Read the next object, if it is of type Principal, then
        // store it in the principal variable
        Object obj = stream.readObject();
        if (obj instanceof Principal) {
            principal = (Principal)obj;
            id = (String) stream.readObject();
        }
        else {
            principal = null;
            id = (String) obj;
        }
        // END SJSWS 6371339
        if (debug >= 2)
            log("readObject() loading session " + id);

        // START PWC 6444754
        obj = stream.readObject();
        int n = 0;
        if (obj instanceof String) {
            authType = (String) obj;
            n = ((Integer) stream.readObject()).intValue();
        } else {
            n = ((Integer) obj).intValue();
        }
        // END PWC 6444754

        // Deserialize the attribute count and attribute values
        if (attributes == null)
            attributes = new ConcurrentHashMap<String, Object>();
        /* PWC 6444754
        int n = ((Integer) stream.readObject()).intValue();
        */
        boolean isValidSave = isValid;
        isValid = true;
        for (int i = 0; i < n; i++) {
            String name = (String) stream.readObject();
            Object value = stream.readObject();
            if ((value instanceof String) && (value.equals(NOT_SERIALIZED)))
                continue;
            if (debug >= 2)
                log("  loading attribute '" + name +
                    "' with value '" + value + "'");
            attributes.put(name, value);
        }
        isValid = isValidSave;
    }


    /**
     * Write a serialized version of this session object to the specified
     * object output stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The owning Manager will not be stored
     * in the serialized representation of this Session.  After calling
     * <code>readObject()</code>, you must set the associated Manager
     * explicitly.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  Any attribute that is not Serializable
     * will be unbound from the session, with appropriate actions if it
     * implements HttpSessionBindingListener.  If you do not want any such
     * attributes, be sure the <code>distributable</code> property of the
     * associated Manager is set to <code>true</code>.
     *
     * @param stream The output stream to write to
     *
     * @exception IOException if an input/output error occurs
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(SERIALIZED_FORM_VERSION);
        stream.writeObject(Long.valueOf(creationTime));
        stream.writeObject(Long.valueOf(lastAccessedTime));
        stream.writeObject(Integer.valueOf(maxInactiveInterval));
        stream.writeObject(Boolean.valueOf(isNew));
        stream.writeObject(Boolean.valueOf(isValid));
        stream.writeObject(Long.valueOf(thisAccessedTime));
        // START SJSWS 6371339
        // If the principal is serializable, write it out
        // START PWC 6444754
        boolean serialPrincipal = false;
        // END PWC 6444754
        if (principal instanceof java.io.Serializable) {
            // START PWC 6444754
            serialPrincipal = true;
            // END PWC 6444754
            stream.writeObject(principal);
        }
        // END SJSWS 6371339
        stream.writeObject(id);
        if (debug >= 2)
            log("writeObject() storing session " + id);

        // START PWC 6444754
        if (serialPrincipal && authType != null) {
            stream.writeObject(authType);
        }
        // END PWC 6444754

        // Accumulate the names of serializable and non-serializable attributes
        String keys[] = keys();
        ArrayList<String> saveNames = new ArrayList<String>();
        ArrayList<Object> saveValues = new ArrayList<Object>();
        for (int i = 0; i < keys.length; i++) {
            Object value = attributes.get(keys[i]);
            if (value == null) {
                continue;            

            //HERCULES:mod
            /* original PE code next 4 lines
            else if (value instanceof Serializable) {
                saveNames.add(keys[i]);
                saveValues.add(value);
            }
             */ 
            //original Hercules code was next line
            //else if (value instanceof Serializable || value instanceof javax.ejb.EJBLocalObject || value instanceof javax.naming.Context || value instanceof javax.ejb.EJBLocalHome ) { //Bug 4853798
            //FIXME: IndirectlySerializable includes more than 3 classes in Hercules code
            //need to explore implications of this

            } else if (isSerializable(value)) {    
                saveNames.add(keys[i]);
                saveValues.add(value);
            //end HERCULES:mod             
            } else {
                removeAttribute(keys[i], true, true);
            }
        }

        // Serialize the attribute count and the Serializable attributes
        int n = saveNames.size();
        stream.writeObject(Integer.valueOf(n));
        for (int i = 0; i < n; i++) {
            stream.writeObject(saveNames.get(i));
            //HERCULES:mod
            /* orignal PE code            
            try {
                stream.writeObject(saveValues.get(i));
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value '" + saveValues.get(i) + "'");
            } catch (NotSerializableException e) {
                log(sm.getString("standardSession.notSerializable",
                                 saveNames.get(i), id), e);
                //standardSession.notSerializable=PWC2785: Cannot serialize session attribute {0} for session {1}
                stream.writeObject(NOT_SERIALIZED);
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value NOT_SERIALIZED");
            }
             *end original PE code
             */ 
            
            //following is replacement code from Hercules
            try {
                stream.writeObject(saveValues.get(i));
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value '" + saveValues.get(i) + "'");
            } catch (NotSerializableException e) {
                String msg = MessageFormat.format(rb.getString(CANNOT_SERIALIZE_SESSION_EXCEPTION),
                                                  new Object[] {saveNames.get(i), id});
                log(msg, e);
                stream.writeObject(NOT_SERIALIZED);
                if (debug >= 2)
                    log("  storing attribute '" + saveNames.get(i) +
                        "' with value NOT_SERIALIZED");
            } catch (IOException ioe) {
		if ( ioe.getCause() instanceof NotSerializableException ) {
                String msg = MessageFormat.format(rb.getString(CANNOT_SERIALIZE_SESSION_EXCEPTION),
                                                  new Object[] {saveNames.get(i), id});
                	log(msg, ioe);
                	stream.writeObject(NOT_SERIALIZED);
                	if (debug >= 2)
                    		log("  storing attribute '" + saveNames.get(i) +
                        	"' with value NOT_SERIALIZED");
		} else 
			throw ioe;
	    }
            //end HERCULES:mod
        }

        stream.writeObject(sipAppSessionId);
        stream.writeObject(beKey);

    }



    /**
     * Exclude attribute that cannot be serialized.
     * @param name the attribute's name
     */
    protected boolean exclude(String name){

        for (int i = 0; i < excludedAttributes.length; i++) {
            if (name.equalsIgnoreCase(excludedAttributes[i]))
                return true;
        }

        return false;
    }


    protected void evaluateIfValid() {
        /*
	 * If this session has expired or is in the process of expiring or
	 * will never expire, return
	 */
        if (!this.isValid || expiring || maxInactiveInterval < 0)
            return;

        isValid();

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Fire container events if the Context implementation is the
     * <code>org.apache.catalina.core.StandardContext</code>.
     *
     * @param context Context for which to fire events
     * @param type Event type
     * @param data Event data
     *
     * @exception Exception occurred during event firing
     */
    protected void fireContainerEvent(Context context, String type, Object data)
            throws Exception {

        if (!(context instanceof StandardContext)) {
            return; // Container events are not supported
        }
        // NOTE:  Race condition is harmless, so do not synchronize
        if (containerEventMethod == null) {
            containerEventMethod =
                context.getClass().getMethod("fireContainerEvent",
                                             containerEventTypes);
        }
        Object containerEventParams[] = new Object[2];
        containerEventParams[0] = type;
        containerEventParams[1] = data;
        containerEventMethod.invoke(context, containerEventParams);

    }
                                      


    /**
     * Notify all session event listeners that a particular event has
     * occurred for this Session.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireSessionEvent(String type, Object data) {

        // Fire monitoring probe
        if (Session.SESSION_CREATED_EVENT.equals(type)) {
            context.sessionCreatedEvent(this);
        }  else {
            context.sessionDestroyedEvent(this);
        }

        if (listeners.size() < 1)
            return;
        SessionEvent event = new SessionEvent(this, type, data);
        SessionListener list[] = new SessionListener[0];
        synchronized (listeners) {
            list = listeners.toArray(list);
        }

        for (int i = 0; i < list.length; i++){
            (list[i]).sessionEvent(event);
        }

    }


    /**
     * Return the names of all currently defined session attributes
     * as an array of Strings.  If there are no defined attributes, a
     * zero-length array is returned.
     */
    protected String[] keys() {
        if (attributes.size() > 0) {
            // take a snapshot of attributes.keySet()
            List<String> list = new ArrayList<String>();
            for (String key : attributes.keySet()) {
                list.add(key);
            }
            return list.toArray(new String[list.size()]);
        } else {
            return EMPTY_ARRAY;
        }
    }


    /**
     * Return the value of an attribute without a check for validity.
     */
    protected Object getAttributeInternal(String name) {
        return (attributes.get(name));
    }


    /**
     * Log a message on the Logger associated with our Manager (if any).
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message);
        } else {
            log.log(Level.INFO, "StandardSession: " + message);
        }
    }


    /**
     * Log a message on the Logger associated with our Manager (if any).
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    protected void log(String message, Throwable t) {
        if ((manager != null) && (manager instanceof ManagerBase)) {
            ((ManagerBase) manager).log(message, t);
        } else {
            log.log(Level.WARNING, "StandardSession: " + message, t);
        }
    }


    /**
     * Returns true if the given value may be serialized, false otherwise.
     *
     * A given value is considered serializable if it is an instance of
     * java.io.Serializable or
     * com.sun.enterprise.spi.io.BaseIndirectlySerializable, or if special
     * serialization logic for it exists. For example, in the case of
     * GlassFish, instances of javax.naming.Context are replaced with
     * corresponding instances of SerializableJNDIContext during serialization
     * (this is done by the specialized object outputstream returned by
     * the JavaEEObjectStreamFactory mechanism).
     * 
     * @return true if the given value may be serialized, false otherwise
     */
    static boolean isSerializable(Object value) {
        if ((value instanceof Serializable)
                || (value instanceof BaseIndirectlySerializable)
                || (value instanceof javax.naming.Context)) {
            return true;
        } else {
            return false;
        }
    }

}


// ------------------------------------------------------------ Protected Class


/**
 * This class is a dummy implementation of the <code>HttpSessionContext</code>
 * interface, to conform to the requirement that such an object be returned
 * when <code>HttpSession.getSessionContext()</code> is called.
 *
 * @author Craig R. McClanahan
 *
 * @deprecated As of Java Servlet API 2.1 with no replacement.  The
 *  interface will be removed in a future version of this API.
 */

final class StandardSessionContext implements HttpSessionContext {


    private HashMap<?, String> dummy = new HashMap<String, String>();

    /**
     * Return the session identifiers of all sessions defined
     * within this context.
     *
     * @deprecated As of Java Servlet API 2.1 with no replacement.
     *  This method must return an empty <code>Enumeration</code>
     *  and will be removed in a future version of the API.
     */
    public Enumeration<String> getIds() {
        return (new Enumerator<String>(dummy));
    }


    /**
     * Return the <code>HttpSession</code> associated with the
     * specified session identifier.
     *
     * @param id Session identifier for which to look up a session
     *
     * @deprecated As of Java Servlet API 2.1 with no replacement.
     *  This method must return null and will be removed in a
     *  future version of the API.
     */
    public HttpSession getSession(String id) {
        return (null);
    }
}
