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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

/**
 *
 * @author steve
 */
public class FileSystemItemValidator extends Validator {

    private final boolean exists;
    private final boolean writable;
    private final boolean readable;
    private final boolean filesAllowed;
    private final boolean directoryAllowed;

    public FileSystemItemValidator(boolean exists, boolean readable, boolean writable, boolean filesAllowed, boolean directoryAllowed) {
        this.exists = exists;
        this.readable = readable;
        this.writable = writable;
        this.filesAllowed = filesAllowed;
        this.directoryAllowed = directoryAllowed;
        if (!filesAllowed && !directoryAllowed) {
            // TODO this combination is not allowed
        }
    }

    @Override
    boolean validate(String optionValue) throws ValidationException {
        File file = new File(optionValue);

        if (exists) {
            // given file must exist
            if (!file.exists()) {
                // and it does not exist
                throw new ValidationException(MessageFormat.format(RuntimeOptions.commandlogstrings.getString((filesAllowed ? "fileDoesNotExist" : "directoryDoesNotExist")), optionValue));
            } else {
                // given file exists
                if (file.isDirectory() && !directoryAllowed) {
                    // but is a directory and it's not allowed
                    throw new ValidationException(MessageFormat.format(RuntimeOptions.commandlogstrings.getString("fileIsDirectory"), optionValue));
                }
                if (!file.isDirectory() && !filesAllowed) {
                    // but is a file and it's not allowed
                    throw new ValidationException(MessageFormat.format(RuntimeOptions.commandlogstrings.getString("directoryDoesNotExist"), optionValue));
                }
            }
        }

        if (readable && !file.canRead()) {
            throw new ValidationException(MessageFormat.format(RuntimeOptions.commandlogstrings.getString((filesAllowed ? "fileNotReadable" : "directoryNotReadable")), optionValue));
        }

        if (writable && !file.canWrite()) {
            throw new ValidationException(MessageFormat.format(RuntimeOptions.commandlogstrings.getString((filesAllowed ? "fileNotWritable" : "directoryNotWritable")), optionValue));
        }
        return true;
    }
}
