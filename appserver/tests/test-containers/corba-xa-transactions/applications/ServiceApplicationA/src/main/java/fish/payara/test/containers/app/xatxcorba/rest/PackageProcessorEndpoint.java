package fish.payara.test.containers.app.xatxcorba.rest;

import fish.payara.test.containers.app.xatxcorba.service.PackageProcesserService;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/packager")
@RequestScoped
public class PackageProcessorEndpoint {

    @EJB
    PackageProcesserService processerService;

    @POST
    @Path("/{packages}")
    public void process(@PathParam("packages") Integer packageNumber){
        processerService.processPackages(packageNumber);
    }
}
