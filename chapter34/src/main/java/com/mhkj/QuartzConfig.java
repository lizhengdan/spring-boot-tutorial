package com.mhkj;

import org.quartz.TriggerListener;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBeanCustomizer dataSourceCustomizer() {
        return (schedulerFactoryBean) -> {
            schedulerFactoryBean.setGlobalTriggerListeners(traceTriggerListener());
        };
    }

    @Bean
    public TriggerListener traceTriggerListener() {
        return new TraceTriggerListener();
    }

}
