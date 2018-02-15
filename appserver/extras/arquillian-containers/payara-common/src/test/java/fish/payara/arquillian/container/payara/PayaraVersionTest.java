package fish.payara.arquillian.container.payara;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PayaraVersionTest {

    private PayaraVersion v1;
    private PayaraVersion v2;

    @Test
    public void moreRecentVersionTest() {
        v1 = new PayaraVersion("4.1.2.174");
        v2 = new PayaraVersion("4.1.2.181");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("4.1.2.181");
        v2 = new PayaraVersion("4.1.3.181");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("4.1.2.181");
        v2 = new PayaraVersion("4.2.2.181");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("4.1.2.174");
        v2 = new PayaraVersion("4.1.2.181-SNAPSHOT");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("4.1.2.181-SNAPSHOT");
        v2 = new PayaraVersion("4.1.2.181");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("5.Beta2");
        v2 = new PayaraVersion("5.181-SNAPSHOT");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));

        v1 = new PayaraVersion("5.181-SNAPSHOT");
        v2 = new PayaraVersion("5.181");
        assertTrue("The version check failed.", v2.isMoreRecentThan(v1));
    }
}