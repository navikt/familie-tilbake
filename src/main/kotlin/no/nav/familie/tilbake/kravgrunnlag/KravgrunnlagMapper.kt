package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPeriodeDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagDetaljerDto
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.JaNeiDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import java.util.UUID

object KravgrunnlagMapper {
    fun tilKravgrunnlag431(
        kravgrunnlag: DetaljertKravgrunnlagDto,
        behandlingId: UUID,
    ): Kravgrunnlag431 =
        Kravgrunnlag431(
            behandlingId = behandlingId,
            vedtakId = kravgrunnlag.vedtakId,
            omgjortVedtakId = kravgrunnlag.vedtakIdOmgjort,
            kravstatuskode = Kravstatuskode.fraKode(kravgrunnlag.kodeStatusKrav),
            fagområdekode = Fagområdekode.fraKode(kravgrunnlag.kodeFagomraade),
            fagsystemId = kravgrunnlag.fagsystemId,
            fagsystemVedtaksdato = kravgrunnlag.datoVedtakFagsystem,
            gjelderVedtakId = kravgrunnlag.vedtakGjelderId,
            gjelderType = GjelderType.fraKode(kravgrunnlag.typeGjelderId.value()),
            utbetalesTilId = kravgrunnlag.utbetalesTilId,
            utbetIdType = GjelderType.fraKode(kravgrunnlag.typeUtbetId.value()),
            hjemmelkode = kravgrunnlag.kodeHjemmel,
            beregnesRenter = "J" == kravgrunnlag.renterBeregnes?.value(),
            ansvarligEnhet = kravgrunnlag.enhetAnsvarlig,
            behandlingsenhet = kravgrunnlag.enhetBehandl,
            bostedsenhet = kravgrunnlag.enhetBosted,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            saksbehandlerId = kravgrunnlag.saksbehId,
            referanse = kravgrunnlag.referanse,
            eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
            perioder = tilKravgrunnlagsperiode(kravgrunnlag.tilbakekrevingsPeriode),
        )

    private fun tilKravgrunnlagsperiode(perioder: List<DetaljertKravgrunnlagPeriodeDto>): Set<Kravgrunnlagsperiode432> =
        perioder
            .map {
                Kravgrunnlagsperiode432(
                    periode = Månedsperiode(it.periode.fom, it.periode.tom),
                    månedligSkattebeløp = it.belopSkattMnd,
                    beløp = tilKravgrunnlagsbeløp(it.tilbakekrevingsBelop),
                )
            }.toSet()

    private fun tilKravgrunnlagsbeløp(beløpPosteringer: List<DetaljertKravgrunnlagBelopDto>): Set<Kravgrunnlagsbeløp433> =
        beløpPosteringer
            .map {
                val klassetype = Klassetype.fraKode(it.typeKlasse.value())
                Kravgrunnlagsbeløp433(
                    klassetype = klassetype,
                    klassekode = Klassekode.fraKode(it.kodeKlasse, klassetype),
                    opprinneligUtbetalingsbeløp = it.belopOpprUtbet,
                    nyttBeløp = it.belopNy,
                    tilbakekrevesBeløp = it.belopTilbakekreves,
                    uinnkrevdBeløp = it.belopUinnkrevd,
                    skatteprosent = it.skattProsent,
                    resultatkode = it.kodeResultat,
                    årsakskode = it.kodeAArsak,
                    skyldkode = it.kodeSkyld,
                )
            }.toSet()

    fun KravgrunnlagDetaljerDto.tilDetaljertKravgrunnlagDto(): DetaljertKravgrunnlagDto =
        DetaljertKravgrunnlagDto().also {
            it.kravgrunnlagId = kravgrunnlagId.toBigInteger()
            it.vedtakId = vedtakId.toBigInteger()
            it.kodeStatusKrav = kodeStatusKrav
            it.kodeFagomraade = kodeFagomraade
            it.fagsystemId = fagsystemId
            it.datoVedtakFagsystem = datoVedtakFagsystem
            it.vedtakIdOmgjort = vedtakIdOmgjort.takeIf { id -> id != 0L }?.toBigInteger()
            it.vedtakGjelderId = gjelderId
            it.typeGjelderId = TypeGjelderDto.valueOf(typeGjelder)
            it.utbetalesTilId = utbetalesTilId
            it.typeUtbetId = TypeGjelderDto.valueOf(typeUtbetalesTilId)
            it.kodeHjemmel = kodeHjemmel
            it.renterBeregnes = when {
                renterBeregnes -> JaNeiDto.J
                else -> JaNeiDto.N
            }
            it.enhetAnsvarlig = enhetAnsvarlig
            it.enhetBosted = enhetBosted
            it.enhetBehandl = enhetBehandl
            it.kontrollfelt = kontrollfelt
            it.saksbehId = saksbehandlerId
            it.referanse = referanse
            it.tilbakekrevingsPeriode.addAll(perioder.map { p -> p.tilDetaljertKravgrunnlagPeriodeDto() })
        }

    private fun DetaljerPeriodeDto.tilDetaljertKravgrunnlagPeriodeDto(): DetaljertKravgrunnlagPeriodeDto =
        DetaljertKravgrunnlagPeriodeDto().also {
            it.periode = PeriodeDto().also { p ->
                p.fom = periodeFom
                p.tom = periodeTom
            }
            it.belopSkattMnd = belopSkattMnd
            it.tilbakekrevingsBelop.addAll(posteringer.map { p -> p.tilDetaljertKravgrunnlagBelopDto() })
        }

    private fun DetaljerPosteringDto.tilDetaljertKravgrunnlagBelopDto(): DetaljertKravgrunnlagBelopDto =
        DetaljertKravgrunnlagBelopDto().also {
            it.kodeKlasse = kodeKlasse
            it.typeKlasse = TypeKlasseDto.valueOf(typeKlasse)
            it.belopOpprUtbet = belopOpprinneligUtbetalt
            it.belopNy = belopNy
            it.belopTilbakekreves = belopTilbakekreves
            it.belopUinnkrevd = belopUinnkrevd
            it.skattProsent = skattProsent
            it.kodeResultat = kodeResultat
            it.kodeAArsak = kodeAarsak
            it.kodeSkyld = kodeSkyld
        }
}
