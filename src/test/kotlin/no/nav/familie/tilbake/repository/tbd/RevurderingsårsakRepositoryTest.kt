package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Årsakstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

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

        assertThat(lagretRevurderingsårsak).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(revurderingsårsak)
        assertEquals(1, lagretRevurderingsårsak.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Revurderingsårsak i basen`() {
        revurderingsårsakRepository.insert(revurderingsårsak)
        var lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)
        val oppdatertRevurderingsårsak = lagretRevurderingsårsak.copy(årsakstype = Årsakstype.FEIL_FAKTA)

        revurderingsårsakRepository.update(oppdatertRevurderingsårsak)

        lagretRevurderingsårsak = revurderingsårsakRepository.findByIdOrThrow(revurderingsårsak.id)
        assertThat(lagretRevurderingsårsak).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertRevurderingsårsak)
        assertEquals(2, lagretRevurderingsårsak.versjon)
    }

}
