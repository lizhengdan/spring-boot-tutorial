整合ElasticJob之快速入门
------------------------

写在前面的文章

- [第三十二章：配置定时任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter32)
- [第三十三章：整合Quartz之最简配置](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter33)
- [第三十四章：整合Quartz之实现增删查改动态管理任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter34)
- [第三十五章：整合Quartz之基于数据库动态管理任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter35)

### 相关知识

#### 什么是 ElasticJob

ElasticJob 是当当网开源的一个分布式调度解决方案，由两个相互独立的子项目 `Elastic-Job-Lite` 和 `Elastic-Job-Cloud` 组成。

Elastic-Job-Lite 定位为轻量级无中心化解决方案，使用jar包的形式提供分布式任务的协调服务；
Elastic-Job-Cloud 采用自研 Mesos Framework 的解决方案，额外提供资源治理、应用分发以及进程隔离等功能。

#### ElasticJob 特性

- 分布式调度协调
- 弹性扩容缩容
- 失效转移
- 错过执行作业重触发
- 作业分片一致性，保证同一分片在分布式环境中仅一个执行实例
- 自诊断并修复分布式不稳定造成的问题
- 支持并行调度
- 支持作业生命周期操作
- 丰富的作业类型
- Spring整合以及命名空间提供
- 运维平台

#### 官网资料

Elastic-Job官网地址：http://elasticjob.io/index_zh.html
Elastic-Job-Lite官方文档地址：http://elasticjob.io/docs/elastic-job-lite/00-overview/intro/

### 目标

整合 ElasticJob，实现定时任务打印输出

### 准备工作

#### 安装 Zookeeper

使用 Docker 进行简单安装

##### 拉取镜像

```
docker pull zookeeper
```

##### 启动容器

```
docker run -d -p 2181:2181 --name zookeeper --restart always zookeeper
```

### 操作步骤

#### 添加依赖

引入 Spring Boot Starter 父工程

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.0.5.RELEASE</version>
</parent>
```

添加 `elastic-job-lite` 的依赖，本文使用的版本是 `2.1.5`，添加后的整体依赖如下

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
  
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>com.dangdang</groupId>
        <artifactId>elastic-job-lite-core</artifactId>
        <version>2.1.5</version>
    </dependency>

    <dependency>
        <groupId>com.dangdang</groupId>
        <artifactId>elastic-job-lite-spring</artifactId>
        <version>2.1.5</version>
    </dependency>
</dependencies>
```

#### 配置

因为 SpringBoot 并没有提供 ElasticJob 的相关 starter，所以配置参数需要我们自行定义

```yaml
zookeeper:
  url: 127.0.0.1:2181
  namespace: testElasticJob
```

#### 编码

##### 编写定时任务执行类

需要实现 `SimpleJob` 接口

```java
public class ExampleJob implements SimpleJob {

    @Override
    public void execute(ShardingContext shardingContext) {
        System.out.println(String.format("Thread ID: %s, 作业分片总数: %s, " +
                        "当前分片项: %s.当前参数: %s," +
                        "作业名称: %s.作业自定义参数: %s",
                Thread.currentThread().getId(),
                shardingContext.getShardingTotalCount(),
                shardingContext.getShardingItem(),
                shardingContext.getShardingParameter(),
                shardingContext.getJobName(),
                shardingContext.getJobParameter()
        ));
    }
}
```

##### 注册 Zookeeper

使用前面的配置进行注册

```java
@Configuration
protected class JobRegistryCenterConfig {

    @Bean(initMethod = "init")
    public ZookeeperRegistryCenter regCenter(@Value("${zookeeper.url}") final String serverList,
                                             @Value("${zookeeper.namespace}") final String namespace) {
        return new ZookeeperRegistryCenter(new ZookeeperConfiguration(serverList, namespace));
    }
}
```

##### 注册定时任务

使用前面创建的 `ExampleJob` 进行注册 `JobScheduler`，并执行其 `init` 方法。

```java
@Configuration
@Import(JobRegistryCenterConfig.class)
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
        SimpleJobConfiguration simpleJobConfig = 
                new SimpleJobConfiguration(simpleCoreConfig, jobClass.getCanonicalName());
        // 定义Lite作业根配置
        return LiteJobConfiguration.newBuilder(simpleJobConfig).overwrite(true).build();
    }
}
```

ElasticJobProperties 类用于支持使用配置文件进行配置，因为使用了默认值，所以在配置文件中并没有进行相关配置

```java
@Data
@ConfigurationProperties(prefix = "elastic-job")
public class ElasticJobProperties {
    // cron表达式，用于控制作业触发时间
    private String cron = "0/5 * * * * ?";
    // 总分片数
    private int shardingTotalCount = 3;
    // 分片序列号和参数用等号分隔，多个键值对用逗号分隔，
    // 分片序列号从0开始，不可大于或等于作业分片总数
    private String shardingItemParameters = "0=A,1=B,2=C";
    // 作业自定义参数，可通过传递该参数为作业调度的业务方法传参，用于实现带参数的作业
    private String jobParameters = "parameter";
}
```

#### 验证

启动项目，查看日志

```
// 第一次
Thread ID: 92, 作业分片总数: 3, 当前分片项: 0.当前参数: A,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter
Thread ID: 93, 作业分片总数: 3, 当前分片项: 1.当前参数: B,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter
Thread ID: 94, 作业分片总数: 3, 当前分片项: 2.当前参数: C,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter
// 第二次
Thread ID: 95, 作业分片总数: 3, 当前分片项: 0.当前参数: A,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter
Thread ID: 96, 作业分片总数: 3, 当前分片项: 1.当前参数: B,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter
Thread ID: 97, 作业分片总数: 3, 当前分片项: 2.当前参数: C,作业名称: com.mhkj.ExampleJob.作业自定义参数: parameter

```

### 源码地址

本章源码 : [https://github.com/lizhengdan/spring-boot-tutorial.git](https://github.com/lizhengdan/spring-boot-tutorial.git)

### 结束语

将任务加入 Elastic-Job 的步骤就是

```java
// 第一步：初始化注册中心
CoordinatorRegistryCenter regCenter = ...;
// 第二步：初始化作业配置
LiteJobConfiguration liteJobConfig = ...;
// 第三步：注册执行
new JobScheduler(regCenter, liteJobConfig).init(); 
```

### 扩展

#### 开启事件追踪

`Elastic-Job-Lite` 在提供了 `JobEventConfiguration`，目前支持数据库方式配置，操作步骤如下：

```java
// 初始化数据源
DataSource dataSource = ...;
// 定义日志数据库事件溯源配置
JobEventConfiguration jobEventRdbConfig = new JobEventRdbConfiguration(dataSource);
// 初始化注册中心
CoordinatorRegistryCenter regCenter = ...;
// 初始化作业配置
LiteJobConfiguration liteJobConfig = ...;
new JobScheduler(regCenter, liteJobConfig, jobEventRdbConfig).init(); 
```

需要配置一个数据源，配置之后，对应库自动创建 `JOB_EXECUTION_LOG` 和 `JOB_STATUS_TRACE_LOG` 两张表以及若干索引。

#### 开启任务监听

监听器分为每台作业节点均执行和分布式场景中仅单一节点执行2种。

##### 为每台作业节点均执行的监听

若作业处理作业服务器的文件，处理完成后删除文件，可考虑使用每个节点均执行清理任务。
此类型任务实现简单，且无需考虑全局分布式任务是否完成，请尽量使用此类型监听器。

- 定义监听器

```java
public class MyElasticJobListener implements ElasticJobListener {
  
    @Override
    public void beforeJobExecuted(ShardingContexts shardingContexts) {
        // do something ...
    }
  
    @Override
    public void afterJobExecuted(ShardingContexts shardingContexts) {
        // do something ...
    }
}
```

- 将监听器作为参数传入JobScheduler

```java
// 初始化注册中心
CoordinatorRegistryCenter regCenter = ...;
// 初始化作业配置
LiteJobConfiguration liteJobConfig = ...;
new JobScheduler(regCenter, liteJobConfig, new MyElasticJobListener()).init(); 
```

##### 分布式场景中仅单一节点执行的监听

若作业处理数据库数据，处理完成后只需一个节点完成数据清理任务即可。
此类型任务处理复杂，需同步分布式环境下作业的状态同步，提供了超时设置来避免作业不同步导致的死锁，请谨慎使用。

- 定义监听器

```java
public class DistributeOnceElasticJobListener extends AbstractDistributeOnceElasticJobListener {
  
    public DistributeOnceElasticJobListener(long startTimeoutMills, long completeTimeoutMills) {
        super(startTimeoutMills, completeTimeoutMills);
    }
  
    @Override
    public void doBeforeJobExecutedAtLastStarted(ShardingContexts shardingContexts) {
        // do something ...
    }
  
    @Override
    public void doAfterJobExecutedAtLastCompleted(ShardingContexts shardingContexts) {
        // do something ...
    }
}
```

- 将监听器作为参数传入JobScheduler

```java
// 初始化注册中心
CoordinatorRegistryCenter regCenter = ...;
// 初始化作业配置
LiteJobConfiguration liteJobConfig = ...;
new JobScheduler(regCenter, liteJobConfig, new DistributeOnceElasticJobListener(5000L, 10000L)).init(); 
```
