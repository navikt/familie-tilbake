package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import java.time.LocalDate
import java.time.Period

object VarselbrevUtil {

    private const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    private const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "

    fun sammenstillInfoFraFagsystemerForSending(fagsak: Fagsak,
                                                behandling: Behandling,
                                                adresseinfo: Adresseinfo,
                                                personinfo: Personinfo,
                                                ventetid: Period?,
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
                                    fristdato = LocalDate.now().plus(ventetid),
                                    revurderingsvedtaksdato = LocalDate.now())
        // TODO revurderingVedtakDato = grunninformasjon.getVedtakDato()
    }

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling: Behandling,
                                                                 personinfo: Personinfo,
                                                                 adresseinfo: Adresseinfo,
                                                                 fagsak: Fagsak,
                                                                 ventetid: Period?,
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
                                    fristdato = LocalDate.now().plus(ventetid),
                                    revurderingsvedtaksdato = feilutbetalingFakta.revurderingsvedtaksdato)
    }

    private fun getTittelForVarselbrev(ytelseNavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelseNavn
        else TITTEL_VARSEL_TILBAKEBETALING + ytelseNavn
    }

    private fun mapFeilutbetaltePerioder(varsel: Varsel?): List<Periode> {
        return varsel?.perioder?.map { Periode(it.fom, it.tom) } ?: emptyList()
    }

    private fun mapFeilutbetaltePerioder(feilutbetalingFakta: FaktaFeilutbetalingDto): List<Periode> {
        return feilutbetalingFakta.feilutbetaltePerioder.map { Periode(it.periode.fom, it.periode.tom) }
    }


}
