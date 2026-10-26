package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.entities.KravStatusmeldingEntity
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

    override fun tilEntity(tilbakekrevingId: String): KravStatusmeldingEntity {
        return KravStatusmeldingEntity(
            id = id,
            tilbakekrevingRef = tilbakekrevingId,
            vedtakId = vedtakId,
            kravstatuskode = kravstatuskode,
            vedtakGjelder = vedtakGjelder.tilEntity(),
            referanse = referanse,
            kodeFagomraade = kodeFagomraade,
            fagsystemId = fagsystemId,
            opprettet = opprettet,
        )
    }
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
