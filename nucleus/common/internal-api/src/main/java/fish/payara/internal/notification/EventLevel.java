/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2024] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.internal.notification;

import java.util.function.BiPredicate;
import java.util.logging.Level;

/**
 *
 */
public enum EventLevel {
    INFO(800),
    WARNING(900),
    SEVERE(1000);

    private final int severityLevel;

    public static EventLevel fromNameOrWarning (String name) {
        try {
            return EventLevel.valueOf(name.toUpperCase());
        } catch (Exception e) {
            return WARNING;
        }
    }

    public static EventLevel fromLogLevel (Level level) {
        if (level.intValue() <= INFO.severityLevel) {
            return INFO;
        }
        if (level.intValue() <= WARNING.severityLevel) {
            return WARNING;
        }
        return SEVERE;
    }

    EventLevel (int severityLevel) {
        this.severityLevel = severityLevel;
    }

    public int getSeverityLevel () {
        return severityLevel;
    }

    public boolean compare (EventLevel other, BiPredicate<Integer, Integer> predicate) {
        return predicate.test(this.severityLevel, other.severityLevel);
    }
}
