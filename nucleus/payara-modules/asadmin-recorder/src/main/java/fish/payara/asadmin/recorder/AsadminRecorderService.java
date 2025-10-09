/*
 *
 * Copyright (c) 2016-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.asadmin.recorder;

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
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.config.support.TranslatedConfigView;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * Service to record asadmin commands.
 * This can be for both commands entered at the command line and
 * for actions in the admin console.
 * @author Andrew Pielage
 * @since 4.1.1.162
 */
@Service(name = "asadmin-recorder")
@RunLevel(StartupRunLevel.VAL)
public class AsadminRecorderService implements EventListener {

    private static final List<String> FILTERED_PARAMETERS = Arrays.asList("userpassword");

    private List<String> filteredCommands;
    private String filteredCommandsString;

    private List<String> prependedOptions;
    private String prependedOptionsString;

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
        splitFilteredCommands();
        setPrependedOptions();
    }

    @Override
    public void event(Event event) {
    }

    private String constructAsadminCommand(String commandName, ParameterMap parameters) {

        StringBuilder asadminCommand = new StringBuilder(commandName);
        StringBuilder mandatoryOption = new StringBuilder();

        if (Boolean.parseBoolean(asadminRecorderConfiguration.prependEnabled()) && prependedOptions != null) {
            for (String s : prependedOptions) {
                asadminCommand.append(" ").append(s);
            }
        }

        for (Map.Entry<String, List<String>> parameter : parameters.entrySet()) {
            // Check for broken parameters
            if (!FILTERED_PARAMETERS.contains(parameter.getKey())) {
                if (parameter.getKey().equals("DEFAULT")) {
                    // This can have sub-parameters, so loop through and add spaces
                    // between the sub-parameters.
                    for (int i = 0; i < parameter.getValue().size(); i++) {
                        mandatoryOption.append(parameter.getValue().get(i));
                        if (i != (parameter.getValue().size() - 1)) {
                            mandatoryOption.append(" ");
                        }
                    }
                } else {
                    asadminCommand.append(" --").append(parameter.getKey()).append("=").append(parameter.getValue().get(0));
                }
            }
        }

        asadminCommand.append(" ").append(mandatoryOption);
        asadminCommand.append("\n");

        return asadminCommand.toString();
    }

    /**
     * Returns true is the asadmin recorder is enabled
     * @return 
     */
    public boolean isEnabled() {
        boolean enabled = false;
        if (asadminRecorderConfiguration == null) {
            Logger.getLogger(AsadminRecorderService.class.getName()).
                    log(Level.FINE, "No Asadmin Recorder Service configuration found, it is likely missing from the"
                            + " domain.xml. Setting enabled to default of false");
        } else {
            enabled = Boolean.parseBoolean(asadminRecorderConfiguration.isEnabled());
        }
        return enabled;
    }

    /**
     * Records a given asadmin command
     * @param commandName The name of the command
     * @param parameters A map of all of the parameters passed as oart of the command
     */
    public void recordAsadminCommand(String commandName, ParameterMap parameters) {
        String asadminCommand = "";

        if (asadminRecorderConfiguration.getPrependedOptions() != null
                && !prependedOptionsString.equals(asadminRecorderConfiguration.getPrependedOptions())) {
            setPrependedOptions();
        }

        // Check if the configuration has been updated
        if (!filteredCommandsString.equals(asadminRecorderConfiguration.getFilteredCommands())) {
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
                    asadminCommand = constructAsadminCommand(commandName, parameters);
                }
            }
        } else {
            asadminCommand = constructAsadminCommand(commandName, parameters);
        }

        // Substitution is needed for fetching the output location, so enable it temporarily
        boolean substitutionEnabled = TranslatedConfigView.doSubstitution.get();
        TranslatedConfigView.doSubstitution.set(true);
        String asadminRecorderFileLocation = asadminRecorderConfiguration.getOutputLocation();
        TranslatedConfigView.doSubstitution.set(substitutionEnabled);

        // Append to file
        try (Writer writer = new BufferedWriter(new FileWriter(new File(
                asadminRecorderFileLocation), true))) {
            writer.write(asadminCommand);
        } catch (IOException ex) {
            Logger.getLogger(AsadminRecorderService.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void splitFilteredCommands() {
        filteredCommandsString = asadminRecorderConfiguration.getFilteredCommands();
        filteredCommands = new ArrayList<>(Arrays.asList(filteredCommandsString.split(",")));
    }

    private void setPrependedOptions() {
        prependedOptionsString = asadminRecorderConfiguration.getPrependedOptions();
        if (prependedOptionsString != null) {
            prependedOptions = new ArrayList<>(Arrays.asList(prependedOptionsString.split(",")));
            for (String option : prependedOptions) {
                // As some options have parameters and single character options use one hyphen, check the size of the first 
                // option, ignorning any parameters present by splitting on " " or "=".
                String optionWithoutParameters = option.split("( |=)")[0];
                if (optionWithoutParameters.length() == 1) {
                    prependedOptions.set(prependedOptions.indexOf(option), "-" + option);
                } else {
                    prependedOptions.set(prependedOptions.indexOf(option), "--" + option);
                }
            }
        }
    }
}
