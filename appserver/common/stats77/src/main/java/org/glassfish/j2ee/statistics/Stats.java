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
 * The Stats model and its submodels specify performance data attributes for each of the specific managed object types. 
 */
public interface Stats {
    /**
     * Get a Statistic by name. 
     */
    Statistic getStatistic(String statisticName);

    /**
     * Returns an array of Strings which are the names of the attributes from the specific Stats submodel that this object supports. Attributes named in the list must correspond to attributes that will return a Statistic object of the appropriate type which contains valid performance data.  The return value of attributes in the Stats submodel that are not included in the statisticNames list must be null. For each name in the statisticNames list there must be one Statistic with the same name in the statistics list. 
     */
    String [] getStatisticNames();

    /**
     * Returns an array containing all of the Statistic objects supported by this Stats object. 
     */
    Statistic[] getStatistics();

}
