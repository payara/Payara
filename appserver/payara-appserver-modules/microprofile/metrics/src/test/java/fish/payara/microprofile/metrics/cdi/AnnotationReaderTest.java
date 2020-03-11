package fish.payara.microprofile.metrics.cdi;

import java.lang.annotation.Annotation;

import org.eclipse.microprofile.metrics.annotation.Metric;

/**
 * Tests the formal correctness of the {@link AnnotationReader} utility.
 *
 * As most {@link Annotation} types behave identical with respect to the tested operation the tests will use different
 * ones in different tests to cover all of them.
 *
 * The {@link Metric} annotation however is special and needs dedicated test coverage.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class AnnotationReaderTest {

}
