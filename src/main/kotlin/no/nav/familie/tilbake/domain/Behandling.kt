package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.*

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      val status: Behandlingsstatus = Behandlingsstatus.OPPRETTET,
                      val type: Behandlingstype,
                      val saksbehandlingstype: Saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
                      val opprettetDato: LocalDate = LocalDate.now(),
                      val avsluttetDato: LocalDate?,
                      val ansvarligSaksbehandler: String?,
                      val ansvarligBeslutter: String?,
                      val behandlendeEnhet: String?,
                      val behandlendeEnhetsNavn: String?,
                      val manueltOpprettet: Boolean,
                      val eksternId: UUID?,
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternBehandling: Set<EksternBehandling> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val varsler: Set<Varsel> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val verger: Set<Verge> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val resultater: Set<Behandlingsresultat> = setOf(),
                      val versjon: Int = 0,
                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar())

data class EksternBehandling(@Id
                             val id: UUID = UUID.randomUUID(),
                             val henvisning: String,
                             val eksternId: UUID?,
                             val aktiv: Boolean = true,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())

data class Varsel(@Id
                  val id: UUID = UUID.randomUUID(),
                  val varseltekst: String,
                  @Column("varselbelop")
                  val varselbeløp: Long?,
                  val aktiv: Boolean = true,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar())

data class Verge(@Id
                 val id: UUID = UUID.randomUUID(),
                 val ident: String?,
                 val orgNr: String?,
                 val gyldigFom: LocalDate,
                 val gyldigTom: LocalDate,
                 val aktiv: Boolean = true,
                 val type: Vergetype,
                 val navn: String,
                 val kilde: String,
                 val begrunnelse: String?,
                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                 val sporbar: Sporbar = Sporbar())

enum class Vergetype(val navn: String) {
    VERGE_BARN("Verge for barn under 18 år"),
    VERGE_FORELDRELØST_BARN("Verge for foreldreløst barn under 18 år"),
    VERGE_VOKSEN("Verge for voksen"),
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