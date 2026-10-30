/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020-2023 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.metrics.cdi;

import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.AnnotatedMember;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.InjectionPoint;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.eclipse.microprofile.metrics.annotation.Timed;

import static java.util.Arrays.asList;

/**
 * Utility that allows reading the different MP metrics {@link Annotation}s from different annotated abstractions
 * providing a common interface to allow generic handling and a common logic independent of the source of the
 * {@link Annotation}.
 *
 * Supported are:
 * <ul>
 * <li>{@link AnnotatedElement}</li>
 * <li>{@link Annotated}</li>
 * <li>{@link InjectionPoint}</li>
 * </ul>
 *
 * It is important to realise that {@link Annotated} and {@link InjectionPoint} have to be used as a source when
 * available as they allow to add or remove {@link Annotation} effectively acting as a runtime override of the compiled
 * information provided by {@link AnnotatedElement}.
 *
 * This utility also encodes most of the logic as defined by the MP Metrics specification. This includes the logic of
 * which annotation applied and how the metrics effective name if computed from annotation values and the annotated
 * element. For this reason the methods are documented in great detail.
 *
 * @author Jan Bernitt
 * @since 5.202
 *
 * @param <T> Type of the MP metrics annotation
 */
public final class AnnotationReader<T extends Annotation> {

    /**
     * Get {@link AnnotationReader} for a provided {@link Annotation}.
     *
     * @param annotationType
     * @return The {@link AnnotationReader} for the provided {@link Annotation} type
     * @throws IllegalAccessException In case no such reader exists
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> AnnotationReader<T> forAnnotation(Class<T> annotationType) {
        AnnotationReader<?> reader = READERS_BY_ANNOTATION.get(annotationType);
        if (reader == null) {
            throw new IllegalArgumentException("Unsupported Metrics [" + annotationType.getName() + "]");
        }
        return (AnnotationReader<T>) reader;
    }

    /**
     * @return all available {@link AnnotationReader}s
     */
    public static Iterable<AnnotationReader<?>> readers() {
        return READERS_BY_ANNOTATION.values();
    }

    private static final Map<Class<? extends Annotation>, AnnotationReader<?>> READERS_BY_ANNOTATION = new HashMap<>();



    public static final AnnotationReader<Counted> COUNTED = new AnnotationReader<>(
            Counted.class, Counted.class.getName(),
            Counted::name,
            Counted::tags,
            Counted::description,
            Counted::absolute,
            Counted::unit,
            Counted::scope);

    public static final AnnotationReader<Gauge> GAUGE = new AnnotationReader<>(
            Gauge.class, Gauge.class.getName(),
            Gauge::name,
            Gauge::tags,
            Gauge::description,
            Gauge::absolute,
            Gauge::unit,
            Gauge::scope);


    public static final AnnotationReader<Timed> TIMED = new AnnotationReader<>(
            Timed.class, Timed.class.getName(),
            Timed::name,
            Timed::tags,
            Timed::description,
            Timed::absolute,
            Timed::unit,
            Timed::scope);

    public static final AnnotationReader<Metric> METRIC = new AnnotationReader<>(
            Metric.class, Metric.class.getName(),
            Metric::name,
            Metric::tags,
            Metric::description,
            Metric::absolute,
            Metric::unit,
            Metric::scope);

    private static void register(AnnotationReader<?> reader) {
        READERS_BY_ANNOTATION.put(reader.annotationType(), reader);
    }

    static {
        register(COUNTED);
        register(GAUGE);
        register(TIMED);
        register(METRIC);
    }

    private final Class<T> annotationType;
    private final String nameType;
    private final Function<T, String> name;
    private final Function<T, String[]> tags;
    private final Function<T, String> description;
    private final Predicate<T> absolute;
    private final Function<T, String> unit;

    private final Function<T, String> scope;

    private AnnotationReader(Class<T> annotationType, String nameType,
            Function<T, String> name,
            Function<T, String[]> tags,
            Function<T, String> description,
            Predicate<T> absolute,
            Function<T, String> unit, Function<T, String> scope) {
        this.annotationType = annotationType;
        this.nameType = nameType;
        this.name = name;
        this.tags = tags;
        this.description = description;
        this.absolute = absolute;
        this.unit = unit;
        this.scope = scope;
    }

    public Class<T> annotationType() {
        return annotationType;
    }

    /**
     * If this {@link AnnotationReader} reads {@link Metric} {@link Annotation} it can be associated with different
     * metric class name type using the {@link AnnotationReader}.
     *
     * @param nameType String representing the type class of the metric
     * @return A new {@link AnnotationReader} using the provided
     * @throws IllegalStateException In case this method is called on {@link AnnotationReader} that is not reading
     *                               {@link Metric} {@link Annotation}.
     */
    public AnnotationReader<T> asType(String nameType) {
        if (this.annotationType != Metric.class) {
            throw new IllegalStateException("Only Metric reader can be typed!");
        }
        return new AnnotationReader<>(annotationType, nameType, name, tags, description, absolute, unit, scope);
    }

    /**
     * Infers the {@link org.eclipse.microprofile.metrics.Metric} {@link Class},  from the provided generic Type
     *
     * @param genericType the actual type of the {@link org.eclipse.microprofile.metrics.Metric} as declared by a
     *                    {@link Member} or {@link Parameter}.
     * @return A new {@link AnnotationReader} which uses the inferred
     * @throws IllegalArgumentException in case the given type does not implement any of the known metric types.
     */
    private AnnotationReader<T> asType(Type genericType) {
        Class<?> type = (Class<?>) (genericType instanceof Class
                ? genericType
                : ((java.lang.reflect.ParameterizedType) genericType).getRawType());
        return asType(type.getName());
    }

    private AnnotationReader<T> asAutoType(Type genericType) {
        return annotationType == Metric.class ? asType(genericType) : this;
    }


    /**
     * Returns the effective annotation for the provided bean and element.
     *
     * @param bean    type of the bean that declared the provided element
     * @param element a {@link AnnotatedElement} <b>possibly</b> annotated with this {@link AnnotationReader}'s
     *                {@link #annotationType()}
     * @return the effective {@link Annotation}, or {@code null}. The element's annotations take precedence over the
     *         bean's annotations.
     */
    public <E extends Member & AnnotatedElement> T annotation(Class<?> bean, E element) {
        try {
            return compute(bean, element, Function.identity(), Function.identity());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Reads the effective {@link Annotation} for the provided {@link InjectionPoint}.
     *
     * @param point source {@link InjectionPoint} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return the effective annotation for the provided {@link InjectionPoint}, never {@code null}
     * @throws IllegalArgumentException In case the provided {@link InjectionPoint} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public T annotation(InjectionPoint point) {
        return compute(point, (annotation, name) -> annotation);
    }

    /**
     * Checks if this {@link AnnotationReader}'s {@link #annotationType()} is present either at the provided
     * {@link AnnotatedElement} or the provided bean {@link Class}.
     *
     * @param bean    type of the bean that declared the provided element
     * @param element a {@link AnnotatedElement} <b>possibly</b> annotated with this {@link AnnotationReader}'s
     *                {@link #annotationType()}
     * @return true, if provided element or bean are annotated with this {@link AnnotationReader}'s
     *         {@link #annotationType()}, else false.
     */
    public <E extends Member & AnnotatedElement> boolean isPresent(Class<?> bean, E element) {
        return nameType == GAUGE.nameType
                ? element instanceof Method && element.isAnnotationPresent(annotationType)
                : annotation(bean, element) != null;
    }

    /**
     * Returns the metric name as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return name value of the provided source annotation
     */
    public String name(T annotation) {
        return name.apply(annotation);
    }

    /**
     * Returns the metric name as defined by the MP specification for the annotation situation at hand for the provided
     * {@link InjectionPoint}. This does take into account that annotations might have been added or removed at runtime.
     *
     * @param point source {@link InjectionPoint} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link InjectionPoint} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public String name(InjectionPoint point) {
        return compute(point, this::name);
    }

    /**
     * Returns the metric name as defined by the MP specification for the annotation situation at hand for the provided
     * {@link AnnotatedMember}. This does take into account that annotations might have been added or removed at runtime.
     *
     * @param member source {@link AnnotatedMember} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link AnnotatedMember} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public String name(AnnotatedMember<?> member) {
        return compute(member, this::name);
    }

    /**
     * Reads the effective name for the provided bean {@link Class} and {@link AnnotatedElement}. Either bean or element
     * must have this {@link AnnotationReader}'s {@link Annotation}.
     *
     * @param bean    type of the bean that declared the provided element <b>possibly</b> annotated with this
     *                {@link AnnotationReader}'s {@link #annotationType()}
     * @param element a {@link AnnotatedElement} <b>possibly</b> annotated with this {@link AnnotationReader}'s
     *                {@link #annotationType()}
     * @return full metric name as required by the MP specification
     * @throws IllegalArgumentException In case neither the {@link AnnotatedElement} or the {@link Class} isn't
     *                                  annotated with this {@link AnnotationReader}'s {@link Annotation}.
     */
    public <E extends Member & AnnotatedElement> String name(Class<?> bean, E element) {
        return compute(bean, element, this::name);
    }

    private String name(@SuppressWarnings("unused") T annotation, String name) {
        return name; // used as method-ref-lambda
    }

    /**
     * Returns the metric tags as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return tags value of the provided source annotation
     */
    public Tag[] tags(T annotation) {
        return tagsFromString(tags.apply(annotation));
    }

    /**
     * Returns the {@link MetricID} as defined by the provided {@link Annotation}'s name and tags attributes.
     *
     * @param annotation source annotation to read, not {@code null}
     * @return {@link MetricID} value of the provided source annotation
     */
    public MetricID metricID(T annotation) {
        return new MetricID(name(annotation), tags(annotation));
    }

    /**
     * Returns the metric {@link MetricID} as defined by the MP specification for the annotation situation at hand for
     * the provided {@link InjectionPoint}. This does take into account that annotations might have been added or
     * removed at runtime.
     *
     * @param point source {@link InjectionPoint} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return {@link MetricID} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link InjectionPoint} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public MetricID metricID(InjectionPoint point) {
        return compute(point, this::metricID);
    }

    /**
     * Returns the metric {@link MetricID} as defined by the MP specification for the annotation situation at hand for
     * the provided {@link InjectionPoint}. This does take into account that annotations might have been added or
     * removed at runtime.
     *
     * @param point source {@link AnnotatedMember} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return {@link MetricID} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link AnnotatedMember} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public MetricID metricID(AnnotatedMember<?> member) {
        return compute(member, this::metricID);
    }

    /**
     * Reads the effective {@link MetricID} for the provided bean {@link Class} and {@link AnnotatedElement}. Either
     * bean or element must have this {@link AnnotationReader}'s {@link Annotation}.
     *
     * @param bean    type of the bean that declared the provided element <b>possibly</b> annotated with this
     *                {@link AnnotationReader}'s {@link #annotationType()}
     * @param element a {@link AnnotatedElement} <b>possibly</b> annotated with this {@link AnnotationReader}'s
     *                {@link #annotationType()}
     * @return {@link MetricID} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case neither the {@link AnnotatedElement} or the {@link Class} isn't
     *                                  annotated with this {@link AnnotationReader}'s {@link Annotation}.
     */
    public <E extends Member & AnnotatedElement> MetricID metricID(Class<?> bean, E element) {
        return compute(bean, element, this::metricID);
    }

    private MetricID metricID(T annotation, String name) {
        return new MetricID(name, tags(annotation));
    }

    /**
     * Returns the metric description as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return description of the provided source annotation
     */
    public String description(T annotation) {
        return description.apply(annotation);
    }

    /**
     * Returns the metric unit as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return unit of the provided source annotation
     */
    public String unit(T annotation) {
        return unit.apply(annotation);
    }

    /**
     * Returns the scope as defined by the provided {@Link Annotation}
     * @param annotation source annotation to read, not {@code null}
     * @return scope of the provided source annotation
     */
    public String scope(T annotation){
        return scope.apply(annotation);
    }

    /**
     * Returns the metric absolute flag as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return absolute flag of the provided source annotation
     */
    public boolean absolute(T annotation) {
        return absolute.test(annotation);
    }

    /**
     * Returns the full {@link Metadata} as defined by the provided {@link Annotation}
     *
     * @param annotation source annotation to read, not {@code null}
     * @return {@link Metadata} of the provided source annotation
     */
    public Metadata metadata(T annotation) {
        return metadata(annotation, name(annotation));
    }

    /**
     * Returns the metric {@link Metadata} as defined by the MP specification for the annotation situation at hand for
     * the provided {@link InjectionPoint}. This does take into account that annotations might have been added or
     * removed at runtime.
     *
     * @param point source {@link InjectionPoint} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return {@link Metadata} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link InjectionPoint} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public Metadata metadata(InjectionPoint point) {
        AnnotationReader<T> reader = asAutoType(point.getType());
        return reader.compute(point, reader::metadata);
    }

    /**
     * Returns the metric {@link Metadata} as defined by the MP specification for the annotation situation at hand for
     * the provided {@link InjectionPoint}. This does take into account that annotations might have been added or
     * removed at runtime.
     *
     * @param point source {@link AnnotatedMember} for an annotated element having this {@link AnnotationReader}'s
     *              {@link #annotationType()}, not {@code null}
     * @return {@link Metadata} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case the provided {@link AnnotatedMember} isn't effectively annotated with
     *                                  this {@link AnnotationReader}'s {@link Annotation}.
     */
    public Metadata metadata(AnnotatedMember<?> member) {
        AnnotationReader<T> reader = asAutoType(member.getBaseType());
        return reader.compute(member, reader::metadata);
    }

    /**
     * Reads the effective {@link Metadata} for the provided bean {@link Class} and {@link AnnotatedElement}. Either
     * bean or element must have this {@link AnnotationReader}'s {@link Annotation}.
     *
     * @param bean    type of the bean that declared the provided element <b>possibly</b> annotated with this
     *                {@link AnnotationReader}'s {@link #annotationType()}
     * @param element a {@link AnnotatedElement} <b>possibly</b> annotated with this {@link AnnotationReader}'s
     *                {@link #annotationType()}
     * @return {@link Metadata} with full metric name as required by the MP specification
     * @throws IllegalArgumentException In case neither the {@link AnnotatedElement} or the {@link Class} isn't
     *                                  annotated with this {@link AnnotationReader}'s {@link Annotation}.
     */
    public <E extends Member & AnnotatedElement> Metadata metadata(Class<?> bean, E element) {
        AnnotationReader<T> reader = this;
        if (element instanceof Method) {
            reader = asAutoType(((Method) element).getGenericReturnType());
        } else if (element instanceof Field) {
            reader = asAutoType(((Field) element).getGenericType());
        }
        return reader.compute(bean, element, reader::metadata);
    }

    private Metadata metadata(T annotation, String name) {
        return Metadata.builder()
                .withName(name)
                .withDescription(description(annotation))
                .withUnit(unit(annotation))
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AnnotationReader && annotationType == ((AnnotationReader<?>) obj).annotationType;
    }

    @Override
    public int hashCode() {
        return annotationType.hashCode();
    }

    @Override
    public String toString() {
        return annotationType.toString();
    }

    /**
     * Checks if an {@link Annotation} does not provide any information beyond the required name and tags.
     *
     * @param annotation source annotation to read, not {@code null}
     * @return true, of no property is set to a value that would require using {@link Metadata} when registering, else
     *         false.
     */
    public boolean isReference(T annotation) {
        return unit(annotation).equals(MetricUnits.NONE)
               && description(annotation).isEmpty();
    }

    /**
     * Resolves the {@link org.eclipse.microprofile.metrics.Metric} referred to by the provided {@link InjectionPoint}.
     * If it does not exist, the metric is created. Lookup and creation are one atomic operation. Depending on the
     * provided information in the effective {@link Annotation} for the provided {@link InjectionPoint} the metric is
     * resolved or registered using {@link Metadata}, name and {@link Tag}s or just its name.
     *
     * A {@link org.eclipse.microprofile.metrics.Gauge} can only be resolved, not created.
     *
     * @param point    source {@link InjectionPoint} for an annotated element having this {@link AnnotationReader}'s
     *                 {@link #annotationType()}, not {@code null}
     * @param metric   type of the {@link org.eclipse.microprofile.metrics.Metric} to find or create, not {@code null}
     * @param registry {@link MetricRegistry} to use, not {@code null}
     * @return the resolved or registered metric, or {@code null} if a {@link org.eclipse.microprofile.metrics.Gauge}
     *         did not exist
     */
    public <M extends org.eclipse.microprofile.metrics.Metric> M getOrRegister(InjectionPoint point, Class<M> metric, MetricRegistry registry) {
        T annotation = null;
        try {
            annotation = annotation(point);
        } catch (IllegalArgumentException ex) {
            // there was no annotation
            String name = MetricRegistry.name(point.getMember().getDeclaringClass().getCanonicalName(), localName(point.getMember()));
            return MetricUtils.getOrRegisterByName(registry, metric, name);
        }
        if (isReference(annotation)) {
            return MetricUtils.getOrRegisterByNameAndTags(registry, metric, name(point), tags(annotation));
        }
        return MetricUtils.getOrRegisterByMetadataAndTags(registry, metric, metadata(point), tags(annotation));
    }

    private <R> R compute(InjectionPoint point, BiFunction<T, String, R> func) {
        Annotated annotated = point.getAnnotated();
        if (annotated instanceof AnnotatedMember) {
            return compute((AnnotatedMember<?>) annotated, func);
        }
        if (annotated instanceof AnnotatedParameter) {
            return compute(point, (AnnotatedParameter<?>) annotated, func);
        }
        throw new IllegalArgumentException("Unable to retrieve data for injection point [" + point
                + "], only members and parameters are supported");
    }

    private <R> R compute(InjectionPoint point, AnnotatedParameter<?> parameter, BiFunction<T, String, R> func) {
        //NB: This is a workaround as arquillians InjectionPoint implementation for parameters does return null for getJavaParameter
        Executable annotated = (Executable) point.getMember();
        Member member = new MemberParameter(annotated.getParameters()[parameter.getPosition()]);
        return compute(member.getDeclaringClass(), member, parameter::getAnnotation, func);
    }

    private <R> R compute(AnnotatedMember<?> member, BiFunction<T, String, R> func) {
        Member jmember = ((AnnotatedMember<?>) member).getJavaMember();
        return compute(jmember.getDeclaringClass(), jmember, member::getAnnotation, func);
    }

    private <E extends AnnotatedElement & Member, R> R compute(Class<?> bean, E element, BiFunction<T, String, R> func) {
        return compute(bean, element, element::getAnnotation, func);
    }

    private <R> R compute(Class<?> bean, Member member, Function<Class<T>, T> element, BiFunction<T, String, R> func) {
        return compute(bean, member, element,
                annotation -> func.apply(annotation, namedOnElementLevel(annotation, member)),
                annotation -> func.apply(annotation, namedOnClassLevel(annotation, bean, member)));
    }

    private <E extends AnnotatedElement & Member, R> R compute(Class<?> bean, E element, Function<T, R> onElement,
            Function<T, R> onClass) {
        return compute(bean, element, element::getAnnotation, onElement, onClass);
    }

    private <R> R compute(Class<?> bean, Member member, Function<Class<T>, T> element, Function<T, R> onElement,
            Function<T, R> onClass) {
        T annotation = element.apply(annotationType);
        if (annotation != null) {
            return onElement.apply(annotation);
        }
        if (bean.isAnnotationPresent(annotationType)) {
            return onClass.apply(bean.getAnnotation(annotationType));
        }
        for (Annotation a : bean.getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(Stereotype.class) && a.annotationType().isAnnotationPresent(annotationType)) {
                return onClass.apply(a.annotationType().getAnnotation(annotationType));
            }
        }
        if (bean.getSuperclass() != null) {
            return compute(bean.getSuperclass(), member, element, onElement, onClass);
        }
        throw illegal(bean, member);
    }

    private String namedOnElementLevel(T annotation, Member member) {
        String localName = name(annotation);
        if (localName.isEmpty()) {
            localName = localName(member);
        }
        return absolute(annotation)
                ? localName
                : MetricRegistry.name(member.getDeclaringClass().getCanonicalName(), localName);
    }

    private String namedOnClassLevel(T annotation, Class<?> bean, Member member) {
        String context = name(annotation);
        if (context.isEmpty()) {
            context = absolute(annotation) ? bean.getSimpleName() : bean.getCanonicalName();
        } else if (!absolute(annotation)) {
            context = MetricRegistry.name(bean.getPackage().getName(), context);
        }
        return MetricRegistry.name(context, localName(member));
    }

    private static String localName(Member member) {
        return member instanceof Constructor ? member.getDeclaringClass().getSimpleName() : member.getName();
    }

    private IllegalArgumentException illegal(Class<?> bean, Member member) {
        return new IllegalArgumentException("Neither given member " + member + "nor the given bean " + bean
                + " are annotated with " + annotationType);
    }

    /**
     * A simple wrapper that lets us pass a {@link Parameter} as if it is a {@link Member}
     * so that rest of the code can use {@link Member} as common abstraction.
     */
    private static final class MemberParameter implements Member {

        private final Parameter param;

        MemberParameter(Parameter param) {
            this.param = param;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return param.getDeclaringExecutable().getDeclaringClass();
        }

        @Override
        public String getName() {
            return param.getName();
        }

        @Override
        public int getModifiers() {
            return param.getModifiers();
        }

        @Override
        public boolean isSynthetic() {
            return param.isSynthetic();
        }

        @Override
        public String toString() {
            return param.toString();
        }
    }

    public static Tag[] tagsFromString(String[] tags) {
        if (tags == null || tags.length == 0) {
            return new Tag[0];
        }
        return asList(tags).stream().map(AnnotationReader::tagFromString).toArray(Tag[]::new);
    }


    private static Tag tagFromString(String tag) {
        int splitIndex = tag.indexOf('=');
        if (splitIndex == -1) {
            throw new IllegalArgumentException("invalid tag: " + tag + ", tags must be in the form key=value");
        }
        return new Tag(tag.substring(0, splitIndex), tag.substring(splitIndex + 1));
    }
}
