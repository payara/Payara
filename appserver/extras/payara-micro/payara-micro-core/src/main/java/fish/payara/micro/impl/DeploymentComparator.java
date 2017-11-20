package fish.payara.micro.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Compares deployment file locations by their file extension.
 */
public class DeploymentComparator implements Comparator<String> {

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
     * extension.
     *
     * @param s1 the first file path to compare
     * @param s2 the second file path to compare
     * @return a negative integer, zero or a positive integer if the first file
     * should be deployed before, at the same time as, or after the second file.
     */
    @Override
    public int compare(String s1, String s2) {
        String format1 = null;
        String format2 = null;
        for (String format : fileExtensions) {
            if (s1.endsWith(format)) {
                format1 = format;
            }
            if (s2.endsWith(format)) {
                format2 = format;
            }
        }
        // Get the index of the extension in the list, or 5 if it's not in the list (meaning sort to back)
        int index1 = (format1 == null) ? 5 : fileExtensions.indexOf(format1);
        int index2 = (format2 == null) ? 5 : fileExtensions.indexOf(format2);

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
