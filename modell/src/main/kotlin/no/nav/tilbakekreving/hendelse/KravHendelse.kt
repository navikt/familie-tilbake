package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.entities.KravHendelseEntity
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.historikk.Historikk
import java.math.BigInteger
import java.util.UUID

sealed interface KravHendelse : Historikk.HistorikkInnslag<UUID> {
    val vedtakId: BigInteger
    val kravstatuskode: Kravstatuskode
    val vedtakGjelder: Aktør
    val referanse: String?

    fun valider(sporing: Sporing)

    fun tilEntity(tilbakekrevingId: String): KravHendelseEntity
}

enum class Kravstatuskode(
    val oppdragKode: String,
    val navn: String,
) {
    ANNULERT("ANNU", "Kravgrunnlag annullert"),
    ANNULLERT_OMG("ANOM", "Kravgrunnlag annullert ved omg"),
    AVSLUTTET("AVSL", "Avsluttet kravgrunnlag"),
    BEHANDLET("BEHA", "Kravgrunnlag ferdigbehandlet"),
    ENDRET("ENDR", "Endret kravgrunnlag"),
    FEIL("FEIL", "Feil på kravgrunnlag"),
    MANUELL("MANU", "Manuell behandling"),
    NY("NY", "Nytt kravgrunnlag"),
    SPERRET("SPER", "Kravgrunnlag sperret"),
    ;

    companion object {
        fun forOppdragKode(kode: String) = entries.single { it.oppdragKode == kode }
    }
}
