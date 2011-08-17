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

import javax.management.MBeanOperationInfo;
import org.glassfish.admin.amx.annotation.ManagedAttribute;
import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.admin.amx.annotation.Param;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;


/**
	Provides access to log files.
	
	@since AS 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public interface LogFileAccess
{
    /**
        Value meaning the most current version of the log file.
     */
    public static final String  MOST_RECENT_NAME = "MostRecentLogFileName";
     
	/**
		Key designating the server.log log files.
		<i>Not necessarily the same as the file name of the log file</i>.
	 */
    public static final String	SERVER_KEY	= "server";
    
	/**
		Key designating the access.log log file.
		<i>Not necessarily the same as the file name of the log file</i>.
	 */
    public static final String	ACCESS_KEY	= "access";
    
    /**
    	Keys which may be used to specify which log file to access.
    	Legal values include:
    	<ul>
    	<li>{@link #SERVER_KEY}</li>
    	<li>{@link #ACCESS_KEY} <b>is not supported</b></li>
    	</ul>
    	@return a String[] of the legal keys which designate log files
     */
    @ManagedAttribute
    public String[]	getLogFileKeys();
    
    
	/**
		The names returned are <i>not</i> full paths but the simple file
		names of the log files.
		The last name in the resulting String[] will always be that of the
		current log file.  In other words if the String[] las length 3,
		then result[2] will be the name of the current log file.
		<p>
		Note that it is possible for log file rotation to occur after making
		this call, thus rendering the list incomplete.
		<p>
		The resulting names may be passed to {@link #getLogFile} to retrieve
		the contents.
		<p>
		The only legal key supported is {@link #SERVER_KEY}.
		
		@param key	a key specifying the type of log file
		@return String[] of all log filenames 
	 */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
	public String[] getLogFileNames( @Param(name="key") final String key );
	
	/**
		The entire specified log file is read, converted into a String, and returned.
		The resulting String may be quite large.
		<p>
		The only legal key supported is {@link #SERVER_KEY}.
		
		@param key a legal key designating a log file
		@param fileName	a log file name as returned by {@link #getLogFileNames} or
		{@link #MOST_RECENT_NAME} if current log file is desired.
		@return the contents of the specified log file in a String
		@see #getLogFileKeys
	 */
    @ManagedOperation(impact=MBeanOperationInfo.INFO)
	public String getLogFile(
        @Param(name="key") final String key,
        @Param(name="fileName") final String fileName );
	
    /**
		Rotate all log files as soon as possible.  May return
		prior to the rotation occuring.
     */
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    public void rotateAllLogFiles();
    
    
    /**
    	Rotate the log file of the specified type.  Legal values are those
    	returned by {@link #getLogFileKeys}.
    	Legal values include:
    	<ul>
    	<li>{@link #SERVER_KEY}</li>
    	<li>{@link #ACCESS_KEY} <b>is not supported</b></li>
    	</ul>
    	@param key
     */
    @ManagedOperation(impact=MBeanOperationInfo.ACTION)
    public void rotateLogFile( @Param(name="key") String key );
}














