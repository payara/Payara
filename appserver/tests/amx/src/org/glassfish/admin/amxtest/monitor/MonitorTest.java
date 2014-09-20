/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.admin.amxtest.monitor;

import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.j2ee.statistics.BoundaryStatisticImpl;
import com.sun.appserv.management.j2ee.statistics.BoundedRangeStatisticImpl;
import com.sun.appserv.management.j2ee.statistics.CountStatisticImpl;
import com.sun.appserv.management.j2ee.statistics.RangeStatisticImpl;
import com.sun.appserv.management.j2ee.statistics.StatisticFactory;
import com.sun.appserv.management.j2ee.statistics.StatsImpl;
import com.sun.appserv.management.j2ee.statistics.StringStatistic;
import com.sun.appserv.management.j2ee.statistics.StringStatisticImpl;
import com.sun.appserv.management.j2ee.statistics.TimeStatisticImpl;
import com.sun.appserv.management.monitor.MonitoringStats;
import com.sun.appserv.management.util.j2ee.J2EEUtil;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.StringUtil;

import javax.management.ObjectName;
import org.glassfish.j2ee.statistics.BoundaryStatistic;
import org.glassfish.j2ee.statistics.BoundedRangeStatistic;
import org.glassfish.j2ee.statistics.CountStatistic;
import org.glassfish.j2ee.statistics.RangeStatistic;
import org.glassfish.j2ee.statistics.Statistic;
import org.glassfish.j2ee.statistics.Stats;
import org.glassfish.j2ee.statistics.TimeStatistic;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;


public final class MonitorTest
        extends AMXMonitorTestBase {
    public MonitorTest() {
    }


    public void
    checkStatisticNames(final MonitoringStats mon) {
        final Stats stats = mon.getStats();

        final Set<String> namesFromMon = GSetUtil.newStringSet(mon.getStatisticNames());
        final Set<String> namesFromStats = GSetUtil.newStringSet(stats.getStatisticNames());

        assert (namesFromStats.equals(namesFromMon)) :
                "statistic names from stats.getStatisticNames() != mon.getStatisticNames(): " +
                        namesFromStats + " != " + namesFromMon;
    }

    public void
    checkNumStatistics(final MonitoringStats mon) {
        final Stats stats = mon.getStats();
        assert (stats != null) : "null Stats from: " + Util.getObjectName(mon);
        final String[] allNames = mon.getStatisticNames();

        final Statistic[] statistics = mon.getStatistics(allNames);
        assert (statistics.length == allNames.length) :
                "wrong number of statistics from: " + Util.getObjectName(mon) +
                        ", got " + statistics.length + ", should be " + allNames.length;
    }

    public void
    checkOpenStats(final MonitoringStats mon) {
        final CompositeDataSupport openStats = mon.getOpenStats();
        assert (openStats != null) : "null OpenStats from: " + Util.getObjectName(mon);

        final StatsImpl stats = new StatsImpl(openStats);

        final Set<String> fromOpenStats = GSetUtil.newStringSet(stats.getStatisticNames());
        final Set<String> fromStats = GSetUtil.newStringSet(mon.getStats().getStatisticNames());
        assert (fromOpenStats.equals(fromStats)) :
                "openStats Statistic names don't match Stats Statistic names: " +
                        fromOpenStats + " != " + fromStats;
    }

    private final boolean
    isLegalStatistic(final Statistic s) {
        // current, we do not allow MapStatistic as these types cover all
        return (
                (s instanceof CountStatistic) ||
                        (s instanceof BoundaryStatistic) ||
                        (s instanceof RangeStatistic) ||
                        (s instanceof BoundedRangeStatistic) ||
                        (s instanceof TimeStatistic) ||
                        (s instanceof StringStatistic));
    }

    private final boolean
    isLegalStatisticImpl(final Statistic s) {
        boolean isLegal = isLegalStatistic(s);
        if (isLegal) {
            final Class theClass = s.getClass();

            if ((theClass == CountStatisticImpl.class) ||
                    (theClass == BoundaryStatisticImpl.class) ||
                    (theClass == RangeStatisticImpl.class) ||
                    (theClass == BoundedRangeStatisticImpl.class) ||
                    (theClass == TimeStatisticImpl.class) ||
                    (theClass == StringStatisticImpl.class)) {
                ;
            }
        }
        return (isLegal);
    }


    private void
    checkLegalStatistic(
            final ObjectName objectName,
            final Statistic s) {
        assert (isLegalStatistic(s)) : "Statistic " + s.getName() +
                " in \"" + objectName +
                "\" is not a known type of Statistic";

        assert (isLegalStatisticImpl(s)) : "Statistic " + s.getName() +
                " in \"" + objectName +
                "\" uses an implementation not intended by the API: " +
                s.getClass();
    }

    public void
    checkGetStatistic(final MonitoringStats mon) {
        final Stats stats = mon.getStats();

        final ObjectName objectName = Util.getObjectName(mon);

        final String[] names = mon.getStatisticNames();
        for (int i = 0; i < names.length; ++i) {
            final String name = names[i];
            final Statistic s = mon.getStatistic(name);
            assert (s != null);
            assert (s.getName().equals(name));

            checkLegalStatistic(objectName, s);
        }
    }

    public void
    checkGetStats(final MonitoringStats mon) {
        final Stats stats = mon.getStats();

        final ObjectName objectName = Util.getObjectName(mon);

        final String[] names = stats.getStatisticNames();
        for (int i = 0; i < names.length; ++i) {
            final Statistic s = stats.getStatistic(names[i]);
            assert (s != null);
            assert (s.getName().equals(names[i]));

            checkLegalStatistic(objectName, s);
        }
    }

    public void
    checkGetOpenStatistic(final MonitoringStats mon) {
        final Stats stats = mon.getStats();

        final String[] names = mon.getStatisticNames();
        for (int i = 0; i < names.length; ++i) {
            final String name = names[i];

            final CompositeData d = mon.getOpenStatistic(name);
            final Statistic s = StatisticFactory.create(d);
            final Statistic s2 = mon.getStatistic(name);

            assert (s.getName().equals(name));
            // values may have changed, but check the static fields
        }

        final CompositeDataSupport[] all = mon.getOpenStatistics(names);
        assert (all != null);
        assert (all.length == names.length);
    }

    public void
    checkMonitoringStats(final ObjectName objectName)
            throws Exception {
        final MonitoringStats mon = getProxyFactory().getProxy(objectName, MonitoringStats.class);

        mon.refresh();
        mon.getStatsInterfaceName();

        checkNumStatistics(mon);

        checkStatisticNames(mon);

        checkGetStatistic(mon);

        checkGetStats(mon);

        checkGetOpenStatistic(mon);

        checkOpenStats(mon);
    }

    private Set<MonitoringStats>
    getAllMonitoringStats()
            throws ClassNotFoundException {
        final long start = now();

        final Set<MonitoringStats> all =
                getQueryMgr().queryInterfaceSet(MonitoringStats.class.getName(), null);

        for (final MonitoringStats stats : all) {
        }

        printElapsed("getAllMonitoringStats", all.size(), start);

        return (all);
    }


    private String
    getterToName(final String getterName) {
        return StringUtil.stripPrefix(getterName, JMXUtil.GET);
    }

    private String[]
    methodsToNames(final Method[] methods) {
        final String[] result = new String[methods.length];

        for (int i = 0; i < methods.length; ++i) {
            result[i] = getterToName(methods[i].getName());
        }

        Arrays.sort(result);
        return (result);
    }

    public void
    checkStats(
            final MonitoringStats mon,
            final Stats stats)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final ObjectName objectName = Util.getObjectName(mon);

        trace("checkStats: " + objectName);

        final Method[] methodsViaNames = J2EEUtil.getStatisticGetterMethodsUsingNames(stats);

        final Method[] methods = stats.getClass().getMethods();

        final Set<String> statisticNames = GSetUtil.newSet(stats.getStatisticNames());

        for (int methodIdx = 0; methodIdx < methodsViaNames.length; ++methodIdx) {
            final Method method = methodsViaNames[methodIdx];
            final String methodName = method.getName();

            final Class<?> returnType = method.getReturnType();

            final String statisticName = getterToName(methodName);
            if (!statisticNames.contains(statisticName)) {
                warning("Statistic " + quote(statisticName) + " as derived from " + method +
                        " missing from " + quote(objectName) +
                        " available names = " + toString(statisticNames));
            }

            try {
                final Object o = method.invoke(stats, (Object[]) null);
                assert (o != null);
                assert (Statistic.class.isAssignableFrom(o.getClass()));

                assert (returnType.isAssignableFrom(o.getClass())) :
                        "Method " + methodName + " of MBean " + objectName +
                                " returned object not assignable to " + returnType.getName();

                final Statistic stat = (Statistic) method.invoke(stats, (Object[]) null);
                assert (method.getReturnType().isAssignableFrom(stat.getClass()));

                final Statistic s = mon.getStatistic(stat.getName());
                assert (returnType.isAssignableFrom(s.getClass())) :
                        "getStatistic() of MBean " + objectName +
                                " returned Statistic not assignable to " + returnType.getName();

                //printVerbose( "Verified " + stat.getClass().getName()  + " " + stat.getName() );
            }
            catch (Exception e) {
                final Throwable rootCause = ExceptionUtil.getRootCause(e);

                warning(
                        "Failure calling " + method + " on Stats for " + objectName + " = " +
                                rootCause.getClass().getName() + "\n" +
                                "Statistic names = " + toString(stats.getStatisticNames()));
            }
        }
    }

    public void
    checkAllStats(final ObjectName objectName)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        trace("checkAllStats: " + objectName);

        final MonitoringStats mon =
                getProxyFactory().getProxy(objectName, MonitoringStats.class);

        final Method[] methods = mon.getClass().getMethods();

        final Method specificStatsMethod = getSpecificStatsGetterMethod(mon);

        // the type of Stats returned from getStats() should be the same as the type
        // returned from the (only) specific getAbcStats()
        final Stats plainStats = mon.getStats();
        assert (specificStatsMethod.getReturnType().isAssignableFrom(plainStats.getClass())) :
                "Stats returned from " + objectName + " getStats() should be assignable to " +
                        specificStatsMethod.getReturnType().getName();
        checkStats(mon, plainStats);

        Stats stats = null;
        try {
            // verify that we can get it
            stats = (Stats) specificStatsMethod.invoke(mon, (Object[]) null);
        }
        catch (Exception e) {
            final Throwable rootCause = ExceptionUtil.getRootCause(e);

            failure(
                    "Failure calling " + specificStatsMethod.getName() + "() on " + objectName + " = " +
                            rootCause.getClass().getName());
        }

        assert (plainStats.getClass() == stats.getClass());
        checkStats(mon, stats);
    }

    /**
     Test the MonitoringStats interface.
     */
    public void
    testMonitoringStats()
            throws Exception {
        final long start = now();

        final Set<MonitoringStats> all = getAllMonitoringStats();

        testAll(Util.toObjectNames(all), "checkMonitoringStats");

        printElapsed("testMonitoringStats", all.size(), start);
    }

    public void
    xtestStats()
            throws Exception {
        trace("testStats: ");
        final long start = now();

        final Set<MonitoringStats> all = getAllMonitoringStats();
        assert (all.size() >= 10) : "Monitoring is not turned on";

        //final Set	all	= getQueryMgr().queryInterfaceSet( com.sun.appserv.management.monitor.HTTPServiceVirtualServerMonitor.class.getName(), null );

        testAll(Util.toObjectNames(all), "checkAllStats");

        printElapsed("testStats", all.size(), start);
    }


    /**
     Get the specific (non-generic) Stats getter. Example:
     getJVMStats() versus plain getStats().
     */
    public Method
    getSpecificStatsGetterMethod(final MonitoringStats mon)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        final Method[] methods = mon.getClass().getMethods();

        Method result = null;

        for (int methodIdx = 0; methodIdx < methods.length; ++methodIdx) {
            final Method method = methods[methodIdx];
            final String methodName = method.getName();

            if (JMXUtil.isGetter(method) && !methodName.equals("getStats") &&
                    Stats.class.isAssignableFrom(method.getReturnType()) &&
                    method.getParameterTypes().length == 0) {
                result = method;
                break;
            }
        }

        if (result == null) {
            throw new NoSuchMethodException("Can't find specific Stats getter in " +
                    quote(Util.getObjectName(mon)));
        }


        return (result);
    }


    public void
    checkStatsClassSuppliesAllStatistics(final ObjectName objectName)
            throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        //trace( "testStatsClassSuppliesAllStatistics: " + objectName);

        try {
            final MonitoringStats mon = getProxyFactory().getProxy(objectName, MonitoringStats.class);

            final Method m = getSpecificStatsGetterMethod(mon);
            final Stats stats = (Stats) m.invoke(mon, (Object[]) null);
            final Method[] methodsViaIntrospection = J2EEUtil.getStatisticGetterMethodsUsingIntrospection(stats);
            final Method[] methodsViaNames = J2EEUtil.getStatisticGetterMethodsUsingNames(stats);

            assert GSetUtil.newSet(methodsViaNames).equals(GSetUtil.newSet(methodsViaIntrospection)) :
                    "Statistic names for " + quote(objectName) +
                            " obtained via Statistic names do not match those obtained via introspection: \n" +
                            "via names:" + toString(methodsToNames(methodsViaNames)) +
                            "\nvia introspection: " + toString(methodsToNames(methodsViaIntrospection));

            final String[] namesFromMethods = methodsToNames(methodsViaNames);

            assert GSetUtil.newSet(namesFromMethods).equals(GSetUtil.newSet(stats.getStatisticNames())) :
                    "MBean " + quote(objectName) + " Stats object of class " + stats.getClass().getName() +
                            " has Statistic methods that don't match getStatisticNames() =>\n" +
                            toString(namesFromMethods) + " != " +
                            toString(stats.getStatisticNames());
        }
        catch (Exception e) {
            trace("Caught exception for " + StringUtil.quote(JMXUtil.toString(objectName)) +
                    " = " + e.getClass().getName() + ": " + StringUtil.quote(e.getMessage()) + "\n" +
                    ExceptionUtil.getStackTrace(ExceptionUtil.getRootCause(e)));
        }
    }

    /**
     Verify that the Stats class for each MonitoringStats supplies all Statistics
     found in itself, and that this matches those advertised by MonitoringStats.
     */
    public void
    testStatsClassSuppliesAllStatistics()
            throws Exception {
        //trace( "testStatsClassSuppliesAllStatistics: ");
        final long start = now();

        final Set<MonitoringStats> all = getAllMonitoringStats();

        testAll(Util.toObjectNames(all), "checkStatsClassSuppliesAllStatistics");

        printElapsed("testStatsClassSuppliesAllStatistics", all.size(), start );
	}
}









