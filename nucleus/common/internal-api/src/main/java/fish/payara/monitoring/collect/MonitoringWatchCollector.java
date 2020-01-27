/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.monitoring.collect;

/**
 * API for collecting watches. A watch that is not shared during a collection is stopped. When a watch of same name is
 * shared with different values for thresholds or for conditions any existing watch of the same name but different
 * values is stopped and a new watch with the given values is started.
 * 
 * This API is limited an only allows starting watches of a standard type. This limitation is present as for now only
 * such watches are started by sources.
 * 
 * Note that watches only have any affect on the DAS instance.
 * 
 * This interface exist in its current form to allow a {@link MonitoringDataSource} to start watches without making the
 * monitoring implementation dependent on the source module or vice versa. Therefore none of the implementation specific
 * classes is used which causes the {@link #watch(CharSequence, String, String)} and {@link WatchBuilder} methods.
 * 
 * @author Jan Bernitt
 */
@FunctionalInterface
public interface MonitoringWatchCollector {

    /**
     * Starts creation of a watch with the given name and unit watching the given key metric. If a watch of the same
     * name already exists the watch continues unless any of the values specified differs in which case the existing
     * watch is stopped and an new one is started.
     * 
     * @param series  metric key name similar to collect method in {@link MonitoringDataCollector}
     * @param name watches installed using this method use the name to identify the watch they refer to.
     * @param unit "count", "percent", "bytes", "sec", "ms" or "ns"
     * @throws IllegalArgumentException when unit is not one of the recognised unit short names
     */
    WatchBuilder watch(CharSequence series, String name, String unit);

    @FunctionalInterface
    interface WatchBuilder {

        /**
         * Start and stop thresholds are given in case a condition exist, otherwise use null.
         * 
         * Negative thresholds can be used to express a 'less than' comparison for start while any positive number
         * expressed a 'greater than' comparison for start. Stop condition is always the other way around. Green level
         * always also allows equal values to the threshold.
         * 
         * If a for value is given without the corresponding threshold it is ignored.
         * 
         * For-Values use:
         * 
         * - {@link Integer} positive values to refer to n times in a row,
         *   zero includes all available values, 
         *   negative values refer to any 1 value in last abs(n) values.
         * - {@link Long} values to refer to n milliseconds
         * - {@code null} to not use any for condition, which is same as matching the start condition a single time by 
         *   the most recent value
         *
         * @param level          "red", "amber", or "green"
         * @param startThreshold the allowed upper limit, anything above causes an alert
         * @param startForLast   if given, start condition needs to be met for the specified continuance
         * @param startOnAverage true if start threshold should be compared to the average of the last points
         * @param stopTheshold   if given, the lower, anything below stops the alert
         * @param stopForLast    if given, stop condition needs to be met for the specified continuance
         * @param stopOnAverage  true if stop threshold should be compared to the average of the last points
         * @return This builder for chaining
         */
        WatchBuilder with(String level, long startThreshold, Number startForLast, boolean startOnAverage,
                Long stopTheshold, Number stopForLast, boolean stopOnAverage);

        default WatchBuilder red(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
                Number stopFor, boolean stopOnAverage) {
            return with("red", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
        }

        default WatchBuilder amber(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
                Number stopFor, boolean stopOnAverage) {
            return with("amber", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
        }

        default WatchBuilder green(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
                Number stopFor, boolean stopOnAverage) {
            return with("green", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
        }
    }
}
