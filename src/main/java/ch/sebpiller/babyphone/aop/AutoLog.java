package ch.sebpiller.babyphone.aop;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLog {
    boolean entering() default true;

    boolean exiting() default true;

    boolean exception() default true;

    boolean measureExecutionTime() default true;

    boolean printArguments() default true;

    boolean showStackTrace() default true;

    boolean printReturnValue() default true;

    boolean warnSlowCalls() default false;

    int slowCallThreshold() default 5;
}
