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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * The {@link Circumstance} in context of {@link Alert}s describes under what {@link Condition}s a certain
 * {@link Alert.Level} {@link #starts(SeriesDataset, SeriesLookup)} and {@link #stops(SeriesDataset)}.
 *
 * @author Jan Bernitt
 */
public final class Circumstance {

    /**
     * A "null" value object to use for a {@link Circumstance} that does not exist.
     */
    public static final Circumstance UNSPECIFIED = new Circumstance(Level.WHITE, Condition.NONE, Condition.NONE);

    public final Level level;
    public final Condition start;
    public final Condition stop;
    public final Condition suppress;
    public final Metric suppressing;

    public Circumstance(Level level, Condition start) {
        this(level, start, Condition.NONE);
    }

    public Circumstance(Level level, Condition start, Condition stop) {
        this(level, start, stop, null, Condition.NONE);
    }

    private Circumstance(Level level, Condition start, Condition stop, Metric suppressing, Condition suppress) {
        this.level = level;
        this.start = start;
        this.stop = stop;
        this.suppressing = suppressing;
        this.suppress = suppress;
        ensureOnlyNonExistentHasWhiteLevel();
    }

    private  void ensureOnlyNonExistentHasWhiteLevel() {
        if  (level == Level.WHITE && (!start.isNone() || !stop.isNone() || !suppress.isNone())) {
            throw new IllegalArgumentException("Only NON_EXISTENT can have WHITE level");
        }
    }

    public Circumstance suppressedWhen(Metric suppressing, Condition suppress) {
        return new Circumstance(level, start, stop, suppressing, suppress);
    }

    public boolean isUnspecified() {
        return level == Level.WHITE;
    }

    public boolean starts(SeriesDataset data, SeriesLookup lookup) {
        if (isUnspecified()) {
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
        if (!suppress.isNone()) {
            str.append(" unless ").append(suppressing.series).append(' ').append(suppress.toString());
        }
        if (!stop.isNone()) {
            str.append(" until ").append(stop.toString());
        }
        return str.toString();
    }

    public JsonValue toJSON() {
        if (isUnspecified()) {
            return JsonValue.NULL;
        }
        return Json.createObjectBuilder()
                .add("level", level.name().toLowerCase())
                .add("start", start.toJSON())
                .add("stop", stop.toJSON())
                .add("suppress", suppress.toJSON())
                .add("suppressing", suppressing == null ? JsonValue.NULL : suppressing.toJSON())
                .build();
    }

    public static Circumstance fromJson(JsonValue value) {
        if (value == JsonValue.NULL || value == null) {
            return Circumstance.UNSPECIFIED;
        }
        JsonObject obj = value.asJsonObject();
        return new Circumstance(
                Level.parse(obj.getString("level")), 
                Condition.fromJSON(obj.get("start")),
                Condition.fromJSON(obj.get("stop")),
                Metric.fromJSON(obj.get("suppressing")),
                Condition.fromJSON(obj.get("suppress")));
    }
}
