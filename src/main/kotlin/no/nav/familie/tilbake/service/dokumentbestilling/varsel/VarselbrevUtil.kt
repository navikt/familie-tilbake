package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import java.time.LocalDate

object VarselbrevUtil {

    private const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    private const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "

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
                                   endringsdato = LocalDate.now(),
                                   fristdatoForTilbakemelding = LocalDate.now().plus(Constants.brukersSvarfrist),
                                   varseltekstFraSaksbehandler = varsel?.varseltekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(varsel))

        // TODO revurderingVedtakDato = grunninformasjon.vedtakDato
    }

    fun sammenstillInfoForForhåndvisningVarselbrev(adresseinfo: Adresseinfo,
                                                   request: ForhåndsvisVarselbrevRequest,
                                                   personinfo: Personinfo): Varselbrevsdokument {

        val tittel = getTittelForVarselbrev(request.ytelsestype.navn[request.språkkode]!!, false)
        val vergenavn = BrevmottagerUtil.getVergenavn(request.verge, adresseinfo)

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
                                    ansvarligSaksbehandler = ContextService.hentSaksbehandler(),
                                    tittel = tittel)

        return Varselbrevsdokument(brevmetadata = metadata,
                                   beløp = request.feilutbetaltePerioderDto.sumFeilutbetaling,
                                   endringsdato = request.vedtaksdato ?: LocalDate.now(),
                                   fristdatoForTilbakemelding = LocalDate.now().plus(Constants.brukersSvarfrist),
                                   varseltekstFraSaksbehandler = request.varseltekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(request.feilutbetaltePerioderDto))
    }

    fun sammenstillInfoForBrevmetadata(behandling: Behandling,
                                       personinfo: Personinfo,
                                       adresseinfo: Adresseinfo,
                                       fagsak: Fagsak,
                                       vergenavn: String?,
                                       erKorrigert: Boolean): Brevmetadata {

        return Brevmetadata(sakspartId = personinfo.ident,
                            sakspartsnavn = personinfo.navn,
                            finnesVerge = behandling.harVerge,
                            vergenavn = vergenavn,
                            mottageradresse = adresseinfo,
                            behandlendeEnhetId = behandling.behandlendeEnhet,
                            behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                            ansvarligSaksbehandler = "VL",
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
                                   endringsdato = feilutbetalingsfakta.revurderingsvedtaksdato,
                                   fristdatoForTilbakemelding = LocalDate.now().plus(Constants.brukersSvarfrist),
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
