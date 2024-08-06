配置定时任务
---

定时任务的几种实现方式：
 - Timer：Java自带的java.util.Timer类，这个类允许你调度一个java.util.TimerTask任务。使用这种方式可以让你的程序按照某一个频度执行，但不能在指定时间运行。一般用的较少。
 - Quartz：使用Quartz，这是一个功能比较强大的的调度器，可以让你的程序在指定时间执行，也可以按照某一个频度执行，配置起来稍显复杂。
 - Spring Task：Spring3.0以后自带的task，可以将它看成一个轻量级的Quartz，而且使用起来比Quartz简单许多。

### 目标
在 SpringBoot 项目实现每秒打印一次当前时间

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
添加 `spring-boot-starter-web` 的依赖，添加后的整体依赖如下
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
</dependencies>
```
#### 编码
##### 编写启动类
```java
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

##### 编写定时任务调度类
 - 通过 `@EnableScheduling` 注解，开启定时任务调度功能
 - 通过 `@Scheduled` 注解在需要执行的方法上，使用 `cronExpression` 表达式定义定时任务的执行策略

> 文章最后会详细讲解 `cronExpression` 表达式

```java
@EnableScheduling
@Configuration
public class TaskConfig {

    /**
     * 每秒执行一次.
     * cron: 定时任务表达式.
     *
     * 指定：秒，分钟，小时，日期，月份，星期，年（可选）.
     * *：任意.
     */
    @Scheduled(cron="0/1 * * * * *")
    public void clock() {
        System.out.println("time : " + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss")));
    }

}
```

#### 验证
启动项目，查看日志
```
time : 07:23:45
time : 07:23:46
time : 07:23:47
time : 07:23:48
time : 07:23:49
```

### 源码地址
本章源码 : <https://gitee.com/gongm_24/spring-boot-tutorial.git>

### 扩展
#### cronExpression 表达式
##### 表达式结构
```
秒 分钟 小时 日期 月份 星期 年
```
##### 配置说明
字段|允许值|允许的特殊字符
---|---|---
秒|0-59|, - * /
分|0-59|, - * /
小时|0-23|, - * /
日期|1-31|, - * ? / LWC
月份|1-12 或者 JAN-DEC|, - * /
星期|1-7 或者 SUN-SAT|, - * ? / LC #
年（可选）|留空, 1970-2099|, - * /

*特殊字符说明*：

 - 星号(*)

可用在所有字段下，表示对应时间域名的每一个时刻，如*用在分钟字段，表示“每分钟”。

 - 问号(?)

只能用在日期和星期字段，代表无意义的值，比如使用L设定为当月的最后一天，则配置日期配置就没有意义了，可用？作占位符的作用。

 - 减号(-)

表示一个范围，如在日期字段5-10，表示从五号到10号，相当于使用逗号的5,6,7,8,9,10

 - 逗号(,)

表示一个并列有效值，比如在月份字段使用JAN,DEC表示1月和12月

 - 斜杠(/)

x/y表示一个等步长序列，x为起始值，y为增量步长值，如在小时使用1/3相当于1,4,7,10当时用*/y时，相当于0/y

 - L

L(Last)只能在日期和星期字段使用，但意思不同。在日期字段，表示当月最后一天，在星期字段，表示星期六（如果按星期天为一星期的第一天的概念，星期六就是最后一天。如果L在星期字段，且前面有一个整数值X，表示“这个月的最后一个星期X”，比如3L表示某个月的最后一个星期二。

 - W

选择离给定日期最近的工作日（周一至周五）。例如你指定“15W”作为day of month字段的值，就意味着“每个月与15号最近的工作日”。所以，如果15号是周六，则触发器会在14号（周五）触发。如果15号是周日，则触发器会在16号（周一）触发。如果15号是周二，则触发器会在15号（周二）触发。但是，如果你指定“1W”作为day of month字段的值，且1号是周六，则触发器会在3号（周一）触发。quartz不会“跳出”月份的界限。

 - LW组合

在日期字段可以组合使用LW,表示当月最后一个工作日（周一至周五）

 - 井号(#)

只能在星期字段中使用指定每月第几个星期X。例如day of week字段的“6＃3”，就意味着“每月第3个星期五”（day3=星期五，＃3=第三个）；“2＃1”就意味着“每月第1个星期一”；“4＃5”就意味着“每月第5个星期3。需要注意的是“＃5”，如果在当月没有第5个星期三，则触发器不会触发。

 - C

只能在日期和星期字段中使用，表示计划所关联的诶其，如果日期没有被关联，相当于日历中的所有日期，如5C在日期字段相当于5号之后的第一天，1C在日期字段使用相当于星期填后的第一天

##### 示例：
```
0 0 12 * * ?            //每天中午十二点触发
0 15 10 ? * *           //每天早上10：15触发
0 15 10 * * ?           //每天早上10：15触发
0 15 10 * * ? *         //每天早上10：15触发
0 15 10 * * ? 2005      //2005年的每天早上10：15触发
0 * 14 * * ?            //每天从下午2点开始到2点59分每分钟一次触发
0 0/5 14 * * ?          //每天从下午2点开始到2：55分结束每5分钟一次触发
0 0/5 14,18 * * ?       //每天的下午2点至2：55和6点至6点55分两个时间段内每5分钟一次触发
0 0-5 14 * * ?          //每天14:00至14:05每分钟一次触发
0 10,44 14 ? 3 WED      //三月的每周三的14：10和14：44触发
0 15 10 ? * MON-FRI     //每个周一、周二、周三、周四、周五的10：15触发
```