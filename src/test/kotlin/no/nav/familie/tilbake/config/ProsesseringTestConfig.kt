package no.nav.familie.tilbake.config

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Profile("integrasjonstest")
@Configuration
class ProsesseringTestConfig {
    @Bean
    fun prosesseringShutdownDisabler(
        threadPoolTaskScheduler: ThreadPoolTaskScheduler,
        @Qualifier("taskExecutor") taskExecutor: TaskExecutor,
    ): SmartInitializingSingleton =
        SmartInitializingSingleton {
            threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(false)
            threadPoolTaskScheduler.setAwaitTerminationSeconds(0)
            val executor = taskExecutor as ThreadPoolTaskExecutor
            executor.setWaitForTasksToCompleteOnShutdown(false)
            executor.setAwaitTerminationSeconds(0)
        }
}
