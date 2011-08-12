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
import org.glassfish.j2ee.statistics.Statistic;
import java.io.Serializable;

/** An interface that gives a flexibility to set various values for a particular Statistic.
 * This provision is basically to have the same data structure to collect and
 * return the (read-only copy of) statistical data. This extends the
 * java.io.Serializable, so that its subclasses can be serialized to facilitate
 * working with other management clients.
 * Refer to the package description
 * to understand the intent of this interface. 
 * <P>
 * Methods of this class should be called judiciously by the component that is 
 * gathering the statistics.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public interface MutableStatistic extends Serializable {

    /** 
     * Returns a read-only view of this Statistic. An implementing class has
     * to return the instances of Statistic interfaces defined in {@link javax.management.j2ee.statistic}
     * and {@link com.sun.enterprise.admin.monitor.stats} packages.
     * @return      an instance of a specific Statistic interface
     */
    public Statistic unmodifiableView();

    /**
     * Returns an instance of Statistic whose state can be changed by the caller. It is important
     * to know that caller should not cache the return value from this method. In general, there
     * is a problem in this contract in that, a client would not know from a Stats.getCreateCount()
     * method whether the return value is something that is being modified or is invariant. Hence
     * the caller should not cache the returned value in a collection and then collectively
     * process it. The main idea behind providing this method is to control the
     * number of instances created (per Mahesh's Suggestion).
     * @return      an instance of Statistic interface that should not be cached.
     */
    public Statistic modifiableView();
    
    /** 
     * Resets the encapsulated Statistic interface to its initial value. The idea being, if (after
     * creation of the instance) the state changes, the initial state can be easily regained by
     * calling this method. Note that the time of last sampling changes to the instant
     * when this method is invoked.
     */
    public void reset();
}
