/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.appserver.zendesk.admin;

import com.sun.enterprise.util.StringUtils;
import fish.payara.appserver.zendesk.config.ZendeskSupportConfiguration;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ServerEnvironment;
/**
 *
 * @author Jonathan Coustick
 */
@Service(name = "get-domain-xml")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean = ZendeskSupportConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-domain-xml",
            description = "Gets a copy of the full domain.xml")
})
public class GetDomainXMLCommand implements AdminCommand{
    
    @Inject
    ServerEnvironment se;
        
    @Override
    public void execute(AdminCommandContext context) {
        
        File configFile = new File(se.getConfigDirPath().getAbsolutePath() + "/domain.xml");
        
        try (BufferedReader reader =  new BufferedReader(new FileReader(configFile))){
            String all = "";
            String line = reader.readLine();
            while (line != null){
                all += line;
                all += StringUtils.EOL;
                line = reader.readLine();
            }
            
            ActionReport actionReport = context.getActionReport(); 
            actionReport.appendMessage(all);
            Map<String, Object> extraPropsMap = new HashMap<>();
            extraPropsMap.put("domainxml", all);
            Properties extraProps = new Properties();
            extraProps.put("zendeskSupportConfiguration", extraPropsMap);
        
            actionReport.setExtraProperties(extraProps);       
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GetDomainXMLCommand.class.getName()).log(Level.SEVERE, "domain.xml not found");
        } catch (IOException ex) {
            Logger.getLogger(GetDomainXMLCommand.class.getName()).log(Level.SEVERE, "Error reading domain.xml");
        }
        
    }
    
       
}