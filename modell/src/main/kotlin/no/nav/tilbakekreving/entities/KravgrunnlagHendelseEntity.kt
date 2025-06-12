package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Kravstatuskode
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Serializable
data class KravgrunnlagHendelseEntity(
    val internId: String,
    val vedtakId: String,
    val kravstatuskode: String,
    val fagsystemVedtaksdato: String,
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
            internId = UUID.fromString(internId),
            vedtakId = BigInteger(vedtakId),
            kravstatuskode = Kravstatuskode.fraNavn(kravstatuskode),
            fagsystemVedtaksdato = LocalDate.parse(fagsystemVedtaksdato),
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
