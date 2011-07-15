/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * Profiler.java
 *
 * Created on September 17, 2001, 12:42 PM
 */
package com.sun.enterprise.util;

/**
 * A easy-to-use class that wraps one global ProfilerImpl object.  Use it to begin
 * and end profiling in one 'profiling thread'.  I.e. use this object to get timing for
 * sub-operations.  Use separate ProfilerImpl objects to get timings for overlapping 
 * profile needs.
 *
 * <p> WARNING: Call reset at the end to avoid memory leaks. 
 *
 * @author  bnevins
 * @version 
 */
public class Profiler {

    private Profiler() {
    }

    /** Reset the global ProfilerImpl instance.
     **/
    public static void reset() {
        profiler.reset();
    }

    /** Start timing an item.
     **/
    public static void beginItem() {
        profiler.beginItem();
    }

    /** Start timing an item.
     * @param desc - Descriptive text for the item
     */
    public static void beginItem(String desc) {
        profiler.beginItem(desc);
    }

    /** Stop timing of the latest item
     */
    public static void endItem() {
        profiler.endItem();
    }

    /** return a String report of all the timings
     * @return  */
    public static String report() {
        return profiler.toString();
    }

    /**
     * Convenience method to avoid endItem() beginItem() bracketing
     * @param desc - Descriptive text for the item
     */
    public static void subItem(String desc) {
        endItem();
        beginItem(desc);
    }
    
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @param notUsed  */
    public static void main(String[] notUsed) {
        try {
            profiler.beginItem("first item");
            Thread.sleep(3000);
            profiler.beginItem("second item here dude whoa yowser yowser");
            Thread.sleep(1500);
            profiler.endItem();
            profiler.endItem();
            System.out.println("" + profiler);
        } catch (Exception e) {
        }
    }
    static ProfilerImpl profiler = new ProfilerImpl();
}
