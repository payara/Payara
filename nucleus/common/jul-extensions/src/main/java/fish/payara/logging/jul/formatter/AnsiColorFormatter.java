/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.logging.jul.formatter;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * {@link PayaraLogFormatter} which is able to print colored logs.
 *
 * @since 4.1.1.173
 * @author Steve Millidge (Payara Foundation)
 * @author David Matejcek
 */
public abstract class AnsiColorFormatter extends PayaraLogFormatter {

    private AnsiColor loggerColor = AnsiColor.BOLD_INTENSE_BLUE;
    private ColorMap colors = new ColorMap();
    private boolean ansiColor;

    public AnsiColorFormatter(final HandlerId handlerId) {
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
        configure(this, FormatterConfigurationHelper.forHandlerId(handlerId));
    }


    public AnsiColorFormatter() {
        configure(this, FormatterConfigurationHelper.forFormatterClass(getClass()));
    }


    private static void configure(final AnsiColorFormatter formatter, final FormatterConfigurationHelper helper) {
        formatter.ansiColor = helper.getBoolean("ansiColor", false);
        formatter.loggerColor = helper.getAnsiColor("loggerColor", formatter.loggerColor);
        formatter.colors.overwriteIfNotNull(Level.INFO, helper.getAnsiColor("infoColor", null));
        formatter.colors.overwriteIfNotNull(Level.WARNING, helper.getAnsiColor("warnColor", null));
        formatter.colors.overwriteIfNotNull(Level.SEVERE, helper.getAnsiColor("severeColor", null));
    }


    /**
     * Enables/disables ANSI coloring in logs
     *
     * @param ansiColor true to enable
     */
    public void setAnsiColor(final boolean ansiColor) {
        this.ansiColor = ansiColor;
    }


    /**
     * @return true if ANSI coloring is enabled (default: true)
     */
    protected boolean isAnsiColor() {
        return ansiColor;
    }


    /**
     * @param loggerColor {@link AnsiColor} used for the logger name.
     */
    public void setLoggerColor(final AnsiColor loggerColor) {
        this.loggerColor = loggerColor;
    }


    /**
     * @return {@link AnsiColor} for the logger name value
     */
    public AnsiColor getLoggerColor() {
        return loggerColor;
    }


    /**
     * @param mapping colors used for log levels
     */
    public void setLevelColors(final Map<Level, AnsiColor> mapping) {
        this.colors = new ColorMap(mapping);
    }


    /**
     * @param level
     * @return {@link AnsiColor} for the level value or null if {@link #isAnsiColor()} returns
     *         false.
     */
    protected AnsiColor getLevelColor(final Level level) {
        if (!isAnsiColor()) {
            return null;
        }
        return colors.get(level);
    }

    private static class ColorMap extends HashMap<Level, AnsiColor> {

        private static final long serialVersionUID = 1L;

        ColorMap() {
            put(Level.INFO, AnsiColor.BOLD_INTENSE_GREEN);
            put(Level.WARNING, AnsiColor.BOLD_INTENSE_YELLOW);
            put(Level.SEVERE, AnsiColor.BOLD_INTENSE_RED);
        }


        ColorMap(final Map<Level, AnsiColor> originalMap) {
            super(originalMap);
        }


        public void overwriteIfNotNull(final Level key, final AnsiColor color) {
            if (color != null) {
                put(key, color);
            }
        }
    }
}
