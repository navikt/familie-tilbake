package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class InnhentDokumentasjonbrevService(private val fagsakRepository: FagsakRepository,
                                      private val behandlingRepository: BehandlingRepository,
                                      private val eksterneDataForBrevService: EksterneDataForBrevService,
                                      private val pdfBrevService: PdfBrevService) {

    fun hentForhåndsvisningInnhentDokumentasjonBrev(behandlingId: UUID,
                                                    fritekst: String): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val brevmottager: Brevmottager = if (behandling.harVerge) Brevmottager.VERGE else Brevmottager.BRUKER
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val dokumentasjonsbrevSamletInfo =
                settOppInnhentDokumentasjonsbrevSamletInfo(behandling, fagsak, fritekst, brevmottager)
        val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokumentasjonsbrevSamletInfo)
        return pdfBrevService.genererForhåndsvisning(Brevdata(mottager = brevmottager,
                                                              metadata = fritekstbrevsdata.brevmetadata,
                                                              overskrift = fritekstbrevsdata.overskrift,
                                                              brevtekst = fritekstbrevsdata.brevtekst))
    }

    private fun lagInnhentDokumentasjonsbrev(dokumentasjonsbrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo)
            : Fritekstbrevsdata {
        val overskrift =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsoverskrift(dokumentasjonsbrevSamletInfo)
        val brevtekst =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsfritekst(dokumentasjonsbrevSamletInfo)
        return Fritekstbrevsdata(overskrift = overskrift,
                                 brevtekst = brevtekst,
                                 brevmetadata = dokumentasjonsbrevSamletInfo.brevmetadata)
    }

    private fun settOppInnhentDokumentasjonsbrevSamletInfo(behandling: Behandling,
                                                           fagsak: Fagsak,
                                                           fritekst: String,
                                                           brevmottager: Brevmottager): InnhentDokumentasjonsbrevSamletInfo {
        //verge
        val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
                eksterneDataForBrevService.hentAdresse(personinfo, brevmottager, behandling.aktivVerge, fagsak.fagsystem)
        val vergeNavn = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)
        val brevMetadata = Brevmetadata(sakspartId = personinfo.ident,
                                        sakspartsnavn = personinfo.navn,
                                        finnesVerge = behandling.harVerge,
                                        vergenavn = vergeNavn,
                                        mottageradresse = adresseinfo,
                                        behandlendeEnhetId = behandling.behandlendeEnhet,
                                        behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                                        ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
                                        saksnummer = fagsak.eksternFagsakId,
                                        språkkode = fagsak.bruker.språkkode,
                                        ytelsestype = fagsak.ytelsestype,
                                        tittel = getTittel(brevmottager) + fagsak.ytelsestype.navn[Språkkode.NB])
        return InnhentDokumentasjonsbrevSamletInfo(brevmetadata = brevMetadata,
                                                   fristdato = LocalDate.now().plus(Constants.brukersSvarfrist),
                                                   fritekstFraSaksbehandler = fritekst)
    }

    private fun getTittel(brevmottager: Brevmottager): String {
        return if (Brevmottager.VERGE == brevmottager) TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE
        else TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG
    }

    companion object {

        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG = "Innhent dokumentasjon Tilbakekreving"
        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE = "Innhent dokumentasjon Tilbakekreving til verge"
    }
}
