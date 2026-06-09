/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2026 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.samples;

import static java.lang.Runtime.getRuntime;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Methods to execute "cli commands" against various servers.
 *
 * @author Arjan Tijms
 */
public class CliCommands {

    private static final Logger logger = Logger.getLogger(CliCommands.class.getName());
    private static final String OS = System.getProperty("os.name").toLowerCase();

    public static int payaraGlassFish(String... cliCommands) {
        return payaraGlassFish(asList(cliCommands), null);
    }

    public static int payaraGlassFish(List<String> cliCommands) {
        return payaraGlassFish(cliCommands, null);
    }

    public static int payaraGlassFish(List<String> cliCommands, List<String> output) {
        String productRoot = System.getProperty("com.sun.aas.productRoot");
        if (productRoot == null) {
            throw new IllegalStateException("com.sun.aas.productRoot not specified");
        }

        Path gfHomePath = Paths.get(productRoot);
        if (!gfHomePath.toFile().exists()) {
            throw new IllegalStateException("com.sun.aas.productRoot at " + productRoot + " does not exists");
        }

        if (!gfHomePath.toFile().isDirectory()) {
            throw new IllegalStateException("com.sun.aas.productRoot at " + productRoot + " is not a directory");
        }

        Path asadminPath = gfHomePath.resolve(isWindows()? "bin/asadmin.bat" : "bin/asadmin");

        if (!asadminPath.toFile().exists()) {
            throw new IllegalStateException("asadmin command at " + asadminPath.toAbsolutePath() + " does not exists");
        }

        List<String> cmd = new ArrayList<>();

        cmd.add(asadminPath.toAbsolutePath().toString());
        cmd.addAll(cliCommands);
        logger.info("Executing command: \n" + String.join(" ", cmd));

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);

        try {
            return
                waitToFinish(
                    readAllInput(
                        output,
                        destroyAtShutDown(
                            processBuilder.start())));
        } catch (IOException e) {
            return -1;
        }
    }

    public static Process destroyAtShutDown(final Process process) {
        getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (process != null) {
                    process.destroy();
                    try {
                        process.waitFor();
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
        }));

        return process;
    }

    public static Process readAllInput(List<String> output, Process process) {
        // Read any output from the process
        try (Scanner scanner = new Scanner(process.getInputStream())) {
            while (scanner.hasNextLine()) {
                String nextLine = scanner.nextLine();
                System.out.println(nextLine);
                if (output != null) {
                    output.add(nextLine);
                }
            }
        }

        return process;
    }

    public static int waitToFinish(Process process) {

        // Wait up to 30s for the process to finish
        int startupTimeout = 30 * 1000;
        while (startupTimeout > 0) {
           startupTimeout -= 200;
           try {
               Thread.sleep(200);
           } catch (InterruptedException e1) {
               // Ignore
           }

           try {
              int exitValue = process.exitValue();

              System.out.println("Asadmin process exited with status " + exitValue);
              return exitValue;

           } catch (IllegalThreadStateException e) {
              // process is still running
           }
        }

        throw new IllegalStateException("Asadmin process seems stuck after waiting for 30 seconds");
    }

    public static boolean isWindows() {
        return OS.contains("win");
    }

}
