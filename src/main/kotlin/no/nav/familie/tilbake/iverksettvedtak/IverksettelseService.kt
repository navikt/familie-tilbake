package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.integration.økonomi.OppdragStatusMedMelding
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class IverksettelseService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val økonomiXmlSendtRepository: ØkonomiXmlSendtRepository,
    private val tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService,
    private val beregningService: TilbakekrevingsberegningService,
    private val behandlingVedtakService: BehandlingsvedtakService,
    private val oppdragClient: OppdragClient,
    private val fagsakRepository: FagsakRepository,
    private val featureToggleService: FeatureToggleService,
) {
    private val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

    @Transactional
    fun sendIverksettVedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        if (behandling.status == Behandlingsstatus.AVSLUTTET)
            {
                val kvittering = økonomiXmlSendtRepository.findByBehandlingId(behandlingId)?.kvittering
                if (kvittering != null)
                    {
                        secureLogger.warn("Behandling=$behandlingId er allerede avsluttet og kvittert i økonomi")
                        return
                    }

                // stop prosessering
            }

        val beregnetPerioder = tilbakekrevingsvedtakBeregningService.beregnVedtaksperioder(behandlingId, kravgrunnlag)
        // Validerer beregning slik at rapporterte beløp må være samme i vedtaksbrev og iverksettelse
        validerBeløp(behandlingId, beregnetPerioder)

        val request = lagIveksettelseRequest(behandling.ansvarligSaksbehandler, kravgrunnlag, beregnetPerioder)
        // lagre request i en separat transaksjon slik at det lagrer selv om tasken feiler
        // TODO Transaksjon opprettes i samme service - denne vil ikke fungere
        // Hvis vi fikser denne bør vi kanskje også bytte til insert or update?
        // foreslår å ikke endre denne, men rulle tilbake ved feil.
        // select * from okonomi_xml_sendt where kvittering is null => ingen treff
        val requestXml = TilbakekrevingsvedtakMarshaller.marshall(behandlingId, request)
        secureLogger.info("Sender tilbakekrevingsvedtak til økonomi for behandling=$behandlingId request=$requestXml")
        var økonomiXmlSendt = lagreIverksettelsesvedtakRequest(behandlingId, requestXml)

        // Send request til økonomi
        // TODO wrap denne i try catch -> bruk status fra oppdrag og oppdater økonomiXmlSendt med "enkel" status dersom den allerede er utført (KVITTERT_OK)"
        val kvittering = sendRequestTilØkonomi(behandlingId, request)

        // oppdater respons
        økonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        økonomiXmlSendtRepository.update(økonomiXmlSendt.copy(kvittering = kvittering))

        behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)
    }

    private fun sendRequestTilØkonomi(
        behandlingId: UUID,
        request: TilbakekrevingsvedtakRequest,
    ): String? {
        try {
            val respons = oppdragClient.iverksettVedtak(behandlingId, request)
            return objectMapper.writeValueAsString(respons.mmel)
        } catch (e: IntegrasjonException) {
            if (hentOppdragStatus(behandlingId).status == OppdragStatus.KVITTERT_OK) {
                secureLogger.warn("Kvittering for behandling=$behandlingId er KVITTERT_OK av økonomi")
                return "Forenklet kvitteringsmelding: Oppdrag har allerede behandlet request og kvittert KVITTERT_OK"
            } else {
                throw e
            }
        }
    }

    fun hentOppdragStatus(
        behandlingId: UUID,
    ): OppdragStatusMedMelding {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val eksternId = behandling.aktivFagsystemsbehandling.eksternId
        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
        val ident = fagsak.bruker.ident
        val fagområdekode = fagsak.ytelsestype.tilFagområdekode()
        val oppdragId = OppdragId(fagsystem = fagområdekode.toString(), behandlingsId = eksternId, personIdent = ident)
        return oppdragClient.hentStatus(oppdragId)
    }

    fun lagreIverksettelsesvedtakRequest(
        behandlingId: UUID,
        requestXml: String,
    ): ØkonomiXmlSendt {
        return økonomiXmlSendtRepository.insert(ØkonomiXmlSendt(behandlingId = behandlingId, melding = requestXml))
    }

    private fun lagIveksettelseRequest(
        ansvarligSaksbehandler: String,
        kravgrunnlag: Kravgrunnlag431,
        beregnetPerioder: List<Tilbakekrevingsperiode>,
    ): TilbakekrevingsvedtakRequest {
        val request = TilbakekrevingsvedtakRequest()
        val vedtak = TilbakekrevingsvedtakDto()
        vedtak.apply {
            vedtakId = kravgrunnlag.vedtakId
            kodeAksjon = KodeAksjon.FATTE_VEDTAK.kode
            kodeHjemmel = "22-15" // fast verdi
            datoVedtakFagsystem = kravgrunnlag.fagsystemVedtaksdato ?: LocalDate.now()
            enhetAnsvarlig = kravgrunnlag.ansvarligEnhet
            kontrollfelt = kravgrunnlag.kontrollfelt
            saksbehId = ansvarligSaksbehandler
            tilbakekrevingsperiode.addAll(lagVedtaksperiode(beregnetPerioder))
        }
        return request.apply { tilbakekrevingsvedtak = vedtak }
    }

    private fun lagVedtaksperiode(beregnetPerioder: List<Tilbakekrevingsperiode>): List<TilbakekrevingsperiodeDto> {
        return beregnetPerioder.map {
            val tilbakekrevingsperiode = TilbakekrevingsperiodeDto()
            tilbakekrevingsperiode.apply {
                val periode = PeriodeDto()
                periode.fom = it.periode.fom.atDay(1)
                periode.tom = it.periode.tom.atEndOfMonth()
                this.periode = periode
                belopRenter = it.renter
                tilbakekrevingsbelop.addAll(lagVedtaksbeløp(it.beløp))
            }
        }
    }

    private fun lagVedtaksbeløp(beregnetBeløper: List<Tilbakekrevingsbeløp>): List<TilbakekrevingsbelopDto> {
        return beregnetBeløper.map {
            val tilbakekrevingsbeløp = TilbakekrevingsbelopDto()
            tilbakekrevingsbeløp.apply {
                kodeKlasse = it.klassekode.name
                belopNy = it.nyttBeløp
                belopOpprUtbet = it.utbetaltBeløp
                belopTilbakekreves = it.tilbakekrevesBeløp
                belopUinnkrevd = it.uinnkrevdBeløp
                belopSkatt = it.skattBeløp
                if (Klassetype.YTEL == it.klassetype) {
                    kodeResultat = utledKodeResultat(it)
                    kodeAarsak = "ANNET" // fast verdi
                    kodeSkyld = "IKKE_FORDELT" // fast verdi
                }
            }
        }
    }

    private fun utledKodeResultat(tilbakekrevingsbeløp: Tilbakekrevingsbeløp): String {
        return if (harSattDelvisTilbakekrevingMenKreverTilbakeFulltBeløp(tilbakekrevingsbeløp)) {
            secureLogger.warn(
                """Fant tilbakekrevingsperiode med delvis tilbakekreving hvor vi krever tilbake hele beløpet.
                | Økonomi krever trolig at vi setter full tilbakekreving. 
                | Dersom kjøringen feiler mot økonomi med feilmelding: Innkrevd beløp = feilutbetalt ved delvis tilbakekreving.
                | Vurder å skru på featuretoggle familie-tilbake-overstyr-delvis-tilbakekreving og rekjør.
                | Tilbakekrevingsbeløp=$tilbakekrevingsbeløp """.trimMargin(),
            )
            if (featureToggleService.isEnabled(FeatureToggleConfig.OVERSTYR_DELVILS_TILBAKEKREVING_TIL_FULL_TILBAKEKREVING)) {
                KodeResultat.FULL_TILBAKEKREVING.kode
            } else {
                KodeResultat.DELVIS_TILBAKEKREVING.kode
            }
        } else {
            tilbakekrevingsbeløp.kodeResultat.kode
        }
    }

    private fun harSattDelvisTilbakekrevingMenKreverTilbakeFulltBeløp(tilbakekrevingsbeløp: Tilbakekrevingsbeløp) =
        tilbakekrevingsbeløp.kodeResultat == KodeResultat.DELVIS_TILBAKEKREVING && tilbakekrevingsbeløp.uinnkrevdBeløp == BigDecimal.ZERO

    fun validerBeløp(
        behandlingId: UUID,
        beregnetPerioder: List<Tilbakekrevingsperiode>,
    ) {
        val beregnetResultat = beregningService.beregn(behandlingId)
        val beregnetPerioderForVedtaksbrev = beregnetResultat.beregningsresultatsperioder

        // Beløpene beregnes for vedtaksbrev
        val totalTilbakekrevingsbeløpUtenRenter = beregnetPerioderForVedtaksbrev.sumOf { it.tilbakekrevingsbeløpUtenRenter }
        val totalRenteBeløp = beregnetPerioderForVedtaksbrev.sumOf { it.rentebeløp }
        val totalSkatteBeløp = beregnetPerioderForVedtaksbrev.sumOf { it.skattebeløp }

        // Beløpene beregnes for iverksettelse
        val beregnetTotatlTilbakekrevingsbeløpUtenRenter =
            beregnetPerioder.sumOf { it.beløp.sumOf { beløp -> beløp.tilbakekrevesBeløp } }
        val beregnetTotalRenteBeløp = beregnetPerioder.sumOf { it.renter }
        val beregnetSkattBeløp = beregnetPerioder.sumOf { it.beløp.sumOf { beløp -> beløp.skattBeløp } }

        if (totalTilbakekrevingsbeløpUtenRenter != beregnetTotatlTilbakekrevingsbeløpUtenRenter ||
            totalRenteBeløp != beregnetTotalRenteBeløp || totalSkatteBeløp != beregnetSkattBeløp
        ) {
            throw Feil(
                message =
                    "Det gikk noe feil i beregning under iverksettelse for behandlingId=$behandlingId." +
                        "Beregnet beløp i vedtaksbrev er " +
                        "totalTilbakekrevingsbeløpUtenRenter=$totalTilbakekrevingsbeløpUtenRenter," +
                        "totalRenteBeløp=$totalRenteBeløp, totalSkatteBeløp=$totalSkatteBeløp mens " +
                        "Beregnet beløp i iverksettelse er " +
                        "beregnetTotatlTilbakekrevingsbeløpUtenRenter=$beregnetTotatlTilbakekrevingsbeløpUtenRenter," +
                        "beregnetTotalRenteBeløp=$beregnetTotalRenteBeløp, beregnetSkattBeløp=$beregnetSkattBeløp",
            )
        }
    }
}

fun Ytelsestype.tilFagområdekode(): Fagområdekode =
    when (this) {
        Ytelsestype.BARNETRYGD -> Fagområdekode.BA
        Ytelsestype.KONTANTSTØTTE -> Fagområdekode.KS
        Ytelsestype.OVERGANGSSTØNAD -> Fagområdekode.EFOG
        Ytelsestype.BARNETILSYN -> Fagområdekode.EFBT
        Ytelsestype.SKOLEPENGER -> Fagområdekode.EFSP
    }
