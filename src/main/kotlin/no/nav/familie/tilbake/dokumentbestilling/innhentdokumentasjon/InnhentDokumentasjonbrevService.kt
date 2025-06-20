package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.DistribusjonshåndteringService
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmetadataUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.innhentdokumentasjon.TekstformatererInnhentDokumentasjonsbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InnhentDokumentasjonbrevService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val organisasjonService: OrganisasjonService,
    private val distribusjonshåndteringService: DistribusjonshåndteringService,
    private val brevmetadataUtil: BrevmetadataUtil,
) {
    fun sendInnhentDokumentasjonBrev(
        behandling: Behandling,
        fritekst: String,
        brevmottager: Brevmottager? = null,
    ) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        if (brevmottager == null) {
            distribusjonshåndteringService.sendBrev(behandling, Brevtype.INNHENT_DOKUMENTASJON) { mottaker, brevmetadata ->
                val dokument = settOppInnhentDokumentasjonsbrevsdokument(behandling, fagsak, fritekst, mottaker, brevmetadata, logContext)
                val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokument)
                Brevdata(
                    mottager = mottaker,
                    metadata = fritekstbrevsdata.brevmetadata,
                    overskrift = fritekstbrevsdata.overskrift,
                    brevtekst = fritekstbrevsdata.brevtekst,
                )
            }
        } else {
            val dokument = settOppInnhentDokumentasjonsbrevsdokument(behandling, fagsak, fritekst, brevmottager, null, logContext)
            val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokument)
            val brevdata =
                Brevdata(
                    mottager = brevmottager,
                    metadata = fritekstbrevsdata.brevmetadata,
                    overskrift = fritekstbrevsdata.overskrift,
                    brevtekst = fritekstbrevsdata.brevtekst,
                )
            pdfBrevService.sendBrev(
                behandling = behandling,
                fagsak = fagsak,
                brevtype = Brevtype.INNHENT_DOKUMENTASJON,
                data = brevdata,
                fritekst = fritekst,
            )
        }
    }

    fun hentForhåndsvisningInnhentDokumentasjonBrev(
        behandlingId: UUID,
        fritekst: String,
    ): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val (metadata, brevmottager) =
            brevmetadataUtil.lagBrevmetadataForMottakerTilForhåndsvisning(behandlingId)
        val dokument = settOppInnhentDokumentasjonsbrevsdokument(behandling, fagsak, fritekst, brevmottager, metadata, logContext)
        val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokument)

        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                metadata = fritekstbrevsdata.brevmetadata,
                overskrift = fritekstbrevsdata.overskrift,
                brevtekst = fritekstbrevsdata.brevtekst,
            ),
        )
    }

    private fun lagInnhentDokumentasjonsbrev(dokument: InnhentDokumentasjonsbrevsdokument): Fritekstbrevsdata {
        val overskrift =
            TekstformatererInnhentDokumentasjonsbrev.lagOverskrift(dokument.brevmetadata)
        val brevtekst = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)
        return Fritekstbrevsdata(
            overskrift = overskrift,
            brevtekst = brevtekst,
            brevmetadata = dokument.brevmetadata,
        )
    }

    private fun settOppInnhentDokumentasjonsbrevsdokument(
        behandling: Behandling,
        fagsak: Fagsak,
        fritekst: String,
        brevmottager: Brevmottager,
        forhåndsgenerertMetadata: Brevmetadata?,
        logContext: SecureLog.Context,
    ): InnhentDokumentasjonsbrevsdokument {
        val brevmetadata =
            forhåndsgenerertMetadata ?: run {
                val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem, logContext)
                val adresseinfo: Adresseinfo =
                    eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem, logContext)
                val vergenavn = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)
                val ansvarligSaksbehandler =
                    eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(behandling.ansvarligSaksbehandler, logContext)
                val gjelderDødsfall = personinfo.dødsdato != null

                Brevmetadata(
                    sakspartId = personinfo.ident,
                    sakspartsnavn = personinfo.navn,
                    finnesVerge = behandling.harVerge,
                    vergenavn = vergenavn,
                    mottageradresse = adresseinfo,
                    behandlendeEnhetId = behandling.behandlendeEnhet,
                    behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                    ansvarligSaksbehandler = ansvarligSaksbehandler,
                    saksnummer = fagsak.eksternFagsakId,
                    språkkode = fagsak.bruker.språkkode,
                    ytelsestype = fagsak.ytelsestype.tilDTO(),
                    gjelderDødsfall = gjelderDødsfall,
                    institusjon =
                        fagsak.institusjon?.let {
                            organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
                        },
                )
            }
        return InnhentDokumentasjonsbrevsdokument(
            brevmetadata = brevmetadata.copy(tittel = getTittel(brevmottager) + fagsak.ytelsestype.navn[Språkkode.NB]),
            fristdato = Constants.brukersSvarfrist(),
            fritekstFraSaksbehandler = fritekst,
        )
    }

    private fun getTittel(brevmottager: Brevmottager): String =
        if (Brevmottager.VERGE == brevmottager) {
            TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE
        } else {
            TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG
        }

    companion object {
        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG = "Innhent dokumentasjon Tilbakekreving"
        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE =
            "Innhent dokumentasjon Tilbakekreving til verge"
    }
}
