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

import static fish.payara.monitoring.alert.Alert.Level.*;
import static java.util.Collections.emptyList;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.collect.MonitoringWatchCollector.WatchBuilder;
import fish.payara.monitoring.collect.MonitoringWatchSource;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * A {@link Watch} uses descriptions of {@link Circumstance}s and their {@link Condition}s to determine a {@link Level}
 * the values of a particular {@link SeriesDataset}.
 * 
 * When the {@link Alert.Level} reaches {@link Level#AMBER} or {@link Level#RED} an {@link Alert} is created which ends
 * first when the same {@link SeriesDataset} reaches {@link Level#GREEN} or {@link Level#WHITE} again.
 * 
 * @see Alert
 * 
 * @author Jan Bernitt
 */
public final class Watch implements WatchBuilder, Iterable<Watch.State> {

    private static final String GREEN_PROPERTY = "green";
    private static final String AMBER_PROPERTY = "amber";
    private static final String RED_PROPERTY = "red";

    /**
     * A {@link Watch} is a state machine where the state of each {@link SeriesDataset} matching the
     * {@link Watch#watched} {@link Metric} is tracked individually with {@link Level} and potentially the ongoing
     * {@link Alert}.
     */
    public static final class State {

        final SeriesDataset watchingSince;
        volatile Level level = Level.WHITE;
        volatile Alert ongoing;

        State(SeriesDataset watched) {
            this.watchingSince = watched;
        }

        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            str.append(level).append(" ").append(getSeries()).append(" ").append(getInstance());
            if (ongoing != null) {
                str.append(ongoing.toString());
            }
            return str.toString();
        }

        public String getInstance() {
            return watchingSince.getInstance();
        }

        public Series getSeries() {
            return watchingSince.getSeries();
        }

        public Level getLevel() {
            return level;
        }
    }

    public final String name;
    public final Metric watched;
    public final Circumstance red;
    public final Circumstance amber;
    public final Circumstance green;
    private final Metric[] captured;
    private final Map<String, State> statesByInstanceSeries;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean disabled;
    private final boolean programmatic;

    public Watch(String name, Metric watched) {
        this(name, watched, false, Circumstance.UNSPECIFIED, Circumstance.UNSPECIFIED, Circumstance.UNSPECIFIED);
    }

    public Watch(String name, Metric watched, boolean programmatic, Circumstance red, Circumstance amber,
            Circumstance green, Metric... captured) {
        this(name, watched, programmatic, red, amber, green, captured, new AtomicBoolean(false),
                new ConcurrentHashMap<>());
    }

    private Watch(String name, Metric watched, boolean programmatic, Circumstance red, Circumstance amber,
            Circumstance green, Metric[] captured, AtomicBoolean disabled,
            Map<String, State> statesByInstanceSeries) {
        this.name = name;
        this.watched = watched;
        this.programmatic = programmatic;
        this.red = red;
        this.amber = amber;
        this.green = green;
        this.captured = captured;
        this.disabled = disabled;
        this.statesByInstanceSeries = statesByInstanceSeries;
    }

    @Override
    public Iterator<State> iterator() {
        return statesByInstanceSeries.values().iterator();
    }

    public State state(SeriesDataset data) {
        return statesByInstanceSeries.computeIfAbsent(key(data), key -> new State(data));
    }

    public void stop() {
        if (stopped.compareAndSet(false, true)) {
            stopAlertsOfThisWatch();
        }
    }

    private void stopAlertsOfThisWatch() {
        for (State s : statesByInstanceSeries.values()) {
            if (s.ongoing != null) {
                s.ongoing.stop(WHITE, (System.currentTimeMillis() / 1000L) * 1000L);
                s.ongoing = null;
                s.level = WHITE;
            }
        }
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isDisabled() {
        return disabled.get();
    }

    public void disable() {
        if (disabled.compareAndSet(false, true)) {
            stopAlertsOfThisWatch();
        }
    }

    public void enable() {
        disabled.set(false);
    }

    public boolean isProgrammatic() {
        return programmatic;
    }

    /**
     * Programmatic watches are collected from {@link MonitoringWatchSource}s or created during initialisation.
     * The programmatic flag can be used by UIs to determine if a watch should be possible to delete.
     * 
     * @return A new {@link Watch} instance that {@link #isProgrammatic()}. 
     */
    public Watch programmatic() {
        return new Watch(name, watched, true, red, amber, green, captured, disabled, statesByInstanceSeries);
    }

    public List<Alert> check(SeriesLookup lookup) {
        if (isStopped() || isDisabled()) {
            return emptyList();
        }
        List<Alert> raised = new ArrayList<>();
        for (SeriesDataset data : lookup.selectSeries(watched.series)) {
            Alert alert = check(lookup, data);
            if (alert != null) {
                raised.add(alert);
            }
        }
        return raised;
    }

    private static String key(SeriesDataset data) {
        return data.getSeries().toString() + '#' + data.getInstance();
    }

    private Alert check(SeriesLookup lookup, SeriesDataset data) {
        State state = state(data);
        switch (state.level) {
        default:
        case WHITE: return checkWhite(lookup, data, state);
        case GREEN: return checkGreen(lookup, data, state);
        case AMBER: return checkAmber(lookup, data, state);
        case RED:   return checkRed(lookup, data, state);
        }
    }

    private Alert checkWhite(SeriesLookup lookup, SeriesDataset data, State state) {
        if (red.starts(data, lookup)) {
            return transitionTo(RED, lookup, data, state);
        }
        if (amber.starts(data, lookup)) {
            return transitionTo(AMBER, lookup, data, state);
        }
        if (green.starts(data, lookup)) {
            return transitionTo(GREEN, lookup, data, state);
        }
        return null;
    }

    private Alert checkGreen(SeriesLookup lookup, SeriesDataset data, State state) {
        // green => red?
        if (red.starts(data, lookup)) {
            return transitionTo(RED, lookup, data, state);
        }
        // green => amber?
        if (amber.starts(data, lookup)) {
            return transitionTo(AMBER, lookup, data, state);
        }
        // continue green?
        if (!green.stops(data) || green.starts(data, lookup)) {
            return null;
        }
        return transitionTo(WHITE, lookup, data, state);
    }

    private Alert checkAmber(SeriesLookup lookup, SeriesDataset data, State state) {
        // amber => red?
        if (red.starts(data, lookup)) {
            return transitionTo(RED, lookup, data, state);
        }
        // continue amber?
        if (!amber.stops(data) || amber.starts(data, lookup)) {
            return null; // continue
        }
        // amber => green?
        if (green.starts(data, lookup)) {
            return transitionTo(GREEN, lookup, data, state);
        }
        return transitionTo(WHITE, lookup, data, state);
    }

    private Alert checkRed(SeriesLookup lookup, SeriesDataset data, State state) {
        // continue red?
        if (!red.stops(data) || red.starts(data, lookup)) {
            return null; // continue
        }
        // red => amber?
        if (amber.starts(data, lookup)) {
            return transitionTo(AMBER, lookup, data, state);
        }
        // red => green?
        if (green.starts(data, lookup)) {
            return transitionTo(GREEN, lookup, data, state);
        }
        return transitionTo(WHITE, lookup, data, state);
    }

    private Alert transitionTo(Level to, SeriesLookup lookup, SeriesDataset data, State state) {
        state.level = to;
        Alert alert = state.ongoing;
        if (to == WHITE || to == GREEN) {
            if (alert != null) {
                alert.stop(to, data.lastTime());
            }
            state.ongoing = null;
            return null;
        }
        Alert raised = null;
        if (alert == null) {
            raised = new Alert(this);
            state.ongoing = raised;
        }
        state.ongoing.addTransition(to, data, capturedData(lookup, data));
        return raised;
    }

    private List<SeriesDataset> capturedData(SeriesLookup lookup, SeriesDataset data) {
        List<SeriesDataset> res = new ArrayList<>();
        for (int i = 0; i < captured.length; i++) {
            res.addAll(lookup.selectSeries(captured[i].series, data.getInstance()));
        }
        return res;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Watch && equalTo((Watch) obj);
    }

    public boolean equalTo(Watch other) {
        return name.equals(other.name);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(name).append(" ~ ").append(watched).append('\n');
        if (!red.isUnspecified()) {
            str.append('\t').append(red).append('\n');
        }
        if (!amber.isUnspecified()) {
            str.append('\t').append(amber).append('\n');
        }
        if (!green.isUnspecified()) {
            str.append('\t').append(green).append('\n');
        }
        if (captured.length > 0) {
            str.append('\t').append(Arrays.toString(captured)).append('\n');
        }
        str.append("State:\n");
        for (State s : statesByInstanceSeries.values()) {
            str.append("\t\t").append(s).append('\n');
        }
        return str.toString();
    }

    @Override
    public Watch with(String level, long startThreshold, Number startForLast, boolean startOnAverage, Long stopTheshold,
            Number stopForLast, boolean stopOnAverage) {
        switch (level) {
        case RED_PROPERTY:
            return isEqual(red, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : with(
                    create(RED, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage),
                    amber, green);
        case AMBER_PROPERTY:
            return isEqual(amber, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : with(red,
                    create(AMBER, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage),
                    green);

        case GREEN_PROPERTY:
            return isEqual(green, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : with(red, amber, //
                    create(GREEN, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, startOnAverage));
        default:
            throw new IllegalArgumentException("No such alert level: " + level);
        }
    }

    private Watch with(Circumstance red, Circumstance amber, Circumstance green) {
        return new Watch(name, watched, programmatic, red, amber, green, captured, disabled, statesByInstanceSeries);
    }

    @Override
    public Watch red(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with(RED_PROPERTY, startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
    }

    @Override
    public Watch amber(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with(AMBER_PROPERTY, startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
    }

    @Override
    public Watch green(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with(GREEN_PROPERTY, startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
    }

    private static boolean isEqual(Circumstance sample, long startThreshold, Number startForLast,
            boolean startOnAverage, Long stopTheshold, Number stopForLast, boolean stopOnAverage) {
        return isEqual(sample.start, startThreshold, startForLast, startOnAverage)
                && isEqual(sample.stop, stopTheshold, stopForLast, stopOnAverage);
    }

    private static boolean isEqual(Condition sample, Long threshold, Number forLast, boolean onAverage) {
        if (threshold == null) {
            return sample.isNone();
        }
        if (!onAverage && forLast instanceof Integer && forLast.intValue() == 1) {
            forLast = null;
        }
        return !sample.isNone()
                && sample.threshold == Math.abs(threshold.longValue())
                && (Objects.equals(sample.forLast, forLast))
                && sample.onAverage == onAverage;
    }

    private static Circumstance create(Level level, long startThreshold, Number startForLast,
            boolean startOnAverage, Long stopTheshold, Number stopForLast, boolean stopOnAverage) {
        Operator startComparison = startThreshold < 0 
                ? level == GREEN ? Operator.LE : Operator.LT
                : level == GREEN ? Operator.GE : Operator.GT;
        Operator stopOperator = stopTheshold != null && stopTheshold < 0
                ? Operator.GT
                : Operator.LT;
        Condition start = new Condition(startComparison, Math.abs(startThreshold), startForLast, startOnAverage);
        Condition stop = stopTheshold == null
                ? Condition.NONE
                : new Condition(stopOperator, Math.abs(stopTheshold), stopForLast, stopOnAverage);
        return new Circumstance(level, start, stop);
    }

    public JsonObject toJSON() {
        JsonArrayBuilder capturedArray = Json.createArrayBuilder();
        for (Metric m : captured) {
            capturedArray.add(m.toJSON());
        }
        return Json.createObjectBuilder()
                .add("name", name)
                .add("watched", watched.toJSON())
                .add("programmatic", programmatic)
                .add("disabled", isDisabled())
                .add("stopped", isStopped())
                .add(RED_PROPERTY, red.toJSON())
                .add(AMBER_PROPERTY, amber.toJSON())
                .add(GREEN_PROPERTY, green.toJSON())
                .add("captured", capturedArray.build())
                .build();
    }

    public static Watch fromJSON(String json) {
        JsonValue value = null;
        try (JsonParser parser = Json.createParser(new StringReader(json))) {
            if (!parser.hasNext()) {
                return null;
            }
            parser.next();
            value = parser.getValue();
        }
        if (value == null || value == JsonValue.NULL) {
            return null;
        }
        JsonObject obj = value.asJsonObject(); 
        JsonArray array = obj.getJsonArray("captured");
        Metric[] captured = array == null 
                ? new Metric[0]
                : array.stream().map(Metric::fromJSON).toArray(Metric[]::new);
        Watch out = new Watch(obj.getString("name"), 
                Metric.fromJSON(obj.get("watched")), 
                obj.getBoolean("programmatic", false), 
                Circumstance.fromJson(obj.get(RED_PROPERTY)), 
                Circumstance.fromJson(obj.get(AMBER_PROPERTY)), 
                Circumstance.fromJson(obj.get(GREEN_PROPERTY)), 
                captured);
        if (obj.getBoolean("disabled", false)) {
            out.disable();
        }
        if (obj.getBoolean("stopped", false)) {
            out.stop();
        }
        return out;
    }

}
