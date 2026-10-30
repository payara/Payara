/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.cmd.options;

import java.text.MessageFormat;

public class RequestTracingValidator extends Validator {

    @Override
    boolean validate(String optionValue) throws ValidationException {
        if (optionValue != null) {
            String[] requestTracingValues = optionValue.split("(?<=\\d)(?=\\D)|(?=\\d)(?<=\\D)");
            // If valid, there should be no more than 2 entries
            if (requestTracingValues.length <= 2) {
                // If the first entry is a number, the second entry should be a String
                if (requestTracingValues[0].matches("\\d+") &&
                        (requestTracingValues.length == 2 && !requestTracingValues[1].matches("\\D+"))) {
                    // If there is a second entry, and it's not a String
                    throw new ValidationException(MessageFormat.format(
                            RuntimeOptions.commandlogstrings.getString("requestTracingIncorrect"), optionValue));
                }

                // If the first entry is a String, there shouldn't be a second entry
                if (requestTracingValues[0].matches("\\D+") && (requestTracingValues.length == 2)) {
                    throw new ValidationException(MessageFormat.format(
                            RuntimeOptions.commandlogstrings.getString("requestTracingIncorrect"), optionValue));
                }
            } else {
                throw new ValidationException(MessageFormat.format(
                        RuntimeOptions.commandlogstrings.getString("requestTracingIncorrect"), optionValue));
            }
        }

        return true;
    }
}
