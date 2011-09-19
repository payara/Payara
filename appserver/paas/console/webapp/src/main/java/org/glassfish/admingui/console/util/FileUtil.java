/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.admingui.console.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 * @author anilam
 */
public class FileUtil {

    static public File inputStreamToFile(InputStream inputStream, String origFileName) throws IOException {
        int index = origFileName.indexOf(".");
        String suffix = null;
        if (index > 0) {
            suffix = origFileName.substring(index);
        }
        String prefix = origFileName.substring(0, index);
        File tmpFile = File.createTempFile("gf-" + prefix, suffix);
        tmpFile.deleteOnExit();
        OutputStream out = new FileOutputStream(tmpFile);
        byte buf[] = new byte[4096];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        inputStream.close();
        System.out.println("\ntmp is created." + tmpFile.getAbsolutePath());
        return tmpFile;
    }


}
