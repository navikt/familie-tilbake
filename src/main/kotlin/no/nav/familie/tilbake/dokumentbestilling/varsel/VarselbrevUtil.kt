package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VarselbrevUtil(private val eksterneDataForBrevService: EksterneDataForBrevService) {

    companion object {

        private const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "
        private const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    }

    fun sammenstillInfoFraFagsystemerForSending(fagsak: Fagsak,
                                                behandling: Behandling,
                                                adresseinfo: Adresseinfo,
                                                personinfo: Personinfo,
                                                varsel: Varsel?,
                                                vergenavn: String?): Varselbrevsdokument {
        val metadata = sammenstillInfoForBrevmetadata(behandling,
                                                      personinfo,
                                                      adresseinfo,
                                                      fagsak,
                                                      vergenavn,
                                                      false)

        return Varselbrevsdokument(brevmetadata = metadata,
                                   beløp = varsel?.varselbeløp ?: 0L,
                                   revurderingsvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                                   fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
                                   varseltekstFraSaksbehandler = varsel?.varseltekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(varsel))

    }

    fun sammenstillInfoForForhåndvisningVarselbrev(adresseinfo: Adresseinfo,
                                                   request: ForhåndsvisVarselbrevRequest,
                                                   personinfo: Personinfo): Varselbrevsdokument {

        val tittel = getTittelForVarselbrev(request.ytelsestype.navn[request.språkkode]!!, false)
        val vergenavn = BrevmottagerUtil.getVergenavn(request.verge, adresseinfo)
        val ansvarligSaksbehandler =
                eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(ContextService.hentSaksbehandler())

        val metadata = Brevmetadata(behandlendeEnhetId = request.behandlendeEnhetId,
                                    behandlendeEnhetsNavn = request.behandlendeEnhetsNavn,
                                    sakspartId = personinfo.ident,
                                    mottageradresse = adresseinfo,
                                    saksnummer = request.eksternFagsakId,
                                    sakspartsnavn = personinfo.navn,
                                    finnesVerge = request.verge != null,
                                    vergenavn = vergenavn,
                                    ytelsestype = request.ytelsestype,
                                    språkkode = request.språkkode,
                                    ansvarligSaksbehandler = ansvarligSaksbehandler,
                                    tittel = tittel)

        return Varselbrevsdokument(brevmetadata = metadata,
                                   beløp = request.feilutbetaltePerioderDto.sumFeilutbetaling,
                                   revurderingsvedtaksdato = request.vedtaksdato ?: LocalDate.now(),
                                   fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
                                   varseltekstFraSaksbehandler = request.varseltekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(request.feilutbetaltePerioderDto))
    }

    fun sammenstillInfoForBrevmetadata(behandling: Behandling,
                                       personinfo: Personinfo,
                                       adresseinfo: Adresseinfo,
                                       fagsak: Fagsak,
                                       vergenavn: String?,
                                       erKorrigert: Boolean): Brevmetadata {
        val ansvarligSaksbehandler =
                eksterneDataForBrevService.hentPåloggetSaksbehandlernavnMedDefault(behandling.ansvarligSaksbehandler)

        return Brevmetadata(sakspartId = personinfo.ident,
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
                            tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert))

    }


    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(metadata: Brevmetadata,
                                                                 fritekst: String?,
                                                                 feilutbetalingsfakta: FaktaFeilutbetalingDto,
                                                                 varsel: Varsel?): Varselbrevsdokument {

        return Varselbrevsdokument(brevmetadata = metadata,
                                   beløp = feilutbetalingsfakta.totaltFeilutbetaltBeløp.toLong(),
                                   revurderingsvedtaksdato = feilutbetalingsfakta.revurderingsvedtaksdato,
                                   fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
                                   varseltekstFraSaksbehandler = fritekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingsfakta),
                                   erKorrigert = varsel != null,
                                   varsletDato = varsel?.sporbar?.opprettetTid?.toLocalDate(),
                                   varsletBeløp = varsel?.varselbeløp)
    }

    private fun getTittelForVarselbrev(ytelsesnavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelsesnavn
        else TITTEL_VARSEL_TILBAKEBETALING + ytelsesnavn
    }

    private fun mapFeilutbetaltePerioder(feilutbetaltePerioderDto: FeilutbetaltePerioderDto): List<Handlebarsperiode> {
        return feilutbetaltePerioderDto.perioder.map { Handlebarsperiode(it.fom, it.tom) }
    }

    private fun mapFeilutbetaltePerioder(varsel: Varsel?): List<Handlebarsperiode> {
        return varsel?.perioder?.map { Handlebarsperiode(it.fom, it.tom) } ?: emptyList()
    }

    private fun mapFeilutbetaltePerioder(feilutbetalingsfakta: FaktaFeilutbetalingDto): List<Handlebarsperiode> {
        return feilutbetalingsfakta.feilutbetaltePerioder.map { Handlebarsperiode(it.periode.fom, it.periode.tom) }
    }

}
