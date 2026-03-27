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
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
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
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
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
import no.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
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
    fun hentVedtaksbrevData(behandlingId: UUID, vedtaksbrevInfo: VedtaksbrevInfo): VedtaksbrevDataDto {
        val signatur = SignaturDto(
            vedtaksbrevInfo.signatur.ansvarligEnhet,
            eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrevInfo.signatur.ansvarligSaksbehandlerIdent),
            vedtaksbrevInfo.signatur.ansvarligBeslutterIdent?.let(eksterneDataForBrevService::hentSaksbehandlernavn),
        )

        val (sistOppdatert, lagredeData) = vedtaksbrevDataRepository.hentVedtaksbrevData(behandlingId) ?: return VedtaksbrevDataDto(
            hovedavsnitt = HovedavsnittDto(
                tittel = BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo),
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(RentekstElementDto("")),
            ),
            avsnitt = vedtaksbrevInfo.perioder.map(BrevFormatterer::lagAvsnitt),
            sistOppdatert = OffsetDateTime.now(),
            brevGjelder = vedtaksbrevInfo.brukerdata,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            ytelse = vedtaksbrevInfo.ytelse,
            bunntekster = vedtaksbrevInfo.bunntekster.map(::tilStandardtekst),
            signatur = signatur,
            saksnummer = vedtaksbrevInfo.tilbakekrevingId,
        )

        return VedtaksbrevDataDto(
            hovedavsnitt = mapHovedavsnitt(lagredeData.hovedavsnitt, vedtaksbrevInfo),
            avsnitt = vedtaksbrevInfo.perioder.map {
                mapAvsnitt(it, lagredeData.avsnitt)
            },
            brevGjelder = vedtaksbrevInfo.brukerdata,
            ytelse = vedtaksbrevInfo.ytelse,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
            bunntekster = vedtaksbrevInfo.bunntekster.map(::tilStandardtekst),
            signatur = signatur,
            saksnummer = vedtaksbrevInfo.tilbakekrevingId,
        )
    }

    fun oppdaterVedtaksbrevData(
        behandlingId: UUID,
        data: VedtaksbrevRedigerbareDataUpdateDto,
        info: VedtaksbrevInfo,
    ): VedtaksbrevRedigerbareDataDto {
        val (sistOppdatert, data) = vedtaksbrevDataRepository.oppdaterVedtaksbrevData(
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
            tittel = BrevFormatterer.lagHovedavsnittTittel(info),
            forklaring = Forklaringstekster.HOVEDAVSNITT,
            underavsnitt = data.underavsnitt.map {
                mapUnderavsnitt(it)
            },
        )
    }

    fun mapAvsnitt(
        periode: BegrunnetPeriode,
        avsnitt: List<VedtaksbrevDataRepository.PeriodeavsnittEntity>,
    ): AvsnittDto {
        val lagretAvsnitt = avsnitt
            .firstOrNull { lagretAvsnitt -> lagretAvsnitt.id == periode.id }
            ?: return BrevFormatterer.lagAvsnitt(periode)
        return AvsnittDto(
            forklaring = Forklaringstekster.PERIODE_AVSNITT,
            id = lagretAvsnitt.id,
            meldingerTilSaksbehandler = periode.meldingerTilSaksbehandler
                .forPeriodeavsnitt()
                .map { it.melding },
            tittel = BrevFormatterer.lagPeriodeavsnittTittel(periode.periode),
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
        }
    }

    fun journalførVedtaksbrev(
        behov: VedtaksbrevJournalføringBehov,
    ): OpprettJournalpostResponse {
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = behov.bruker.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = pdfGenClient.hentPdfForVedtak(hentVedtaksbrevData(behov.behandlingId, behov.vedtaksbrevInfo)),
                    filtype = Filtype.PDFA,
                    filnavn = "vedtak.pdf",
                    tittel = lagTittel(behov),
                    dokumenttype = null,
                ),
            ),
            fagsakId = behov.fagsakId,
            journalførendeEnhet = behov.journalførendeEnhet,
            eksternReferanseId = lagEksternReferanseId(behov.behandlingId, Brevtype.VEDTAK, Brevmottager.BRUKER),
            avsenderMottaker = AvsenderMottaker(
                id = behov.bruker.ident,
                idType = AvsenderMottakerIdType.FNR,
                navn = behov.bruker.navn,
            ),
        )

        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = arkiverDokumentRequest,
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
    }
}
