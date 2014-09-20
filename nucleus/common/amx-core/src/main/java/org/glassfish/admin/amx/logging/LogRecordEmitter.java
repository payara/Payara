/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.logging;

import org.glassfish.admin.amx.base.ListenerInfo;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.util.SetUtil;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;


/**
	Emits {@link LogRecord} as JMX {@link Notification}.
	<p>
	The {@link LogRecord} is embedded within a Notification as follows:
	notif.getType() => a Notification type beginning with {@link #LOG_RECORD_NOTIFICATION_PREFIX}<br>
	notif.getSource() => ObjectName of {@link Logging} MBean that emitted the Notification
	notif.getUserData() => Map&lt;String,Object> with keys as described below.
	<p>
    To avoid deserialization problems, an actual {@link LogRecord} is <i>not</i>
    transmitted.  Instead, its important fields are extracted and placed into a Map
    obtained as follows:
<pre><code>
    final Map<String,Object>    fields  = (Map<String,Object>)notif.getUserData();
</code></pre>
    Alternately, you may use {@link Util#getAMXNotificationValue} to extract any]
    particular field.
    <p>
	Valid keys within the Map are:
	<ul>
	<li>{@link #LOG_RECORD_AS_STRING_KEY}  value is the String version of the entire LogRecord</li>
	</ul>
    <p>

	Here is an example of how to retrieve the LogRecord information when a Notification is received:
<code><pre>
final Notifcation notif	= <the notification>;
final String type       = notif.getType();
final LogRecord logRecord	= (LogRecord){@link Util#getAMXNotificationValue}( notif, LOG_RECORD_KEY );
final String    logRecordString	= (String){@link Util#getAMXNotificationValue}( notif, LOG_RECORD_STRING_KEY );
</pre></code>
	@since AS 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public interface LogRecordEmitter extends NotificationEmitter, ListenerInfo
{
	/**
		An emitted Notification will have this prefix for its type. Subtypes include:
		<ul>
		<li>{@link #LOG_RECORD_NOTIFICATION_SEVERE_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_WARNING_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_INFO_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_CONFIG_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_FINE_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_FINER_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_FINEST_TYPE}</li>
		<li>{@link #LOG_RECORD_NOTIFICATION_OTHER_TYPE}</li>
		</ul>
	 */
	/** Notification type <i>prefix</i> for a LogRecord Notification */
	public static final String LOG_RECORD_NOTIFICATION_PREFIX	= "org.glassfish.admin.amx.logging.Logging.";
	
	/** Notification type for a SEVERE LogRecord */
	public static final String LOG_RECORD_SEVERE_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.SEVERE;
	
	/** Notification type for a WARNING LogRecord */
	public static final String LOG_RECORD_WARNING_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.WARNING;
	
	/** Notification type for an INFO LogRecord */
	public static final String LOG_RECORD_INFO_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.INFO;
	
	/** Notification type for a CONFIG LogRecord */
	public static final String LOG_RECORD_CONFIG_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.CONFIG;
	
	/** Notification type for a FINE LogRecord */
	public static final String LOG_RECORD_FINE_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.FINE;
	
	/** Notification type for a FINER LogRecord */
	public static final String LOG_RECORD_FINER_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.FINER;
	
	/** Notification type for a FINEST LogRecord */
	public static final String LOG_RECORD_FINEST_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + Level.FINEST;
	
	/** Notification type for any other level eg a specific level number not equivalent to any of the standard ones */
	public static final String LOG_RECORD_OTHER_NOTIFICATION_TYPE	= LOG_RECORD_NOTIFICATION_PREFIX + "OTHER";
	
	public static final Set<String> ALL_LOG_RECORD_NOTIFICATION_TYPES   = Collections.unmodifiableSet(
	    SetUtil.newSet( new String[]
        	{
        	    LOG_RECORD_SEVERE_NOTIFICATION_TYPE,
        	    LOG_RECORD_WARNING_NOTIFICATION_TYPE,
        	    LOG_RECORD_INFO_NOTIFICATION_TYPE,
        	    LOG_RECORD_CONFIG_NOTIFICATION_TYPE,
        	    LOG_RECORD_FINE_NOTIFICATION_TYPE,
        	    LOG_RECORD_FINER_NOTIFICATION_TYPE,
        	    LOG_RECORD_FINEST_NOTIFICATION_TYPE,
        	    LOG_RECORD_OTHER_NOTIFICATION_TYPE,
        	} ));
	
	
	/**
	    All keys within the Map found in Notification.getUserData() have this prefix.
	 */
	public static final String  LOG_RECORD_KEY_PREFIX   = "LogRecord.";
	
	/**
		Key to access the string representation of the {@link java.util.logging.LogRecord}.
		This value will always be present and non-null.
		Use LOG_RECORD_STRING_KEY
		to obtain this value. 
	 */
	public static final String LOG_RECORD_AS_STRING_KEY	= LOG_RECORD_KEY_PREFIX + "toString";
	
	/** value: Level */
	public static final String LOG_RECORD_LEVEL_KEY	= LOG_RECORD_KEY_PREFIX + "Level";
	
	/** value: String */
	public static final String LOG_RECORD_LOGGER_NAME_KEY	= LOG_RECORD_KEY_PREFIX + "LoggerName";
	
	/** value: String */
	public static final String LOG_RECORD_MESSAGE_KEY	= LOG_RECORD_KEY_PREFIX + "Message";
	
	/** value: Long */
	public static final String LOG_RECORD_MILLIS_KEY	= LOG_RECORD_KEY_PREFIX + "Millis";
	
	/** value: Long */
	public static final String LOG_RECORD_SEQUENCE_NUMBER_KEY	= LOG_RECORD_KEY_PREFIX + "SequenceNumber";
	
	/** value: String */
	public static final String LOG_RECORD_SOURCE_CLASS_NAME_KEY	= LOG_RECORD_KEY_PREFIX + "SourceClassName";
	
	/** value: String */
	public static final String LOG_RECORD_SOURCE_METHOD_NAME_KEY	= LOG_RECORD_KEY_PREFIX + "SourceMethodName";
	
	/** value: Integer */
	public static final String LOG_RECORD_THREAD_ID_KEY	= LOG_RECORD_KEY_PREFIX + "ThreadID";
	
	/**
	    value: Throwable
	    <p>
	    All Throwables are remapped to standard java exceptions; the value will
	    be a Throwable, but it may not be the original Throwable as thrown on the server side.
	    <p>
	    If nothing was thrown this key will not be found in the Map.
	  */
	public static final String LOG_RECORD_THROWN_KEY	= LOG_RECORD_KEY_PREFIX + "Thrown";
	
	/**
	    value: Throwable
	    <p>
	    If LogRecord.getThrown() was non-null, this value is the innermost Throwable eg the root cause
	    found by following Throwable.getCause() until the innermost Throwable is reached.
	    <p>
	    If the root cause is the same as the thrown exception, then this value will not exist.
	  */
	public static final String LOG_RECORD_ROOT_CAUSE_KEY	= LOG_RECORD_KEY_PREFIX + "ThrownRootCause";
	
	/**
		Get the number of listeners for the specified log level.
		@param logLevel
		@return number of listeners listening for messages of the specified level, or higher level
	 */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
	public int	getLogLevelListenerCount( @Param(name="logLevel") final Level logLevel );
}





