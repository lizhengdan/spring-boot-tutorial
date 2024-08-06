package com.mhkj;

import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class QuartzController {

    @Autowired
    private Scheduler scheduler;

    @GetMapping("/add")
    public String addTimeJob(String job, String name, String cron) {
        Map<String, Object> param = new HashMap<>(2);
        param.put("name", name);
        QuartzUtils.createJob(scheduler, TimeJob.class, job, "def", cron, param);
        return "OK";
    }

    @GetMapping("/pause")
    public String pauseJob(String job) {
        QuartzUtils.pauseJob(scheduler, job, "def");
        return "OK";
    }

    @GetMapping("/resume")
    public String resumeJob(String job) {
        QuartzUtils.resumeJob(scheduler, job, "def");
        return "OK";
    }

    @GetMapping("/update")
    public String updateJob(String job, String cron) {
        QuartzUtils.refreshJob(scheduler, job, "def", cron);
        return "OK";
    }

}
