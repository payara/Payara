/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.ejb.entitybean.container;

import java.util.Date;

/**
 * Per-primary key information stored for read-only beans.
 *
 * @author Kenneth Saks
 */

final class ReadOnlyBeanInfo
{

    Object primaryKey;

    // Used to track staleness versus the bean-level refresh.
    int beanLevelSequenceNum;

    // Set to true when a programmatic refresh takes place.  
    boolean refreshNeeded;
    
    // Sequence number associated with a point in time when refresh occurred.
    // Each context for this pk also has a sequence number value.  If they
    // differ it means the context needs an ejbLoad.
    int pkLevelSequenceNum;

    // last time when refresh was programattically requested for this PK.
    long lastRefreshRequestedAt;
    
    // time at which refresh actually occurred.
    long lastRefreshedAt;
    
    Object	cachedEjbLocalObject;	    //Cached only for findByPK

    Object	cachedEjbObject;	    //Cached only for findByPK

    public String toString() {
        
        StringBuffer buffer = new StringBuffer();
        buffer.append("Read Only Bean Info for " + primaryKey + "\n");
        buffer.append("Refresh needed = " + refreshNeeded + "\n");
        buffer.append("Bean level sequence num = " + beanLevelSequenceNum 
                      + "\n");
        buffer.append("PK level sequence num = " + pkLevelSequenceNum + "\n");
        if( lastRefreshRequestedAt > 0 ) {
            buffer.append("Last refresh requested at " + 
                          new Date(lastRefreshRequestedAt) 
                          + "\n");
        } else {
            buffer.append("Refresh has never been requested\n");
        }
        if( lastRefreshedAt > 0 ) {
            buffer.append("Last refreshed at " + 
                          new Date(lastRefreshedAt) + "\n");
        } else {
            buffer.append("Never refreshed\n");
        }
        
        return buffer.toString();
    }
    
}
