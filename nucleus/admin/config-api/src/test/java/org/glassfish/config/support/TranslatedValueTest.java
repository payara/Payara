/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2022] Payara Foundation and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author cfiguera
 */
public class TranslatedValueTest {
    private static ServiceLocator habitat;

    @BeforeClass
    public static void setup() {
        habitat = Utils.getNewHabitat();
        TranslatedConfigView.setHabitat(habitat);
    }

    @AfterClass
    public static void teardown() {
        habitat.shutdown();
        TranslatedConfigView.setHabitat(null);
    }

    @Test
    public void translationNotRequired() {
        System.out.println("translationNotRequired");
        assertEquals("value", TranslatedConfigView.expandConfigValue("value"));
    }

    @Test
    public void envTranslationRequired() {
        System.out.println("envTranslationRequired");
        assertEquals("${ENV=NOT_EXISTING_VARIABLE}", TranslatedConfigView.expandConfigValue("${ENV=NOT_EXISTING_VARIABLE}"));
    }

    @Test
    public void envTranslationRequiredWithDefault() {
        System.out.println("envTranslationRequiredWithDefault");
        assertEquals("", TranslatedConfigView.expandConfigValue("${ENV=NOT_EXISTING_VARIABLE:}"));
        assertEquals("defaultVariable", TranslatedConfigView.expandConfigValue("${ENV=NOT_EXISTING_VARIABLE:defaultVariable}"));
        assertEquals("default:variable", TranslatedConfigView.expandConfigValue("${ENV=NOT_EXISTING_VARIABLE:default:variable}"));
    }

    @Test
    public void envTranslationRequiredWithDefaultMultiple() {
        System.out.println("envTranslationRequiredWithDefaultMultiple");
        assertEquals("jdbc:postgresql://localhost:5432/test", TranslatedConfigView.expandValue("jdbc:postgresql://${ENV=db.host:localhost}:${ENV=db.port:5432}/${ENV=DB_NAME:test}"));
    }

    @Test
    public void envTranslationOneRequired() {
        System.out.println("envTranslationOneRequired");
        assertEquals("${ENV=NOT_EXISTING_VARIABLE}_defaultVariable", TranslatedConfigView.expandValue("${ENV=NOT_EXISTING_VARIABLE}_${ENV=NOT_EXISTING_VARIABLE_2:defaultVariable}"));
    }
}
