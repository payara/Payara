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
public final class Alert implements Iterable<Alert.Frame> {

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

        public static Level parse(String level) {
            return valueOf(level.toUpperCase());
        }
    }

    public static final class Frame implements Iterable<SeriesDataset> {
        public final Level level;
        public final SeriesDataset cause;
        public final long start;
        private final List<SeriesDataset> captured;
        long end;

        public Frame(Level level, SeriesDataset cause, List<SeriesDataset> captured) {
            this.level = level;
            this.cause = cause;
            this.captured = captured;
            this.start = cause.lastTime();
        }

        @Override
        public Iterator<SeriesDataset> iterator() {
            return captured.iterator();
        }

        public long getEnd() {
            return end;
        }
    }

    /**
     * The maximal number of {@link Frame}s each {@link Alert} can have. When further frames are added old frames are
     * compacted away by cutting out a pair of amber-red or red-amber transitions while updating the end of the frame
     * before to connect to the frame after the cut out frames.
     * 
     * Frames are limited to avoid excessive use of memory and data transfer.
     */
    static final int MAX_FRAMES = 20;

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
    private final List<Frame> frames = new CopyOnWriteArrayList<>();
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
    public Iterator<Frame> iterator() {
        return frames.iterator();
    }

    public Alert addTransition(Level to, SeriesDataset cause, List<SeriesDataset> captured) {
        assertRedOrAmberLevel(to);
        if (!isStopped()) {
            if (!frames.isEmpty()) {
                Frame recent = getEndFrame();
                assertSameSeriesAndInstance(cause, recent.cause);
                recent.end = cause.lastTime();
                acknowledged = acknowledged && to.isLessSevereThan(recent.level);
            } else {
                assertMatchesWachtedSeries(cause);
                acknowledged = false;
            }
            frames.add(new Frame(to, cause, captured));
            CHANGE_COUNT.incrementAndGet();
            level = to;
            if (frames.size() > MAX_FRAMES) {
                compactFrames();
            }
        }
        return this;
    }

    /**
     * This is not synchronized since there is only one thread updating alerts so there should not be concurrent
     * changes.
     */
    private void compactFrames() {
        frames.get(0).end = frames.get(2).end;
        frames.remove(1);
        frames.remove(1); // which was 2 before
    }

    public boolean isStarted() {
        return !frames.isEmpty();
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
        return level.isLessSevereThan(Level.AMBER) && !frames.isEmpty();
    }

    public void stop(Level to, long now) {
        if (!isStopped()) {
            assertGreenOrWhiteLevel(to);
            this.level = to;
            getEndFrame().end = now;
            CHANGE_COUNT.incrementAndGet();
        }
    }

    public Level getLevel() {
        return level;
    }

    public long getStartTime() {
        return frames.isEmpty() ? -1L : frames.get(0).start;
    }

    public long getEndTime() {
        return frames.isEmpty() ? -1L : getEndFrame().end;
    }

    public Series getSeries() {
        return getEndFrame().cause.getSeries();
    }

    public String getInstance() {
        return getEndFrame().cause.getInstance();
    }

    public Frame getEndFrame() {
        return frames.get(frames.size() - 1);
    }

    public int getFrameCount() {
        return frames.size();
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
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) {
                str.append(" => ");
            }
            str.append(frames.get(i).level);
        }
        return str.toString();
    }

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_TIME;

    private static String formatTime(long epochMillis) {
        return TIME_FORMATTER.format(ofInstant(ofEpochMilli(epochMillis), systemDefault()));
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

    private static void assertGreenOrWhiteLevel(Level to) {
        if (to != Level.GREEN && to != Level.WHITE) {
            throw new IllegalArgumentException("Alerts only end on GREEN or WHITE levels but got: " + to);
        }
    }
}
