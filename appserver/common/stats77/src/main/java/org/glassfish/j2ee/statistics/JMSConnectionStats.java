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
 * Specifies the statistics provided by a JMS connection
 *
 * @author Hans Hrasna
 */
public interface JMSConnectionStats extends Stats {
    /**
     * Returns an array of JMSSessionStats that provide statistics
     * about the sessions associated with the referencing JMSConnectionStats.
     */
    JMSSessionStats[] getSessions();

    /**
     * Returns the transactional state of this JMS connection.
     * If true, indicates that this JMS connection is transactional.
     */
    boolean isTransactional();

}
