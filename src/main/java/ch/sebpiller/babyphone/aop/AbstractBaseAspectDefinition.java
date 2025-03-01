package ch.sebpiller.babyphone.aop;

import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

abstract class AbstractBaseAspectDefinition {
    @Pointcut("execution(public * *(..))")
    public void publicMethod() {
    }

    protected <T extends Annotation> T getAnnotation(Class<?> clazz, Method method, Class<T> annot) {
        return findAnnotation(clazz, method, annot)
                .orElseThrow(() -> new IllegalStateException("Annotation " + annot.getSimpleName() + " not found on method " + method.getName()));
    }

    protected <T extends Annotation> Optional<T> findAnnotation(Class<?> clazz, Method method, Class<T> annot) {
        var result = AnnotationUtils.findAnnotation(method, annot);
        if (result != null) {
            return Optional.of(result);
        }

        var cl = clazz;
        while (cl != Object.class) {
            for (var i : cl.getInterfaces()) {
                Method superMethod = null;
                try {
                    superMethod = i.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    // ignore
                }

                if (superMethod != null) {
                    result = AnnotationUtils.findAnnotation(superMethod, annot);
                    if (result != null) {
                        return Optional.of(result);
                    }
                }
            }
            cl = cl.getSuperclass();
        }

        return Optional.ofNullable(AnnotationUtils.findAnnotation(clazz, annot));
    }


}
