package fish.payara.microprofile.faulttolerance.policy;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

import org.eclipse.microprofile.faulttolerance.Fallback;

/**
 * Utility class to find a {@link Method} of a certain name with the same argument types as a given sample method as
 * required to lookup the {@link Fallback}'s fallback method.
 * 
 * @author Jan Bernitt
 */
public final class MethodLookupUtils {

    private MethodLookupUtils() {
        // util
    }

    public static Method findMethodWithMatchingNameAndArguments(String name, Method sample) {
        Class<?> currentType = sample.getDeclaringClass();
        while (currentType != Object.class) {
            for (Method candidate : currentType.getDeclaredMethods()) {
                if (name.equals(candidate.getName()) && isMatchingParameterList(sample, candidate)) {
                    return candidate;
                }
            }
            for (Class<?> implemented : currentType.getInterfaces()) {
                for (Method candidate : implemented.getDeclaredMethods()) {
                    if (name.equals(candidate.getName()) && isMatchingParameterList(sample, candidate)) {
                        return candidate;
                    }
                }
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

    private static boolean isMatchingParameterList(Method sample, Method candidate) {
        return isMatchtingParameterList(sample.getGenericParameterTypes(), candidate.getGenericParameterTypes());
    }

    private static boolean isMatchtingParameterList(Type[] samples, Type[] candidates) {
        if (samples.length != candidates.length) {
            return false;
        }
        for (int i = 0; i < samples.length; i++) {
            if (!isMatchtingType(samples[i], candidates[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMatchtingType(Type sample, Type candidate) {
        return sample.equals(candidate) 
                || (candidate instanceof TypeVariable)
                || (candidate instanceof GenericArrayType)
                || isMatchingGenericType(sample, candidate)
                || isMatchingWildcardType(sample, candidate);
    }

    private static boolean isMatchingWildcardType(Type sample, Type candidate) {
        if (sample instanceof WildcardType && candidate instanceof WildcardType) {
            WildcardType sampleType = (WildcardType) sample;
            WildcardType candidateType = (WildcardType) candidate;
            return isMatchtingParameterList(sampleType.getLowerBounds(), candidateType.getLowerBounds())
                    && isMatchtingParameterList(sampleType.getUpperBounds(), candidateType.getUpperBounds());
        }
        return false;
    }

    private static boolean isMatchingGenericType(Type sample, Type candidate) {
        if (sample instanceof ParameterizedType && candidate instanceof ParameterizedType) {
            ParameterizedType sampleType = (ParameterizedType) sample;
            ParameterizedType candidateType = (ParameterizedType) candidate;
            if (sampleType.getRawType() != candidateType.getRawType()) {
                return false;
            }
            return isMatchtingParameterList(sampleType.getActualTypeArguments(),
                    candidateType.getActualTypeArguments());
        }
        return false;
    }
}
