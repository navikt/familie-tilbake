package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.feil.Sporing
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

sealed interface StatusmeldingHendelse : KravHendelse {
    override val vedtakId: BigInteger
    override val kravstatuskode: Kravstatuskode
    override val vedtakGjelder: Aktør
    override val referanse: String?
    val kodeFagomraade: String
    val fagsystemId: String
}

data class EndretStatusmeldingHendelse(
    override val id: UUID,
    override val opprettet: LocalDateTime,
    override val vedtakId: BigInteger,
    override val kravstatuskode: Kravstatuskode,
    override val vedtakGjelder: Aktør,
    override val referanse: String?,
    override val kodeFagomraade: String,
    override val fagsystemId: String,
) : KravHendelse, StatusmeldingHendelse {
    override fun valider(sporing: Sporing) = Unit
}

data class SperretStatusmeldingHendelse(
    override val id: UUID,
    override val opprettet: LocalDateTime,
    override val vedtakId: BigInteger,
    override val kravstatuskode: Kravstatuskode,
    override val vedtakGjelder: Aktør,
    override val referanse: String?,
    override val kodeFagomraade: String,
    override val fagsystemId: String,
) : KravHendelse, StatusmeldingHendelse {
    override fun valider(sporing: Sporing) = Unit
}

data class AvsluttetStatusmeldingHendelse(
    override val id: UUID,
    override val opprettet: LocalDateTime,
    override val vedtakId: BigInteger,
    override val kravstatuskode: Kravstatuskode,
    override val vedtakGjelder: Aktør,
    override val referanse: String?,
    override val kodeFagomraade: String,
    override val fagsystemId: String,
) : KravHendelse, StatusmeldingHendelse {
    override fun valider(sporing: Sporing) = Unit
}
