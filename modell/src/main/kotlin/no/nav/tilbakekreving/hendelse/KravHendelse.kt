package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Kravstatuskode
import no.nav.tilbakekreving.historikk.Historikk
import java.math.BigInteger
import java.util.UUID

sealed interface KravHendelse : Historikk.HistorikkInnslag<UUID> {
    val vedtakId: BigInteger
    val kravstatuskode: Kravstatuskode
    val vedtakGjelder: Aktør
    val referanse: String?

    fun valider(sporing: Sporing)
}
