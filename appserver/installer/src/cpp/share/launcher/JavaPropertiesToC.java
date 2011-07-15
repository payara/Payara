/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

/**
 * This class, via its <code>main</code> method, reads a set of Java resource
 * property files and prints, to <code>System.out</code>, compilable C source
 * code. The usage for this class is as follows:
 * <p>
 * <code>
 * java -classpath &lt...&gt JavaPropertiesToC [-options] -bundle &ltresource bundle name&gt
 * </code>
 * <p>
 * where options include
 * <p>
 * <code>
 * -srcdir &ltdirectory to look for resource bundle&gt
 * </<code>
 */
public class JavaPropertiesToC {

    public static void main(String[] args) {

        // Determine platform
        String osname = System.getProperty("os.name");
        boolean windows = false;
        if (osname != null && osname.toLowerCase().startsWith("windows"))
            windows = true;

        // Evaluate arguments
        String bundle = null;
        File srcdir = new File(System.getProperty("user.dir"));
        for (int i = 0; i < args.length; i++) {
            if ("-bundle".equals(args[i])) {
                if (i + 1 < args.length)
                    bundle = args[++i];
                else
                    printUsage(args[0] + " option needs to be followed by a bundle name");
            }
            else if ("-srcdir".equals(args[i])) {
                if (i + 1 < args.length)
                    srcdir = new File(args[++i]);
                else
                    printUsage(args[0] + " option needs to be followed by a bundle name");
            }
            else {
                printUsage(args[0] + " is not a valid option");
            }
        }
        if (bundle == null)
            printUsage("Required arguments are missing");
        if (!srcdir.isDirectory() || !srcdir.canRead())
            printUsage("Source directory " + srcdir + " is not a readable directory");

        // Print required includes
        String exportMacro = "EXPORT_DATA";
        if (windows) {
            System.out.println("#include <wchar.h>");
            System.out.println("#define " + exportMacro + " __declspec(dllexport)");
        }
        else {
            System.out.println("#define " + exportMacro);
        }
        System.out.println("#ifdef __cplusplus");
        System.out.println("extern \"C\" {");
        System.out.println("#endif");

        // Iterate through <bundle name>*.properties files in source directory
        String[] fileList = srcdir.list();
        String fileExtension = ".properties";
        for (int i = 0; i < fileList.length; i++) {

            if (!fileList[i].startsWith(bundle) || !fileList[i].endsWith(fileExtension))
                continue;
            String locale = fileList[i].substring(bundle.length(), fileList[i].length() - fileExtension.length());
            if (locale.length() > 0 && !locale.startsWith("_"))
                continue;

            // Load the file into a properties object
            Properties props = new Properties();
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(new File(srcdir, fileList[i]));
                props.load(fis);
            }
            catch (Exception e) {
                printUsage(e.getMessage());
            }
            finally {
                try {
                    if (fis != null)
                        fis.close();
                }
                catch (Throwable t) {}
            }

            // Write out the properties to as wchar_t arrays on Windows and
            // UTF-8 on Unix platforms. We need to use UTF-8 on Unix platforms
            // because the wc*to*mb and mb*to*wc functions assume that the
            // content in the wchar_t array is the same encoding as the user's
            // current encoding instead of being Unicode!
            String prefix = bundle + locale + "_";
            Enumeration enume = props.keys();
            while (enume.hasMoreElements()) {
                String key = (String)enume.nextElement();
                String value = (String)props.getProperty(key);
                if (windows)
                    System.out.print(exportMacro + " const wchar_t *" + prefix + key + "() { return L\"");
                else
                    System.out.print(exportMacro + " const char *" + prefix + key + "() { return \"");
                // Print each character in the value in hexadecimal format so
                // no escape characters are required
                if (value != null) {
                    if (windows) {
                        char[] chars = value.toCharArray();
                        for (int j = 0; j < chars.length; j++)
                            System.out.print("\\x" + Integer.toHexString(chars[j]));
                    }
                    else {
                        try {
                            byte[] bytes = value.getBytes("UTF8");
                            for (int j = 0; j < bytes.length; j++)
                            {
                                int k = bytes[j];
                                if (k < 0)
                                    k += 256;
                                System.out.print("\\x" + Integer.toHexString(k));
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            System.exit(1);
                        }
                    }
                }
                System.out.println("\"; }");
            }

        }
        System.out.println("#ifdef __cplusplus");
        System.out.println("}");
        System.out.println("#endif");

        // If we made it this far, everything went OK
        System.exit(0);

    }

    public static void printUsage(String msg) {

        if (msg != null)
            System.err.println(msg);

        System.err.println(
"Usage: java -classpath " + System.getProperty("java.class.path") + " [-options] -bundle <ltresource bundle name>\n" +
"where -options include:\n" +
"    -srcdir <directory to look for resource bundle>");

        System.exit(1);

    }

}
