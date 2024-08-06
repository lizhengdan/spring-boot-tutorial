package com.mhkj;

import org.quartz.*;

import java.util.Map;

public class QuartzUtils {

    /**
     * 创建任务
     */
    public static void createJob(Scheduler scheduler, Class<? extends Job> jobClass,
                                 String jobName, String jobGroup,
                                 String cronExpression, Map<String, Object> param) {
        JobDetail jobDetail = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .build();
        if (param != null && !param.isEmpty()) {
            param.forEach((key, value) -> jobDetail.getJobDataMap().put(key, value));
        }
        try {
            scheduler.scheduleJob(jobDetail, buildCronTrigger(jobName, jobGroup, cronExpression));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("任务加载失败", e);
        }
    }

    /**
     * 修改任务
     */
    public static void refreshJob(Scheduler scheduler, String jobName, String jobGroup,
                                  String cronExpression) {
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        try {
            scheduler.rescheduleJob(triggerKey, buildCronTrigger(jobName, jobGroup, cronExpression));
        } catch (SchedulerException e) {
            e.printStackTrace();
            throw new RuntimeException("任务更新失败", e);
        }
    }

    private static Trigger buildCronTrigger(String jobName, String jobGroup, String cronExpression) {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression);
        return TriggerBuilder.newTrigger()
                .withIdentity(jobName, jobGroup).withSchedule(scheduleBuilder)
                .build();
    }

    /**
     * 恢复任务
     */
    public static void resumeJob(Scheduler scheduler, String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            scheduler.resumeJob(jobKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("任务恢复失败", e);
        }
    }

    /**
     * 暂停任务
     */
    public static void pauseJob(Scheduler scheduler, String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            scheduler.pauseJob(jobKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("任务恢复失败", e);
        }
    }

    /**
     * 删除任务
     */
    public static void deleteJob(Scheduler scheduler, String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        try {
            scheduler.unscheduleJob(TriggerKey.triggerKey(jobName, jobGroup));
            scheduler.deleteJob(jobKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("任务恢复失败", e);
        }
    }

}
