package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.*

data class Aksjonspunkt(@Id
                        val id: UUID = UUID.randomUUID(),
                        val behandlingId: UUID,
                        val behandlingsstegstype: Behandlingsstegstype,
                        val aksjonspunktsdefinisjon: Aksjonspunktsdefinisjon,
                        val totrinnsbehandling: Boolean,
                        val status: Aksjonspunktsstatus,
                        val tidsfrist: LocalDateTime?,
                        val ventearsak: Venteårsak = Venteårsak.UDEFINERT,
                        val reaktiveringsstatus: Reaktiveringsstatus = Reaktiveringsstatus.AKTIV,
                        val manueltOpprettet: Boolean = false,
                        val revurdering: Boolean = false,
                        val versjon: Int = 0,
                        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                        val sporbar: Sporbar = Sporbar())

enum class Aksjonspunktsstatus(val navn: String) {
    OPPRETTET("Opprettet"),
    UTFØRT("Utført"),
    AVBRUTT("Avbrutt");
}

enum class Reaktiveringsstatus {
    AKTIV,
    INAKTIV,
    SLETTET
}

enum class Venteårsak(val navn: String) {

    VENT_PÅ_BRUKERTILBAKEMELDING("Venter på tilbakemelding fra bruker"),
    VENT_PÅ_TILBAKEKREVINGSGRUNNLAG("Venter på tilbakekrevingsgrunnlag fra økonomi"),
    AVVENTER_DOKUMENTASJON("Avventer dokumentasjon"),
    UTVIDET_TILSVAR_FRIST("Utvidet tilsvarsfrist"),
    ENDRE_TILKJENT_YTELSE("Mulig endring i tilkjent ytelse"),
    VENT_PÅ_MULIG_MOTREGNING("Mulig motregning med annen ytelse"),
    UDEFINERT("");
}
