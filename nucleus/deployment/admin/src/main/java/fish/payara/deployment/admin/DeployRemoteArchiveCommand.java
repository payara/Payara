/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.deployment.admin;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import fish.payara.deployment.util.GAVConvertor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Andrew Pielage
 */
@Service(name="deploy-remote-archive")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive"),
    @RestEndpoint(configBean = Cluster.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive", params = {
        @RestParam(name = "target", value = "$parent")
    }),
    @RestEndpoint(configBean = Server.class, opType = RestEndpoint.OpType.POST, path = "deploy-remote-archive", params = {
        @RestParam(name = "target", value = "$parent")
    })
})
public class DeployRemoteArchiveCommand extends DeployCommandParameters implements AdminCommand {

    private final static Logger logger = Logger.getLogger(DeployRemoteArchiveCommand.class.getName());
    
    @Param(primary = true)
    private String path;
    
    @Param(name = "additionalRepositories", optional = true, multiple = true, alias = "additionalrepositories")
    private String[] additionalRepositories;
    
    @Inject
    ServiceLocator serviceLocator;
    
    private static final String defaultMavenRepository = "https://repo.maven.apache.org/maven2/";
    
    @Override
    public void execute(AdminCommandContext context) {
        CommandRunner commandRunner = serviceLocator.getService(CommandRunner.class);
        ActionReport actionReport = context.getActionReport();
        
        // Initialise to null so we can do a null check later
        File fileToDeploy = null;
        
        // Assume only Http or Https connections are direct URLs
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                // Download the file to temp, and return a File object to pass to the deploy command
                fileToDeploy = convertUriToFile(new URI(path));
            } catch (IOException | URISyntaxException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                actionReport.setMessage("Exception converting URI to File: " + path);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            
            // If a name hasn't been provided, get it from the URI
            if (name == null) {
                if (path.endsWith(".jar")) {
                    name = path.substring(path.lastIndexOf("/") + 1, path.indexOf(".jar"));
                } else if (path.endsWith(".war")) {
                    name = path.substring(path.lastIndexOf("/") + 1, path.indexOf(".war"));
                } else if (path.endsWith(".ear")) {
                    name = path.substring(path.lastIndexOf("/") + 1, path.indexOf(".ear"));
                }
            }
        } else {     
            try {
                // If the path String doesn't start with Http or Https, then assume it's a GAV coordinate
                logger.log(Level.FINE, "Path does not appear to be a URL, will attempt to read as GAV coordinate");
                
                // Convert the Array to a List of Urls, and append "/" to the Urls if they don't already end with one
                List<URL> repositoryUrls = formatRepositoryUrls(additionalRepositories);
                
                // Get the URL for the given GAV coordinate
                GAVConvertor gavConvertor = new GAVConvertor();
                Entry<String, URL> artefactEntry = gavConvertor.getArtefactMapEntry(path, repositoryUrls);
                
                // Download the file to temp, and return a File object to pass to the deploy command
                fileToDeploy = convertUriToFile(artefactEntry.getValue().toURI());
                
                // If a name hasn't been provided, get it from the URI
                if (name == null) {
                    name = artefactEntry.getKey();
                }
            } catch (MalformedURLException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                actionReport.setMessage("Exception converting GAV to URL: " + path);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            } catch (IOException | URISyntaxException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
                actionReport.setMessage("Exception converting URI to File: " + path);
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
        }
        
        // Only continue if we actually have a file to deploy
        if (fileToDeploy != null) {
            ActionReport subReport = actionReport.addSubActionsReport();
            CommandRunner.CommandInvocation commandInvocation = 
                    commandRunner.getCommandInvocation("deploy", subReport, context.getSubject());
            ParameterMap parameters = createAndPopulateParameterMap(fileToDeploy);
            commandInvocation.parameters(parameters);
            commandInvocation.execute();
        } else {
            actionReport.setMessage("Provided path does not appear to be a valid URL or GAV coordinate: " + path 
                    + "\nSee the server log for more details");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
    
    private List<URL> formatRepositoryUrls(String[] additionalRepositories) throws MalformedURLException {
        List<URL> repositoryUrls = new ArrayList<>();
        for (String repository : additionalRepositories) {
            if (!repository.endsWith("/")) {
                repositoryUrls.add(new URL(repository + "/"));
            } else {
                repositoryUrls.add(new URL(repository));
            }
        }
        
        return repositoryUrls;
    }
    
    private ParameterMap createAndPopulateParameterMap(File fileToDeploy) {
        ParameterMap parameterMap = new ParameterMap();
        
        parameterMap.add("name", name);
        parameterMap.add("path", fileToDeploy.getAbsolutePath());
        
        if (contextroot != null) {
            parameterMap.add("contextroot", contextroot);
        }
        
        if (virtualservers != null) {
            parameterMap.add("virtualservers", virtualservers);
        }
        
        if (libraries != null) {
            parameterMap.add("libraries", libraries);
        }
        
        if (force != null) {
            parameterMap.add("force", force.toString());
        }
        
        if (precompilejsp != null) {
            parameterMap.add("precompilejsp", precompilejsp.toString());
        }
        
        if (verify != null) {
            parameterMap.add("verify", verify.toString());
        }
        
        if (retrieve != null) {
            parameterMap.add("retrieve", retrieve);
        }
        
        if (dbvendorname != null) {
            parameterMap.add("dbvendorname", dbvendorname);
        }
        
        if (createtables != null) {
            parameterMap.add("createtables", createtables.toString());
        }
        
        if (dropandcreatetables != null) {
            parameterMap.add("dropandcreatetables", dropandcreatetables.toString());
        }
        
        if (uniquetablenames != null) {
            parameterMap.add("uniquetablenames", uniquetablenames.toString());
        }
        
        if (deploymentplan != null) {
            parameterMap.add("deploymentplan", deploymentplan.getAbsolutePath());
        }
        
        if (altdd != null) {
            parameterMap.add("altdd", altdd.getAbsolutePath());
        }
        
        if (runtimealtdd != null) {
            parameterMap.add("runtimealtdd", runtimealtdd.getAbsolutePath());
        }
        
        if (enabled != null) {
            parameterMap.add("enabled", enabled.toString());
        }
        
        if (generatermistubs != null) {
            parameterMap.add("generatermistubs", generatermistubs.toString());
        }
        
        if (availabilityenabled != null) {
            parameterMap.add("availabilityenabled", availabilityenabled.toString());
        }
        
        if (asyncreplication != null) {
            parameterMap.add("asyncreplication", asyncreplication.toString());
        }
        
        if (target != null) {
            parameterMap.add("target", target);
        }
        
        if (keepreposdir != null) {
            parameterMap.add("keepreposdir", keepreposdir.toString());
        }
        
        if (keepfailedstubs != null) {
            parameterMap.add("keepfailedstubs", keepfailedstubs.toString());
        }
        
        if (isredeploy != null) {
            parameterMap.add("isredeploy", isredeploy.toString());
        }
        
        if (logReportedErrors != null) {
            parameterMap.add("logReportedErrors", logReportedErrors.toString());
        }
        
        if (properties != null) {
            String propertiesString = properties.toString();
            propertiesString = propertiesString.replaceAll(", ", ":");
            parameterMap.add("properties", propertiesString);
        }
        
        if (property != null) {
            String propertyString = property.toString();
            propertyString = propertyString.replaceAll(", ", ":");
            parameterMap.add("property", propertyString);
        }
        
        if (type != null) {
            parameterMap.add("type", type);
        }
        
        if (keepstate != null) {
            parameterMap.add("keepstate", keepstate.toString());
        }
        
        if (lbenabled != null) {
            parameterMap.add("lbenabled", lbenabled);
        }
        
        if (deploymentorder != null) {
            parameterMap.add("deploymentorder", deploymentorder.toString());
        }
        
        return parameterMap;
    }
    
    
    private File convertUriToFile(URI uri) throws URISyntaxException, IOException {       
        try (InputStream in = uri.toURL().openStream()) {
            return createFile(in);
        }
    }

    private File createFile(InputStream in) throws IOException {
        File file;
        file = File.createTempFile("app", "tmp");
        file.deleteOnExit();
        
        try (OutputStream out = new FileOutputStream(file)) {
            copyStream(in, out);
        } 
        
        return file;
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) >= 0) {
            out.write(buf, 0, len);
        }
    }
}
