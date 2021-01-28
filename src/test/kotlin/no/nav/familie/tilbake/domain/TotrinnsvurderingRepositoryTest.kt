package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.TotrinnsvurderingRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

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

        Assertions.assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(totrinnsvurdering, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Totrinnsvurdering i basen`() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)
        val oppdatertTotrinnsvurdering = totrinnsvurdering.copy(begrunnelse = "bob")

        totrinnsvurderingRepository.update(oppdatertTotrinnsvurdering)

        val lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        Assertions.assertThat(lagretTotrinnsvurdering).isEqualToIgnoringGivenFields(oppdatertTotrinnsvurdering, "sporbar")
    }

}