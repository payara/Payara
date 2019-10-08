package fish.payara.test.containers.tools.junit;

import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.DockerEnvironmentConfiguration;
import fish.payara.test.containers.tools.env.DockerEnvironmentConfigurationParser;
import fish.payara.test.containers.tools.properties.Properties;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit extension; logs test durations and manages environment startup and shutdown.
 *
 * @author David Matějček
 */
public class DockerITestExtension
    implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(DockerITestExtension.class);
    private static final String START_TIME_METHOD = "start time method";
    private static final String START_TIME_CLASS = "start time class";
    private static final File LOGGING_PROPERTIES_BENCH_CONTAINER_PATH = new File(
        "/payara/glassfish/domain1/config/logging-benchmark.properties");
    private static final File LOGGING_PROPERTIES_CONTAINER_PATH = new File(
        "/payara/glassfish/domain1/config/logging.properties");
    private static boolean alreadyTried = false;
    private static boolean inBenchmarkRegime = false;
    private Namespace namespaceClass;
    private Namespace namespaceMethod;


    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        LOG.debug("beforeAll(context={})", context);
        this.namespaceClass = Namespace.create(context.getRequiredTestClass());
        LOG.debug("alreadyTried={}", alreadyTried);
        if (!alreadyTried) {
            alreadyTried = true;
            if (DockerEnvironment.getInstance() == null) {
                final Properties properties = new Properties("test.properties");
                final DockerEnvironmentConfiguration cfg = DockerEnvironmentConfigurationParser.parse(properties);
                final DockerEnvironment dockerEnvironment = DockerEnvironment.createEnvironment(cfg);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    dockerEnvironment.close();
                }));
            }
        }
        final boolean newRegimeIsBenchmark = context.getTags().contains("benchmark");
        if (newRegimeIsBenchmark != inBenchmarkRegime) {
            inBenchmarkRegime = newRegimeIsBenchmark;
            changeLoggingRegime(newRegimeIsBenchmark);
        }
        context.getStore(this.namespaceClass).put(START_TIME_CLASS, LocalDateTime.now());
    }


    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        LOG.info("beforeEach(context). Test name: {}", context.getRequiredTestMethod().getName());
        this.namespaceMethod = Namespace.create(context.getRequiredTestClass(), context.getRequiredTestMethod());
        context.getStore(this.namespaceMethod).put(START_TIME_METHOD, LocalDateTime.now());
    }


    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        final LocalDateTime startTime = context.getStore(this.namespaceMethod).remove(START_TIME_METHOD,
            LocalDateTime.class);
        LOG.info("afterEach(). Test name: {}, started at {}, test time: {} ms", //
            context.getRequiredTestMethod().getName(), //
            DateTimeFormatter.ISO_LOCAL_TIME.format(startTime), //
            startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS));

        // in the sake of flushing all IO buffers in all virtual machines.
        Thread.yield();
    }


    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        final LocalDateTime startTime = context.getStore(this.namespaceClass).remove(START_TIME_CLASS,
            LocalDateTime.class);
        if (startTime == null) {
            // don't log if the beforeAll failed.
            return;
        }
        LOG.info("afterAll(). Test class name: {}, started at {}, test time: {} ms", //
            context.getRequiredTestClass().getName(), //
            DateTimeFormatter.ISO_LOCAL_TIME.format(startTime), //
            startTime.until(LocalDateTime.now(), ChronoUnit.MILLIS));
    }


    /**
     * Resets the server logging to lighter (benchmarks) or heavier (functionalities) version.
     *
     * @param newRegimeIsBenchmark
     */
    private void changeLoggingRegime(final boolean newRegimeIsBenchmark) {
        final DockerEnvironment environment = DockerEnvironment.getInstance();
        if (environment != null) {
            environment.reconfigureLogging(
                newRegimeIsBenchmark ? LOGGING_PROPERTIES_BENCH_CONTAINER_PATH : LOGGING_PROPERTIES_CONTAINER_PATH);
        }
    }
}
