package no.nav.tilbakekreving.entities

import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

data class KravgrunnlagHendelseEntity(
    val internId: UUID,
    val vedtakId: BigInteger,
    val kravstatuskode: String,
    val fagsystemVedtaksdato: LocalDate,
    val vedtakGjelder: AktørEntity,
    val utbetalesTil: AktørEntity,
    val skalBeregneRenter: Boolean,
    val ansvarligEnhet: String,
    val kontrollfelt: String,
    val kravgrunnlagId: String,
    val referanse: String,
    val perioder: List<PeriodeEntity>,
)
