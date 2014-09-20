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

package org.glassfish.admin.amxtest.ext.logging;

import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.ext.logging.LogModuleNames;
import com.sun.appserv.management.ext.logging.LogQueryResult;
import com.sun.appserv.management.ext.logging.Logging;
import static com.sun.appserv.management.ext.logging.Logging.LOWEST_SUPPORTED_QUERY_LEVEL;
import static com.sun.appserv.management.ext.logging.Logging.SERVER_KEY;
import com.sun.appserv.management.helper.LoggingHelper;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.Set;


/**
 Test the LoggingHelper.
 */
public final class LoggingHelperTest
        extends AMXTestBase {
    public LoggingHelperTest() {
    }

    final Set<Logging>
    getAllLogging() {
        return getQueryMgr().queryJ2EETypeSet(XTypes.LOGGING);
    }

    public LoggingHelper
    createHelper(final Logging logging) {
        return new LoggingHelper(logging);
    }


    private void
    validateResult(final LogQueryResult result) {
        assert (result != null);
        assert (result.getFieldNames() != null);
        assert (result.getEntries() != null);
    }

    public void
    testQueryServerLogSingle() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);
            final LogQueryResult result =
                    helper.queryServerLog(LOWEST_SUPPORTED_QUERY_LEVEL, "EJB");
            validateResult(result);
        }
    }


    public void
    testQueryServerLogLevelAndModules() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult result =
                    helper.queryServerLog(LOWEST_SUPPORTED_QUERY_LEVEL,
                                          LogModuleNames.ALL_NAMES);
            validateResult(result);
        }
    }


    public void
    testQueryServerLogLevel() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult result =
                    helper.queryServerLog(LOWEST_SUPPORTED_QUERY_LEVEL);
            validateResult(result);
        }
    }


    public void
    testQueryAllCurrent() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult result = helper.queryAllCurrent();
            validateResult(result);
        }
    }


    private static final int HOUR_MILLIS = 60 * 60 * 1000;

    public void
    testQueryServerLogRecent() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult result =
                    helper.queryServerLogRecent(HOUR_MILLIS);
            validateResult(result);
        }
    }


    public void
    testQueryServerLogRecentWithModules() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult result =
                    helper.queryServerLogRecent(
                            HOUR_MILLIS, LogModuleNames.ALL_NAMES);
            validateResult(result);
        }
    }


    public void
    testQueryAllInFile() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final String[] names = logging.getLogFileNames(SERVER_KEY);
            for (final String name : names) {
                final LogQueryResult result = helper.queryAllInFile(name);
                validateResult(result);
            }
        }
    }

    public void
    testQueryAll() {
        final Set<Logging> loggings = getAllLogging();

        for (final Logging logging : loggings) {
            final LoggingHelper helper = createHelper(logging);

            final LogQueryResult[] results = helper.queryAll();
            for (final LogQueryResult result : results) {
                validateResult(result);
            }
        }
    }


}





















