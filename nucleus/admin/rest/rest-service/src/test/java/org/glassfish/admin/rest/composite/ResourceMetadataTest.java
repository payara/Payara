/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite;

import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.admin.rest.composite.resource.DummyResource;
import org.testng.annotations.Test;

/**
 *
 * @author jdlee
 */
public class ResourceMetadataTest {
    @Test(groups="offline")
    public void testMetadata() throws JSONException {
        RestResourceMetadata rrmd = new RestResourceMetadata(DummyResource.class);
        System.out.println(rrmd.toJson().toString(4));
    }
}
