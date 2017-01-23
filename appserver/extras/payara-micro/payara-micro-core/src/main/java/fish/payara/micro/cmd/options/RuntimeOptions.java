/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.cmd.options;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Class to specify the runtime options for Payara Micro
 *
 * @author steve
 */
public class RuntimeOptions {

    private Map<RUNTIME_OPTION, List<String>> options;
    static ResourceBundle bundle = ResourceBundle.getBundle("commandoptions");

    public static void printHelp() {
        System.err.println();
        for (RUNTIME_OPTION option : RUNTIME_OPTION.values()) {
            System.err.print("--" + option.name());
            System.err.print(' ');
            try {
                System.err.println(bundle.getString(option.name()));
            } catch (MissingResourceException mre){
                //ignore as there is no description for this option
                System.err.println();
            }
        }
    }
    
    public Set<RUNTIME_OPTION> getOptions() {
        return options.keySet();
    }
    
    public List<String> getOption(RUNTIME_OPTION option) {
        return options.get(option);
    }
    
    public RuntimeOptions(String args[]) throws ValidationException {
        // parse the arguments into a match 
        options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--")) {
                // this is a command switch
                try {
                    RUNTIME_OPTION option = RUNTIME_OPTION.valueOf(arg.substring(2).toLowerCase());
                    String value = null;
                    if (option.getValue()) {
                        // there is a second value
                        value = args[i+1];
                        if (value.startsWith("--")) {
                            throw new IndexOutOfBoundsException();
                        }
                        option.validate(value);
                    }
                    List<String> values = options.get(option);
                    if (values == null) {
                        values = new LinkedList<String>();
                        options.put(option, values);
                    }
                    values.add(value);
                } catch (IllegalArgumentException iae) {
                    throw new ValidationException(MessageFormat.format(bundle.getString("notValidArgument"),arg));
                } catch (IndexOutOfBoundsException ex) {
                    throw new ValidationException(MessageFormat.format(bundle.getString("expectedArgument"),arg));
                } catch (ValidationException ve) {
                    throw new ValidationException(arg + " " + ve.getMessage(),ve);
                }
            }
        }
    }


}
