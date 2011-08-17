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

import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;


/**
	Indices into log record fields as returned by
	{@link LogQuery#queryServerLog}.  Also 
	@since AppServer 9.0
 */
@Taxonomy(stability = Stability.EXPERIMENTAL)
public final class LogRecordFields
{
	private LogRecordFields()	{}
	
	/** Value is of class java.lang.Integer */
    public final static int    RECORD_NUMBER_INDEX   = 0;
    
	/** Value is of class java.util.Date */
    public final static int    DATE_INDEX            = 1;
    
	/** Value is of class java.lang.String */
    public final static int    LEVEL_INDEX           = 2;
    
	/** Value is of class java.lang.String */
    public final static int    PRODUCT_NAME_INDEX    = 3;
    
	/** Value is of class java.lang.Integer */
    public final static int    MESSAGE_INDEX         = 7;
    
	/** Value is of class java.lang.String */
    public final static int    MESSAGE_ID_INDEX      = 6;  //need to extract from the message text
    
	/** Value is of class java.lang.String */
    public final static int    MODULE_INDEX          = 4;
    
	/** Value is of class java.lang.String */
    public final static int    NAME_VALUE_PAIRS_INDEX  = 5;
    
    /** Number of fields provided by {@link LogQuery#queryServerLog} */
    public final static int    NUM_FIELDS   = MESSAGE_INDEX + 1;
    
    public final static String THREAD_ID_KEY   = "_ThreadID";
    public final static String OBJECTNAME_KEY   = "_ObjectName";
}

