package com.mhkj;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

public class ElasticJobUtils {

    public static void addJob(ZookeeperRegistryCenter registryCenter,
                              SimpleJob job, ElasticJobProperties properties) {
        new SpringJobScheduler(job, registryCenter,
                getLiteJobConfiguration(job.getClass(),
                        properties.getCron(), properties.getShardingTotalCount(),
                        properties.getShardingItemParameters(), properties.getJobParameters())).init();
    }

    private static LiteJobConfiguration getLiteJobConfiguration(final Class<? extends SimpleJob> jobClass,
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
