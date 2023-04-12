package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.Brevdata
import no.nav.familie.tilbake.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.Fritekstbrevsdata
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.organisasjon.OrganisasjonService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InnhentDokumentasjonbrevService(
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val pdfBrevService: PdfBrevService,
    private val organisasjonService: OrganisasjonService
) {

    fun sendInnhentDokumentasjonBrev(behandling: Behandling, fritekst: String, brevmottager: Brevmottager) {
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val dokument = settOppInnhentDokumentasjonsbrevsdokument(behandling, fagsak, fritekst, brevmottager)
        val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokument)
        val brevdata = Brevdata(
            mottager = brevmottager,
            metadata = fritekstbrevsdata.brevmetadata,
            overskrift = fritekstbrevsdata.overskrift,
            brevtekst = fritekstbrevsdata.brevtekst
        )
        pdfBrevService.sendBrev(
            behandling = behandling,
            fagsak = fagsak,
            brevtype = Brevtype.INNHENT_DOKUMENTASJON,
            data = brevdata,
            fritekst = fritekst
        )
    }

    fun hentForhåndsvisningInnhentDokumentasjonBrev(
        behandlingId: UUID,
        fritekst: String
    ): ByteArray {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val brevmottager: Brevmottager = BrevmottagerUtil.utledBrevmottager(behandling, fagsak)
        val dokument = settOppInnhentDokumentasjonsbrevsdokument(behandling, fagsak, fritekst, brevmottager)
        val fritekstbrevsdata: Fritekstbrevsdata = lagInnhentDokumentasjonsbrev(dokument)

        return pdfBrevService.genererForhåndsvisning(
            Brevdata(
                mottager = brevmottager,
                metadata = fritekstbrevsdata.brevmetadata,
                overskrift = fritekstbrevsdata.overskrift,
                brevtekst = fritekstbrevsdata.brevtekst
            )
        )
    }

    private fun lagInnhentDokumentasjonsbrev(dokument: InnhentDokumentasjonsbrevsdokument): Fritekstbrevsdata {
        val overskrift =
            TekstformatererInnhentDokumentasjonsbrev.lagOverskrift(dokument.brevmetadata)
        val brevtekst = TekstformatererInnhentDokumentasjonsbrev.lagFritekst(dokument)
        return Fritekstbrevsdata(
            overskrift = overskrift,
            brevtekst = brevtekst,
            brevmetadata = dokument.brevmetadata
        )
    }

    private fun settOppInnhentDokumentasjonsbrevsdokument(
        behandling: Behandling,
        fagsak: Fagsak,
        fritekst: String,
        brevmottager: Brevmottager
    ): InnhentDokumentasjonsbrevsdokument {
        val personinfo: Personinfo = eksterneDataForBrevService.hentPerson(fagsak.bruker.ident, fagsak.fagsystem)

        val adresseinfo = eksterneDataForBrevService.hentAdresse(
            personinfo,
            brevmottager,
            behandling.aktivVerge,
            fagsak.fagsystem,
            behandling.id
        )
        val vergenavn = BrevmottagerUtil.getVergenavn(behandling.aktivVerge, adresseinfo)

        val ansvarligSaksbehandler =
            eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(behandling.ansvarligSaksbehandler)
        val gjelderDødsfall = personinfo.dødsdato != null

        val brevmetadata = Brevmetadata(
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
            ytelsestype = fagsak.ytelsestype,
            tittel = getTittel(brevmottager) + fagsak.ytelsestype.navn[Språkkode.NB],
            gjelderDødsfall = gjelderDødsfall,
            institusjon = fagsak.institusjon?.let {
                organisasjonService.mapTilInstitusjonForBrevgenerering(it.organisasjonsnummer)
            }
        )
        return InnhentDokumentasjonsbrevsdokument(
            brevmetadata = brevmetadata,
            fristdato = Constants.brukersSvarfrist(),
            fritekstFraSaksbehandler = fritekst
        )
    }

    private fun getTittel(brevmottager: Brevmottager): String {
        return if (Brevmottager.VERGE == brevmottager) {
            TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE
        } else {
            TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG
        }
    }

    companion object {

        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG = "Innhent dokumentasjon Tilbakekreving"
        const val TITTEL_INNHENTDOKUMENTASJONBREV_HISTORIKKINNSLAG_TIL_VERGE =
            "Innhent dokumentasjon Tilbakekreving til verge"
    }
}
