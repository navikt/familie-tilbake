package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.integration.økonomi.MockØkonomiClient
import no.nav.familie.tilbake.integration.økonomi.ØkonomiClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@ComponentScan(value = ["no.nav.familie.tilbake.kravgrunnlag"])
@Profile("mock-økonomi")
class ØkonomiClientLokalConfig(private val kravgrunnlagRepository: KravgrunnlagRepository,
                               private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository) {

    @Bean
    @Primary
    fun økonomiClient(): ØkonomiClient {
        return MockØkonomiClient(kravgrunnlagRepository, økonomiXmlMottattRepository)
    }
}
