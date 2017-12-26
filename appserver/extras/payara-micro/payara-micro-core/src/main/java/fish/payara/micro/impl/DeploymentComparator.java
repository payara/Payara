package fish.payara.micro.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compares deployment files by their file extension.
 */
public class DeploymentComparator implements Comparator<File> {

    // All the file extensions - in the order they should be deployed.
    private final List<String> fileExtensions;

    public DeploymentComparator() {
        fileExtensions = new ArrayList<>();
        fileExtensions.add(".rar");
        fileExtensions.add(".jar");
        fileExtensions.add(".war");
        fileExtensions.add(".ear");
    }

    /**
     * Compare two files to see which should be deployed first based on file
     * extension. Unknown extensions and null files are sorted to the back in
     * that order.
     *
     * @param f1 the first file to compare
     * @param f2 the second file to compare
     * @return a negative integer, zero or a positive integer if the first file
     * should be deployed before, at the same time as, or after the second file.
     */
    @Override
    public int compare(File f1, File f2) {
        String extension1 = null;
        String extension2 = null;

        // First check for null files, and sort them to the back
        if (f1 == null) {
            return 5;
        }
        if (f2 == null) {
            return -5;
        }

        // Now get the extensions of the files. Unknown extensions will be sorted to the back.
        for (String format : fileExtensions) {
            if (f1.getAbsolutePath().endsWith(format)) {
                extension1 = format;
            }
            if (f2.getAbsolutePath().endsWith(format)) {
                extension2 = format;
            }
        }

        // Now sort unknown extensions to the back, but in front of null files.
        if (extension1 == null) {
            return 5;
        }
        if (extension2 == null) {
            return -5;
        }

        // Get the index of the extension in the list.
        int index1 = fileExtensions.indexOf(extension1);
        int index2 = fileExtensions.indexOf(extension2);

        return index1 - index2;
    }

    /**
     * Get the possible file extensions.
     *
     * @return
     */
    public List<String> getFileExtensions() {
        return fileExtensions;
    }

}
