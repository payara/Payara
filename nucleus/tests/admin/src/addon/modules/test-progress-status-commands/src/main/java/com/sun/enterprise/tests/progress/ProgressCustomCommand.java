/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.tests.progress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.Progress;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author mmares
 */
@Service(name = "progress-custom")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("progress")
@Progress
public class ProgressCustomCommand implements AdminCommand {
    
    /** Value must be in for {@code [Nx][MINSEC-]MAXSEC}
     */
    @Param(primary=true, optional=true, multiple=true, defaultValue="0")
    String[] intervals;
    
    private static final Pattern keyPattern = Pattern.compile("([\\ds]+x){0,1}(\\d+-){0,1}\\d+");
    
    private static class Interval {
        
        private boolean valid = true;
        private String origInteval;
        private int multiplicator = 1;
        private int minSec = -1;
        private int maxSec = 0;
        private boolean spin = false;
        
        private Interval(String interval) {
            origInteval = interval;
            try {
                if (!keyPattern.matcher(interval).matches()) {
                    valid = false;
                    return;
                }
                int ind = interval.indexOf('x');
                if (ind > 0) {
                    String substring = interval.substring(0, ind);
                    if (substring.contains("s")) {
                        this.spin = true;
                    } else {
                        multiplicator = Integer.parseInt(substring);
                    }
                    interval = interval.substring(ind + 1);
                }
                ind = interval.indexOf('-');
                if (ind > 0) {
                    minSec = Integer.parseInt(interval.substring(0, ind));
                    interval = interval.substring(ind + 1);
                }
                if (!interval.isEmpty()) {
                    maxSec = Integer.parseInt(interval);
                }
                if (minSec > maxSec) {
                    int tmp = minSec;
                    minSec = maxSec;
                    maxSec = tmp;
                }
            } catch (Exception ex) {
                valid  = false;
            }
        }
        
        public boolean isSpin() {
            return this.spin;
        }

        public int getMultiplicator() {
            if (valid) {
                return multiplicator;
            } else {
                return 1;
            }
        }

        public String getOrigInteval() {
            return origInteval;
        }

        public boolean isValid() {
            return valid;
        }

        public int getMaxSec() {
            return maxSec;
        }
        
        public long getMilis() {
            if (!valid) {
                return 0L;
            }
            if (minSec < 0) {
                return maxSec * 1000L;
            }
            return Math.round(Math.random() * ((maxSec - minSec) * 1000L)) + (minSec * 1000L);
        }
        
        @Override
        public String toString() {
            return origInteval;
        }
        
    }
    
    private Collection<Interval> parsedIntervals;
    
    private int getStepCount() {
        if (parsedIntervals == null) {
            return 0;
        }
        int result = 0;
        for (Interval interval : parsedIntervals) {
            result += interval.getMultiplicator();
        }
        return result;
    }

    @Override
    public void execute(AdminCommandContext context) {
        ProgressStatus ps = context.getProgressStatus();
        parsedIntervals = new ArrayList<Interval>(intervals != null ? intervals.length : 0);
        ActionReport report = context.getActionReport();
        for (String interval : intervals) {
            parsedIntervals.add(new Interval(interval));
        }
        //Count
        if (parsedIntervals.isEmpty()) {
            report.setMessage("Done command process without waiting interval.");
            return;
        }
        ps.setTotalStepCount(getStepCount());
        int blockId = 0;
        for (Interval interval : parsedIntervals) {
            blockId++;
            if (interval.isValid()) {
                int multip = interval.getMultiplicator();
                if (interval.getMaxSec() == 0) {
                    ps.progress(multip, "Finished block without sleeping: [" +blockId + "] " + interval);
                } else {
                    for (int i = 0; i < multip; i++) {
                        if (i == 0) {
                            ps.progress(0, "Starting block[" +blockId + "] " + interval, interval.isSpin());
                        }
                        try {
                            Thread.sleep(interval.getMilis());
                        } catch (Exception ex) {
                        }
                        if (i == (multip - 1)) {
                            ps.progress(1, "Finished block [" +blockId + "] " + interval);
                        } else {
                            ps.progress(1, "Block [" +blockId + "] " + interval + ", step: " + (i + 1));
                        }
                    }
                }
            } else {
                ps.progress(1, "Finished unparsable block [" +blockId + "] " + interval);
            }
        }
        report.setMessage("Finished command process in " + parsedIntervals.size() + " block(s).");
    }
    
}
