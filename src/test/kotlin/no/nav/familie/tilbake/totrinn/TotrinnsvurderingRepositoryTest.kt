package no.nav.familie.tilbake.totrinn

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.totrinn.domain.Totrinnsvurdering
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TotrinnsvurderingRepositoryTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var totrinnsvurderingRepository: TotrinnsvurderingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var totrinnsvurdering: Totrinnsvurdering
    private lateinit var fagsak: Fagsak

    @BeforeEach
    fun init() {
        fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        totrinnsvurdering = Testdata.lagTotrinnsvurdering(behandling.id)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Totrinnsvurdering til basen`() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)

        val lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)

        lagretTotrinnsvurdering.shouldBeEqualToIgnoringFields(
            totrinnsvurdering,
            Totrinnsvurdering::sporbar,
            Totrinnsvurdering::versjon,
        )
        lagretTotrinnsvurdering.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Totrinnsvurdering i basen`() {
        totrinnsvurderingRepository.insert(totrinnsvurdering)
        var lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        val oppdatertTotrinnsvurdering = lagretTotrinnsvurdering.copy(begrunnelse = "bob")

        totrinnsvurderingRepository.update(oppdatertTotrinnsvurdering)

        lagretTotrinnsvurdering = totrinnsvurderingRepository.findByIdOrThrow(totrinnsvurdering.id)
        lagretTotrinnsvurdering.shouldBeEqualToIgnoringFields(
            oppdatertTotrinnsvurdering,
            Totrinnsvurdering::sporbar,
            Totrinnsvurdering::versjon,
        )
        lagretTotrinnsvurdering.versjon shouldBe 2
    }
}
