/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2000-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.util;

public interface MonitoringConstants
{
    public static final String DEFAULT_MONITORING_ROOT = "orb";
    public static final String DEFAULT_MONITORING_ROOT_DESCRIPTION = 
        "ORB Management and Monitoring Root";

    //
    // Connection Monitoring
    //

    public static final String CONNECTION_MONITORING_ROOT =
	"Connections";
    public static final String CONNECTION_MONITORING_ROOT_DESCRIPTION =
	"Statistics on inbound/outbound connections";

    public static final String INBOUND_CONNECTION_MONITORING_ROOT =
	"Inbound";
    public static final String INBOUND_CONNECTION_MONITORING_ROOT_DESCRIPTION=
	"Statistics on inbound connections";

    public static final String OUTBOUND_CONNECTION_MONITORING_ROOT =
	"Outbound";
    public static final String OUTBOUND_CONNECTION_MONITORING_ROOT_DESCRIPTION=
	"Statistics on outbound connections";

    public static final String CONNECTION_MONITORING_DESCRIPTION =
	"Connection statistics";

    public static final String CONNECTION_TOTAL_NUMBER_OF_CONNECTIONS =
	"NumberOfConnections";
    public static final String CONNECTION_TOTAL_NUMBER_OF_CONNECTIONS_DESCRIPTION =
	"The total number of connections";
    public static final String CONNECTION_NUMBER_OF_IDLE_CONNECTIONS =
	"NumberOfIdleConnections";
    public static final String CONNECTION_NUMBER_OF_IDLE_CONNECTIONS_DESCRIPTION =
	"The number of idle connections";
    public static final String CONNECTION_NUMBER_OF_BUSY_CONNECTIONS =
	"NumberOfBusyConnections";
    public static final String CONNECTION_NUMBER_OF_BUSY_CONNECTIONS_DESCRIPTION =
	"The number of busy connections";
 
    //
    // ThreadPool and WorkQueue monitoring constants
    //

    public static final String THREADPOOL_MONITORING_ROOT = "threadpool";
    public static final String THREADPOOL_MONITORING_ROOT_DESCRIPTION =
	"Monitoring for all ThreadPool instances";
    public static final String THREADPOOL_MONITORING_DESCRIPTION =
	"Monitoring for a ThreadPool";
    public static final String THREADPOOL_CURRENT_NUMBER_OF_THREADS =
	"currentNumberOfThreads";
    public static final String THREADPOOL_CURRENT_NUMBER_OF_THREADS_DESCRIPTION =
	"Current number of total threads in the ThreadPool";
    public static final String THREADPOOL_NUMBER_OF_AVAILABLE_THREADS =
	"numberOfAvailableThreads";
    public static final String THREADPOOL_NUMBER_OF_AVAILABLE_THREADS_DESCRIPTION =
	"Number of available threads in the ThreadPool";
    public static final String THREADPOOL_NUMBER_OF_BUSY_THREADS =
	"numberOfBusyThreads";
    public static final String THREADPOOL_NUMBER_OF_BUSY_THREADS_DESCRIPTION =
	"Number of busy threads in the ThreadPool";
    public static final String THREADPOOL_AVERAGE_WORK_COMPLETION_TIME =
	"averageWorkCompletionTime";
    public static final String THREADPOOL_AVERAGE_WORK_COMPLETION_TIME_DESCRIPTION =
	"Average elapsed time taken to complete a work item by the ThreadPool";
    public static final String THREADPOOL_CURRENT_PROCESSED_COUNT =
	"currentProcessedCount";
    public static final String THREADPOOL_CURRENT_PROCESSED_COUNT_DESCRIPTION =
	"Number of Work items processed by the ThreadPool";

    public static final String WORKQUEUE_MONITORING_DESCRIPTION =
	"Monitoring for a Work Queue";
    public static final String WORKQUEUE_TOTAL_WORK_ITEMS_ADDED =
	"totalWorkItemsAdded";
    public static final String WORKQUEUE_TOTAL_WORK_ITEMS_ADDED_DESCRIPTION =
	"Total number of Work items added to the Queue";
    public static final String WORKQUEUE_WORK_ITEMS_IN_QUEUE =
	"workItemsInQueue";
    public static final String WORKQUEUE_WORK_ITEMS_IN_QUEUE_DESCRIPTION =
	"Number of Work items in the Queue to be processed";
    public static final String WORKQUEUE_AVERAGE_TIME_IN_QUEUE =
	"averageTimeInQueue";
    public static final String WORKQUEUE_AVERAGE_TIME_IN_QUEUE_DESCRIPTION =
	"Average time a work item waits in the work queue";
}

// End of file.
