package fish.payara.samples.accesslog;

import fish.payara.samples.CliCommands;
import fish.payara.samples.NotMicroCompatible;
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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.Assert.fail;

@NotMicroCompatible
@RunWith(PayaraArquillianTestRunner.class)
public class CustomAccessLogHandlerIT {
    private static final String HTTP_CONFIG = "configs.config.server-config.http-service.";
    private static final String ACCESS_LOG = HTTP_CONFIG + "access-log.";
    private static final String ACCESS_LOG_FILE_PREFIX = "server_access_log";
    private static final String ARBITRARY_RESOURCE = "arbitrary-resource";

    @ArquillianResource
    private URL baseURL;

    @Deployment
    public static WebArchive createDeployment () {
        return PayaraTestShrinkWrap.getWebArchive()
                .addClass(CustomAccessLogHandler.class);
    }

    @Test
    public void applyCustomHandlerIT() throws Exception {
        // Add handler class to lib.
        String resourceName = CustomAccessLogHandler.class.getName().replace('.', '/') + ".class";
        Path target = ServerOperations.getDomainPath("lib").resolve("classes").resolve(resourceName);
        Files.createDirectories(target.getParent());

        try (InputStream classBytes = CustomAccessLogHandlerIT.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (classBytes == null) {
                fail("Could not locate compiled handler class on the test class path: " + resourceName);
            }
            Files.copy(classBytes, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // Set config to use the custom handler.
        List<List<String>> in = List.of(
                List.of("set", ACCESS_LOG + "log-handler=Custom"),
                List.of("set", ACCESS_LOG + "custom-log-handler=" + CustomAccessLogHandler.class.getName()),
                List.of("set", ACCESS_LOG + "write-interval-seconds=0"),
                List.of("set", ACCESS_LOG + "buffer-size-bytes=0"),
                List.of("set", HTTP_CONFIG + "access-logging-enabled=true"));

        for (List<String> command : in) {
            List<String> out = new ArrayList<>();
            CliCommands.payaraGlassFish(command, out);
            Assert.assertTrue("Failure executing command: " + String.join(" ", command)
                            + "\nServer responded: " + String.join(", ", out),
                    out.contains("Command set executed successfully."));
        }

        // Ping an arbitrary URL for logging.
        ping(new URL(baseURL + ARBITRARY_RESOURCE));

        // Test the log file contents.
        File log = getLogFile();
        if (log == null) {
            Assert.fail("Access Log file was not found.");
        }

        String line = getAccessLogEntry(log);
        Assert.assertNotNull("Access Log file was empty or did not contain the tested URL.", line);
        Files.deleteIfExists(target);
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
