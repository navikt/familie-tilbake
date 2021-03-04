package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class TotrinnsvurderingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val totrinnsvurdering = Testdata.totrinnsvurdering

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Totrinnsvurdering til basen`() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)

        val lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)

        assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(totrinnsvurdering,
                                                                         "sporbar", "versjon")
        assertEquals(1, lagretTotrinnsvurdering.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Totrinnsvurdering i basen`() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)
        var lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        val oppdatertTotrinnsvurdering = lagretTotrinnsvurdering.copy(begrunnelse = "bob")

        totrinnsvurderingRepository.update(oppdatertTotrinnsvurdering)

        lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(oppdatertTotrinnsvurdering,
                                                                         "sporbar", "versjon")
        assertEquals(2, lagretTotrinnsvurdering.versjon)
    }

}
