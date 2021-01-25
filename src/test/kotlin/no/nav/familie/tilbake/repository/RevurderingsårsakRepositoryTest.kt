package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Årsakstype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class RevurderingsårsakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var revurderingsårsakRepository: RevurderingsårsakRepository

    @Autowired
    private lateinit var aksjonspunktRepository: AksjonspunktRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val revurderingsårsak = Testdata.revurderingsårsak

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        aksjonspunktRepository.insert(Testdata.aksjonspunkt)
    }

    @Test
    fun insertPersistererEnForekomstAvRevurderingsårsakTilBasen() {
        revurderingsårsakRepository.insert(revurderingsårsak)

        val lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)

        Assertions.assertThat(lagretRevurderingsårsak).isEqualToIgnoringGivenFields(revurderingsårsak, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvRevurderingsårsakIBasen() {
        revurderingsårsakRepository.insert(revurderingsårsak)
        val oppdatertRevurderingsårsak = revurderingsårsak.copy(årsakstype = Årsakstype.FEIL_FAKTA)

        revurderingsårsakRepository.update(oppdatertRevurderingsårsak)

        val lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)
        Assertions.assertThat(lagretRevurderingsårsak).isEqualToIgnoringGivenFields(oppdatertRevurderingsårsak, "sporbar")
    }

}