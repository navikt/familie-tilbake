package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.behov.RevurderingVedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.Signatur
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler.Companion.forPeriodeavsnitt
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.breeeev.standardtekster.Bunntekst
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer.tilDto
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppsummeringsdataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppsummertPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.StandardtekstDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderavsnittElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class NyVedtaksbrevService(
    private val vedtaksbrevDataRepository: VedtaksbrevDataRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
    private val pdfGenClient: PdfGenClient,
) {
    private val log = TracedLogger.getLogger<NyVedtaksbrevService>()

    fun hentVedtaksbrevData(behandlingId: UUID, vedtaksbrevInfo: VedtaksbrevInfo, beslutter: Behandler): VedtaksbrevDataDto {
        val lagretBrev = vedtaksbrevDataRepository.hentVedtaksbrevData(behandlingId)
        val lagredeData = lagretBrev?.second
        val hovedavsnitt = lagredeData?.let {
            mapHovedavsnitt(it.hovedavsnitt, vedtaksbrevInfo)
        } ?: HovedavsnittDto(
            tittel = vedtaksbrevInfo.tittel,
            forklaring = when {
                vedtaksbrevInfo.erRevurdering -> Forklaringstekster.HOVEDAVSNITT_REVURDERING
                else -> Forklaringstekster.HOVEDAVSNITT
            },
            underavsnitt = listOf(RentekstElementDto("")),
            hjemler = BrevFormatterer.lagVedtakHjemmelAvsnitt(vedtaksbrevInfo.hjemlerForTilbakekreving, Språkkode.NB),
        )
        val avsnitt = vedtaksbrevInfo.perioder.map { periode ->
            if (lagredeData != null) {
                mapAvsnitt(periode, lagredeData.avsnitt)
            } else {
                BrevFormatterer.lagAvsnitt(periode, flerePerioder = vedtaksbrevInfo.perioder.size > 1)
            }
        }
        val sistOppdatert = lagretBrev?.first?.atOffset(ZoneOffset.UTC) ?: OffsetDateTime.now()

        if (lagredeData != null) {
            val lagredeIder = lagredeData.avsnitt.map { it.id }
            val behandlingIder = vedtaksbrevInfo.perioder.map { it.id }
            if (lagredeIder != behandlingIder) {
                log.medContext(SecureLog.Context.medBehandling(null, behandlingId.toString())) {
                    info("Behandling krever avsnitt for {}, det finnes lagrede avsnitt for {}", behandlingId, lagredeIder)
                }
            }
        }

        return VedtaksbrevDataDto(
            hovedavsnitt = hovedavsnitt,
            avsnitt = avsnitt,
            brevGjelder = vedtaksbrevInfo.brukerdata,
            ytelse = vedtaksbrevInfo.ytelse,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            sistOppdatert = sistOppdatert,
            bunntekster = vedtaksbrevInfo.bunntekster.map(::tilStandardtekst),
            signatur = mapSignatur(vedtaksbrevInfo.signatur, beslutter),
            saksnummer = vedtaksbrevInfo.tilbakekrevingId,
            oppsummeringstabell = when {
                vedtaksbrevInfo.erRevurdering -> null
                else -> OppsummeringsdataDto(
                    beregnerSkatt = vedtaksbrevInfo.beregnerSkatt,
                    perioder = mapOppsummeringsperioder(vedtaksbrevInfo.beregningsresultat),
                    sumFeilutbetaltBeløp = BrevFormatterer.beløpString(vedtaksbrevInfo.beregningsresultat.sumOf { it.feilutbetaltBeløp }),
                    sumTilbakekrevesBeløpEtterSkatt = BrevFormatterer.beløpString(vedtaksbrevInfo.beregningsresultat.sumOf { it.tilbakekrevingsbeløp }),
                )
            },
        )
    }

    private fun mapOppsummeringsperioder(beregningsresultat: List<BeregningsresultatsperiodeDto>): List<OppsummertPeriodeDto> {
        return beregningsresultat.map {
            OppsummertPeriodeDto(
                fom = BrevFormatterer.norskNumeriskDato(it.fom),
                tom = BrevFormatterer.norskNumeriskDato(it.tom),
                feilutbetaltBeløp = BrevFormatterer.beløpString(it.feilutbetaltBeløp),
                redusertBeløp = beløpMedFortegn(it.reduksjon ?: 0, "-"),
                rentebeløp = beløpMedFortegn(it.rentebeløp, "+"),
                skatt = beløpMedFortegn(it.skattebeløp, "-"),
                tilbakekrevingsbeløp = BrevFormatterer.beløpString(it.tilbakekrevingsbeløp),
                tilbakekrevesBeløpEtterSkatt = BrevFormatterer.beløpString(it.tilbakekrevingsbeløp),
            )
        }
    }

    private fun beløpMedFortegn(beløp: Int, fortegn: String): String {
        val formatertBeløp = BrevFormatterer.beløpString(beløp)
        return if (beløp > 0) "$fortegn$formatertBeløp" else formatertBeløp
    }

    private fun oppdaterVedtaksbrevData(
        behandlingId: UUID,
        data: VedtaksbrevRedigerbareDataUpdateDto,
        klokke: Klokke,
    ) = vedtaksbrevDataRepository.oppdaterVedtaksbrevData(
        behandlingId,
        VedtaksbrevDataRepository.VedtaksbrevEntity(
            hovedavsnitt = VedtaksbrevDataRepository.HovedavsnittEntity(
                underavsnitt = data.hovedavsnitt.underavsnitt.mapNotNull(::tilEntity),
            ),
            avsnitt = data.avsnitt.map {
                VedtaksbrevDataRepository.PeriodeavsnittEntity(
                    id = it.id,
                    underavsnitt = it.underavsnitt.mapNotNull(::tilEntity),
                    påkrevdBegrunnelser = it.underavsnitt.mapNotNull(::tilPåkrevdBegrunnelseEntities),
                )
            },
        ),
        klokke,
    )

    fun oppdaterOgHentVedtaksbrevData(
        behandlingId: UUID,
        data: VedtaksbrevRedigerbareDataUpdateDto,
        info: VedtaksbrevInfo,
        klokke: Klokke,
    ): VedtaksbrevRedigerbareDataDto {
        val (sistOppdatert, data) = oppdaterVedtaksbrevData(
            behandlingId = behandlingId,
            data = data,
            klokke = klokke,
        )
        return VedtaksbrevRedigerbareDataDto(
            hovedavsnitt = mapHovedavsnitt(data.hovedavsnitt, info),
            avsnitt = info.perioder.map { mapAvsnitt(it, data.avsnitt) },
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
        )
    }

    fun mapHovedavsnitt(
        data: VedtaksbrevDataRepository.HovedavsnittEntity,
        info: VedtaksbrevInfo,
    ): HovedavsnittDto {
        return HovedavsnittDto(
            tittel = info.tittel,
            forklaring = Forklaringstekster.HOVEDAVSNITT,
            underavsnitt = data.underavsnitt.map {
                mapUnderavsnitt(it)
            },
            hjemler = BrevFormatterer.lagVedtakHjemmelAvsnitt(info.hjemlerForTilbakekreving, Språkkode.NB),
        )
    }

    fun mapAvsnitt(
        periode: BegrunnetPeriode,
        avsnitt: List<VedtaksbrevDataRepository.PeriodeavsnittEntity>,
    ): AvsnittDto {
        val lagretAvsnitt = avsnitt
            .firstOrNull { lagretAvsnitt -> lagretAvsnitt.id == periode.id }
            ?: return BrevFormatterer.lagAvsnitt(periode, flerePerioder = avsnitt.size > 1)
        return AvsnittDto(
            forklaring = Forklaringstekster.PERIODE_AVSNITT,
            id = lagretAvsnitt.id,
            meldingerTilSaksbehandler = periode.meldingerTilSaksbehandler
                .forPeriodeavsnitt()
                .map { it.melding },
            tittel = BrevFormatterer.lagPeriodeavsnittTittel(periode.periode, flerePerioder = avsnitt.size > 1),
            underavsnitt = lagretAvsnitt.underavsnitt.rentekst() + periode.påkrevdeVurderinger.map { påkrevd ->
                lagretAvsnitt.påkrevdBegrunnelser
                    .singleOrNull { it.type == påkrevd.name }
                    ?.let { mapPåkrevdBegrunnelse(it, periode.meldingerTilSaksbehandler.toList()) }
                    ?: påkrevd.tilDto(periode.meldingerTilSaksbehandler.toList())
            },
        )
    }

    fun List<VedtaksbrevDataRepository.UnderavsnittEntity>.rentekst(): List<RotElementDto> {
        return filter { it.type == VedtaksbrevDataRepository.UnderavsnittEntity.Type.RENTEKST }
            .map(::mapUnderavsnitt)
    }

    fun mapPåkrevdBegrunnelse(
        entity: VedtaksbrevDataRepository.PåkrevdBegrunnelse,
        meldingerTilSaksbehandler: List<MeldingTilSaksbehandler>,
    ): PakrevdBegrunnelseDto {
        return VilkårsvurderingBegrunnelse.valueOf(entity.type)
            .tilDto(meldingerTilSaksbehandler, entity.underavsnitt.map { RentekstElementDto(it) })
    }

    fun mapUnderavsnitt(
        avsnitt: VedtaksbrevDataRepository.UnderavsnittEntity,
    ): RotElementDto {
        return when (avsnitt.type) {
            VedtaksbrevDataRepository.UnderavsnittEntity.Type.RENTEKST -> RentekstElementDto(
                tekst = avsnitt.tekst!!,
            )

            VedtaksbrevDataRepository.UnderavsnittEntity.Type.UNDERAVSNITT -> UnderavsnittElementDto(
                tittel = avsnitt.tittel!!,
                underavsnitt = avsnitt.underavsnitt!!.map { RentekstElementDto(it.tekst!!) },
            )
            VedtaksbrevDataRepository.UnderavsnittEntity.Type.REVURDERING -> UnderavsnittElementDto(
                tittel = avsnitt.tittel!!,
                underavsnitt = avsnitt.underavsnitt!!.map { RentekstElementDto(it.tekst!!) },
            )
        }
    }

    fun mapSignatur(signatur: Signatur, beslutter: Behandler): SignaturDto {
        val beslutter = beslutter.takeIf { it.ident != signatur.ansvarligSaksbehandlerIdent }
        return SignaturDto(
            enhetNavn = signatur.ansvarligEnhet,
            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(signatur.ansvarligSaksbehandlerIdent),
            besluttendeSaksbehandler = beslutter?.let { eksterneDataForBrevService.hentSaksbehandlernavn(it.ident) },
        )
    }

    private fun opprettArkiverDokumentRequest(
        bruker: Brukerinfo,
        pdfDocument: ByteArray,
        fagsakId: String,
        tittel: String,
        journalførendeEnhet: String,
        eksternReferanseId: String,
        filnavn: String,
    ): ArkiverDokumentRequest =
        ArkiverDokumentRequest(
            fnr = bruker.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = pdfDocument,
                    filtype = Filtype.PDFA,
                    filnavn = filnavn,
                    tittel = tittel,
                    dokumenttype = null,
                ),
            ),
            fagsakId = fagsakId,
            journalførendeEnhet = journalførendeEnhet,
            eksternReferanseId = eksternReferanseId,
            avsenderMottaker = AvsenderMottaker(
                id = bruker.ident,
                idType = AvsenderMottakerIdType.FNR,
                navn = bruker.navn,
            ),
        )

    fun journalførVedtaksbrev(
        behov: VedtaksbrevJournalføringBehov,
    ): OpprettJournalpostResponse {
        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = opprettArkiverDokumentRequest(
                bruker = behov.bruker,
                pdfDocument = pdfGenClient.hentPdfForVedtak(hentVedtaksbrevData(behov.behandlingId, behov.vedtaksbrevInfo, behov.beslutter)),
                fagsakId = behov.fagsakId,
                tittel = lagTittel(behov),
                journalførendeEnhet = behov.journalførendeEnhet,
                eksternReferanseId = lagEksternReferanseId(behov.behandlingId, Brevtype.VEDTAK, Brevmottager.BRUKER),
                filnavn = "vedtak.pdf",
            ),
            fagsaksystem = behov.ytelse.tilDokarkivFagsaksystem(),
            brevkode = behov.ytelse.tilFagsystemDTO().name + "-TILB",
            tema = behov.ytelse.tilTema(),
            dokuemntkategori = DokumentKlasse.VB,
            behandlingId = behov.behandlingId,
        )
    }

    fun journalførRevurderingVedtaksbrev(
        behov: RevurderingVedtaksbrevJournalføringBehov,
    ): OpprettJournalpostResponse {
        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = opprettArkiverDokumentRequest(
                bruker = behov.bruker,
                pdfDocument = pdfGenClient.hentPdfForVedtak(hentVedtaksbrevData(behov.behandlingId, behov.revurderingVedtaksbrevInfo, behov.beslutter)),
                fagsakId = behov.fagsakId,
                tittel = TITTEL_REVURDERING_TILBAKEKREVING,
                journalførendeEnhet = behov.journalførendeEnhet,
                eksternReferanseId = lagEksternReferanseId(behov.behandlingId, Brevtype.VEDTAK, Brevmottager.BRUKER),
                filnavn = "revurdering_vedtak.pdf",
            ),
            fagsaksystem = behov.ytelse.tilDokarkivFagsaksystem(),
            brevkode = behov.ytelse.tilFagsystemDTO().name + "-TILB",
            tema = behov.ytelse.tilTema(),
            dokuemntkategori = DokumentKlasse.VB,
            behandlingId = behov.behandlingId,
        )
    }

    fun distribuereVedtaksbrev(behov: VedtaksbrevDistribusjonBehov, logContext: SecureLog.Context): String {
        return dokdistClient.brevTilUtsending(
            behandlingId = behov.behandlingId,
            journalpostId = behov.journalpostId,
            fagsystem = behov.fagsystem,
            distribusjonstype = Distribusjonstype.VEDTAK,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            adresse = null,
            logContext = logContext,
        ).bestillingsId
    }

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun tilEntity(dto: RotElementUpdateItemDto): VedtaksbrevDataRepository.UnderavsnittEntity? {
        return when (dto) {
            is RentekstElementDto -> VedtaksbrevDataRepository.UnderavsnittEntity(
                type = VedtaksbrevDataRepository.UnderavsnittEntity.Type.RENTEKST,
                tittel = null,
                tekst = dto.tekst,
                underavsnitt = null,
            )

            is UnderavsnittElementDto -> VedtaksbrevDataRepository.UnderavsnittEntity(
                type = VedtaksbrevDataRepository.UnderavsnittEntity.Type.UNDERAVSNITT,
                tittel = dto.tittel,
                tekst = null,
                underavsnitt = dto.underavsnitt.mapNotNull(::tilEntity),
            )

            is PakrevdBegrunnelseDto, is PakrevdBegrunnelseUpdateItemDto -> null
        }
    }

    fun tilPåkrevdBegrunnelseEntities(dto: RotElementUpdateItemDto) = when (dto) {
        is PakrevdBegrunnelseUpdateItemDto -> VedtaksbrevDataRepository.PåkrevdBegrunnelse(
            dto.begrunnelseType,
            dto.underavsnitt.map { it.tekst },
        )
        else -> null
    }

    private fun lagTittel(vedtaksbrevBehov: VedtaksbrevJournalføringBehov): String {
        val tittel =
            when (vedtaksbrevBehov.vedtaksresultat) {
                Vedtaksresultat.INGEN_TILBAKEBETALING -> TITTEL_VEDTAK_INGEN_TILBAKEBETALING
                else -> TITTEL_VEDTAK_TILBAKEBETALING
            }

        return "$tittel ${vedtaksbrevBehov.ytelse.hentYtelsesnavn(Språkkode.NB)}"
    }

    fun tilStandardtekst(bunntekst: Bunntekst) = StandardtekstDto(
        tittel = bunntekst.tittel,
        underavsnitt = bunntekst.avsnitt.map(::RentekstElementDto),
    )

    companion object {
        private const val TITTEL_VEDTAK_TILBAKEBETALING = "Vedtak tilbakebetaling "
        private const val TITTEL_VEDTAK_INGEN_TILBAKEBETALING = "Vedtak ingen tilbakebetaling "
        private const val TITTEL_REVURDERING_TILBAKEKREVING = "Vedtak om revurdering av tilbakekreving "
    }
}
