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
 */

package com.sun.enterprise.admin.monitor.stats;

import com.sun.enterprise.admin.monitor.stats.StringStatistic;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;

/** 
 * Interface representing statistical information about the request bucket 
 */
public interface PWCRequestStats extends Stats {
    
    /**
     * Gets the method of the last request serviced.
     *
     * @return Method of the last request serviced
     */    
    public StringStatistic getMethod();
    
    /**
     * Gets the URI of the last request serviced.
     *
     * @return URI of the last request serviced
     */    
    public StringStatistic getUri();
    
    /** 
     * Gets the number of requests serviced.
     *
     * @return Number of requests serviced
     */    
    public CountStatistic getCountRequests();
    
    /** 
     * Gets the number of bytes received.
     *
     * @return Number of bytes received, or 0 if this information is
     * not available
     */    
    public CountStatistic getCountBytesReceived();
    
    /** 
     * Gets the number of bytes transmitted.
     *
     * @return Number of bytes transmitted, or 0 if this information
     * is not available
     */    
    public CountStatistic getCountBytesTransmitted();
    
    /** 
     * Gets the rate (in bytes per second) at which data was transmitted
     * over some server-defined interval.
     * 
     * @return Rate (in bytes per second) at which data was
     * transmitted over some server-defined interval, or 0 if this
     * information is not available
     */    
    public CountStatistic getRateBytesTransmitted();
    
    /** 
     * Gets the maximum rate at which data was transmitted over some
     * server-defined interval.
     *
     * @return Maximum rate at which data was transmitted over some
     * server-defined interval, or 0 if this information is not available.
     */    
    public CountStatistic getMaxByteTransmissionRate();
    
    /** 
     * Gets the number of open connections.
     *
     * @return Number of open connections, or 0 if this information
     * is not available
     */    
    public CountStatistic getCountOpenConnections();
    
    /** 
     * Gets the maximum number of open connections.
     *
     * @return Maximum number of open connections, or 0 if this
     * information is not available
     */    
    public CountStatistic getMaxOpenConnections();
    
    /** 
     * Gets the number of 200-level responses sent.
     *
     * @return Number of 200-level responses sent
     */    
    public CountStatistic getCount2xx();
    
    /**
     * Gets the number of 300-level responses sent.
     *
     * @return Number of 300-level responses sent
     */    
    public CountStatistic getCount3xx();
    
    /**
     * Gets the number of 400-level responses sent.
     *
     * @return Number of 400-level responses sent
     */    
    public CountStatistic getCount4xx();
    
    /**
     * Gets the number of 500-level responses sent.
     *
     * @return Number of 500-level responses sent
     */    
    public CountStatistic getCount5xx();
    
    /**
     * Gets the number of responses sent that were not 200, 300, 400,
     * or 500 level.
     *
     * @return Number of responses sent that were not 200, 300, 400,
     * or 500 level
     */    
    public CountStatistic getCountOther();
    
    /**
     * Gets the number of responses with a 200 response code.
     *
     * @return Number of responses with a 200 response code
     */    
    public CountStatistic getCount200();
    
    /**
     * Gets the number of responses with a 302 response code.
     *
     * @return Number of responses with a 302 response code
     */    
    public CountStatistic getCount302();
    
    /**
     * Gets the number of responses with a 304 response code.
     *
     * @return Number of responses with a 304 response code
     */    
    public CountStatistic getCount304();
    
    /**
     * Gets the number of responses with a 400 response code.
     *
     * @return Number of responses with a 400 response code
     */    
    public CountStatistic getCount400();
    
    /**
     * Gets the number of responses with a 401 response code.
     *
     * @return Number of responses with a 401 response code
     */    
    public CountStatistic getCount401();
    
    /**
     * Gets the number of responses with a 403 response code.
     *
     * @return Number of responses with a 403 response code
     */    
    public CountStatistic getCount403();
    
    /**
     * Gets the number of responses with a 404 response code.
     *
     * @return Number of responses with a 404 response code
     */    
    public CountStatistic getCount404();
    
    /**
     * Gets the number of responses with a 503 response code.
     *
     * @return Number of responses with a 503 response code
     */    
    public CountStatistic getCount503();
    
}
