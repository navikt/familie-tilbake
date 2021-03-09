package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.FeilutbetaltePerioderDto
import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
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
                                                finnesVerge: Boolean,
                                                vergeNavn: String?): Varselbrevsdokument {
        val metadata = Brevmetadata(sakspartId = personinfo.ident,
                                    sakspartsnavn = personinfo.navn,
                                    finnesVerge = finnesVerge,
                                    vergenavn = vergeNavn,
                                    mottageradresse = adresseinfo,
                                    behandlendeEnhetId = behandling.behandlendeEnhet,
                                    behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                                    ansvarligSaksbehandler = "VL",
                                    saksnummer = fagsak.eksternFagsakId,
                                    språkkode = fagsak.bruker.språkkode,
                                    ytelsestype = fagsak.ytelsestype,
                                    tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, false))
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
                                    saksnummer = request.saksnummer,
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

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling: Behandling,
                                                                 personinfo: Personinfo,
                                                                 adresseinfo: Adresseinfo,
                                                                 fagsak: Fagsak,
                                                                 friTekst: String?,
                                                                 feilutbetalingFakta: FaktaFeilutbetalingDto,
                                                                 finnesVerge: Boolean,
                                                                 vergeNavn: String?,
                                                                 erKorrigert: Boolean,
                                                                 varsel: Varsel?): Varselbrevsdokument {
        val metadata = Brevmetadata(sakspartId = personinfo.ident,
                                    sakspartsnavn = personinfo.navn,
                                    finnesVerge = finnesVerge,
                                    vergenavn = vergeNavn,
                                    mottageradresse = adresseinfo,
                                    behandlendeEnhetId = behandling.behandlendeEnhet,
                                    behandlendeEnhetsNavn = behandling.behandlendeEnhetsNavn,
                                    ansvarligSaksbehandler = "VL",
                                    saksnummer = fagsak.eksternFagsakId,
                                    språkkode = fagsak.bruker.språkkode,
                                    ytelsestype = fagsak.ytelsestype,
                                    tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert))
        return Varselbrevsdokument(brevmetadata = metadata,
                                   beløp = feilutbetalingFakta.totaltFeilutbetaltBeløp.toLong(),
                                   endringsdato = feilutbetalingFakta.revurderingsvedtaksdato,
                                   fristdatoForTilbakemelding = LocalDate.now().plus(Constants.brukersSvarfrist),
                                   varseltekstFraSaksbehandler = friTekst,
                                   feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingFakta),
                                   erKorrigert = varsel != null,
                                   varsletDato = varsel?.sporbar?.opprettetTid?.toLocalDate(),
                                   varsletBeløp = varsel?.varselbeløp)
    }

    private fun getTittelForVarselbrev(ytelseNavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelseNavn
        else TITTEL_VARSEL_TILBAKEBETALING + ytelseNavn
    }

    private fun mapFeilutbetaltePerioder(feilutbetaltePerioderDto: FeilutbetaltePerioderDto): List<Periode> {
        return feilutbetaltePerioderDto.perioder.map { Periode(it.fom, it.tom) }
    }

    private fun mapFeilutbetaltePerioder(varsel: Varsel?): List<Periode> {
        return varsel?.perioder?.map { Periode(it.fom, it.tom) } ?: emptyList()
    }

    private fun mapFeilutbetaltePerioder(feilutbetalingFakta: FaktaFeilutbetalingDto): List<Periode> {
        return feilutbetalingFakta.feilutbetaltePerioder.map { Periode(it.periode.fom, it.periode.tom) }
    }
}
