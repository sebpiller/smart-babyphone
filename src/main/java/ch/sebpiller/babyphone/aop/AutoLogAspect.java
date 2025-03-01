package ch.sebpiller.babyphone.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1_000)
public class AutoLogAspect extends AbstractBaseAspectDefinition {
    private static final String ARGS_HIDDEN = "-hidden-";
    private static final String LOG_ENTERING = ">> entering {}.{}({})";
    private static final String LOG_ENTERING_DEPRECATED = ">> entering !!DEPRECATED!! {}.{}({})";
    private static final String LOG_EXITING = "<< exiting {}.{}({}) {}";
    private static final String LOG_EXCEPTION = "!! exception in {}.{}({}) {}: {}";

    @Pointcut("within(@AutoLog *)")
    public void taggedAutoLog() {
    }

    @Pointcut("publicMethod() && taggedAutoLog()")
    public void selectedForAutoLog() {
    }

    @Around("selectedForAutoLog()")
    public Object autoLog(ProceedingJoinPoint pjp) throws Throwable {
        var start = System.currentTimeMillis();
        var method = ((MethodSignature) pjp.getSignature()).getMethod();
        var clazz = method.getDeclaringClass();
        var logger = LoggerFactory.getLogger(clazz);

        var autoLog = getAnnotation(clazz, method, AutoLog.class);
        Throwable error = null;
        Object result = null;

        try {
            doBeforeCall(pjp, autoLog, logger, clazz, method);
            result = pjp.proceed();
        } catch (Throwable e) {
            error = e;
            doCatch(pjp, autoLog, logger, clazz, method, e);
            throw e;
        } finally {
            doFinally(pjp, autoLog, logger, clazz, method, result, error, System.currentTimeMillis() - start);
        }

        return result;
    }


    private void doBeforeCall(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method) {
        if (autoLog.entering() && logger.isDebugEnabled()) {
            var deprecated = findAnnotation(clazz, method, Deprecated.class).isPresent();
            logger.debug(
                    deprecated ? LOG_ENTERING_DEPRECATED : LOG_ENTERING,
                    clazz.getSimpleName(),
                    method.getName(),
                    autoLog.printArguments() ? pjp.getArgs() : ARGS_HIDDEN
            );
        }
    }

    private void doCatch(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method, Throwable error) {
        if (autoLog.exception() && logger.isDebugEnabled()) {
            logger.debug(LOG_EXCEPTION,
                    clazz.getSimpleName(),
                    method.getName(),
                    autoLog.printArguments() ? pjp.getArgs() : ARGS_HIDDEN,
                    error.getClass().getName(),
                    error.getMessage()
            );
            if (autoLog.showStackTrace()) {
                logger.debug("Detail:", error);
            }
        }
    }

    private void doFinally(ProceedingJoinPoint pjp, AutoLog autoLog, Logger logger, Class<?> clazz, Method method, Object result, Throwable error, Long timeTook) {
        if (autoLog.exiting() && logger.isDebugEnabled()) {
            String s;
            if (error != null) {
                s = "(error encountered: " + error.getClass() + ")";
            } else if (Void.TYPE.equals(method.getReturnType())) {
                s = "(void method)";
            } else {
                var res = (result == null ? "" : "non ") + "null result";
                s = "(returning " + (autoLog.printReturnValue() ? result : res) + ")";
            }

            if (autoLog.measureExecutionTime()) {
                s += " - duration: " + timeTook + "ms";
            }

            logger.debug(LOG_EXITING,
                    clazz.getSimpleName(),
                    method.getName(),
                    autoLog.printArguments() ? pjp.getArgs() : ARGS_HIDDEN,
                    s
            );
        }
    }
}
