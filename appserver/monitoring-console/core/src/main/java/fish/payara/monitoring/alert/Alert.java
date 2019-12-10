package fish.payara.monitoring.alert;

import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.ZoneId.systemDefault;

import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * An {@linkplain Alert} is raised when a watched series matches the {@link Circumstance}s for {@link Level#RED} or
 * {@link Level#AMBER} and last until the same {@link Series} and instance transitions to {@link Level#GREEN} or
 * {@link Level#WHITE}.
 * 
 * @see Watch
 * 
 * @author Jan Bernitt
 */
public final class Alert implements Iterable<Alert.Transition> {

    public enum Level {
        /**
         * Critical level
         */
        RED,
        /**
         * Elevated level to warn about
         */
        AMBER,
        /**
         * Within expected range
         */
        GREEN,
        /**
         * Not classified
         */
        WHITE;

        public boolean isLessSevereThan(Level other) {
            return ordinal() > other.ordinal();
        }
    }

    public static final class Transition implements Iterable<SeriesDataset> {
        public final Level to;
        public final SeriesDataset cause;
        public final long start;
        private final List<SeriesDataset> captured;
        long end;

        public Transition(Level to, SeriesDataset cause, List<SeriesDataset> captured) {
            this.to = to;
            this.cause = cause;
            this.captured = captured;
            this.start = System.currentTimeMillis();
        }

        @Override
        public Iterator<SeriesDataset> iterator() {
            return captured.iterator();
        }

        public long getEnd() {
            return end;
        }
    }

    private static final AtomicInteger NEXT_SERIAL = new AtomicInteger();
    /**
     * The change count tracks state changes of all {@link Alert} instances.
     */
    private static final AtomicInteger CHANGE_COUNT = new AtomicInteger();

    public static int getChangeCount() {
        return CHANGE_COUNT.get();
    }

    public final int serial;
    public final Watch initiator;
    private final List<Transition> transitions = new CopyOnWriteArrayList<>();
    /**
     * The current state of the alert.
     */
    private Level level = Level.WHITE;
    private boolean acknowledged;

    public Alert(Watch initiator) {
        this.initiator = initiator;
        this.serial = NEXT_SERIAL.incrementAndGet();
    }

    @Override
    public Iterator<Transition> iterator() {
        return transitions.iterator();
    }

    public Alert addTransition(Level to, SeriesDataset cause, List<SeriesDataset> captured) {
        assertRedOrAmberLevel(to);
        if (!isStopped()) {
            if (!transitions.isEmpty()) {
                Transition recent = recent();
                assertSameSeriesAndInstance(cause, recent.cause);
                recent.end = System.currentTimeMillis();
                acknowledged = acknowledged && to.isLessSevereThan(recent.to);
            } else {
                assertMatchesWachtedSeries(cause);
                acknowledged = false;
            }
            transitions.add(new Transition(to, cause, captured));
            CHANGE_COUNT.incrementAndGet();
            level = to;
        }
        return this;
    }

    private static void assertRedOrAmberLevel(Level to) {
        if (to != Level.RED && to != Level.AMBER) {
            throw new IllegalArgumentException("Alerts only transtion between RED and AMBER levels but got: " + to);
        }
    }

    private void assertMatchesWachtedSeries(SeriesDataset cause) {
        if (!initiator.watched.series.matches(cause.getSeries())) {
            throw new IllegalArgumentException("Cause did not match with watched series: " + cause.getSeries());
        }
    }

    private static void assertSameSeriesAndInstance(SeriesDataset a, SeriesDataset b) {
        if (!b.getSeries().equalTo(a.getSeries()) || !b.getInstance().equals(a.getInstance())) {
            throw new IllegalArgumentException(
                    "All transitions for an alert must refer to same cause series and instance but got: " + a);
        }
    }

    public boolean isStarted() {
        return !transitions.isEmpty();
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void acknowledge() {
        if (!isAcknowledged()) {
            acknowledged = true;
            CHANGE_COUNT.incrementAndGet();
        }
    }

    public boolean isStopped() {
        return level == Level.WHITE && !transitions.isEmpty();
    }

    public void stop() {
        if (!isStopped()) {
            level = Level.WHITE;
            recent().end = System.currentTimeMillis();
            CHANGE_COUNT.incrementAndGet();
        }
    }

    public Level getLevel() {
        return level;
    }

    public long getStartTime() {
        return transitions.isEmpty() ? -1L : transitions.get(0).start;
    }

    public long getEndTime() {
        return transitions.isEmpty() ? -1L : recent().end;
    }

    public Series getSeries() {
        return recent().cause.getSeries();
    }

    private Transition recent() {
        return transitions.get(transitions.size() - 1);
    }

    @Override
    public int hashCode() {
        return serial;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Alert && equalTo((Alert) obj);
    }

    public boolean equalTo(Alert other) {
        return serial == other.serial;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append('(').append(serial).append(") ").append(initiator.name);
        long startTime = getStartTime();
        str.append('[');
        if (startTime >= 0) {
            str.append(formatTime(startTime));
        }
        str.append('-');
        long endTime = getEndTime();
        if (endTime >= 0) {
            str.append(formatTime(endTime));
        }
        if (isAcknowledged()) {
            str.append(" ACK");
        }
        str.append(']');
        str.append(' ');
        for (int i = 0; i < transitions.size(); i++) {
            if (i > 0) {
                str.append(" => ");
            }
            str.append(transitions.get(i).to);
        }
        return str.toString();
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_TIME;

    private static String formatTime(long epochMillis) {
        return TIME_FORMATTER.format(ofInstant(ofEpochMilli(epochMillis), systemDefault()));
    }
}
