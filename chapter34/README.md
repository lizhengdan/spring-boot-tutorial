整合Quartz之实现增删查改动态管理任务
---

在前面的文章中，我们已实现了在 SpringBoot 项目中整合 Quartz 框架执行定时任务

 - [第三十二章：配置定时任务](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter32)
 - [第三十三章：整合Quartz之最简配置](https://gitee.com/gongm_24/spring-boot-tutorial/tree/master/chapter33)

但是需要我们手动编码实现将任务注册到 Quartz 框架，但是在实际项目中，我们需要对定时任务更加动态地控制。
需要可以增删查改，也可以暂停及恢复。

### 目标
整合 Quartz，实现使用 Quartz 定时任务增删查改、暂停及恢复

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
本章源码 : <https://gitee.com/gongm_24/spring-boot-tutorial.git>

### 结束语
本文基于 Quartz 框架提供的 Scheduler 类实现了对定时任务的运态管理，所有的数据和状态都将保存在内存中，这样并不安全。
接下来我们将实现使用数据库持久化任务数据及状态，敬请期待。