package com.mhkj;

import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.listeners.TriggerListenerSupport;

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
