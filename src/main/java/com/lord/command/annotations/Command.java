package com.lord.command.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    String name();
    String[] aliases() default {};
    String permission() default "";
    String description() default "";
    String usage() default "";
}