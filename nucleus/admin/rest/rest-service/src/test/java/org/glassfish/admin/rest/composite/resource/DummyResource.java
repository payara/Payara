/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite.resource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.composite.CompositeResource;
import org.glassfish.admin.rest.composite.RestCollection;
import org.glassfish.admin.rest.composite.metadata.HelpText;
import org.glassfish.admin.rest.model.BaseModel;

/**
 *
 * @author jdlee
 */
public class DummyResource extends CompositeResource {
    @GET
    public RestCollection<BaseModel> getDummyDataCollection(
            @QueryParam("type") @HelpText(bundle="org.glassfish.admin.rest.composite.HelpText", key="dummy.type") String type
            ) {
        RestCollection<BaseModel> rc = new RestCollection<BaseModel>();

        return rc;
    }

    @GET
    @Path("{name}")
    public BaseModel getDummyData(@QueryParam("foo") String foo) {
        return compositeUtil.getModel(BaseModel.class);
    }

    @POST
    public Response createDummy(BaseModel model) {
        return Response.ok().build();
    }

    @DELETE
    @Path("{name}")
    public Response deleteDummy(@PathParam("name") String name) {
        return Response.ok().build();
    }
}
