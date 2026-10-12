package fish.payara.samples.accesslog;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.ServerOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Scanner;

import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
public class CustomAccessLogHandlerIT {
    private static final String ACCESS_LOG_FILE_PREFIX = "server_access_log";
    private static final String ARBITRARY_RESOURCE = "arbitrary-resource";

    @ArquillianResource
    private URL baseURL;

    @Deployment
    public static WebArchive createDeployment () {
        return PayaraTestShrinkWrap.getWebArchive();
    }

    @Test
    public void applyCustomHandlerIT() throws Exception {
        ping(new URL(baseURL + ARBITRARY_RESOURCE));

        File log = getLogFile();
        if (log == null) {
            Assert.fail("Access Log file was not found.");
        }

        String line = getAccessLogEntry(log);
        Assert.assertNotNull("Access Log file was empty or did not contain the tested URL.", line);
    }

    private void ping(URL url) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(url.toURI())
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        client.send(request, HttpResponse.BodyHandlers.discarding());
        client.close();
    }

    private File getLogFile() {
        File accessLogFolder = ServerOperations.getDomainPath("logs").resolve("access").toFile();
        File accessLog = null;

        // The Micro version of the test places the access log in the same folder as the server.log
        if (!accessLogFolder.isDirectory()) {
            accessLogFolder = ServerOperations.getDomainPath("logs").toFile();
        }

        if (accessLogFolder.isDirectory()) {
            File[] files = accessLogFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith(ACCESS_LOG_FILE_PREFIX) && file.isFile() && file.canRead()) {
                        accessLog = file;
                        break;
                    }
                }
            }
        }

        return accessLog;
    }

    private String getAccessLogEntry(File logFile) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new FileInputStream(logFile))) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();

                // There will be lines written before the ping, ignore them.
                if (line.contains(ARBITRARY_RESOURCE)) {
                    if (!line.startsWith(CustomAccessLogHandler.PREFIX)) {
                        fail("Access Log line would not formatted by the custom handler. Value found: " + line);
                    }
                    return line;
                }
            }
        }
        return null;
    }
}
