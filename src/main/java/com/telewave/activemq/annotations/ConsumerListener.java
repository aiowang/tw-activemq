package com.telewave.activemq.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Copyright (c) 天维尔信息科技股份有限公司<br>
 * All Rights Reserved<br>
 * http://www.telewave.com.cn<br>
 *
 * @author wanggj
 * @date 2018年3月20日
 */
@Component
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ConsumerListener {

    public static final int TOPIC = 1;

    public static final int QUEUE = 2;

    String destination() default "";

    int type() default TOPIC;
}
