package no.nav.familie.tilbake.brev.dokumentbestilling.varsel

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.brev.dokumentbestilling.handlebars.dto.periode.HbPeriode
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import java.time.LocalDate
import java.time.Period

object VarselbrevUtil {

    private const val TITTEL_VARSEL_TILBAKEBETALING = "Varsel tilbakebetaling "
    private const val TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING = "Korrigert Varsel tilbakebetaling "

    fun sammenstillInfoFraFagsystemerForSending(fagsak: Fagsak,
                                                behandling: Behandling,
                                                adresseinfo: Adresseinfo,
                                                personInfo: PersonInfo,
                                                ventetid: Period?,
                                                varsel: Varsel?,
                                                finnesVerge: Boolean,
                                                vergeNavn: String?): VarselbrevSamletInfo {
        val metadata = BrevMetadata(sakspartId = personInfo.ident,
                                    sakspartNavn = personInfo.navn,
                                    finnesVerge = finnesVerge,
                                    vergeNavn = vergeNavn,
                                    mottakerAdresse = adresseinfo,
                                    behandlendeEnhetId = behandling.behandlendeEnhet,
                                    behandlendeEnhetNavn = behandling.behandlendeEnhetsNavn,
                                    ansvarligSaksbehandler = "VL",
                                    saksnummer = fagsak.eksternFagsakId,
                                    språkkode = fagsak.bruker.språkkode,
                                    ytelsestype = fagsak.ytelsestype,
                                    tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, false))
        return VarselbrevSamletInfo(brevMetadata = metadata,
                                    fritekstFraSaksbehandler = varsel?.varseltekst,
                                    sumFeilutbetaling = varsel?.varselbeløp ?: 0L,
                                    feilutbetaltePerioder = mapFeilutbetaltePerioder(varsel),
                                    fristdato = LocalDate.now().plus(ventetid),
                                    revurderingVedtakDato = LocalDate.now())
        // TODO revurderingVedtakDato = grunninformasjon.getVedtakDato()
    }

    fun sammenstillInfoFraFagsystemerForSendingManueltVarselBrev(behandling: Behandling,
                                                                 personInfo: PersonInfo,
                                                                 adresseinfo: Adresseinfo,
                                                                 fagsak: Fagsak,
                                                                 ventetid: Period?,
                                                                 friTekst: String?,
                                                                 feilutbetalingFakta: FaktaFeilutbetalingDto,
                                                                 finnesVerge: Boolean,
                                                                 vergeNavn: String?,
                                                                 erKorrigert: Boolean): VarselbrevSamletInfo {
        val metadata = BrevMetadata(sakspartId = personInfo.ident,
                                    sakspartNavn = personInfo.navn,
                                    finnesVerge = finnesVerge,
                                    vergeNavn = vergeNavn,
                                    mottakerAdresse = adresseinfo,
                                    behandlendeEnhetId = behandling.behandlendeEnhet,
                                    behandlendeEnhetNavn = behandling.behandlendeEnhetsNavn,
                                    ansvarligSaksbehandler = "VL",
                                    saksnummer = fagsak.eksternFagsakId,
                                    språkkode = fagsak.bruker.språkkode,
                                    ytelsestype = fagsak.ytelsestype,
                                    tittel = getTittelForVarselbrev(fagsak.ytelsesnavn, erKorrigert))
        return VarselbrevSamletInfo(brevMetadata = metadata,
                                    fritekstFraSaksbehandler = friTekst,
                                    sumFeilutbetaling = feilutbetalingFakta.totaltFeilutbetaltBeløp.toLong(),
                                    feilutbetaltePerioder = mapFeilutbetaltePerioder(feilutbetalingFakta),
                                    fristdato = LocalDate.now().plus(ventetid),
                                    revurderingVedtakDato = feilutbetalingFakta.revurderingsvedtaksdato)
    }

    private fun getTittelForVarselbrev(ytelseNavn: String, erKorrigert: Boolean): String {
        return if (erKorrigert) TITTEL_KORRIGERT_VARSEL_TILBAKEBETALING + ytelseNavn
        else TITTEL_VARSEL_TILBAKEBETALING + ytelseNavn
    }

    private fun mapFeilutbetaltePerioder(varsel: Varsel?): List<HbPeriode> {
        return varsel?.perioder?.map { HbPeriode(it.fom, it.tom) } ?: emptyList()
    }

    private fun mapFeilutbetaltePerioder(feilutbetalingFakta: FaktaFeilutbetalingDto): List<HbPeriode> {
        return feilutbetalingFakta.feilutbetaltePerioder.map { HbPeriode(it.periode.fom, it.periode.tom) }
    }


}
