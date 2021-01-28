package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.Årsakstype
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ÅrsakTotrinnsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var årsakTotrinnsvurderingRepository: ÅrsakTotrinnsvurderingRepository

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val årsakTotrinnsvurdering = Testdata.årsakTotrinnsvurdering

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        totrinnsvurderingRepository.insert(Testdata.totrinnsvurdering)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ÅrsakTotrinnsvurdering til basen`() {
        årsakTotrinnsvurderingRepository.insert(årsakTotrinnsvurdering)

        val lagretÅrsakTotrinnsvurdering = årsakTotrinnsvurderingRepository.findByIdOrThrow(årsakTotrinnsvurdering.id)

        Assertions.assertThat(lagretÅrsakTotrinnsvurdering).isEqualToIgnoringGivenFields(årsakTotrinnsvurdering, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ÅrsakTotrinnsvurdering i basen`() {
        årsakTotrinnsvurderingRepository.insert(årsakTotrinnsvurdering)
        val oppdatertÅrsakTotrinnsvurdering = årsakTotrinnsvurdering.copy(årsakstype = Årsakstype.FEIL_REGEL)

        årsakTotrinnsvurderingRepository.update(oppdatertÅrsakTotrinnsvurdering)

        val lagretÅrsakTotrinnsvurdering = årsakTotrinnsvurderingRepository.findByIdOrThrow(årsakTotrinnsvurdering.id)
        Assertions.assertThat(lagretÅrsakTotrinnsvurdering)
                .isEqualToIgnoringGivenFields(oppdatertÅrsakTotrinnsvurdering, "sporbar")
    }

}