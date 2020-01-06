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

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.SeriesDataset;

public final class Circumstance {

    public static final Circumstance NONE = new Circumstance(Level.WHITE, Condition.NONE, Condition.NONE);

    public final Level level;
    public final Condition start;
    public final Condition stop;
    public final Condition suppress;
    public final Metric suppressing;

    public Circumstance(Level level, Condition start, Condition stop) {
        this(level, start, stop, null, Condition.NONE);
    }

    public Circumstance(Level level, Condition start, Condition stop, Metric suppressing, Condition suppress) {
        this.level = level;
        this.start = start;
        this.stop = stop;
        this.suppressing = suppressing;
        this.suppress = suppress;
    }

    public boolean isNone() {
        return level == Level.WHITE;
    }

    public boolean starts(SeriesDataset data, SeriesLookup lookup) {
        if (isNone()) {
            return false;
        }
        if (!suppress.isNone()) {
            for (SeriesDataset set : lookup.selectSeries(suppressing.series, data.getInstance())) {
                if (suppress.isSatisfied(set)) {
                    return false;
                }
            }
        }
        return start.isSatisfied(data);
    }

    public boolean stops(SeriesDataset data) {
        return stop.isNone() ? !start.isSatisfied(data) : stop.isSatisfied(data);
    }

    @Override
    public int hashCode() {
        return level.hashCode() ^ start.hashCode() ^ stop.hashCode() ^ suppress.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Circumstance && equalTo((Circumstance) obj);
    }

    public boolean equalTo(Circumstance other) {
        return level == other.level && start.equalTo(other.start) && stop.equalTo(other.stop)
                && suppress.equalTo(other.suppress) 
                && (suppressing == null && other.suppressing == null || suppressing != null && suppressing.equalTo(other.suppressing));
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(level.name()).append(": ");
        if (!start.isNone()) {
            str.append(start.toString());
        }
        if (!stop.isNone()) {
            str.append(" until ").append(stop.toString());
        }
        if (!suppress.isNone()) {
            str.append(" unless ").append(suppressing).append(' ').append(suppress.toString());
        }
        return str.toString();
    }
}
