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

        assertThat(lagretÅrsakTotrinnsvurdering).isEqualToIgnoringGivenFields(årsakTotrinnsvurdering,
                                                                              "sporbar", "versjon")
        assertEquals(1, lagretÅrsakTotrinnsvurdering.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ÅrsakTotrinnsvurdering i basen`() {
        årsakTotrinnsvurderingRepository.insert(årsakTotrinnsvurdering)
        var lagretÅrsakTotrinnsvurdering = årsakTotrinnsvurderingRepository.findByIdOrThrow(årsakTotrinnsvurdering.id)
        val oppdatertÅrsakTotrinnsvurdering = lagretÅrsakTotrinnsvurdering.copy(årsakstype = Årsakstype.FEIL_REGEL)

        årsakTotrinnsvurderingRepository.update(oppdatertÅrsakTotrinnsvurdering)

        lagretÅrsakTotrinnsvurdering = årsakTotrinnsvurderingRepository.findByIdOrThrow(årsakTotrinnsvurdering.id)
        assertThat(lagretÅrsakTotrinnsvurdering)
                .isEqualToIgnoringGivenFields(oppdatertÅrsakTotrinnsvurdering, "sporbar", "versjon")
        assertEquals(2, lagretÅrsakTotrinnsvurdering.versjon)
    }

}
