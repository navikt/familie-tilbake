package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.domain.tbd.Behandlingsstegstype
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Behandling(@Id
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
                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar()) {

    fun erAvsluttet(): Boolean {
        return Behandlingsstatus.AVSLUTTET == status
    }

    val aktivVerge get() = verger.firstOrNull { it.aktiv }

    val aktivtVarsel get() = varsler.firstOrNull { it.aktiv }

    val aktivtFagsystem get() = fagsystemsbehandling.first { it.aktiv }

    val harVerge get() = verger.any { it.aktiv }

    val sisteResultat get() = resultater.maxByOrNull { it.sporbar.endret.endretTid }

    val opprettetTidspunkt: LocalDateTime
        get() = sporbar.opprettetTid

    val endretTidspunkt: LocalDateTime
        get() = sporbar.endret.endretTid
}

data class Fagsystemsbehandling(@Id
                                val id: UUID = UUID.randomUUID(),
                                val eksternId: String,
                                val aktiv: Boolean = true,
                                val tilbakekrevingsvalg: Tilbakekrevingsvalg,
                                val resultat: String,
                                @Column("arsak")
                                val årsak: String,
                                val revurderingsvedtaksdato: LocalDate,
                                @MappedCollection(idColumn = "fagsystemsbehandling_id")
                                val konsekvenser: Set<Fagsystemskonsekvens> = setOf(),
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())

@Table("fagsystemskonsekvens")
data class Fagsystemskonsekvens(@Id
                                val id: UUID = UUID.randomUUID(),
                                val konsekvens: String,
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())

data class Varsel(@Id
                  val id: UUID = UUID.randomUUID(),
                  val varseltekst: String,
                  @Column("varselbelop")
                  val varselbeløp: Long,
                  @MappedCollection(idColumn = "varsel_id")
                  val perioder: Set<Varselsperiode> = setOf(),
                  val aktiv: Boolean = true,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

data class Varselsperiode(@Id
                          val id: UUID = UUID.randomUUID(),
                          val fom: LocalDate,
                          val tom: LocalDate,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())

data class Verge(@Id
                 val id: UUID = UUID.randomUUID(),
                 val ident: String? = null,
                 val orgNr: String? = null,
                 val gyldigFom: LocalDate,
                 val gyldigTom: LocalDate,
                 val aktiv: Boolean = true,
                 val type: Vergetype,
                 val navn: String,
                 val kilde: String,
                 val begrunnelse: String? = "",
                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                 val sporbar: Sporbar = Sporbar())

@Table("behandlingsarsak")
data class Behandlingsårsak(@Id
                            val id: UUID = UUID.randomUUID(),
                            val originalBehandlingId: UUID?,
                            val type: Behandlingsårsakstype,
                            val versjon: Int = 0,
                            @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                            val sporbar: Sporbar = Sporbar())

enum class Behandlingsårsakstype(val navn: String) {
    REVURDERING_KLAGE_NFP("Revurdering NFP omgjør vedtak basert på klage"),
    REVURDERING_KLAGE_KA("Revurdering etter KA-behandlet klage"),
    REVURDERING_OPPLYSNINGER_OM_VILKÅR("Nye opplysninger om vilkårsvurdering"),
    REVURDERING_OPPLYSNINGER_OM_FORELDELSE("Nye opplysninger om foreldelse"),
    REVURDERING_FEILUTBETALT_BELØP_HELT_ELLER_DELVIS_BORTFALT("Feilutbetalt beløp helt eller delvis bortfalt"),
    UDEFINERT("Ikke Definert")
}

enum class Vergetype(val navn: String) {
    VERGE_FOR_BARN("Verge for barn under 18 år"),
    VERGE_FOR_FORELDRELØST_BARN("Verge for foreldreløst barn under 18 år"),
    VERGE_FOR_VOKSEN("Verge for voksen"),
    ADVOKAT("Advokat/advokatfullmektig"),
    ANNEN_FULLMEKTIG("Annen fullmektig"),
    UDEFINERT("Udefinert");
}

enum class Behandlingsstatus(val kode: String) {

    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED")
}

enum class Behandlingstype(val behandlingssteg: List<Behandlingsstegstype>) {

    TILBAKEKREVING(listOf(Behandlingsstegstype.INNHENT_OPPLYSNINGER,
                          Behandlingsstegstype.VARSEL_OM_TILBAKEKREVING,
                          Behandlingsstegstype.MOTTA_KRAVGRUNNLAG_FRA_ØKONOMI,
                          Behandlingsstegstype.FAKTA_OM_VERGE,
                          Behandlingsstegstype.FAKTA_OM_FEILUTBETALING,
                          Behandlingsstegstype.VURDER_FORELDELSE,
                          Behandlingsstegstype.VURDER_TILBAKEKREVING,
                          Behandlingsstegstype.FORESLÅ_VEDTAK,
                          Behandlingsstegstype.FATTE_VEDTAK,
                          Behandlingsstegstype.IVERKSETT_VEDTAK)),
    REVURDERING_TILBAKEKREVING(listOf(Behandlingsstegstype.HENT_GRUNNLAG_FRA_ØKONOMI,
                                      Behandlingsstegstype.FAKTA_OM_VERGE,
                                      Behandlingsstegstype.FAKTA_OM_FEILUTBETALING,
                                      Behandlingsstegstype.VURDER_FORELDELSE,
                                      Behandlingsstegstype.VURDER_TILBAKEKREVING,
                                      Behandlingsstegstype.FORESLÅ_VEDTAK,
                                      Behandlingsstegstype.FATTE_VEDTAK,
                                      Behandlingsstegstype.IVERKSETT_VEDTAK))
}

enum class Saksbehandlingstype {
    ORDINÆR,
    AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP
}
