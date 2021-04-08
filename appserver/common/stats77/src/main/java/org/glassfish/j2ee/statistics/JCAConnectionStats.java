/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.j2ee.statistics;

/**
 * Specifies the statistics provided by a JCA connection
 */
public interface JCAConnectionStats extends Stats {
    /** 
     * Returns the associated JCAConnectionFactory OBJECT_NAME
     * @return String the OBJECT_NAME of the managed object that identifies
     * the connection factory for this connection
     */
    String getConnectionFactory();

    /**
     * Returns the associated JCAManagedConnectionFactory OBJECT_NAME
     * @return String the OBJECT_NAME of the managed object that identifies
     * the managed connection factory for this connection
     */
    String getManagedConnectionFactory();

    /**
     * Returns the time spent waiting for a connection to be available
     * @return TimeStatistic
     */
    TimeStatistic getWaitTime();

    /**
     * Returns the time spent using a connection
     * @return TimeStatistic
     */
    TimeStatistic getUseTime();
}
