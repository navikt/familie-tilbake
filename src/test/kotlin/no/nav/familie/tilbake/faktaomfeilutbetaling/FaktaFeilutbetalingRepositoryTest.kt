package no.nav.familie.tilbake.faktaomfeilutbetaling

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.VurderingAvBrukersUttalelse
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private lateinit var faktaFeilutbetaling: FaktaFeilutbetaling

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        faktaFeilutbetaling = Testdata.lagFaktaFeilutbetaling(behandling.id)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetaling til basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        lagretFaktaFeilutbetaling.shouldBeEqualToIgnoringFields(
            faktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
        lagretFaktaFeilutbetaling.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetaling i basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        var lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        val oppdatertFaktaFeilutbetaling = lagretFaktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        lagretFaktaFeilutbetaling.shouldBeEqualToIgnoringFields(
            oppdatertFaktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
        lagretFaktaFeilutbetaling.versjon shouldBe 2
    }

    @Test
    fun `findByBehandlingIdAndAktivIsTrue returnerer resultat når det finnes en forekomst`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val findByBehandlingId = faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)

        findByBehandlingId?.shouldBeEqualToIgnoringFields(
            faktaFeilutbetaling,
            FaktaFeilutbetaling::sporbar,
            FaktaFeilutbetaling::versjon,
        )
    }

    @Test
    fun `skal ta med vurdering av brukers uttalelse i fakta feilutbetaling`() {
        val vurderingAvBrukersUttalelse = VurderingAvBrukersUttalelse(harBrukerUttaltSeg = HarBrukerUttaltSeg.JA, beskrivelse = "Hurra")
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling.copy(vurderingAvBrukersUttalelse = vurderingAvBrukersUttalelse))

        val lagretVurderingAvBrukersUttalelse = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id).vurderingAvBrukersUttalelse ?: error("Mangler brukers uttalelse vurdering")

        lagretVurderingAvBrukersUttalelse.shouldBeEqualToIgnoringFields(
            vurderingAvBrukersUttalelse,
            VurderingAvBrukersUttalelse::sporbar,
            VurderingAvBrukersUttalelse::versjon,
        )
    }
}
