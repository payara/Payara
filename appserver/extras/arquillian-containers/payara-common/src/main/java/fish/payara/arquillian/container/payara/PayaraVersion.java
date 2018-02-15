package fish.payara.arquillian.container.payara;

import java.util.Scanner;

/**
 * A utility class to store the Payara Version and check if it's more recent than another version.
 */
public class PayaraVersion {

    private String versionString;

    public PayaraVersion(String versionString) {
        if (versionString == null || versionString.isEmpty()) {
            throw new IllegalArgumentException("Invalid version string.");
        }
        this.versionString = versionString;
    }

    public boolean isMoreRecentThan(PayaraVersion minimum) {
        String minString = minimum.versionString;
        try (Scanner minScanner = new Scanner(minString); Scanner vScanner = new Scanner(versionString)) {
            minScanner.useDelimiter("\\.");
            vScanner.useDelimiter("\\.");

            while (minScanner.hasNext() && vScanner.hasNext()) {
                String minPartString = minScanner.next();
                String vPartString = vScanner.next();

                int minPart = 0;
                try {
                    minPart = Integer.parseInt(minPartString.replaceAll("[^0-9]", ""));
                } catch (Exception ex) {
                    // Leave the number at 0
                }
                int vPart = 0;
                try {
                    vPart = Integer.parseInt(vPartString.replaceAll("[^0-9]", ""));
                } catch (Exception ex) {
                    // Leave the number at 0
                }

                if (minPartString.toUpperCase().contains("-SNAPSHOT")) {
                    minPart--;
                }
                if (vPartString.toUpperCase().contains("-SNAPSHOT")) {
                    vPart--;
                }

                if (vPart > minPart) {
                    return true;
                }
                if (minPart > vPart) {
                    return false;
                }
            }
        }
        return true;
    }
}