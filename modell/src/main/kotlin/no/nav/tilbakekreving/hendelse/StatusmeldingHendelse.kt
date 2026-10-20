package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.feil.Sporing
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
    val kodeFagomraade: String,
    val fagsystemId: String,
) : KravHendelse {
    override fun valider(sporing: Sporing) = Unit

    fun kravgrunnlagBortfalt() = kravstatuskode == Kravstatuskode.AVSLUTTET

    fun erKravgrunnlagEndret() = kravstatuskode == Kravstatuskode.ENDRET
}
