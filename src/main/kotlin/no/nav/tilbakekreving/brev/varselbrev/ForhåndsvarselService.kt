package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil
import no.nav.familie.tilbake.dokumentbestilling.varsel.VarselbrevUtil.Companion.TITTEL_VARSEL_TILBAKEBETALING
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.FeatureToggles
import no.nav.tilbakekreving.LesContext
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.behandling.UttalelseInfo
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarselbrevDataDto
import no.nav.tilbakekreving.pdf.Dokumentvariant
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokprodTilHtml
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import no.nav.tilbakekreving.breeeev.standardtekster.forhåndsvarsel.Bunntekst
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderAvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarselbrevTeksterDto

@Service
class ForhåndsvarselService(
    private val pdfBrevService: PdfBrevService,
    private val varselbrevUtil: VarselbrevUtil,
    private val dokarkivClient: DokarkivClient,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfGenClient: PdfGenClient,
    private val pdfFactory: () -> PdfGenerator = { PdfGenerator() },
) {
    private val logger = TracedLogger.getLogger<ForhåndsvarselService>()

    fun hentVarselbrevTekster(context: LesContext, behandlingId: UUID, tilbakekreving: Tilbakekreving): Varselbrevtekst {
        val varselbrevInfo = tilbakekreving.hentVarselbrevInfo(behandlingId, context)
        val brevmetadata = opprettMetadata(varselbrevInfo)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false)
        val brevbody = TekstformatererVarselbrev.lagFritekst(opprettVarselbrevsdokument(varselbrevInfo, brevmetadata), false)

        val varselbrevAvsnitter = VarselbrevParser.parse(brevbody)
        val varselbrevtekst = Varselbrevtekst(overskrift = overskrift, avsnitter = varselbrevAvsnitter)
        return varselbrevtekst
    }

    fun forhåndsvisVarselbrev(
        context: LesContext,
        tilbakekreving: Tilbakekreving,
        bestillBrevDto: BestillBrevDto,
    ): ByteArray {
        val varselbrevInfo = tilbakekreving.hentVarselbrevInfo(bestillBrevDto.behandlingId, context)
        val varselbrevsdokument = opprettVarselbrevsdokument(
            varselbrevInfo = varselbrevInfo,
            brevmetadata = opprettMetadata(varselbrevInfo),
        ).copy(varseltekstFraSaksbehandler = bestillBrevDto.fritekst)

        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = Brevmottager.BRUKER,
                overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(varselbrevsdokument.brevmetadata, false),
                brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false),
                metadata = varselbrevsdokument.brevmetadata,
                vedleggHtml = varselbrevUtil.lagVedlegg(varselbrevsdokument, bestillBrevDto.behandlingId),
            ),
        )
    }

    fun lagreUttalelse(
        tilbakekreving: Tilbakekreving,
        behandlingId: UUID,
        brukeruttalelse: BrukeruttalelseDto,
        sideeffektContext: SideeffektContext,
    ) {
        when (brukeruttalelse.harBrukerUttaltSeg) {
            HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL, HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG -> {
                val uttalelsedetaljer = requireNotNull(brukeruttalelse.uttalelsesdetaljer) {
                    "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"
                }.also {
                    require(it.isNotEmpty()) {
                        "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tom"
                    }
                }[0]
                tilbakekreving.gjørSaksbehandling(behandlingId, sideeffektContext) {
                    lagreUttalelse(
                        uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                        uttalelseInfo = UttalelseInfo(UUID.randomUUID(), uttalelsedetaljer.uttalelsesdato, uttalelsedetaljer.hvorBrukerenUttalteSeg, uttalelsedetaljer.uttalelseBeskrivelse),
                        kommentar = null,
                    )
                }
            }
            HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL, HarBrukerUttaltSeg.UNNTAK_INGEN_UTTALELSE -> {
                val kommentar = requireNotNull(brukeruttalelse.kommentar) {
                    "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null"
                }.also {
                    require(it.isNotBlank()) { "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tom" }
                }

                tilbakekreving.gjørSaksbehandling(behandlingId, sideeffektContext) {
                    lagreUttalelse(
                        uttalelseVurdering = UttalelseVurdering.valueOf(brukeruttalelse.harBrukerUttaltSeg.name),
                        uttalelseInfo = null,
                        kommentar = kommentar,
                    )
                }
            }
            else -> throw IllegalArgumentException("Ukjent verdi for uttalelseVurdering: ${brukeruttalelse.harBrukerUttaltSeg} ")
        }
    }

    fun nyLagreUttalelse(behandlingId: UUID, tilbakekreving: Tilbakekreving, uttalelseDto: UttalelseDto, sideeffektContext: SideeffektContext) {
        val uttalelseVurdering = when (uttalelseDto.harBrukerUttaltSeg) {
            UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL -> UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL
            UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL -> UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL
            UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG -> UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG
            UttalelseVurderingDto.UNNTAK_INGEN_UTTALELSE -> UttalelseVurdering.UNNTAK_INGEN_UTTALELSE
            UttalelseVurderingDto.IKKE_VURDERT -> throw IllegalStateException(
                "Burde ikke være i denne tilstanden. IKKE_VURDERT er enum til frontend.",
            )
        }

        when (uttalelseVurdering) {
            UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG, UttalelseVurdering.JA -> {
                tilbakekreving.gjørSaksbehandling(behandlingId, sideeffektContext) {
                    lagreUttalelse(
                        uttalelseVurdering = uttalelseVurdering,
                        uttalelseInfo = UttalelseInfo(
                            id = UUID.randomUUID(),
                            uttalelsesdato = requireNotNull(uttalelseDto.uttalelsesdato) { "Det kreves uttalelsesdato når brukeren har uttalet seg. uttalelsesdato var null" },
                            hvorBrukerenUttalteSeg = requireNotNull(uttalelseDto.hvorBrukerenUttalteSeg) {
                                "Det kreves hvorBrukerenUttalteSeg når brukeren har uttalet seg. hvorBrukerenUttalteSeg var null"
                            },
                            uttalelseBeskrivelse = requireNotNull(uttalelseDto.beskrivelse) {
                                "Det kreves beskrivelse når brukeren har uttalet seg. beskrivelse var null"
                            },
                        ),
                        kommentar = null,
                    )
                }
            }
            UttalelseVurdering.NEI_ETTER_FORHÅNDSVARSEL, UttalelseVurdering.UNNTAK_INGEN_UTTALELSE, UttalelseVurdering.NEI -> {
                tilbakekreving.gjørSaksbehandling(behandlingId, sideeffektContext) {
                    lagreUttalelse(
                        uttalelseVurdering = uttalelseVurdering,
                        uttalelseInfo = null,
                        kommentar = requireNotNull(uttalelseDto.beskrivelse) {
                            "Det kreves kommentar/beskrivelse når brukeren ikke uttalte seg. beskrivelse var null"
                        },
                    )
                }
            }
        }
    }

    private fun opprettMetadata(varselbrevInfo: VarselbrevInfo): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevInfo.brukerinfo.ident,
            sakspartsnavn = varselbrevInfo.brukerinfo.navn,
            mottageradresse = Adresseinfo(varselbrevInfo.brukerinfo.ident, varselbrevInfo.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevInfo.forhåndsvarselinfo.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(varselbrevInfo.forhåndsvarselinfo.ansvarligSaksbehandler.ident),
            saksnummer = varselbrevInfo.eksternFagsakId,
            språkkode = varselbrevInfo.brukerinfo.språkkode,
            ytelsestype = varselbrevInfo.ytelseType,
            gjelderDødsfall = varselbrevInfo.brukerinfo.dødsdato != null,
        )
    }

    private fun opprettVarselbrevsdokument(varselbrevInfo: VarselbrevInfo, brevmetadata: Brevmetadata): Varselbrevsdokument {
        return Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevInfo.forhåndsvarselinfo.beløp,
            revurderingsvedtaksdato = varselbrevInfo.forhåndsvarselinfo.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = varselbrevInfo.opprinneligUttalelsesfrist,
            feilutbetaltePerioder = varselbrevInfo.forhåndsvarselinfo.feilutbetaltePerioder,
            varsletBeløp = varselbrevInfo.forhåndsvarselinfo.beløp,
            varsletDato = varselbrevInfo.varsletDato,
            hjemlerForTilbakekreving = BrevFormatterer.lagForhåndsvarselHjemmelAvsnitt(varselbrevInfo.hjemlerForTilbakekreving, Språkkode.NB),
            nyModell = true,
        )
    }

    fun journalførVarselbrev(
        varselbrevBehov: VarselbrevJournalføringBehov,
        logContext: SecureLog.Context,
        features: FeatureToggles,
    ): OpprettJournalpostResponse {
        val fristForUttalelse = varselbrevBehov.info.opprinneligUttalelsesfrist
        val brevdata = hentBrevdata(varselbrevBehov, fristForUttalelse, logContext)
        val dokument = Dokument(
            dokument = if (features[Toggle.ForhåndsvarselTypst]) {
                pdfGenClient.hentPdfForForhåndsvarsel(
                    VarselbrevDataDto(
                        brevGjelder = BrevmottakerDto(
                            navn = varselbrevBehov.brukerinfo.navn,
                            personIdent = varselbrevBehov.brukerinfo.ident,
                        ),
                        ytelse = varselbrevBehov.ytelse.brevmeta(),
                        fullstendigBeløp = varselbrevBehov.info.forhåndsvarselinfo.beløp.toString(),
                        fullstendigPeriode = PeriodeDto(
                            fom = varselbrevBehov.info.forhåndsvarselinfo.feilutbetaltePerioder.first().fom,
                            tom = varselbrevBehov.info.forhåndsvarselinfo.feilutbetaltePerioder.last().tom,
                        ),
                        uttalelsesfrist = varselbrevBehov.info.opprinneligUttalelsesfrist,
                        sendtDato = varselbrevBehov.info.varsletDato,
                        årsakTilFeilutbetaling = varselbrevBehov.info.tekstFraSaksbehandler,
                        foreløpigVurdering = null,
                        signatur = SignaturDto(
                            enhetNavn = varselbrevBehov.info.forhåndsvarselinfo.behandlendeEnhet!!.navn,
                            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(varselbrevBehov.info.forhåndsvarselinfo.ansvarligSaksbehandler.ident),
                            besluttendeSaksbehandler = null,
                        ),
                    ),
                )
            } else {
                hentPdf(brevdata, logContext)
            },
            filtype = Filtype.PDFA,
            filnavn = "brev.pdf",
            tittel = brevdata.tittel,
            dokumenttype = null,
        )

        val arkiverDokument =
            ArkiverDokumentRequest(
                fnr = varselbrevBehov.brukerinfo.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = varselbrevBehov.info.eksternFagsakId,
                journalførendeEnhet = varselbrevBehov.info.forhåndsvarselinfo.behandlendeEnhet!!.kode,
                avsenderMottaker = AvsenderMottaker(
                    id = varselbrevBehov.brukerinfo.ident,
                    idType = AvsenderMottakerIdType.FNR,
                    navn = varselbrevBehov.brukerinfo.navn,
                ),
                eksternReferanseId = lagEksternReferanseId(
                    varselbrevBehov.behandlingId,
                    Brevtype.VARSEL,
                    brevdata.mottager,
                ),
            )

        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = arkiverDokument,
            fagsaksystem = varselbrevBehov.ytelse.tilDokarkivFagsaksystem(),
            brevkode = varselbrevBehov.ytelse.tilFagsystemDTO().name + "-TILB",
            tema = varselbrevBehov.ytelse.tilTema(),
            dokuemntkategori = DokumentKlasse.B,
            behandlingId = varselbrevBehov.behandlingId,
        )
    }

    private fun hentBrevdata(
        varselbrevBehov: VarselbrevJournalføringBehov,
        fristForUttalelse: LocalDate,
        logContext: SecureLog.Context,
    ): Brevdata {
        val brevmetadata = hentBrevMetadata(varselbrevBehov)
        val varselbrevsdokument = Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevBehov.info.forhåndsvarselinfo.beløp,
            revurderingsvedtaksdato = varselbrevBehov.info.forhåndsvarselinfo.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = fristForUttalelse,
            varseltekstFraSaksbehandler = varselbrevBehov.info.tekstFraSaksbehandler,
            feilutbetaltePerioder = varselbrevBehov.info.forhåndsvarselinfo.feilutbetaltePerioder,
            hjemlerForTilbakekreving = BrevFormatterer.lagForhåndsvarselHjemmelAvsnitt(varselbrevBehov.info.hjemlerForTilbakekreving, Språkkode.NB),
            nyModell = true,
        )

        return Brevdata(
            mottager = Brevmottager.BRUKER,
            metadata = brevmetadata,
            tittel = brevmetadata.tittel,
            overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false),
            brevtekst = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false),
            vedleggHtml = hentVedlegg(varselbrevsdokument, varselbrevBehov.info.eksternFagsakId, logContext),
        )
    }

    private fun hentBrevMetadata(varselbrevBehov: VarselbrevJournalføringBehov): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevBehov.brukerinfo.ident,
            sakspartsnavn = varselbrevBehov.brukerinfo.navn,
            tittel = hentVarselbrevTittel(varselbrevBehov),
            mottageradresse = Adresseinfo(varselbrevBehov.brukerinfo.ident, varselbrevBehov.brukerinfo.navn),
            behandlendeEnhetsNavn = requireNotNull(varselbrevBehov.info.forhåndsvarselinfo.behandlendeEnhet) { "Enhetsnavn kreves for journalføring" }.navn,
            ansvarligSaksbehandler = eksterneDataForBrevService.hentSaksbehandlernavn(varselbrevBehov.info.forhåndsvarselinfo.ansvarligSaksbehandler.ident),
            saksnummer = varselbrevBehov.info.eksternFagsakId,
            språkkode = varselbrevBehov.brukerinfo.språkkode,
            ytelsestype = varselbrevBehov.ytelse.tilYtelseDTO(),
            gjelderDødsfall = varselbrevBehov.gjelderDødsfall,
            institusjon = null, // Todo når vi skal håndtere institusjon
        )
    }

    private fun hentVedlegg(
        varselbrevsdokument: Varselbrevsdokument,
        eksternFagsakId: String,
        logContext: SecureLog.Context,
    ): String {
        return varselbrevUtil.lagVedlegg(
            varselbrevsdokument,
            eksternFagsakId,
            varselbrevsdokument.beløp,
            logContext,
        )
    }

    private fun hentPdf(brevdata: Brevdata, logContext: SecureLog.Context): ByteArray {
        val pdfGenerator = pdfFactory()
        val html = lagHtml(brevdata)
        try {
            return pdfGenerator.genererPDFMedLogo(
                html = html,
                dokumentvariant = Dokumentvariant.ENDELIG,
                dokumenttittel = brevdata.overskrift,
            )
        } catch (e: Exception) {
            logger.medContext(logContext) {
                info("Feil ved generering av brev: brevData=$brevdata, html=$html", e)
            }
            throw e
        }
    }

    private fun lagHtml(data: Brevdata): String {
        val header = TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )
        val innholdHtml = DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun hentVarselbrevTittel(varselbrevBehov: VarselbrevJournalføringBehov): String {
        return "$TITTEL_VARSEL_TILBAKEBETALING ${varselbrevBehov.ytelse.hentYtelsesnavn(Språkkode.NB)}"
    }

    fun hentForhåndsvarselTekster(
        tilbakekreving: Tilbakekreving,
        lesContext: LesContext,
        behandlingId: UUID,
    ) : VarselbrevTeksterDto {
        val varselbrevInfo = tilbakekreving.hentVarselbrevInfo(behandlingId, lesContext)
        val ytelse = tilbakekreving.eksternFagsak.hentYtelse()
        return VarselbrevTeksterDto(
            overskrift = BrevFormatterer.lagForhåndsvarselOverskrift(ytelse),
            innledning = BrevFormatterer.forhåndsvarselInneldning(
                beløp = varselbrevInfo.forhåndsvarselinfo.beløp.toString(),
                periode = varselbrevInfo.forhåndsvarselinfo.feilutbetaltePerioder.first(),
                frist = varselbrevInfo.opprinneligUttalelsesfrist,
                ytelse = ytelse
            ),
            underAvsnitt = Bunntekst.STANDARD_BUNNTEKSTER.map {
                UnderAvsnittDto(it.tittel, it.avsnitt(ytelse).toList())
            }
        )
    }
}
