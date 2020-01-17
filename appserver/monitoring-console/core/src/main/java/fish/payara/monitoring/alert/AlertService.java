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
package fish.payara.monitoring.alert;

import java.util.Collection;
import java.util.function.Predicate;

import org.jvnet.hk2.annotations.Contract;

import fish.payara.monitoring.model.Series;

/**
 * The {@link AlertService} manages and evaluates {@link Watch}s that cause {@link Alert}s.
 *
 * @author Jan Bernitt
 */
@Contract
public interface AlertService {

    /*
     * Alerts
     */

    class AlertStatistics {
        /**
         * Can be used by (asynchronous) consumers to determine if they have seen the most recent state of alerts. If
         * the change count is still the same they have processed already there is nothing new to process.
         */
        public int changeCount;
        public int unacknowledgedRedAlerts;
        public int acknowledgedRedAlerts;
        public int unacknowledgedAmberAlerts;
        public int acknowledgedAmberAlerts;
        public int watches;
    }

    AlertStatistics getAlertStatistics();

    Collection<Alert> alertsMatching(Predicate<Alert> filter);

    default Alert alertBySerial(int serial) {
        Collection<Alert> matches = alertsMatching(alert -> alert.serial == serial);
        return matches.isEmpty() ? null : matches.iterator().next();
    }

    default Collection<Alert> alertsFor(Series series) {
        return alertsMatching(alert -> alert.getSeries().equalTo(series));
    }

    default Collection<Alert> alerts() {
        return alertsMatching(alert -> true);
    }

    /*
     * Watches
     */

    /**
     * Adds a watch to the evaluation loop. To remove the watch just use {@link Watch#stop()}.
     *
     * @param watch new watch to add to evaluation loop
     */
    void addWatch(Watch watch);

    /**
     * @return All watches registered for evaluation.
     */
    Collection<Watch> watches();

    /**
     * @param series a simple or pattern {@link Series}, not null
     * @return All watches matching the given {@link Series}. {@link Series#ANY} will match all watches similar to
     *         {@link #watches()}.
     */
    Collection<Watch> wachtesFor(Series series);
}
