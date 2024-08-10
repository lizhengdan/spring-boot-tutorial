整合Quartz之基于数据库动态管理任务
----------------------------------

在前面的文章中，我们已实现了基于内存动态管理 Quartz 任务

- [第三十二章：配置定时任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter32)
- [第三十三章：整合Quartz之最简配置](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter33)
- [第三十四章：整合Quartz之实现增删查改动态管理任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter34)

本章将实现对任务信息及状态持久化至数据库。

### 目标

整合 Quartz，基于 mysql 实现对 Quartz 任务增删查改、暂停及恢复

### 思路

任务管理完全委托 Scheduler 类进行操作，所以使用内存进行存储与使用数据库进行存储，只是在存储策略上的一个改变，
这并不影响我们的操作，所以，本章与上一章中的操作部分代码，完全一样。

SpringBoot 提供的 QuartzAutoConfiguration 已经为我们进行了封装，所以只需要设置数据源并修改 `JobStoreType` 参数为 `jdbc` 即可

```java
@Bean
public SchedulerFactoryBeanCustomizer dataSourceCustomizer(
        QuartzProperties properties, DataSource dataSource,
        @QuartzDataSource ObjectProvider<DataSource> quartzDataSource,
        ObjectProvider<PlatformTransactionManager> transactionManager) {
    return (schedulerFactoryBean) -> {
        if (properties.getJobStoreType() == JobStoreType.JDBC) {
            DataSource dataSourceToUse = getDataSource(dataSource,
                    quartzDataSource);
            schedulerFactoryBean.setDataSource(dataSourceToUse);
            PlatformTransactionManager txManager = transactionManager
                    .getIfUnique();
            if (txManager != null) {
                schedulerFactoryBean.setTransactionManager(txManager);
            }
        }
    };
}
```

### 准备工作

#### 初始化数据库

Quartz 已经为不同数据库准备了初始化脚本，脚本路径为 `org/quartz/impl/dbcjobstore`，
脚本名称为 `tables_@@platform@@.sql`，其中 `platform` 会使用数据库名称替换。

因为本章使用 mysql 数据库作为示例，搜索 `tables_mysql_innodb.sql` 文件，在数据库中执行。

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

添加 `spring-boot-starter-quartz`、`jpa` 及 `mysql` 的依赖，添加后的整体依赖如下

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

    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>
```

#### 配置

- 配置数据源
- 配置 Jpa
- 配置 Quartz，job-store-type 设置为 jdbc，表示使用数据库进行数据存储

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/test?characterEncoding=utf8&useSSL=false
    username: test
    password: 123456
    platform: mysql
  jpa:
    database: mysql
    show-sql: true
    hibernate:
      ddl-auto: update
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: embedded
```

#### 编码

##### 编写定时任务执行类

需要继承 `QuartzJobBean` 类

```java
@Data
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

##### 实现增删查改

对任务的增删查改都是基于 Scheduler 类，SpringBoot 自带的 QuartzAutoConfiguration 对其进行了注册。

> 对任务的增删查改操作使用工具类 QuartzUtils 来进行实现。

```java
@RestController
public class QuartzController {

    @Autowired
    private Scheduler scheduler;

    @GetMapping("/add")
    public String addTimeJob(String job, String name, String cron) throws SchedulerException {
        Map<String, Object> param = new HashMap<>(2);
        param.put("name", name);
        QuartzUtils.createJob(scheduler, TimeJob.class, job, "def", cron, param);
        return "OK";
    }

    @GetMapping("/pause")
    public String pauseJob(String job) throws SchedulerException {
        QuartzUtils.pauseJob(scheduler, job, "def");
        return "OK";
    }

    @GetMapping("/resume")
    public String resumeJob(String job) throws SchedulerException {
        QuartzUtils.resumeJob(scheduler, job, "def");
        return "OK";
    }

    @GetMapping("/update")
    public String updateJob(String job, String cron) throws SchedulerException {
        QuartzUtils.refreshJob(scheduler, job, "def", cron);
        return "OK";
    }

}
```

##### 工具类

```java
public class QuartzUtils {

    public static void createJob(Scheduler scheduler, Class<? extends Job> jobClass,
                                 String jobName, String jobGroup,
                                 String cron, Map<String, Object> param) throws SchedulerException {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .build();
        if (param != null && !param.isEmpty()) {
            param.forEach((key, value) -> jobDetail.getJobDataMap().put(key, value));
        }
        scheduler.scheduleJob(jobDetail, buildCronTrigger(jobName, jobGroup, cron));
    }

    public static void refreshJob(Scheduler scheduler, String jobName, String jobGroup,
                                  String cron) throws SchedulerException {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        scheduler.rescheduleJob(triggerKey, buildCronTrigger(jobName, jobGroup, cron));
    }

    private static Trigger buildCronTrigger(String jobName, String jobGroup, String cron) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
        return TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup).withSchedule(scheduleBuilder)
                .build();
    }

    public static void resumeJob(Scheduler scheduler, String jobName, String jobGroup) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobName, jobGroup));
    }

    public static void pauseJob(Scheduler scheduler, String jobName, String jobGroup) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobName, jobGroup));
    }

    public static void deleteJob(Scheduler scheduler, String jobName, String jobGroup) throws SchedulerException {
        scheduler.unscheduleJob(TriggerKey.triggerKey(jobName, jobGroup));
        scheduler.deleteJob(JobKey.jobKey(jobName, jobGroup));
    }

}
```

#### 验证

##### 创建任务

请求地址 `http://localhost:8080/add?job=job1&name=user&cron=0/5 * * * * ?`，创建一个名称为 job1 的任务，每五秒执行一次
查看日志

```
execute timeJob at 06:14:39: hello user
execute timeJob at 06:14:44: hello user
execute timeJob at 06:14:49: hello user
```

##### 修改任务

请求地址 `http://localhost:8080/add?job=job1&name=user&cron=0/1 * * * * ?`，修改名称为 job1 的任务，变为每秒执行一次
查看日志

```
execute timeJob at 06:14:55: hello user
execute timeJob at 06:14:56: hello user
execute timeJob at 06:14:57: hello user
```

##### 暂停和重启任务

请求地址 `http://localhost:8080/pause?job=job1`，暂停任务job1
请求地址 `http://localhost:8080/resume?job=job1`，重启任务job1

### 源码地址

本章源码 : [https://github.com/lizhengdan/spring-boot-tutorial.git](https://github.com/lizhengdan/spring-boot-tutorial.git)

### 扩展

#### 开启任务监听

- 实现监听器

```java
public class TraceTriggerListener extends TriggerListenerSupport {

    @Override
    public String getName() {
        return "TraceTriggerListener";
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext jobExecutionContext) {
        System.out.println("开始执行任务" + trigger.getKey());
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext jobExecutionContext,
                                Trigger.CompletedExecutionInstruction completedExecutionInstruction) {
        System.out.println("任务成功结束" + trigger.getKey());
    }

}
```

- 注册监听器

```java
@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBeanCustomizer dataSourceCustomizer() {
        return (schedulerFactoryBean) -> {
            schedulerFactoryBean.setGlobalTriggerListeners(new TraceTriggerListener());
        };
    }

}
```
