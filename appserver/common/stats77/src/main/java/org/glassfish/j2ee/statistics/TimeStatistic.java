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
 * Specifies standard timing measurements. 
 */
public interface TimeStatistic extends Statistic {
    /**
     * Number of times the operation was invoked since the beginning of this measurement. 
     */
    long getCount();

    /**
     * The maximum amount of time taken to complete one invocation of this operation since the beginning of this measurement. 
     */
    long getMaxTime();

    /**
     * The minimum amount of time taken to complete one invocation of this operation since the beginning of this measurement. 
     */
    long getMinTime();

    /**
     * This is the sum total of time taken to complete every invocation of this operation since the beginning of this measurement. Dividing totalTime by count will give you the average execution time for this operation. 
     */
    long getTotalTime();
}
