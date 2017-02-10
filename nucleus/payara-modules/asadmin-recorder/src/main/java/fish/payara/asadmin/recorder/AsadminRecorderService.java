/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.asadmin.recorder;

import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service (name = "asadmin-recorder")
@RunLevel(StartupRunLevel.VAL)
public class AsadminRecorderService implements EventListener {
    private List<String> filteredCommands;
    private String filteredCommandsString;
    
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    AsadminRecorderConfiguration asadminRecorderConfiguration;
    
    @Inject
    Events events;
    
    @Inject
    ServiceLocator habitat;
    
    @PostConstruct
    void postConstruct() {
        events.register(this);
        asadminRecorderConfiguration = habitat.getService(AsadminRecorderConfiguration.class);
    }
    
    @Override
    public void event(Event event) {           
    }
    
    private String constructAsadminCommand(String commandName, 
            ParameterMap parameters) {
        
        String asadminCommand = commandName;
        String mandatoryOption = "";
        for (Map.Entry<String, List<String>> parameter : 
                parameters.entrySet()) {
            if (parameter.getKey().equals("DEFAULT")) {
                // This can have sub-parameters, so loop through and add spaces
                // between the sub-parameters.
                for (int i = 0; i < parameter.getValue().size(); i++) {
                    mandatoryOption += parameter.getValue().get(i);
                    if (i != (parameter.getValue().size() - 1)) {
                        mandatoryOption += " ";
                    }
                }    
            } else {
                asadminCommand += " --" + parameter.getKey() + "="
                    + parameter.getValue().get(0); 
            }
        }

        asadminCommand += " " + mandatoryOption;
        asadminCommand += "\n";
        
        return asadminCommand;
    }

    public boolean isEnabled() {
        boolean enabled = false;
        if (asadminRecorderConfiguration == null) {
            Logger.getLogger(AsadminRecorderService.class.getName()).
                    log(Level.FINE, "No Asadmin Recorder Service configuration "
                            + "found, it is likely missing from the domain.xml."
                            + " Setting enabled to default of false");
        } else {
            enabled = Boolean.parseBoolean(asadminRecorderConfiguration.isEnabled());
        }
        return enabled;
    }
    
    public void recordAsadminCommand(String commandName, 
            ParameterMap parameters) {
        String asadminCommand = "";
     
        // Initialise the string if it hasn't been yet
        if (filteredCommandsString == null) {
            splitFilteredCommands();
        }
        
        // Check if the configuration has been updated
        if (!filteredCommandsString.equals(asadminRecorderConfiguration.
                getFilteredCommands())) {
            splitFilteredCommands();
        }
        
        if (Boolean.parseBoolean(asadminRecorderConfiguration.filterCommands())) {
            if (!(filteredCommands.contains(commandName))) {
                boolean regexMatched = false;
                
                // Check regular expressions
                for (String filteredCommand : filteredCommands) {
                    if (commandName.matches(filteredCommand)) {
                        regexMatched = true;
                    }
                }
                
                if (!regexMatched) {
                    asadminCommand = constructAsadminCommand(commandName, 
                            parameters);
                }
            }
        } else {
            asadminCommand = constructAsadminCommand(commandName, parameters);
        }
        
        // Append to file
        try (Writer writer = new BufferedWriter(new FileWriter(new File(
                asadminRecorderConfiguration.getOutputLocation()), true))) {
            writer.write(asadminCommand);
        } catch (IOException ex) {
            Logger.getLogger(AsadminRecorderService.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
    
    private void splitFilteredCommands() {
        filteredCommandsString = asadminRecorderConfiguration.
                getFilteredCommands();
        filteredCommands = new ArrayList<>(Arrays.asList(filteredCommandsString.
                split(",")));
    }         
}
