package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Kravstatuskode
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

data class KravgrunnlagHendelseEntity(
    val id: UUID,
    val tilbakekrevingId: String,
    val vedtakId: BigInteger,
    val kravstatuskode: Kravstatuskode,
    val fagsystemVedtaksdato: LocalDate?,
    val vedtakGjelder: AktørEntity,
    val utbetalesTil: AktørEntity,
    val skalBeregneRenter: Boolean,
    val ansvarligEnhet: String,
    val kontrollfelt: String,
    val kravgrunnlagId: String,
    val referanse: String,
    val perioder: List<KravgrunnlagPeriodeEntity>,
) {
    fun fraEntity(): KravgrunnlagHendelse {
        return KravgrunnlagHendelse(
            id = id,
            vedtakId = vedtakId,
            kravstatuskode = kravstatuskode,
            fagsystemVedtaksdato = fagsystemVedtaksdato,
            vedtakGjelder = vedtakGjelder.fraEntity(),
            utbetalesTil = utbetalesTil.fraEntity(),
            skalBeregneRenter = skalBeregneRenter,
            ansvarligEnhet = ansvarligEnhet,
            kontrollfelt = kontrollfelt,
            kravgrunnlagId = kravgrunnlagId,
            referanse = referanse,
            perioder = perioder.map { it.fraEntity() },
        )
    }
}
