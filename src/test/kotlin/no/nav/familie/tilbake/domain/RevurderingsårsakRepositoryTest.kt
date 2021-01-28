package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.AksjonspunktRepository
import no.nav.familie.tilbake.repository.tbd.RevurderingsårsakRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.Årsakstype

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
    fun `insert med gyldige verdier skal persistere en forekomst av Revurderingsårsak til basen`() {
        revurderingsårsakRepository.insert(revurderingsårsak)

        val lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)

        Assertions.assertThat(lagretRevurderingsårsak).isEqualToIgnoringGivenFields(revurderingsårsak, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Revurderingsårsak i basen`() {
        revurderingsårsakRepository.insert(revurderingsårsak)
        val oppdatertRevurderingsårsak = revurderingsårsak.copy(årsakstype = Årsakstype.FEIL_FAKTA)

        revurderingsårsakRepository.update(oppdatertRevurderingsårsak)

        val lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)
        Assertions.assertThat(lagretRevurderingsårsak).isEqualToIgnoringGivenFields(oppdatertRevurderingsårsak, "sporbar")
    }

}