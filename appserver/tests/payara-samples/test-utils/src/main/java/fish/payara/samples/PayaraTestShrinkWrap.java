package fish.payara.samples;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Utility class used to generate shrinkwrap archives that will be safe to run
 * on the server side by including all potentially required dependent classes.
 * 
 * @author Matt Gill
 */
public final class PayaraTestShrinkWrap {

    private PayaraTestShrinkWrap() {
    }

    public static WebArchive getWebArchive() {
        return getArchive(WebArchive.class);
    }

    public static JavaArchive getJavaArchive() {
        return getArchive(JavaArchive.class);
    }

    private static <T extends Archive<T> & ClassContainer<T>> T getArchive(Class<T> archiveType) {
        return ShrinkWrap.create(archiveType).addClass(PayaraArquillianTestRunner.class)
                .addClass(PayaraTestRunnerDelegate.class).addClass(SincePayara.class).addClass(NotMicroCompatible.class)
                .addClass(PayaraVersion.class);
    }
}