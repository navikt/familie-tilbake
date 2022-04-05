package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevsporing
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Table("fagsak")
data class Vedtaksbrevgrunnlag(@Id
                               val id: UUID,
                               @Embedded(prefix = "bruker_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val bruker: Bruker,
                               val eksternFagsakId: String,
                               val fagsystem: Fagsystem,
                               val ytelsestype: Ytelsestype,
                               @MappedCollection(idColumn = "fagsak_id")
                               val behandlinger: Set<Vedtaksbrevbehandling>
) {

    val behandling get() = behandlinger.first { it.avsluttetDato == null }

    val klagebehandling get() = behandling.sisteÅrsak?.type == Behandlingsårsakstype.REVURDERING_KLAGE_NFP

    val harVerge get() = behandling.verger.any { it.aktiv }

    val brevmottager get() = if (harVerge) Brevmottager.VERGE else Brevmottager.BRUKER

    val aktivVerge get() = behandling.verger.firstOrNull { it.aktiv }

    val erRevurdering get() = behandling.type == Behandlingstype.REVURDERING_TILBAKEKREVING

    val varsletBeløp get() = behandling.aktivtVarsel?.varselbeløp?.let { BigDecimal(it) }

    val aktivFagsystemsbehandling get() = behandling.fagsystemsbehandling.first { it.aktiv }

    val erRevurderingEtterKlageNfp
        get() =
            behandling.erRevurdering && behandling.årsaker.any { it.type == Behandlingsårsakstype.REVURDERING_KLAGE_NFP }

    val vilkårsvurderingsperioder get() = behandling.vilkårsvurdering.firstOrNull { it.aktiv }?.perioder ?: emptySet()
    val vurdertForeldelse get() = behandling.vurderteForeldelser.firstOrNull { it.aktiv }
    val faktaFeilutbetaling get() = behandling.faktaFeilutbetaling.firstOrNull { it.aktiv }

    val sisteVarsel
        get() = behandling.brevsporing
                .filter { it.brevtype in setOf(Brevtype.VARSEL, Brevtype.KORRIGERT_VARSEL) }
                .maxByOrNull { it.sporbar.opprettetTid }

    fun finnOriginalBehandlingVedtaksdato(): LocalDate? {
        return if (erRevurdering) {
            val behandlingÅrsak = behandling.årsaker.first()
            behandlingÅrsak.originalBehandlingId ?: error("Mangler orginalBehandlingId for behandling: ${behandling.id}")

            behandlinger.first { it.id == behandlingÅrsak.originalBehandlingId }
                    .sisteResultat
                    ?.behandlingsvedtak
                    ?.vedtaksdato
            ?: error("Mangler vedtaksdato for orginal behandling med id : ${behandlingÅrsak.originalBehandlingId}")
        } else {
            null
        }
    }

    fun utledVedtaksbrevstype(): Vedtaksbrevstype {
        return if (erTilbakekrevingRevurderingHarÅrsakFeilutbetalingBortfalt()) {
            Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT
        } else {
            Vedtaksbrevstype.ORDINÆR
        }
    }

    private fun erTilbakekrevingRevurderingHarÅrsakFeilutbetalingBortfalt(): Boolean {
        return Behandlingstype.REVURDERING_TILBAKEKREVING == behandling.type
               && behandling.årsaker.any {
            Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT == it.type
        }
    }


}

@Table("behandling")
data class Vedtaksbrevbehandling(@Id
                                 val id: UUID,
                                 val type: Behandlingstype,
                                 val ansvarligSaksbehandler: String,
                                 val ansvarligBeslutter: String? = null,
                                 val avsluttetDato: LocalDate? = null,
                                 val behandlendeEnhet: String,
                                 val behandlendeEnhetsNavn: String,
                                 @MappedCollection(idColumn = "behandling_id")
                                 val verger: Set<Verge> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val fagsystemsbehandling: Set<Fagsystemsbehandling> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val vedtaksbrevOppsummering: Vedtaksbrevsoppsummering?,
                                 @MappedCollection(idColumn = "behandling_id")
                                 val eksisterendePerioderForBrev: Set<Vedtaksbrevsperiode> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val vilkårsvurdering: Set<Vilkårsvurdering> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val faktaFeilutbetaling: Set<FaktaFeilutbetaling> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val varsler: Set<Varsel> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val brevsporing: Set<Brevsporing> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val resultater: Set<Behandlingsresultat> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val årsaker: Set<Behandlingsårsak> = setOf(),
                                 @MappedCollection(idColumn = "behandling_id")
                                 val vurderteForeldelser: Set<VurdertForeldelse> = setOf()) {

    val erRevurdering get() = type == Behandlingstype.REVURDERING_TILBAKEKREVING

    val sisteResultat get() = resultater.maxByOrNull { it.sporbar.endret.endretTid }

    val sisteÅrsak get() = årsaker.firstOrNull()

    val aktivtVarsel get() = varsler.firstOrNull { it.aktiv }

}

