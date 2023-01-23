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
 * Statistics provided by a JDBC resource
 *
 * @author Hans Hrasna
 */
public interface JDBCStats extends Stats{
    /* Returns an array of JDBCConnectionStats that provide statistics about the
     * non-pooled connections associated with the referencing JDBC resource stats
     * @return JDBCConnectionStats []
     */
    JDBCConnectionStats[] getConnections();

    /* Returns an array of JDBCConnectionPoolStats that provide statistics about the
     * connection pools associated with the referencing JDBC resource stats
     * @return JDBCConnectionStats []
     */
    JDBCConnectionPoolStats[] getConnectionPools();
}
