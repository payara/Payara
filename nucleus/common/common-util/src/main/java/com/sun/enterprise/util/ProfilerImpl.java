/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * ProfilerImpl.java
 *
 * Created on September 17, 2001, 12:42 PM
 */
package com.sun.enterprise.util;

import java.util.*;

/** Simple class for profiling code.  beginItem/endItem pairs start and stop the timing for an item.
 *
 * @author  bnevins
 */
public class ProfilerImpl {

    /** Create an empty object
     */
    public ProfilerImpl() {
    }

    /**Reset all the timing information
     */
    public void reset() {
        currItem = null;
        items.clear();
        numBegins = 0;
        numEnds = 0;
        numActualEnds = 0;
    }

    /** Start timing an item.
     **/
    public void beginItem() {
        beginItem("No Description");
    }

    /** Start timing an item.
     * @param desc - Descriptive text for the item
     **/
    public void beginItem(String desc) {
        //if(currItem != null)
        //Reporter.assert(currItem.hasEnded());

        currItem = new Item(desc);
        items.add(currItem);
        ++numBegins;
    }

    /** Stop timing an item and store the information.
     **/
    public void endItem() {
        ++numEnds;
        Item item = getLastNotEnded();

        if (item != null) {
            item.end();
        }
        ++numActualEnds;
    }

    /** Return a formatted String with the timing information
     **/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nBegins: ").append(numBegins).append(", Ends: ").append(numEnds)
                .append(", Actual Ends: ").append(numActualEnds).append("\n");

        sb.append(Item.getHeader());
        sb.append("\n");


        for (Iterator iter = items.iterator(); iter.hasNext();) {
            Item item = (Item) iter.next();
            sb.append(item.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    ////////////////////////////////////////////////////////////////////////////
    private Item getLastNotEnded() {
        int index = items.size();

        while (--index >= 0) {
            Item item = (Item) items.get(index);

            if (!item.hasEnded()) {
                return item;
            }
        }
        return null;
    }

    ////////////////////////////////////////////////////////////////////////////
    private static class Item {

        Item(String desc) {
            title = desc;
            startTime = System.currentTimeMillis();
            endTime = startTime;
            setLongestTitle(title.length());
        }

        boolean hasEnded() {
            return ended;
            //return endTime > startTime;
        }

        void end() {
            endTime = System.currentTimeMillis();
            ended = true;
        }

        @Override
        public String toString() {
            long finish = hasEnded() ? endTime : System.currentTimeMillis();

            String totalTime = "" + (finish - startTime);

            if (totalTime.equals("0")) {
                totalTime = "< 1";
            }

            String desc = StringUtils.padRight(title, longestTitle + 1);
            String time = StringUtils.padLeft(totalTime, 8);

            if (!hasEnded()) {
                time += "  ** STILL RUNNING **";
            }

            return desc + time;
        }

        public static String getHeader() {
            return "\n" + StringUtils.padRight("Description", longestTitle + 1) + StringUtils.padLeft("msec", 8);
        }

        private static void setLongestTitle(int len) {
            synchronized (lock) {
                if (len > longestTitle) {
                    longestTitle = len;
                }
            }
        }
        String title;
        long startTime;
        long endTime;
        static int longestTitle = 12;
        private final static Object lock = new Object();
        boolean ended = false;
    }
    ////////////////////////////////////////////////////////////////////////////
    Item currItem = null;
    List<Item> items = new ArrayList<Item>();
    int numBegins = 0;
    int numEnds = 0;
    int numActualEnds = 0;

    ////////////////////////////////////////////////////////////////////////////
    /** Simple unit test
     **/
    public static void main(String[] notUsed) {
        ProfilerImpl p = new ProfilerImpl();

        try {
            p.beginItem("first item");
            Thread.sleep(3000);
            p.beginItem("second item here dude whoa yowser yowser");
            Thread.sleep(1500);
            p.endItem();
            p.endItem();
            System.out.println("" + p);
        } catch (Exception e) {
        }
    }
}
