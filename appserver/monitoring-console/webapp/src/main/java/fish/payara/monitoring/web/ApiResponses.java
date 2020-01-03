/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.web;

import static java.util.stream.Collectors.toList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fish.payara.monitoring.alert.Watch;
import fish.payara.monitoring.alert.Alert;
import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.AlertService.AlertStatistics;
import fish.payara.monitoring.alert.Circumstance;
import fish.payara.monitoring.alert.Condition;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.web.ApiRequests.SeriesQuery;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.RequestTracingService;

/**
 * Types used in the web API to model mapped responses.
 * 
 * The purpose of these classes is to decouple the API from internal server classes so that renaming or restructuring of
 * internal classes does not break the API.
 * 
 * @see ApiRequests
 * 
 * @author Jan Bernitt
 */
public final class ApiResponses {

    /**
     * A {@link SeriesResponse} is the answer to a {@link SeriesRequest}.
     * 
     * It consists of an {@link Alerts} statistic and a {@link SeriesMatch} for each {@link SeriesQuery}.
     */
    public static final class SeriesResponse {

        public final Alerts alerts;
        public final List<SeriesMatch> matches;

        public SeriesResponse(SeriesQuery[] queries, List<List<SeriesDataset>> data, List<Collection<Watch>> watches,
                List<Collection<Alert>> alerts, AlertStatistics alertStatistics) {
            this.alerts = new Alerts(alertStatistics);
            this.matches = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                matches.add(new SeriesMatch(queries[i], data.get(i), watches.get(i), alerts.get(i)));
            }
        }
    }

    public static final class Alerts {

        public final int changeCount;
        public final int unacknowledgedRedAlerts;
        public final int acknowledgedRedAlerts;
        public final int unacknowledgedAmberAlerts;
        public final int acknowledgedAmberAlerts;

        public Alerts(AlertStatistics stats) {
            this.changeCount = stats.changeCount;
            this.unacknowledgedRedAlerts = stats.unacknowledgedRedAlerts;
            this.acknowledgedRedAlerts = stats.acknowledgedRedAlerts;
            this.unacknowledgedAmberAlerts = stats.unacknowledgedAmberAlerts;
            this.acknowledgedAmberAlerts = stats.acknowledgedAmberAlerts;
        }
    }

    /**
     * Corresponding answer to a {@link SeriesQuery}.
     */
    public static final class SeriesMatch {

        public final List<SeriesData> data;
        public final List<WatchData> watches;
        public final List<AlertData> alerts;

        public SeriesMatch(SeriesQuery query, List<SeriesDataset> data, Collection<Watch> watches, Collection<Alert> alerts) {
            this.alerts = alerts.stream().map(alert -> new AlertData(alert, query.truncateAlerts)).collect(toList());
            this.watches = watches.stream().map(WatchData::new).collect(toList());
            this.data = data.stream().map(set -> new SeriesData(set, query.truncatePoints)).collect(toList());
        }
    }

    public static final class WatchData {

        public final String name;
        public final String series;
        public final String unit;
        public final CircumstanceData red;
        public final CircumstanceData amber;
        public final CircumstanceData green;
        public final Map<String, Map<String, String>> states = new HashMap<>();

        public WatchData(Watch watch) {
            this.name = watch.name;
            this.series = watch.watched.series.toString();
            this.unit = watch.watched.unit.toString();
            this.red = watch.red.isNone() ? null : new CircumstanceData(watch.red);
            this.amber = watch.amber.isNone() ? null : new CircumstanceData(watch.amber);
            this.green = watch.green.isNone() ? null : new CircumstanceData(watch.green);
            for (Watch.State state : watch) {
                states.computeIfAbsent(state.getSeries().toString(), key -> new HashMap<>())
                    .put(state.getInstance(), state.getLevel().name().toLowerCase());
            }
        }
    }

    public static final class CircumstanceData {

        public final String level;
        public final ConditionData start;
        public final ConditionData stop;
        public final ConditionData suppress;
        public final String surpressingSeries;
        public final String surpressingUnit;

        public CircumstanceData(Circumstance circumstance) {
            this.level = circumstance.level.name().toLowerCase();
            this.start = circumstance.start.isNone() ? null : new ConditionData(circumstance.start);
            this.stop = circumstance.stop.isNone() ? null : new ConditionData(circumstance.stop);
            this.suppress = circumstance.suppress.isNone() ? null : new ConditionData(circumstance.suppress);
            this.surpressingSeries = circumstance.suppressing == null ? null : circumstance.suppressing.series.toString();
            this.surpressingUnit = circumstance.suppressing == null ? null : circumstance.suppressing.unit.toString();
        }
    }

    public static final class ConditionData {

        public final String operator;
        public final long threshold;
        public final Integer forTimes;
        public final Long forMillis;
        public final boolean onAverage;

        public ConditionData(Condition condition) {
            this.operator = condition.comparison.toString();
            this.threshold = condition.threshold;
            this.forTimes = condition.isForLastTimes() ? condition.forLast.intValue() : null;
            this.forMillis = condition.isForLastMillis() ? condition.forLast.longValue() : null;
            this.onAverage = condition.onAverage;
        }
    }

    public static final class SeriesData {

        public final String series;
        public final String instance;
        public final long[] points;
        public final long observedMax;
        public final long observedMin;
        public final BigInteger observedSum;
        public final int observedValues;
        public final int observedValueChanges;
        public final long observedSince;
        public final int stableCount;
        public final long stableSince;

        public SeriesData(SeriesDataset set) {
            this(set, false);
        }

        public SeriesData(SeriesDataset set, boolean truncatePoints) {
            this.instance = set.getInstance();
            this.series = set.getSeries().toString(); 
            this.points = truncatePoints ? new long[] {set.lastTime(), set.lastValue()} : set.points();
            this.observedMax = set.getObservedMax();
            this.observedMin = set.getObservedMin();
            this.observedSum = set.getObservedSum();
            this.observedValues = set.getObservedValues();
            this.observedValueChanges = set.getObservedValueChanges();
            this.observedSince = set.getObservedSince();
            this.stableCount = set.getStableCount();
            this.stableSince = set.getStableSince();
        }
    }

    public static final class RequestTraceResponse {

        public final UUID id;
        public final long startTime;
        public final long endTime;
        public final long elapsedTime; 
        public final List<RequestTraceSpan> spans = new ArrayList<>();

        public RequestTraceResponse(RequestTrace trace) {
            this.id = trace.getTraceId();
            this.startTime = trace.getStartTime().toEpochMilli();
            this.endTime = trace.getEndTime().toEpochMilli();
            this.elapsedTime = trace.getElapsedTime();
            for (fish.payara.notification.requesttracing.RequestTraceSpan span : trace.getTraceSpans()) {
                this.spans.add(new RequestTraceSpan(span));
            }
        }
    }

    public static final class RequestTraceSpan {

        public final UUID id;
        public final String operation;
        public final long startTime;
        public final long endTime;
        public final long duration;
        public final Map<String, String> tags;

        public RequestTraceSpan(fish.payara.notification.requesttracing.RequestTraceSpan span) {
            this.id = span.getId();
            this.operation = RequestTracingService.stripPackageName(span.getEventName());
            this.startTime = span.getTimeOccured();
            this.endTime = span.getTraceEndTime().toEpochMilli();
            this.duration = span.getSpanDuration();
            this.tags = span.getSpanTags();
        }
    }

    public static final class AlertsResponse {

        public final List<AlertData> alerts;

        public AlertsResponse(Collection<Alert> alerts) {
            this.alerts = alerts.stream().map(AlertData::new).collect(toList());
        }

    }

    public static final class AlertData {

        public final int serial;
        public final String level;
        public final String series;
        public final String instance;
        public final WatchData initiator;
        public final boolean acknowledged;
        public final boolean stopped;
        public final List<AlertFrame> frames;

        public AlertData(Alert alert) {
            this(alert, false);
        }

        public AlertData(Alert alert, boolean truncateAlerts) {
            this.serial = alert.serial;
            this.level = alert.getLevel().name().toLowerCase();
            this.series = alert.getSeries().toString();
            this.instance = alert.getInstance();
            this.initiator = new WatchData(alert.initiator);
            this.acknowledged = alert.isAcknowledged();
            this.stopped = alert.isStopped();
            this.frames = new ArrayList<>();
            if (truncateAlerts) {
                this.frames.add(new AlertFrame(alert.getEndFrame()));
            } else {
                for (Alert.Frame t : alert) {
                    this.frames.add(new AlertFrame(t));
                }
            }
        }
    }

    /**
     * Each time an {@link Alert} transitions between {@link Level#RED} and {@link Level#AMBER} a new frame is created
     * capturing the {@link AlertFrame#cause} of the transition as well as the {@link AlertFrame#captured} metrics.
     */
    public static final class AlertFrame {

        public final String level;
        public final SeriesData cause;
        public final List<SeriesData> captured;
        public final long start;
        public final Long end;

        public AlertFrame(Alert.Frame frame) {
            this.level = frame.level.name().toLowerCase();
            this.cause = new SeriesData(frame.cause, true); // for now the points data isn't used, change to false if needed
            this.start = frame.start;
            this.end = frame.getEnd() <= 0 ? null : frame.getEnd();
            this.captured = new ArrayList<>();
            for (SeriesDataset capture : frame) {
                this.captured.add(new SeriesData(capture, true)); // for now the points data isn't used, change to false if needed
            }
        }
    }

    public static final class WatchesResponse {
        public final List<WatchData> watches;

        public WatchesResponse(Collection<Watch> watches) {
            this.watches = watches.stream().map(WatchData::new).collect(toList());
        }
    }
}
