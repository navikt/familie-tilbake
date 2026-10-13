package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Kravstatuskode
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

data class StatusmeldingHendelse(
    override val id: UUID,
    override val opprettet: LocalDateTime,
    override val vedtakId: BigInteger,
    override val kravstatuskode: Kravstatuskode,
    override val vedtakGjelder: Aktør,
    override val referanse: String?,
) : KravHendelse {
    override fun valider(sporing: Sporing) = Unit
}
