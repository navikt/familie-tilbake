package no.nav.tilbakekreving.mapper

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.kontrakter.frontend.models.BurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DelerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForaarsaketAvMottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForsettligDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoEllerBurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GodTroDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GrovtUaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HeleDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeAktueltDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IngentingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.JaSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NeiSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnnlatelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingIkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

object ForårsaketAvBrukerMapper {
    fun mapTilForårsaketAvBruker(
        vilkaarsvurderingDto: VilkaarsvurderingDto,
        tilbakekreving: Tilbakekreving,
    ): ForårsaketAvBruker {
        val logContext = SecureLog.Context.fra(tilbakekreving)

        return when (val valg = vilkaarsvurderingDto.valg) {
            is ForstoEllerBurdeForstaattDto -> when (val forståelse = valg.forståelse) {
                is ForstoDto -> NivåAvForståelse.Forstod(
                    begrunnelseMottakersForståelse = forståelse.begrunnelse,
                    begrunnelse = "TODO",
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(forståelse.unnlatelse, logContext),
                )

                is BurdeForstaattDto -> NivåAvForståelse.BurdeForstått(
                    grad = NivåAvForståelse.Grad.BURDE_FORSTÅTT,
                    begrunnelseMottakersForståelse = forståelse.begrunnelse,
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(forståelse.unnlatelse, logContext),
                    begrunnelse = "TODO",
                )
            }

            is ForaarsaketAvMottakerDto -> when (val aktsomhet = valg.aktsomhet) {
                is ForsettligDto -> Skyldgrad.Forsett(
                    begrunnelse = "TODO",
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )

                is GrovtUaktsomtDto -> Skyldgrad.GrovUaktsomhet(
                    begrunnelse = "TODO",
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    reduksjonSærligeGrunner = mapReduksjonSærligeGrunner(aktsomhet.erDetSærligeGrunner, logContext),
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )

                is UaktsomtDto -> Skyldgrad.Uaktsomt(
                    begrunnelse = "TODO",
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(aktsomhet.unnlatelse, logContext),
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )
            }

            is GodTroDto -> NivåAvForståelse.GodTro(
                beløpIBehold = when (val beløpIBehold = valg.beløpIBehold) {
                    is DelerDto -> NivåAvForståelse.GodTro.BeløpIBehold.DelerIBehold(beløpIBehold.beløp.toBigDecimal())
                    is HeleDto -> when (beløpIBehold.reduksjon) {
                        is SkalIkkeReduseresDto -> NivåAvForståelse.GodTro.BeløpIBehold.HeleIBehold()
                        is SkalReduseresDto -> NivåAvForståelse.GodTro.BeløpIBehold.HeleIBehold()
                    }
                    is IngentingDto -> NivåAvForståelse.GodTro.BeløpIBehold.Nei
                },
                begrunnelseForGodTro = valg.begrunnelse,
                begrunnelse = "TODO",
            )

            is VilkaarsvurderingIkkeVurdertDto ->
                throw Feil("Feil vilkaarsvurderingDto: $vilkaarsvurderingDto!", logContext = logContext)
        }
    }

    private fun mapUnnlatelse(
        unnlatelse: UnnlatelseDto,
        logContext: SecureLog.Context,
    ): KanUnnlates4xRettsgebyr =
        when (unnlatelse) {
            is SkalUnnlatesDto -> KanUnnlates4xRettsgebyr.Unnlates
            is IkkeAktueltDto -> KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                mapReduksjonSærligeGrunner(unnlatelse.erDetSærligeGrunner, logContext),
            )
            is SkalIkkeUnnlatesDto -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
                mapReduksjonSærligeGrunner(unnlatelse.erDetSærligeGrunner, logContext),
            )
        }

    private fun mapReduksjonSærligeGrunner(
        dto: SaerligeGrunnerDto,
        logContext: SecureLog.Context,
    ): ReduksjonSærligeGrunner =
        when (dto) {
            is JaSaerligeGrunnerDto -> ReduksjonSærligeGrunner(
                begrunnelse = dto.begrunnelse,
                grunner = dto.særligeGrunnerFor.map { mapSærligGrunn(it.moment, dto.annetBegrunnelse, logContext) }.toSet(),
                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Ja(dto.prosentReduksjon),
            )

            is NeiSaerligeGrunnerDto -> ReduksjonSærligeGrunner(
                begrunnelse = dto.begrunnelse,
                grunner = dto.særligeGrunnerMot.map { mapSærligGrunn(it.moment, dto.annetBegrunnelse, logContext) }.toSet(),
                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
            )
        }

    private fun mapSærligGrunn(
        moment: String,
        annetBegrunnelse: String?,
        logContext: SecureLog.Context,
    ): SærligGrunn =
        when (moment) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET.name -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL.name -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.STØRRELSE_BELØP.name -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.TID_FRA_UTBETALING.name -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.ANNET.name -> SærligGrunn.Annet(annetBegrunnelse!!)
            else -> throw Feil("Ukjent særlig grunn: $moment", logContext = logContext)
        }
}
