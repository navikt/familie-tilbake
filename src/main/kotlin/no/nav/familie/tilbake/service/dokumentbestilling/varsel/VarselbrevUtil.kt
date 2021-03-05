package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.FeilutbetaltePerioderDto
import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
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
                                                vergeNavn: String?): VarselbrevSamletInfo {
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
        return VarselbrevSamletInfo(brevmetadata = metadata,
                                    fritekstFraSaksbehandler = varsel?.varseltekst,
                                    sumFeilutbetaling = varsel?.varselbeløp ?: 0L,
                                    feilutbetaltePerioder = mapFeilutbetaltePerioder(varsel),
                                    revurderingsvedtaksdato = LocalDate.now())
        // TODO revurderingVedtakDato = grunninformasjon.vedtakDato
    }

    fun sammenstillInfoForForhåndvisningVarselbrev(adresseinfo: Adresseinfo,
                                                   request: ForhåndsvisVarselbrevRequest,
                                                   personinfo: Personinfo): VarselbrevSamletInfo {

        val tittel = getTittelForVarselbrev(request.ytelsestype.navn[request.språkkode]!!, false)
        val vergenavn = BrevmottagerUtil.getVergenavn(request.verge, adresseinfo)

        val brevMetadata = Brevmetadata(behandlendeEnhetId = request.behandlendeEnhetId,
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

        return VarselbrevSamletInfo(brevmetadata = brevMetadata,
                                    fritekstFraSaksbehandler = request.varseltekst,
                                    sumFeilutbetaling = request.feilutbetaltePerioderDto.sumFeilutbetaling,
                                    feilutbetaltePerioder = mapFeilutbetaltePerioder(request.feilutbetaltePerioderDto),
                                    revurderingsvedtaksdato = request.vedtaksdato)
    }

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling: Behandling,
                                                                 personinfo: Personinfo,
                                                                 adresseinfo: Adresseinfo,
                                                                 fagsak: Fagsak,
                                                                 friTekst: String?,
                                                                 feilutbetalingFakta: FaktaFeilutbetalingDto,
                                                                 finnesVerge: Boolean,
                                                                 vergeNavn: String?,
                                                                 erKorrigert: Boolean): VarselbrevSamletInfo {
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
        return VarselbrevSamletInfo(brevmetadata = metadata,
                                    fritekstFraSaksbehandler = friTekst,
                                    sumFeilutbetaling = feilutbetalingFakta.totaltFeilutbetaltBeløp.toLong(),
                                    feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingFakta),
                                    revurderingsvedtaksdato = feilutbetalingFakta.revurderingsvedtaksdato)
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
