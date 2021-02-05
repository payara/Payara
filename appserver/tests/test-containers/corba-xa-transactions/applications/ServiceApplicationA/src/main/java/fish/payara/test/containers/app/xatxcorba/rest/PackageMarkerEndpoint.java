package fish.payara.test.containers.app.xatxcorba.rest;

import fish.payara.test.containers.app.xatxcorba.service.PackageProcesserService;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/marker")
@RequestScoped
public class PackageMarkerEndpoint {

    @EJB
    PackageProcesserService processerService;

    @POST
    @Path("/{packages}")
    public void process(@PathParam("packages") Integer packageNumber){
        processerService.markPackages(packageNumber);
    }
}
