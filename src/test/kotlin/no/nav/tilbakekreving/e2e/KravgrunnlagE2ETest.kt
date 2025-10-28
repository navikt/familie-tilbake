package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.april
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsperiode
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMediator
import no.nav.tilbakekreving.mai
import no.nav.tilbakekreving.mars
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class KravgrunnlagE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @Autowired
    private lateinit var kravgrunnlagMediator: KravgrunnlagMediator

    @Autowired
    private lateinit var kafkaProducerStub: KafkaProducerStub

    @Test
    fun `kan lese kravgrunnlag for tilleggsstønader`() {
        val fagsystemId = UUID.randomUUID().toString()
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    Tilbakekrevingsperiode(
                        periode = 3.mars(2025) til 3.mars(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4016.kroner,
                                beløpOpprinneligUtbetalt = 4217.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                    Tilbakekrevingsperiode(
                        periode = 1.april(2025) til 1.april(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4418.kroner,
                                beløpOpprinneligUtbetalt = 4418.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                    Tilbakekrevingsperiode(
                        periode = 1.mai(2025) til 1.mai(2025),
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 4418.kroner,
                                beløpOpprinneligUtbetalt = 4418.kroner,
                            ),
                        ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )

        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
        tilbakekreving.shouldNotBeNull()
    }

    @Test
    fun `lagrer bare en gang dersom noe feiler under håndtering av kravgrunnlag`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = "feil1234567"))

        kravgrunnlagMediator.lesKravgrunnlag()

        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.count { it.eksternFagsak.eksternId == fagsystemId } shouldBe 1
    }

    @Test
    fun `flere konsumenter leser fra tabellen samtidig`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlag(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = "sleepy12345"))

        runBlocking(Dispatchers.IO) {
            (0..4).map {
                launch { kravgrunnlagMediator.lesKravgrunnlag() }
            }.joinAll()
        }

        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.count { it.eksternFagsak.eksternId == fagsystemId } shouldBe 1
    }

    @Test
    fun `mottar svar fra fagsystem mens vi venter på svar fra PDL`() {
        runBlocking(Dispatchers.IO) {
            val fagsystemIder = (0..4).map {
                val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
                val future = Thread {
                    fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
                }

                kafkaProducerStub.settFagsysteminfoSvar(fagsystemId) {
                    // Simuler at en ny melding kommer raskt fra en annen tråd
                    future.start()
                }

                sendKravgrunnlag(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = "sleepy12345"))
                kravgrunnlagMediator.lesKravgrunnlag()
                future.join()
                fagsystemId
            }

            val tilbakekrevinger = tilbakekrevingRepository.hentAlleTilbakekrevinger()
                .shouldNotBeNull()
                .filter { it.eksternFagsak.eksternId in fagsystemIder }

            tilbakekrevinger.size shouldBe 5

            tilbakekrevinger.forAll {
                it.eksternFagsak.behandlinger.size shouldBe 2
            }
        }
    }

    @Test
    fun `ruller tilbake tilbakekreving som feiler under opprettelse`() {
        val brukerIdent = KravgrunnlagGenerator.nextPaddedId(48)
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        shouldThrow<Exception> {
            tilbakekrevingService.opprettTilbakekreving(
                OpprettTilbakekrevingHendelse(
                    Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                    OpprettTilbakekrevingHendelse.EksternFagsak(
                        fagsystemId,
                        Ytelse.Tilleggsstønad,
                    ),
                ),
            ) {
                it.håndter(
                    KravgrunnlagHendelse(
                        id = UUID.randomUUID(),
                        vedtakId = KravgrunnlagGenerator.nextPaddedId(6).toBigInteger(),
                        kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NY,
                        fagsystemVedtaksdato = LocalDate.now(),
                        vedtakGjelder = Aktør.Person(brukerIdent),
                        utbetalesTil = Aktør.Person(brukerIdent),
                        skalBeregneRenter = true,
                        ansvarligEnhet = "1337",
                        kontrollfelt = "kontrollfelt",
                        kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(6),
                        referanse = "referanse",
                        perioder = listOf(kravgrunnlagPeriode()),
                    ),
                )
            }
        }

        tilbakekrevingRepository.hentAlleTilbakekrevinger()?.count { it.eksternFagsak.eksternId == fagsystemId } shouldBe 0
    }

    @Test
    fun `statuskode som ikke er støttet blokkerer behandling`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        sendKravgrunnlagOgAvventLesing(QUEUE_NAME, KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, kravStatusKode = "ENDR"))

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
        }

        shouldThrow<ModellFeil.UtenforScopeException> {
            tilbakekrevingService.hentTilbakekreving(behandlingId)
        }

        shouldThrow<ModellFeil.UtenforScopeException> {
            // Henting for skriving
            tilbakekrevingService.hentTilbakekreving(behandlingId) {}
        }
    }

    @Test
    fun `iverksettelse av vedtak med utvidet periode`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021)),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(
                fagsystemId = fagsystemId,
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(fom = 1.januar(2021), tom = 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(fom = 1.januar(2021), tom = 31.januar(2021)),
                    ),
                ),
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg("Z999999", behandlingId, BehandlingsstegGenerator.lagFaktastegVurderingFritekst(1.januar(2021) til 31.januar(2021)))
        utførSteg("Z999999", behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 31.januar(2021)))
        utførSteg("Z999999", behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 31.januar(2021)))
        utførSteg("Z999999", behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        utførSteg("Z111111", behandlingId, BehandlingsstegGenerator.lagGodkjennVedtakVurdering())

        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            val tilbakekrevingsperiode = vedtak.tilbakekrevingsperiode.single()
            tilbakekrevingsperiode.tilbakekrevingsbelop shouldHaveSize 2
            tilbakekrevingsperiode.tilbakekrevingsbelop.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
            }
        }

        val periode = kafkaProducerStub.finnVedtaksoppsummering(behandlingId).single().perioder.single()
        periode.fom shouldBe 1.januar(2021)
        periode.tom shouldBe 31.januar(2021)
    }

    @Test
    fun `kravgrunnlag uten referanse`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val kravgrunnlagId = "560609"
        val kravgrunnlag = KravgrunnlagGenerator.forAAP(
            fagsystemId = fagsystemId,
            kravgrunnlagId = kravgrunnlagId,
        ).replace(Regex("<urn:referanse>[0-9]+</urn:referanse>"), "")

        sendKravgrunnlagOgAvventLesing(QUEUE_NAME, kravgrunnlag)

        val fagsystemInfoBehov = kafkaProducerStub.finnKafkamelding(fagsystemId)
            .filter { (metadata, _) -> metadata == FagsysteminfoBehovHendelse.METADATA }
            .map { (_, behov) -> behov }
            .single()
            .shouldBeInstanceOf<FagsysteminfoBehovHendelse>()

        fagsystemInfoBehov.kravgrunnlagReferanse shouldBe "q+l/W4fQTy6ymStSpiIq9w=="
    }

    fun kravgrunnlagPeriode(
        periode: Datoperiode = 1.januar(2021) til 31.januar(2021),
        ytelsesbeløp: List<KravgrunnlagHendelse.Periode.Beløp> = ytelsesbeløp(),
    ) =
        KravgrunnlagHendelse.Periode(
            id = UUID.randomUUID(),
            periode = periode,
            månedligSkattebeløp = BigDecimal("0.0"),
            beløp = ytelsesbeløp + feilutbetalteBeløp(),
        )

    fun ytelsesbeløp(
        tilbakekrevesBeløp: BigDecimal = 2000.kroner,
        opprinneligBeløp: BigDecimal = 12000.kroner,
    ) =
        listOf(
            KravgrunnlagHendelse.Periode.Beløp(
                id = UUID.randomUUID(),
                klassekode = "",
                klassetype = "YTEL",
                opprinneligUtbetalingsbeløp = opprinneligBeløp,
                nyttBeløp = opprinneligBeløp - tilbakekrevesBeløp,
                tilbakekrevesBeløp = tilbakekrevesBeløp,
                skatteprosent = BigDecimal("0.0"),
            ),
        )

    fun feilutbetalteBeløp() =
        listOf(
            KravgrunnlagHendelse.Periode.Beløp(
                id = UUID.randomUUID(),
                klassekode = "",
                klassetype = "FEIL",
                opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
                nyttBeløp = BigDecimal("10000.0"),
                tilbakekrevesBeløp = BigDecimal("2000.0"),
                skatteprosent = BigDecimal("0.0"),
            ),
        )

    companion object {
        const val QUEUE_NAME = "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG"
    }
}
