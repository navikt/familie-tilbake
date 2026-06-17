package no.nav.familie.tilbake.config

import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile

@TestConfiguration
@Profile("integrasjonstest")
class FeatureToggleMockConfig {
    @Bean
    fun featureToggle(): FeatureToggleService {
        val mockFeatureToggleService: FeatureToggleService = mockk()
        every { mockFeatureToggleService.isEnabled(any<String>()) } answers {
            false
        }
        every { mockFeatureToggleService.isEnabled(any(), any()) } answers {
            secondArg<Boolean>()
        }
        return mockFeatureToggleService
    }
}
