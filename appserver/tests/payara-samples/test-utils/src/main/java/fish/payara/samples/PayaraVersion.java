/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.samples;

import java.util.Scanner;

/**
 * A utility class to store the Payara Version and check if it's more recent than another version.
 * 
 * @author Matt Gill
 */
public class PayaraVersion {

    private static final String VERSION_REGEX = "^([0-9.]+)+.+$";

    private final String versionString;

    public PayaraVersion(String versionString) {
        this.versionString = versionString;
    }

    public boolean isValid() {
        return versionString != null && versionString.matches(VERSION_REGEX);
    }

    public boolean isAtLeast(PayaraVersion minimum) {
        try {
            return compare(minimum) >= 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isMoreRecentThan(PayaraVersion minimum) {
        try {
            return compare(minimum) > 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private int compare(PayaraVersion minimum) {
        if (!isValid() || !minimum.isValid()) {
            throw new IllegalArgumentException("Unable to compare versions.");
        }

        try (Scanner minScanner = new Scanner(minimum.versionString);
            Scanner vScanner = new Scanner(versionString)) {
            minScanner.useDelimiter("\\.");
            vScanner.useDelimiter("\\.");

            while (minScanner.hasNext() || vScanner.hasNext()) {
                // Get the next part of the version
                String versionPart = vScanner.hasNext() ? vScanner.next() : "0";
                int versionPartInteger = extractInteger(versionPart);

                // Get the next part of the minimum version
                String minVersionPart = minScanner.hasNext() ? minScanner.next() : "0";
                int minVersionPartInteger = extractInteger(minVersionPart);

                // If the versions are miles apart, proceed the version part until it is comparable
                // to the minimum version
                while (vScanner.hasNext() && Math.abs(versionPartInteger - minVersionPartInteger) > 100) {
                    versionPart = vScanner.next();
                    versionPartInteger = extractInteger(versionPart);
                }

                // Calculate modifiers as a result of being a SNAPSHOT or RC
                float modifiedMinPart = minVersionPartInteger + calculateModifier(minVersionPart);
                float modifiedVPart = versionPartInteger + calculateModifier(versionPart);

                int comparison = Float.compare(modifiedVPart, modifiedMinPart);
                if (comparison != 0) {
                    return comparison;
                }
            }
        }
        return 0;
    }

    private static int extractInteger(String string) {
        try {
            return Integer.parseInt("0" + string.replaceAll("[^0-9]", ""));
        } catch (Exception ex) {
            return 0;
        }
    }

    private static float calculateModifier(String versionPart) {
        versionPart = versionPart.toUpperCase();
        if (versionPart.contains("-SNAPSHOT")) {
            return -0.5f;
        }
        if (versionPart.contains("RC")) {
            return -1.5f;
        }
        return 0;
    }
}