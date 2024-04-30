package com.github.tumbl3w33d;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.TASKS;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.schedule.Schedule;

@Named
@ManagedLifecycle(phase = TASKS)
@Singleton
public class OAuth2ProxyApiTokenInvalidateQuartz extends StateGuardLifecycleSupport {
    private final TaskScheduler taskScheduler;

    private final String taskCron;

    @Inject
    public OAuth2ProxyApiTokenInvalidateQuartz(final TaskScheduler taskScheduler,
            @Named("${nexus.tasks.oauth2-proxy.api-token-invalidate.cron:-0 */5 * * * ?}") final String taskCron) {
        this.taskScheduler = checkNotNull(taskScheduler);
        this.taskCron = checkNotNull(taskCron);
    }

    @Override
    protected void doStart() throws Exception {
        if (!taskScheduler.listsTasks().stream()
                .anyMatch((info) -> OAuth2ProxyApiTokenInvalidateTaskDescriptor.TYPE_ID
                        .equals(info.getConfiguration().getTypeId()))) {
            TaskConfiguration configuration = taskScheduler.createTaskConfigurationInstance(
                    OAuth2ProxyApiTokenInvalidateTaskDescriptor.TYPE_ID);
            Schedule schedule = taskScheduler.getScheduleFactory().cron(new Date(), taskCron);
            taskScheduler.scheduleTask(configuration, schedule);
        }
    }
}
