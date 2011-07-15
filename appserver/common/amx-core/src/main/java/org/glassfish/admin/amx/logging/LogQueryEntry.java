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

import java.util.Date;
import java.util.Map;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;

/**
	An individual result representing a log entry found
	by {@link LogQuery#queryServerLog}.
	
	@since AS 9.0
	@see LogQueryResult
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public interface LogQueryEntry
{
    /**
        Get the fields associated with this entry.
        The fields are indexed by the values found in
        {@link LogRecordFields}.  A field is always non-null.
     */
    public Object[] getFields();
    
    /**
        The record number within the log file (first one is 0).
     */
    public long getRecordNumber();
    
    /**
        The name of the product.
     */
    public String getProductName();
    
     
    /**
        The Date that the log entry was emitted.
     */
    public Date     getDate();
    
    /**
        The module or Logger that emitted the entry.
     */
    public String   getModule();
    
    /**
        The Level of the entry.
     */
    public String    getLevel();
    
    /**
        The unique message ID identifying the entry.
     */
    public String   getMessageID();
    
    /**
        The free-form message.
     */
    public String   getMessage();
    
    
    /**
        Key for the thread ID within the Map returned by {@link #getNameValuePairsMap}.
        Value is of type java.lang.String.
     */
    public static final String  THREAD_ID_KEY   = "_ThreadID";
    
    /**
        Key for the ObjectName within the Map returned by {@link #getNameValuePairsMap}.
        Value is of type javax.management.ObjectName.
     */
    public static final String  OBJECT_NAME_KEY   = "_ObjectName";
    
    /**
        A Map containing name/value pairs as parsed
        from the String given by {@link #getNameValuePairs}.
        Values which are available for public use are:
        <ul>
        <li>{@link #THREAD_ID_KEY}</li>
        <li>{@link #OBJECT_NAME_KEY}</li>
        </ul>
     */
    public Map<String,String>   getNameValuePairsMap();
    
    /**
        The raw name/value pair String for this log entry.  Each
        pair is separated by the ';' character.
     */
    public String   getNameValuePairs();
    
    /**
        The ID of the thread that emitted the entry (may be null).
     */
    public String   getThreadID();
}






