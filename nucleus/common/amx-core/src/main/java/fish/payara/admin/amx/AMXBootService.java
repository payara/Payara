/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.admin.amx;

import fish.payara.admin.amx.config.AMXConfiguration;
import java.beans.PropertyChangeEvent;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.admin.mbeanserver.BootAMX;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvent;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

/**
 * Service to boot AMX if it is enabled
 * @author jonathan coustick
 * @since 4.1.2.172
 */
@Service(name="amx-boot-service")
@RunLevel(StartupRunLevel.VAL)
public class AMXBootService implements ConfigListener {
    
    @Inject
    AMXConfiguration config;
    
    @Inject
    ServiceLocator habitat;
    
    private boolean enabled;
    private boolean dynamic;
    
    @PostConstruct
    public void postConstruct(){
        enabled = Boolean.valueOf(config.getEnabled());
        if (enabled){
            startup();
        }
        
    }
    
    private void startup(){
        BootAMX bootAMX = habitat.getService(BootAMX.class);
        bootAMX.bootAMX();
    }
    
    public void setEnabled(boolean enabled, boolean dynamic) {
        this.enabled = enabled;
        this.dynamic = dynamic;
        if (enabled && dynamic) {
            startup();
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        UnprocessedChangeEvents unchanged = null;

        for (PropertyChangeEvent event : events) {
            if (event.getPropertyName().contains("enabled")) {
                String change = (String) event.getNewValue();
                if (change.equalsIgnoreCase("false") && dynamic) {
                    unchanged = new UnprocessedChangeEvents(new UnprocessedChangeEvent(event, "AMX can't be disabled dynamically."));
                }
            }
        }

        return unchanged;
    }
}
