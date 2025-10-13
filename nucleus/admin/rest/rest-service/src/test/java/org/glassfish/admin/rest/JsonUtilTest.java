/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
 */
package org.glassfish.admin.rest;

import java.util.Locale;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import org.glassfish.admin.rest.composite.CompositeUtil;
import org.glassfish.admin.rest.model.BaseModel;
import org.glassfish.admin.rest.utils.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
public class JsonUtilTest {
    @Test
    public void testArrayEncoding() throws JsonException {
        Locale locale = null;
        BaseModel model = CompositeUtil.instance().getModel(BaseModel.class);
        model.setStringArray(new String[] {"one", "two"});
        JsonObject json = (JsonObject)JsonUtil.getJsonValue(model);
        Assert.assertNotNull(json);
        Object o = json.get("stringArray");
        Assert.assertTrue(o instanceof JsonArray);
        JsonArray array = (JsonArray)o;
        Assert.assertEquals(array.size(), 2);
        Assert.assertTrue(contains(array, "one"));
        Assert.assertTrue(contains(array, "two"));

        BaseModel model2 = CompositeUtil.instance().unmarshallClass(locale, BaseModel.class, json);
        Assert.assertNotNull(model2);
        Assert.assertNotNull(model2.getStringArray());
        Assert.assertEquals(2, model2.getStringArray().length);
    }

    private boolean contains(JsonArray array, String text) throws JsonException {
        for (int i = 0, len = array.size(); i < len; i++) {
            if (text.equals(array.getString(i))) {
                return true;
            }
        }

        return false;
    }
}
