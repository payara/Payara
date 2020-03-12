package fish.payara.microprofile.metrics.test;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * General test utilities for MP Metrics module.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class TestUtils {

    public static Method firstDeclaredMethod(Class<?> bean) {
        return firstDeclaredNonSynthetic(bean.getDeclaredMethods());
    }

    public static Field firstDeclaredField(Class<?> bean) {
        return firstDeclaredNonSynthetic(bean.getDeclaredFields());
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> firstDeclaredConstructor(Class<T> bean) {
        return (Constructor<T>) firstDeclaredNonSynthetic(bean.getDeclaredConstructors());
    }

    public static <T extends Member> T firstDeclaredNonSynthetic(T[] members) {
        for (T m : members) {
            if (!m.isSynthetic()) {
                return m;
            }
        }
        return null;
    }

    public static Method getAnnotatedMethod() {
        return getTestMethod("_");
    }

    public static Method getTestMethod() {
        return getTestMethod("");
    }

    private static Method getTestMethod(String suffix) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        int i = 0;
        while (!isTestMethod(stackTraceElements[i])) {
            i++;
        }
        StackTraceElement testMethodElement = stackTraceElements[i];
        String testName = testMethodElement.getMethodName();
        try {
            Class<?> testClass = Class.forName(testMethodElement.getClassName());
            for (Method m : testClass.getDeclaredMethods()) {
                if (suffix.isEmpty() ? m.getName().equals(testName) : m.getName().startsWith(testName + "_")) {
                    return m;
                }
            }
            throw new AssertionError("Failed to find annotated method for test: " + testName);
        } catch (Exception e) {
            throw new AssertionError("Failed to find annotated method in test class: ", e);
        }
    }

    private static boolean isTestMethod(StackTraceElement element) {
        if (!element.getClassName().endsWith("Test")) {
            return false;
        }
        try {
            Class<?> testClass = Class.forName(element.getClassName());
            Method elementMethod = testClass.getMethod(element.getMethodName());
            return elementMethod.isAnnotationPresent(org.junit.Test.class);
        } catch (Exception e) {
            return false;
        }
    }

    public static AnnotatedMember<?> fakeMemberOf(Member member, AnnotatedElement element) {
        AnnotatedMember<?> fake = mock(AnnotatedMember.class);
        when(fake.getJavaMember()).thenReturn(member);
        when(fake.getBaseType()).thenReturn(getGenericTypeOf(member));
        initAnnotated(fake, element);
        return fake;
    }

    @SuppressWarnings("unused")
    private static void initAnnotated(Annotated fake, AnnotatedElement element) {
        when(fake.getAnnotation(any()))
            .thenAnswer(invocation -> element.getAnnotation(invocation.getArgument(0)));
        when(fake.getAnnotations())
            .thenReturn(new LinkedHashSet<>(asList(element.getAnnotations())));
        when(fake.getAnnotations(any()))
            .thenAnswer(invocation -> new LinkedHashSet<Annotation>(asList(element.getAnnotationsByType(invocation.getArgument(0)))));
        when(fake.isAnnotationPresent(any())).thenAnswer(invocation -> element.isAnnotationPresent(invocation.getArgument(0)));
    }

    public static InjectionPoint fakeInjectionPointFor(Member member, AnnotatedElement element) {
        InjectionPoint fake = mock(InjectionPoint.class);
        when(fake.getMember()).thenReturn(member);
        AnnotatedMember<?> annotated = fakeMemberOf(member, element);
        when(fake.getAnnotated()).thenReturn(annotated);
        when(fake.getType()).thenReturn(getGenericTypeOf(member));
        return fake;
    }

    public static InjectionPoint fakeInjectionPointFor(Parameter parameter) {
        InjectionPoint fake = mock(InjectionPoint.class);
        AnnotatedParameter<?> param = mock(AnnotatedParameter.class);
        when(param.getPosition()).thenReturn(0);
        when(param.getBaseType()).thenReturn(parameter.getParameterizedType());
        when(param.getJavaParameter()).thenReturn(parameter);
        initAnnotated(param, parameter);
        when(fake.getAnnotated()).thenReturn(param);
        when(fake.getType()).thenReturn(parameter.getParameterizedType());
        return fake;
    }

    private static Type getGenericTypeOf(Member member) {
        if (member instanceof Method) {
            return ((Method) member).getGenericReturnType();
        }
        if (member instanceof Field) {
            return ((Field) member).getGenericType();
        }
        if (member instanceof Constructor) {
            return member.getDeclaringClass();
        }
        throw new UnsupportedOperationException("Generic Type of " + member);
    }
}
