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
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.CountStatistic;

/** 
 * A Stats interface to represent the statistical data exposed by an
 * HTTP Listener. This include data about the GlobalRequestProcessor
 * and the ThreadPool.
 * The GlobalRequestProcessor collects data about request processing 
 * from each of the RequestProcessor threads.
 * @author Murali Vempaty
 * @since S1AS8.0
 * @version 1.0
 */
public interface HTTPListenerStats extends Stats {
    
    // GlobalRequestProcessor statistics for the listener
    // TODO: Consolidate the statistics into Boundary or BoundedRange
    // statistics, as necessitated. For now, will leave everything
    // as a CountStatistic
    
    /**
     * Cumulative value of the bytesReceived by each of the
     * RequestProcessors
     * @return CountStatistic
     */
    public CountStatistic getBytesReceived();

    /**
     * Cumulative value of the bytesSent by each of the
     * RequestProcessors
     * @return CountStatistic
     */
    public CountStatistic getBytesSent();
    
    /**
     * Cumulative value of the errorCount of each of the
     * RequestProcessors. The errorCount represents the number of
     * cases where the response code was >= 400
     * @return CountStatistic
     */
    public CountStatistic getErrorCount();
    
    /**
     * The longest response time for a request. This is not a
     * cumulative value, but is the maximum of the response times
     * for each of the RequestProcessors.
     * @return CountStatistic
     */
    public CountStatistic getMaxTime();
    
    /**
     * Cumulative value of the processing times of each of the
     * RequestProcessors. The processing time of a RequestProcessor
     * is the average of request processing times over the request
     * count.
     * @return CountStatistic
     */
    public CountStatistic getProcessingTime();
    
    /**
     * Cumulative number of the requests processed so far, 
     * by the RequestProcessors.
     * @return CountStatistic
     */
    public CountStatistic getRequestCount();
    
    
    /**
     * Returns the number of responses with a status code in the 2xx range
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code in the 2xx range
     */
    public CountStatistic getCount2xx();


    /**
     * Returns the number of responses with a status code in the 3xx range
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code in the 3xx range
     */
    public CountStatistic getCount3xx();


    /**
     * Returns the number of responses with a status code in the 4xx range
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code in the 4xx range
     */
    public CountStatistic getCount4xx();


    /**
     * Returns the number of responses with a status code in the 5xx range
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code in the 5xx range
     */
    public CountStatistic getCount5xx();


    /**
     * Returns the number of responses with a status code outside the 2xx,
     * 3xx, 4xx, and 5xx range, sent by the HTTP listener whose statistics
     * are exposed by this <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code outside the 2xx, 3xx,
     * 4xx, and 5xx range
     */
    public CountStatistic getCountOther();


    /**
     * Returns the number of responses with a status code equal to 200
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 200
     */
    public CountStatistic getCount200();


    /**
     * Returns the number of responses with a status code equal to 302
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 302
     */
    public CountStatistic getCount302();


    /**
     * Returns the number of responses with a status code equal to 304
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 304
     */
    public CountStatistic getCount304();


    /**
     * Returns the number of responses with a status code equal to 400
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 400
     */
    public CountStatistic getCount400();


    /**
     * Returns the number of responses with a status code equal to 401
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 401
     */
    public CountStatistic getCount401();


    /**
     * Returns the number of responses with a status code equal to 403
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 403
     */
    public CountStatistic getCount403();


    /**
     * Returns the number of responses with a status code equal to 404
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 404
     */
    public CountStatistic getCount404();


    /**
     * Returns the number of responses with a status code equal to 503
     * sent by the HTTP listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Number of responses with a status code equal to 503
     */
    public CountStatistic getCount503();


    /**
     * Returns the number of open connections managed by the HTTP listener
     * whose statistics are exposed by this <code>HTTPListenerStats</code>.
     *
     * @return Number of open connections
     */
    public CountStatistic getCountOpenConnections();


    /**
     * Returns the maximum number of open connections managed by the HTTP
     * listener whose statistics are exposed by this
     * <code>HTTPListenerStats</code>.
     *
     * @return Maximum number of open connections
     */
    public CountStatistic getMaxOpenConnections();


    // ThreadPool statistics for the listener
    
    /**
     * The number of request processing threads currently in the
     * thread pool
     * @return CountStatistic
     */
    public CountStatistic getCurrentThreadCount();
    
    /**
     * The number of request processing threads currently in the
     * thread pool, serving requests.
     * @return CountStatistic
     */
    public CountStatistic getCurrentThreadsBusy();
    
    /**
     * The maximum number of request processing threads that are
     * created by the listener. It determines the maximum number of
     * simultaneous requests that can be handled
     * @return CountStatistic
     */
    public CountStatistic getMaxThreads();
    
    /** 
     * The maximum number of unused request processing threads that will
     * be allowed to exist until the thread pool starts stopping the 
     * unnecessary threads.
     * @return CountStatistic
     */
    public CountStatistic getMaxSpareThreads();

    /**
     * The number of request processing threads that will be created 
     * when this listener is first started.
     * @return CountStatistic
     */
    public CountStatistic getMinSpareThreads();
    
}
