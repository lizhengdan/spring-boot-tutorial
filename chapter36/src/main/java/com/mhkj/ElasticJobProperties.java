package com.mhkj;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "elastic-job")
public class ElasticJobProperties {

    private String cron = "0/5 * * * * ?";
    private int shardingTotalCount = 3;
    private String shardingItemParameters = "0=A,1=B,2=C";
    private String jobParameters = "parameter";

}
