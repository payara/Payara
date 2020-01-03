package fish.payara.monitoring.alert;

import static fish.payara.monitoring.alert.Alert.Level.*;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.collect.MonitoringWatchCollector.WatchBuilder;
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
    private final Map<String, State> statesByInstanceSeries = new ConcurrentHashMap<>();
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public Watch(String name, Metric watched) {
        this(name, watched, Circumstance.NONE, Circumstance.NONE, Circumstance.NONE);
    }

    public Watch(String name, Metric watched, Circumstance red, Circumstance amber, Circumstance green, Metric... captured) {
        this.name = name;
        this.watched = watched;
        this.red = red;
        this.amber = amber;
        this.green = green;
        this.captured = captured;
    }

    @Override
    public Iterator<State> iterator() {
        return statesByInstanceSeries.values().iterator();
    }

    public void stop() {
        stopped.set(true);
        for (State s : statesByInstanceSeries.values()) {
            if (s.ongoing != null) {
                s.ongoing.stop(WHITE);
            }
        }
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public List<Alert> check(SeriesLookup lookup) {
        if (isStopped()) {
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
        State state = statesByInstanceSeries.computeIfAbsent(key(data), key -> new State(data));
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
                alert.stop(to);
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
        if (!red.isNone()) {
            str.append('\t').append(red).append('\n');
        }
        if (!amber.isNone()) {
            str.append('\t').append(amber).append('\n');
        }
        if (!green.isNone()) {
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
        case "red":
            return isEqual(red, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : new Watch(name, watched, //
                    create(RED, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage),
                    amber, green, captured);
        case "amber":
            return isEqual(amber, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : new Watch(name, watched, red, //
                    create(AMBER, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage),
                    green, captured);

        case "green":
            return isEqual(green, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, stopOnAverage)
                ? this
                : new Watch(name, watched, red, amber, //
                    create(GREEN, startThreshold, startForLast, startOnAverage, stopTheshold, stopForLast, startOnAverage),
                    captured);
        default:
            throw new IllegalArgumentException("No such alert level: " + level);
        }
    }

    @Override
    public Watch red(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with("red", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
    }

    @Override
    public Watch amber(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with("amber", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
    }

    @Override
    public Watch green(long startThreshold, Number startFor, boolean startOnAverage, Long stopTheshold,
            Number stopFor, boolean stopOnAverage) {
        return with("green", startThreshold, startFor, startOnAverage, stopTheshold, stopFor, stopOnAverage);
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
        return !sample.isNone()
                && sample.threshold == Math.abs(threshold.longValue())
                && Objects.equals(sample.forLast, forLast)
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

}
