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

import static java.lang.Math.abs;
import static java.lang.Math.min;

import java.util.NoSuchElementException;
import java.util.Objects;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import fish.payara.monitoring.model.SeriesDataset;

/**
 * Describes when a {@link SeriesDataset} {@link #isSatisfied(SeriesDataset)}.
 * 
 * In the simplest form this is a plain comparison with a constant {@link #threshold} value.
 * 
 * More advanced {@link Condition}s check if the condition is satisfied for last number of values in the dataset or for
 * a past number of milliseconds. Such checks either check each included value of the dataset against the threshold
 * (ALL) or compare their average against the threshold in a single check for any number of included values.
 * 
 * @author Jan Berntitt
 */
public final class Condition {

    private static final String FOR_TIMES = "forTimes";
    private static final String FOR_MILLIS = "forMillis";

    public static final Condition NONE = new Condition(Operator.EQ, 0L);

    public enum Operator {
        LT("<"), LE("<="), EQ("="), GT(">"), GE(">=");

        public String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }

        public static Operator parse(String symbol) {
            for (Operator op : values()) {
                if (op.symbol.equals(symbol) ) {
                    return op;
                }
            }
            throw new NoSuchElementException("Operator with symbol does not exist: " + symbol);
        }
    }

    public final Operator comparison;
    public final long threshold;
    public final Number forLast;
    public final boolean onAverage;

    public Condition(Operator comparison, long threshold) {
        this(comparison, threshold, null, false);
    }

    public Condition(Operator comparison, long threshold, Number forLast, boolean onAverage) {
        this.comparison = comparison;
        this.threshold = threshold;
        this.forLast = !onAverage && forLast instanceof Integer && forLast.intValue() == 1 ? null : forLast;
        this.onAverage = onAverage && forLast != null && forLast.longValue() > 0L;
    }

    public Condition forLastMillis(long millis) {
        return new Condition(comparison, threshold, millis, false);
    }

    public Condition forLastTimes(int times) {
        return new Condition(comparison, threshold, times, false);
    }

    public Condition onAverage() {
        return new Condition(comparison, threshold, forLast, true);
    }

    public boolean isNone() {
        return this == NONE;
    }

    public boolean isForLastPresent() {
        return isForLastMillis() || isForLastTimes();
    }

    public boolean isForLastMillis() {
        return forLast instanceof Long;
    }

    public boolean isForLastTimes() {
        return forLast instanceof Integer;
    }

    public boolean isSatisfied(SeriesDataset data) {
        if (isNone()) {
            return true;
        }
        if (data.getObservedValues() == 0) {
            return false;
        }
        long value = data.lastValue();
        if (isForLastMillis()) {
            return isSatisfiedForLastMillis(data);
        }
        if (isForLastTimes()) {
            return isSatisfiedForLastTimes(data);
        }
        return compare(value);
    }

    private boolean isSatisfiedForLastMillis(SeriesDataset data) {
        long forLastMillis = abs(forLast.longValue());
        if (forLastMillis == 0) {
            return isSatisfiedForLastTimes(data.points(), 0);
        }
        boolean all = forLast.longValue() > 0;
        if (data.isStable() && all) {
            return data.getStableSince() <= data.lastTime() - forLastMillis && compare(data.lastValue());
        }
        long startTime = data.lastTime() - forLastMillis;
        long[] points = data.points();
        if (points[0] > startTime && forLastMillis < 30000L && all) {
            return false; // not enough data
        }
        int index = points.length - 2; // last time index
        while (index >= 0 && points[index] > startTime) {
            index -= 2;
        }
        int forLastTimes = index <= 0 ? points.length / 2 : (points.length - index) / 2;
        return isSatisfiedForLastTimes(points, (all ? 1 : -1) * forLastTimes);
    }

    private boolean isSatisfiedForLastTimes(SeriesDataset data) {
        int forLastTimes = forLast.intValue();
        if (data.isStable() && forLastTimes > 0) {
            return data.getStableCount() >= abs(forLastTimes) && compare(data.lastValue());
        }
        return isSatisfiedForLastTimes(data.points(), forLastTimes);
    }

    private boolean isSatisfiedForLastTimes(long[] points, int forLastTimes) {
        int maxPoints = points.length / 2;
        int n = forLastTimes == 0 ? maxPoints : min(maxPoints, abs(forLastTimes));
        if (forLastTimes > 0 && n < forLastTimes && n < 30) {
            return false; // not enough data yet
        }
        if (onAverage) {
            return avgSatisfiedInLastN(points, n);
        }
        if (forLastTimes <= 0) {
            return anySatisfiedInLastN(points, n);
        }
        return allSatisfiedInLastN(points, n);
    }

    private boolean avgSatisfiedInLastN(long[] points, int n) {
        int index = points.length - 1; // last value index
        long sum = 0;
        for (int i = 0; i < n; i++) {
            sum += points[index];
            index -= 2;
        }
        return compare(sum / n);
    }

    private boolean anySatisfiedInLastN(long[] points, int n) {
        int index = points.length - 1; // last value index
        for (int i = 0; i < n; i++) {
            if (compare(points[index])) {
                return true;
            }
            index -= 2;
        }
        return false;
    }

    private boolean allSatisfiedInLastN(long[] points, int n) {
        int index = points.length - 1; // last value index
        for (int i = 0; i < n; i++) {
            if (!compare(points[index])) {
                return false;
            }
            index -= 2;
        }
        return true;
    }

    private boolean compare(long value) {
        switch (comparison) {
        default:
        case EQ: return value == threshold;
        case LE: return value <= threshold;
        case LT: return value < threshold;
        case GE: return value >= threshold;
        case GT: return value > threshold;
        }
    }

    @Override
    public int hashCode() {
        return (int) (comparison.hashCode() ^ threshold ^ (forLast == null ? 0 : forLast.intValue())); // good enough to avoid most collisions
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Condition && equalTo((Condition) obj);
    }

    public boolean equalTo(Condition other) {
        return comparison == other.comparison && threshold == other.threshold
                && Objects.equals(forLast, other.forLast) && onAverage == other.onAverage;
    }

    @Override
    public String toString() {
        if (isNone()) {
            return "";
        }
        StringBuilder str = new StringBuilder();
        boolean any = isForLastPresent() && forLast.intValue() == 0;
        boolean anyN = isForLastPresent() && forLast.intValue() < 0;
        str.append("value ").append(comparison.toString()).append(' ').append(threshold);
        if (isForLastPresent()) {
            if (onAverage) {
                str.append(" for average of last ");
            } else if (anyN) {
                str.append(" in last ");
            } else if (any) {
                str.append(" in sample");
            } else {
                str.append(" for last ");
            }
        }
        if (isForLastTimes() && !any) {
            str.append(abs(forLast.intValue())).append('x');
        }
        if (isForLastMillis()) {
            str.append(abs(forLast.longValue())).append("ms");
        }
        return str.toString();
    }

    public JsonValue toJSON() {
        if (isNone()) {
            return JsonValue.NULL;
        }
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("comparison", comparison.symbol)
                .add("threshold", threshold)
                .add("onAverage", onAverage);
         if (isForLastMillis()) {
             builder.add(FOR_MILLIS, forLast.longValue());
         }
         if (isForLastTimes()) {
             builder.add(FOR_TIMES, forLast.intValue());
         }
         return builder.build();
    }

    public static Condition fromJSON(JsonValue value) {
        if (value == null || value == JsonValue.NULL) {
            return Condition.NONE;
        }
        JsonObject obj = value.asJsonObject();
        Number forLast = null;
        if (obj.containsKey(FOR_MILLIS)) {
            forLast = obj.getJsonNumber(FOR_MILLIS).longValue();
        }
        if (obj.containsKey(FOR_TIMES)) {
            forLast = obj.getInt(FOR_TIMES);
        }
        return new Condition(
                Operator.parse(obj.getString("comparison", ">")), 
                obj.getJsonNumber("threshold").longValue(),
                forLast,
                obj.getBoolean("onAverage", false));
    }
}
