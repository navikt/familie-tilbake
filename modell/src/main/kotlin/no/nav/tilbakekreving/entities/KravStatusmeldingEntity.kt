package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.AvsluttetStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.EndretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.Kravstatuskode
import no.nav.tilbakekreving.hendelse.SperretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.StatusmeldingHendelse
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

data class KravStatusmeldingEntity(
    override val id: UUID,
    override val tilbakekrevingRef: String,
    val vedtakId: BigInteger,
    val kravstatuskode: Kravstatuskode,
    val vedtakGjelder: AktørEntity,
    val referanse: String?,
    val kodeFagomraade: String,
    val fagsystemId: String,
    override val opprettet: LocalDateTime,
) : KravHendelseEntity() {
    override fun fraEntity(): StatusmeldingHendelse {
        return when (kravstatuskode) {
            Kravstatuskode.ENDRET -> EndretStatusmeldingHendelse(
                id = id,
                opprettet = opprettet,
                vedtakId = vedtakId,
                kravstatuskode = kravstatuskode,
                vedtakGjelder = vedtakGjelder.fraEntity(),
                referanse = referanse,
                kodeFagomraade = kodeFagomraade,
                fagsystemId = fagsystemId,
            )
            Kravstatuskode.SPERRET -> SperretStatusmeldingHendelse(
                id = id,
                opprettet = opprettet,
                vedtakId = vedtakId,
                kravstatuskode = kravstatuskode,
                vedtakGjelder = vedtakGjelder.fraEntity(),
                referanse = referanse,
                kodeFagomraade = kodeFagomraade,
                fagsystemId = fagsystemId,
            )
            Kravstatuskode.AVSLUTTET -> AvsluttetStatusmeldingHendelse(
                id = id,
                opprettet = opprettet,
                vedtakId = vedtakId,
                kravstatuskode = kravstatuskode,
                vedtakGjelder = vedtakGjelder.fraEntity(),
                referanse = referanse,
                kodeFagomraade = kodeFagomraade,
                fagsystemId = fagsystemId,
            )
            else -> error("Kravstatuskode $kravstatuskode støttes ikke for statusmelding")
        }
    }
}
