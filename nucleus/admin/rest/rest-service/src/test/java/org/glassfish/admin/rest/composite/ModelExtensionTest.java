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
package org.glassfish.admin.rest.composite;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.composite.metadata.Default;
import org.glassfish.admin.rest.composite.metadata.DefaultsGenerator;
import org.glassfish.admin.rest.composite.metadata.RestMethodMetadata;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.admin.rest.model.BaseModel;
import org.glassfish.admin.rest.model.ModelExt1;
import org.glassfish.admin.rest.model.ModelExt2;
import org.glassfish.admin.rest.model.RelatedModel;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
@Test
public class ModelExtensionTest {
    @Test
    public void testNestedModels() {
        BaseModel model = CompositeUtil.instance().getModel(BaseModel.class);
        List<RelatedModel> related = model.getRelated();
        Assert.assertNull(related);

        RelatedModel rm = CompositeUtil.instance().getModel(RelatedModel.class);
        rm.setId("1");
        rm.setDescription("test");
        related = new ArrayList<RelatedModel>();
        related.add(rm);
        model.setRelated(related);

        related = model.getRelated();
        Assert.assertEquals(related.size(), 1);
    }

    @Test
    public void testModelExtension() {
        BaseModel model = CompositeUtil.instance().getModel(BaseModel.class);
        Assert.assertTrue(ModelExt1.class.isAssignableFrom(model.getClass()));
        Assert.assertTrue(ModelExt2.class.isAssignableFrom(model.getClass()));
    }

    public void testModelInheritance() throws JSONException {
        Model1 m1 = CompositeUtil.instance().getModel(Model1.class);
        Model2 m2 = CompositeUtil.instance().getModel(Model2.class);

        Assert.assertNotNull(m1);
        Assert.assertNotNull(m2);

        RestResourceMetadata rrmd = new RestResourceMetadata(new TestResource());
        final List<RestMethodMetadata> getMethods = rrmd.getResourceMethods().get("GET");
        JSONObject name = getJsonObject(getMethods.get(0).toJson(), "response.properties.name");

        Assert.assertNotNull(name, "'name' should not be null. Inherited methods are not showing up in generated class");
        Assert.assertNotNull(name.get("default"), "The field 'name' should have a default value.");
    }

    // Works with no dot?
    private JSONObject getJsonObject(JSONObject current, String dottedName) {
        Assert.assertNotNull(dottedName);
        String[] parts = dottedName.split("\\.");

        for (String part : parts) {
            try {
                current = (JSONObject)current.get(part);
            } catch (JSONException e) {
                current = null;
                break;
            }
        }

        return current;
    }

    public static class TestResource extends LegacyCompositeResource {
        @GET
        public Model2 getModel() {
            Model2 m2 = CompositeUtil.instance().getModel(Model2.class);

            return m2;
        }
    }

    public static interface Model1 extends RestModel {
        @Default(generator=ModelDefaultGenerator.class)
        String getName();
        void setName(String name);
    }

    public static interface Model2 extends Model1 {
        @Default(generator=ModelDefaultGenerator.class)
        String getName2();
        void setName2(String name);
    }

    public static class ModelDefaultGenerator implements DefaultsGenerator {

        @Override
        public Object getDefaultValue(String propertyName) {
            return "defaultData";
        }

    }
}
