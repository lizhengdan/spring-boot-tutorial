package com.mhkj;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import lombok.AllArgsConstructor;
import org.quartz.TriggerListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(ElasticJobProperties.class)
@ConditionalOnExpression("'${zookeeper.url}'.length() > 0")
public class ElasticJobConfig {

    @Configuration
    protected class JobRegistryCenterConfig {

        @Bean(initMethod = "init")
        public ZookeeperRegistryCenter regCenter(@Value("${zookeeper.url}") final String serverList,
                                                 @Value("${zookeeper.namespace}") final String namespace) {
            return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
        }

    }

    @Configuration
    @Import(ElasticJobConfig.JobRegistryCenterConfig.class)
    @AllArgsConstructor
    protected class LiteJobConfig {

        private ElasticJobProperties properties;

        @Bean
        public SimpleJob exampleJob() {
            return new ExampleJob();
        }

        @Bean(initMethod = "init")
        public JobScheduler simpleJobScheduler(ZookeeperRegistryCenter regCenter, SimpleJob exampleJob) {
            return new SpringJobScheduler(exampleJob, regCenter,
                    getLiteJobConfiguration(exampleJob.getClass(),
                            properties.getCron(), properties.getShardingTotalCount(),
                            properties.getShardingItemParameters(), properties.getJobParameters()));
        }

        private LiteJobConfiguration getLiteJobConfiguration(final Class<? extends SimpleJob> jobClass,
                                                             final String cron,
                                                             final int shardingTotalCount,
                                                             final String shardingItemParameters,
                                                             final String jobParameters) {
            // 定义作业核心配置
            JobCoreConfiguration simpleCoreConfig =
                    JobCoreConfiguration.newBuilder(jobClass.getName(), cron, shardingTotalCount).
                    shardingItemParameters(shardingItemParameters).jobParameter(jobParameters).build();
            // 定义SIMPLE类型配置
            SimpleJobConfiguration simpleJobConfig = new SimpleJobConfiguration(simpleCoreConfig, jobClass.getCanonicalName());
            // 定义Lite作业根配置
            return LiteJobConfiguration.newBuilder(simpleJobConfig).overwrite(true).build();
        }
    }
}
