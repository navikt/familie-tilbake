package no.nav.familie.tilbake.config

import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
class SchedulingConfig {
    // Overstyr familie-prosessering sin egen config av task scheduler
    @Bean
    fun schedulerPoolSizeKonfigurerer(
        threadPoolTaskScheduler: ThreadPoolTaskScheduler,
        @Value("\${spring.task.scheduling.pool.size}") poolSize: Int,
    ): SmartInitializingSingleton =
        SmartInitializingSingleton {
            threadPoolTaskScheduler.poolSize = poolSize
        }
}
