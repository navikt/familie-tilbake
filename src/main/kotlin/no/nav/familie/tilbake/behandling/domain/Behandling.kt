package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.Regelverk
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val status: Behandlingsstatus = Behandlingsstatus.OPPRETTET,
    val type: Behandlingstype,
    val saksbehandlingstype: Saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
    val opprettetDato: LocalDate = LocalDate.now(),
    val avsluttetDato: LocalDate? = null,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String? = null,
    val behandlendeEnhet: String,
    val behandlendeEnhetsNavn: String,
    val manueltOpprettet: Boolean,
    val eksternBrukId: UUID = UUID.randomUUID(),
    @MappedCollection(idColumn = "behandling_id")
    val fagsystemsbehandling: Set<Fagsystemsbehandling> = setOf(),
    @MappedCollection(idColumn = "behandling_id")
    val varsler: Set<Varsel> = setOf(),
    @MappedCollection(idColumn = "behandling_id")
    val verger: Set<Verge> = setOf(),
    @MappedCollection(idColumn = "behandling_id")
    val resultater: Set<Behandlingsresultat> = setOf(),
    @MappedCollection(idColumn = "behandling_id")
    val årsaker: Set<Behandlingsårsak> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val regelverk: Regelverk? = null,
    @Column("begrunnelse_for_tilbakekreving")
    val begrunnelseForTilbakekreving: String?,
) {
    val erAvsluttet get() = Behandlingsstatus.AVSLUTTET == status

    val erUnderIverksettelse get() = Behandlingsstatus.IVERKSETTER_VEDTAK == status

    val erSaksbehandlingAvsluttet get() = erAvsluttet || erUnderIverksettelse

    val aktivVerge get() = verger.firstOrNull { it.aktiv }

    val aktivtVarsel get() = varsler.firstOrNull { it.aktiv }

    val aktivFagsystemsbehandling get() = fagsystemsbehandling.first { it.aktiv }

    val harVerge get() = verger.any { it.aktiv }

    val sisteResultat get() = resultater.maxByOrNull { it.sporbar.endret.endretTid }

    val sisteÅrsak get() = årsaker.firstOrNull()

    val erRevurdering get() = type == Behandlingstype.REVURDERING_TILBAKEKREVING

    val opprettetTidspunkt: LocalDateTime
        get() = sporbar.opprettetTid

    val endretTidspunkt: LocalDateTime
        get() = sporbar.endret.endretTid

    fun utledVedtaksbrevstype(): Vedtaksbrevstype =
        if (erTilbakekrevingRevurderingHarÅrsakFeilutbetalingBortfalt()) {
            Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT
        } else {
            Vedtaksbrevstype.ORDINÆR
        }

    private fun erTilbakekrevingRevurderingHarÅrsakFeilutbetalingBortfalt(): Boolean =
        Behandlingstype.REVURDERING_TILBAKEKREVING == this.type &&
            this.årsaker.any {
                Behandlingsårsakstype.REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT == it.type
            }
}

data class Fagsystemsbehandling(
    @Id
    val id: UUID = UUID.randomUUID(),
    val eksternId: String,
    val aktiv: Boolean = true,
    val tilbakekrevingsvalg: Tilbakekrevingsvalg? = null,
    val resultat: String,
    @Column("arsak")
    val årsak: String,
    val revurderingsvedtaksdato: LocalDate,
    @MappedCollection(idColumn = "fagsystemsbehandling_id")
    val konsekvenser: Set<Fagsystemskonsekvens> = setOf(),
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("fagsystemskonsekvens")
data class Fagsystemskonsekvens(
    @Id
    val id: UUID = UUID.randomUUID(),
    val konsekvens: String,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Varsel(
    @Id
    val id: UUID = UUID.randomUUID(),
    val varseltekst: String,
    @Column("varselbelop")
    val varselbeløp: Long,
    @MappedCollection(idColumn = "varsel_id")
    val perioder: Set<Varselsperiode> = setOf(),
    val aktiv: Boolean = true,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Varselsperiode(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fom: LocalDate,
    val tom: LocalDate,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class Verge(
    @Id
    val id: UUID = UUID.randomUUID(),
    val ident: String? = null,
    val orgNr: String? = null,
    val aktiv: Boolean = true,
    val type: Vergetype,
    val navn: String,
    val kilde: String,
    val begrunnelse: String? = "",
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

@Table("behandlingsarsak")
data class Behandlingsårsak(
    @Id
    val id: UUID = UUID.randomUUID(),
    val originalBehandlingId: UUID?,
    val type: Behandlingsårsakstype,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
