整合Quartz之最简配置
---

在前面的文章中，我们已实现了在 SpringBoot 项目中执行定时任务

 - [第三十二章：执行定时任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter32)

但是，自带的定时任务有其局限性，比如在分布式环境中无法协调多节点，就会导致一个任务会在多个节点同时执行。
接下来，我们将实现对开源作业调度框架 Quartz 的整合。

### 相关知识
#### 什么是 Quartz
Quartz 是一个完全由 Java 编写的开源作业调度框架，为在 Java 应用程序中进行作业调度提供了简单却强大的机制。

#### Quartz 的特点
作为一个优秀的开源调度框架，Quartz 具有以下特点：
 - 强大的调度功能，例如支持丰富多样的调度方法，可以满足各种常规及特殊需求
 - 灵活的应用方式，例如支持任务和调度的多种组合方式，支持调度数据的多种存储方式
 - 分布式和集群能力，Terracotta 收购后在原来功能基础上作了进一步提升
 - Quartz 很容易与 Spring 集成实现灵活可配置的调度功能

#### Quartz 相关概念
 - scheduler：任务调度器
 
Quartz 提供了 DirectSchedulerFactory 及 StdSchedulerFactory 工厂类用于创建。通常使用 StdSchedulerFactory 进行创建。

 - trigger：触发器，用于定义任务调度时间规则。

Quartz 中主要提供了四种类型的 trigger：SimpleTrigger，CronTirgger，DateIntervalTrigger，和 NthIncludedDayTrigger。这四种 trigger 可以满足企业应用中的绝大部分需求。

 - job：被调度的任务，一个 job 可以被多个 trigger 关联，但是一个 trigger 只能关联一个 job

job 有两种类型：无状态的（stateless）和有状态的（stateful）。
对于同一个 trigger 来说，有状态的 job 不能被并行执行，只有上一次触发的任务被执行完之后，才能触发下一次执行。
Job 主要有两种属性：volatility 和 durability，
其中 volatility 表示任务是否被持久化到数据库存储，
而 durability 表示在没有 trigger 关联的时候任务是否被保留。
两者都是在值为 true 的时候任务被持久化或保留。

### 目标
简单整合 Quartz，实现使用 Quartz 执行定时任务

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
添加 `spring-boot-starter-quartz` 的依赖，添加后的整体依赖如下
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
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-quartz</artifactId>
    </dependency>
</dependencies>
```
#### 编码
##### 编写定时任务执行类
 - @DisallowConcurrentExecution 用于处理任务并发执行的问题。
 - @PersistJobDataAfterExecution 成功执行了job类的execute方法后（没有发生任何异常），更新JobDetail中JobDataMap的数据，
使得该job（即JobDetail）在下一次执行的时候，JobDataMap中是更新后的数据，而不是更新前的旧数据。
最好与 @DisallowConcurrentExecution 注解同时使用，以防止并发造成 JobDataMap 中存储的数据不确定。

```java
@Data
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class TimeJob extends QuartzJobBean {

    private String name;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        System.out.println("execute timeJob at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")) +
                ": hello " + this.name);
    }

}
```

##### 注册定时任务
 - 注册 `JobDetail`，内部维护了一个 JobDataMap 数据容器与外界进行交互，可以通过 `usingJobData` 方法，为定时任务传递数据
 - 注册 `Trigger`，关联 `JobDetail`，并设置定时任务执行策略

```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
    
    @Bean
    public JobDetail sampleJobDetail() {
        return JobBuilder.newJob(TimeJob.class).withIdentity("timeJob")
                .usingJobData("name", "Quartz").storeDurably().build();
    }

    @Bean
    public Trigger sampleJobTrigger() {
        // 如果需要使用 cronExpression 表达式，则使用 CronScheduleBuilder 进行创建
        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(2).repeatForever();

        return TriggerBuilder.newTrigger().forJob(sampleJobDetail())
                .withIdentity("timeTrigger").withSchedule(scheduleBuilder).build();
    }

}
```

#### 验证
启动项目，查看日志
```
execute timeJob at 08:24:06: hello Quartz
execute timeJob at 08:24:08: hello Quartz
execute timeJob at 08:24:10: hello Quartz
```

### 源码地址
本章源码 : <https://gitee.com/gongm_24/spring-boot-tutorial.git>

### 结束语
本文实现了对 Quartz 框架的基本整合，通过硬编码实现任务及执行策略的注册。
接下来我们将实现动态管理任务及执行策略，包括增删查改、暂停及重启任务，敬请期待。