/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.hazelcast;

import fish.payara.api.admin.config.NameGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class to generate random names for Payara Micro instances.
 * <p>
 * There are over 14,000 different possible names.
 * @author Andrew Pielage
 */
public final class PayaraMicroNameGenerator {

    public static String generateName() {
        return NameGenerator.generateName();
    }

    /**
     * Generates a unique name. If all names are already is use,
     * returns the UUID.
     * This method is more computationally expensive then fish.payara.api.admin.config.NameGenerator.generateName()
     * @param takenNames a list of all names of instances that already exist
     * @param UUID The UUID of the instance
     * @return a unique name
     */
    public static String generateUniqueName(List<String> takenNames, String UUID) {
        String name = "";
        
        // Generate a Map of all available names
        Map<String, List<String>> names = new HashMap<>();
        for (String adjective : NameGenerator.adjectives) {
            names.put(adjective, Arrays.asList(NameGenerator.fishes));
        }
        
        // Find a name not in use
        for (Entry<String, List<String>> entry : names.entrySet()) {
            // If a name has been found, exit the loop
            if (!name.equals("")) {
                break;
            }

            String adjective = entry.getKey();
            for (String fish : entry.getValue()) {
                String potentialName = adjective + "-" + fish;
                if (!takenNames.contains(potentialName)) {
                    name = potentialName;
                    break;
                }
            }
        }
        
        // If a unique name was not found, just set it to the instance UUID
        if (name.equals("")) {
            name = UUID;
        }
        
        return name;
    }
}
