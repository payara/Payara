/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ai.agent.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Holds the domain objects produced during a single workflow run.
 * <p>
 * The trigger event and the return values of each phase are recorded here in
 * production order, so that later phases can receive earlier results by type
 * through {@link #getByType(Class)}.
 */
public class WorkflowContext {

    private final List<Object> produced =  new ArrayList<>();

    /**
     * Records a value produced by a phase. {@code null} values are ignored, so
     * {@code void} phases and absent results contribute nothing.
     */
    public void add(Object object) {
        if (object != null) {
            produced.add(object);
        }
    }

    /**
     * Returns the most recently produced value assignable to {@code type}.
     * <p>
     * Iterating from newest to oldest ensures a later phase sees the freshest
     * value when several produced objects share a type.
     *
     * @return the matching value, or {@link Optional#empty()} if none was produced
     */
    public <T> Optional<T> getByType(Class<T> type) {
        for (int i = produced.size() - 1; i >= 0; i--) {
            Object object = produced.get(i);
            if (type.isInstance(object)) {
                return Optional.of(type.cast(object));
            }
        }
        return Optional.empty();
    }
}
