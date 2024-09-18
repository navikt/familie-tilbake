package no.nav.familie.tilbake.faktaomfeilutbetaling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.familie.tilbake.api.dto.VurderingAvBrukersUttalelseDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingDomainUtil.lagFaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HarBrukerUttaltSeg
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

internal class FaktaFeilutbetalingServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var faktaFeilutbetalingService: FaktaFeilutbetalingService

    private lateinit var behandling: Behandling
    private val periode =
        Månedsperiode(
            fom = YearMonth.now().minusMonths(2),
            tom = YearMonth.now(),
        )

    @BeforeEach
    fun init() {
        behandling = Testdata.lagBehandling()
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(behandling)
        val kravgrunnlag =
            Testdata
                .lagKravgrunnlag(behandling.id)
                .copy(
                    perioder =
                        setOf(
                            Kravgrunnlagsperiode432(
                                periode = periode,
                                beløp =
                                    setOf(
                                        Testdata.feilKravgrunnlagsbeløp433,
                                        Testdata.ytelKravgrunnlagsbeløp433,
                                    ),
                                månedligSkattebeløp = BigDecimal("123.11"),
                            ),
                        ),
                )
        kravgrunnlagRepository.insert(kravgrunnlag)
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling for en gitt behandling`() {
        lagFaktaomfeilutbetaling(behandling.id)

        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = behandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe "Fakta begrunnelse"
        val varsletData = behandling.aktivtVarsel
        faktaFeilutbetalingDto.varsletBeløp shouldBe varsletData?.varselbeløp

        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling for behandling uten varsel`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val oppdatertBehandling = lagretBehandling.copy(varsler = emptySet())
        behandlingRepository.update(oppdatertBehandling)
        lagFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id)

        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe "Fakta begrunnelse"
        faktaFeilutbetalingDto.varsletBeløp.shouldBeNull()
        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
    }

    @Test
    fun `skal hente inaktive fakta om feilutbetalinger`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        lagFaktaomfeilutbetaling(behandlingId = lagretBehandling.id, hendelsestype = Hendelsestype.SATSER)
        lagFaktaomfeilutbetaling(behandlingId = lagretBehandling.id, hendelsestype = Hendelsestype.DØDSFALL)
        lagFaktaomfeilutbetaling(behandlingId = lagretBehandling.id, hendelsestype = Hendelsestype.INNTEKT)

        val faktaFeilutbetalinger = faktaFeilutbetalingService.hentInaktivFaktaomfeilutbetaling(behandlingId = lagretBehandling.id)

        faktaFeilutbetalinger shouldHaveSize 2
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalinger.first(),
            hendelsestype = Hendelsestype.SATSER,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalinger.last(),
            hendelsestype = Hendelsestype.DØDSFALL,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
    }

    @Test
    fun `hentFaktaomfeilutbetaling med vurdering av brukers uttalelse`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val oppdatertBehandling = lagretBehandling.copy(varsler = emptySet())
        behandlingRepository.update(oppdatertBehandling)
        lagFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id, vurderingAvBrukersUttalelse = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI, beskrivelse = null))

        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe "Fakta begrunnelse"
        faktaFeilutbetalingDto.varsletBeløp.shouldBeNull()
        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = Hendelsestype.ANNET,
            hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
        )
        faktaFeilutbetalingDto.vurderingAvBrukersUttalelse shouldNotBe null
        faktaFeilutbetalingDto.vurderingAvBrukersUttalelse.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
        faktaFeilutbetalingDto.vurderingAvBrukersUttalelse.beskrivelse shouldBe null
    }

    @Test
    fun `oppdatering av fakta skal lage en ny rad i databasen mens den gamle er deaktivert`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val oppdatertBehandling = lagretBehandling.copy(varsler = emptySet())
        behandlingRepository.update(oppdatertBehandling)
        val brukersUttalelseInitiell = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.JA, beskrivelse = "Hurra")
        lagFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id, vurderingAvBrukersUttalelse = brukersUttalelseInitiell)
        val brukersUttalelseEndret = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI, beskrivelse = null)
        lagFaktaomfeilutbetaling(behandlingId = oppdatertBehandling.id, vurderingAvBrukersUttalelse = brukersUttalelseEndret, hendelsestype = Hendelsestype.BOR_MED_SØKER)

        val alle = faktaFeilutbetalingRepository.findAll().filter { it.behandlingId == oppdatertBehandling.id }
        alle shouldHaveSize 2

        val aktiv = alle.first { it.aktiv }
        aktiv.vurderingAvBrukersUttalelse?.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
        aktiv.vurderingAvBrukersUttalelse?.beskrivelse shouldBe null
        aktiv.vurderingAvBrukersUttalelse?.aktiv shouldBe true
        aktiv.perioder.first().hendelsestype shouldBe Hendelsestype.BOR_MED_SØKER

        val inaktiv = alle.first { !it.aktiv }
        inaktiv.vurderingAvBrukersUttalelse?.harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
        inaktiv.vurderingAvBrukersUttalelse?.beskrivelse shouldBe "Hurra"
        inaktiv.vurderingAvBrukersUttalelse?.aktiv shouldBe false
        inaktiv.perioder.first().hendelsestype shouldBe Hendelsestype.ANNET
    }

    @Test
    fun `skal validere vurdering av brukers uttalelse`() {
        val lagretBehandling = behandlingRepository.findByIdOrThrow(behandling.id)
        val oppdatertBehandling = lagretBehandling.copy(varsler = emptySet())
        behandlingRepository.update(oppdatertBehandling)
        val behandlingId = oppdatertBehandling.id
        val ugyldigVerdiBeskrivelseMangler =
            shouldThrow<Feil> {
                val manglerBeskrivelse = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.JA, beskrivelse = null)
                lagFaktaomfeilutbetaling(behandlingId = behandlingId, vurderingAvBrukersUttalelse = manglerBeskrivelse)
            }
        ugyldigVerdiBeskrivelseMangler.message shouldContain "Mangler beskrivelse på vurdering av brukers uttalelse"
        val ugyldiVerdiBeskrivelseSkalVæreTom =
            shouldThrow<Feil> {
                val ugyldigBeskrivelse = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI, beskrivelse = "Skal være null")
                lagFaktaomfeilutbetaling(behandlingId = behandlingId, vurderingAvBrukersUttalelse = ugyldigBeskrivelse)
            }
        ugyldiVerdiBeskrivelseSkalVæreTom.message shouldContain "Skal ikke ha beskrivelse når bruker ikke har uttalt seg"

        val ugyldiVerdiBeskrivelseSkalVæreTomHvisIkkeAktuelt =
            shouldThrow<Feil> {
                val ugyldigBeskrivelse = VurderingAvBrukersUttalelseDto(harBrukerUttaltSeg = HarBrukerUttaltSeg.IKKE_AKTUELT, beskrivelse = "Skal være null")
                lagFaktaomfeilutbetaling(behandlingId = behandlingId, vurderingAvBrukersUttalelse = ugyldigBeskrivelse)
            }
        ugyldiVerdiBeskrivelseSkalVæreTomHvisIkkeAktuelt.message shouldContain "Skal ikke ha beskrivelse når bruker ikke har uttalt seg"
    }

    @Test
    fun `hentFaktaomfeilutbetaling skal hente fakta om feilutbetaling første gang for en gitt behandling`() {
        val faktaFeilutbetalingDto = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId = behandling.id)

        faktaFeilutbetalingDto.begrunnelse shouldBe ""
        faktaFeilutbetalingDto.varsletBeløp shouldBe behandling.aktivtVarsel?.varselbeløp
        assertFagsystemsbehandling(faktaFeilutbetalingDto, behandling)
        assertFeilutbetaltePerioder(
            faktaFeilutbetalingDto = faktaFeilutbetalingDto,
            hendelsestype = null,
            hendelsesundertype = null,
        )
    }

    @Test
    fun `sjekk om fakta feilutbetalingsperioder er like, unntatt periode og beløp, skal returnere true`() {
        val faktaFeilutbetalingsperiode =
            FaktaFeilutbetalingsperiode(
                periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 3)),
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            )

        val faktaFeilutbetalingsperiode2 =
            FaktaFeilutbetalingsperiode(
                periode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
                hendelsestype = Hendelsestype.ANNET,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            )
        val faktaFeilutbetaling =
            lagFaktaFeilutbetaling(
                behandlingId = behandling.id,
                perioder = setOf(faktaFeilutbetalingsperiode, faktaFeilutbetalingsperiode2),
            )

        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandling.id) shouldBe true
    }

    @Test
    fun `sjekk om fakta feilutbetalingsperioder er like, unntatt periode og beløp, skal returnere false`() {
        val faktaFeilutbetalingsperiode =
            FaktaFeilutbetalingsperiode(
                periode = Månedsperiode(YearMonth.of(2024, 1), YearMonth.of(2024, 3)),
                hendelsestype = Hendelsestype.INNTEKT,
                hendelsesundertype = Hendelsesundertype.MEDLEM_SISTE_5_ÅR,
            )

        val faktaFeilutbetalingsperiode2 =
            FaktaFeilutbetalingsperiode(
                periode = Månedsperiode(YearMonth.of(2024, 4), YearMonth.of(2024, 5)),
                hendelsestype = Hendelsestype.INNTEKT,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            )
        val faktaFeilutbetaling =
            lagFaktaFeilutbetaling(
                behandlingId = behandling.id,
                perioder = setOf(faktaFeilutbetalingsperiode, faktaFeilutbetalingsperiode2),
            )

        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        faktaFeilutbetalingService.sjekkOmFaktaPerioderErLike(behandling.id) shouldBe false
    }

    private fun lagFaktaomfeilutbetaling(
        behandlingId: UUID,
        vurderingAvBrukersUttalelse: VurderingAvBrukersUttalelseDto? = null,
        hendelsestype: Hendelsestype = Hendelsestype.ANNET,
    ) {
        val faktaPerioder =
            FaktaFeilutbetalingsperiodeDto(
                periode = periode.toDatoperiode(),
                hendelsestype = hendelsestype,
                hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
            )
        val faktaFeilutbetaling =
            BehandlingsstegFaktaDto(
                begrunnelse = "Fakta begrunnelse",
                feilutbetaltePerioder = listOf(faktaPerioder),
                vurderingAvBrukersUttalelse = vurderingAvBrukersUttalelse,
            )
        faktaFeilutbetalingService.lagreFaktaomfeilutbetaling(behandlingId, faktaFeilutbetaling)
    }

    private fun assertFagsystemsbehandling(
        faktaFeilutbetalingDto: FaktaFeilutbetalingDto,
        behandling: Behandling,
    ) {
        val fagsystemsbehandling = behandling.aktivFagsystemsbehandling
        val faktainfo = faktaFeilutbetalingDto.faktainfo
        faktainfo.tilbakekrevingsvalg shouldBe fagsystemsbehandling.tilbakekrevingsvalg
        faktaFeilutbetalingDto.revurderingsvedtaksdato shouldBe fagsystemsbehandling.revurderingsvedtaksdato
        faktainfo.revurderingsresultat shouldBe fagsystemsbehandling.resultat
        faktainfo.revurderingsårsak shouldBe fagsystemsbehandling.årsak
        faktainfo.konsekvensForYtelser.shouldBeEmpty()
    }

    private fun assertFeilutbetaltePerioder(
        faktaFeilutbetalingDto: FaktaFeilutbetalingDto,
        hendelsestype: Hendelsestype?,
        hendelsesundertype: Hendelsesundertype?,
    ) {
        faktaFeilutbetalingDto.totalFeilutbetaltPeriode shouldBe periode.toDatoperiode()
        faktaFeilutbetalingDto.totaltFeilutbetaltBeløp shouldBe BigDecimal.valueOf(1000000, 2)

        faktaFeilutbetalingDto.feilutbetaltePerioder.size shouldBe 1
        val feilutbetaltePeriode = faktaFeilutbetalingDto.feilutbetaltePerioder.first()
        feilutbetaltePeriode.hendelsestype shouldBe hendelsestype
        feilutbetaltePeriode.hendelsesundertype shouldBe hendelsesundertype
        feilutbetaltePeriode.periode shouldBe periode.toDatoperiode()
    }
}
