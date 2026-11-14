package com.yali.business.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class OutboxDispatchConfig {

    /**
     * 任务进入线程池后才领取数据库记录，因此不需要本地排队。
     * 无界队列会把待发事件隐藏在 JVM 内存里，也可能让领取租约过期。
     */
    @Bean("outboxDispatchExecutor")
    public ThreadPoolTaskExecutor outboxDispatchExecutor(
            @Value("${outbox.dispatch.workers:4}") int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("outbox.dispatch.workers 必须大于 0");
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(workers);
        executor.setMaxPoolSize(workers);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("outbox-dispatch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        // 关闭服务时中断正在循环领取的工作者；未完成记录由租约到期后恢复。
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
