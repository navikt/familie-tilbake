package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottaker
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottakerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksternDataForBrevTjeneste
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.BrevData
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevTjeneste
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.FritekstbrevData
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class InnhentDokumentasjonbrevTjeneste(private val fagsakRepository: FagsakRepository,
                                       private val eksternDataForBrevTjeneste: EksternDataForBrevTjeneste,
                                       private val pdfBrevTjeneste: PdfBrevTjeneste) {

    fun hentForhåndsvisningInnhentDokumentasjonBrev(behandling: Behandling,
                                                    fritekst: String): ByteArray {
        val brevMottaker: BrevMottaker = if (behandling.harVerge) BrevMottaker.VERGE else BrevMottaker.BRUKER
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val dokumentasjonBrevSamletInfo =
                settOppInnhentDokumentasjonBrevSamletInfo(behandling, fagsak, fritekst, brevMottaker)
        val fritekstbrevData: FritekstbrevData = lagInnhentDokumentasjonBrev(dokumentasjonBrevSamletInfo)
        return pdfBrevTjeneste.genererForhåndsvisning(BrevData(mottaker = brevMottaker,
                                                               metadata = fritekstbrevData.brevMetadata,
                                                               overskrift = fritekstbrevData.overskrift,
                                                               brevtekst = fritekstbrevData.brevtekst))
    }

    private fun lagInnhentDokumentasjonBrev(dokumentasjonBrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo): FritekstbrevData {
        val overskrift =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevOverskrift(dokumentasjonBrevSamletInfo)
        val brevtekst = TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevFritekst(dokumentasjonBrevSamletInfo)
        return FritekstbrevData(overskrift = overskrift,
                                brevtekst = brevtekst,
                                brevMetadata = dokumentasjonBrevSamletInfo.brevMetadata)
    }

    private fun settOppInnhentDokumentasjonBrevSamletInfo(behandling: Behandling,
                                                          fagsak: Fagsak,
                                                          fritekst: String,
                                                          brevMottaker: BrevMottaker): InnhentDokumentasjonsbrevSamletInfo {
        //verge
        val personinfo: PersonInfo = eksternDataForBrevTjeneste.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)
        val adresseinfo: Adresseinfo =
                eksternDataForBrevTjeneste.hentAdresse(personinfo, brevMottaker, behandling.aktivVerge, fagsak.fagsystem)
        val vergeNavn = BrevMottakerUtil.getVergeNavn(behandling.aktivVerge, adresseinfo)
        val brevMetadata = BrevMetadata(
                sakspartId = personinfo.ident,
                sakspartNavn = personinfo.navn,
                finnesVerge = behandling.harVerge,
                vergeNavn = vergeNavn,
                mottakerAdresse = adresseinfo,
                behandlendeEnhetId = behandling.behandlendeEnhet,
                behandlendeEnhetNavn = behandling.behandlendeEnhetsNavn,
                ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
                saksnummer = fagsak.eksternFagsakId,
                språkkode = fagsak.bruker.språkkode,
                ytelsestype = fagsak.ytelsestype,
                tittel = getTittel(brevMottaker) + fagsak.ytelsestype.navn[Språkkode.NB])
        return InnhentDokumentasjonsbrevSamletInfo(brevMetadata = brevMetadata,
                                                   fristDato = LocalDate.now().plus(Constants.brukersSvarfrist),
                                                   fritekstFraSaksbehandler = fritekst)
    }

    private fun getTittel(brevMottaker: BrevMottaker): String {
        return if (BrevMottaker.VERGE == brevMottaker) TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE
        else TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG
    }

    companion object {

        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG = "Innhent dokumentasjon Tilbakekreving"
        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE = "Innhent dokumentasjon Tilbakekreving til verge"
    }
}