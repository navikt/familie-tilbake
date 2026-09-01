package no.nav.tilbakekreving.mapper

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.saksbehandling.RelevantMomentGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.SkalIkkeUnnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.Unnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.BurdeForstått
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.Forstod
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.GodTro
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.GodTro.BeløpIBehold.DelerIBehold
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.GodTro.BeløpIBehold.HeleIBehold
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse.Grad
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.SkalReduseres
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.SkalReduseres.Ja
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
import no.nav.tilbakekreving.kontrakter.frontend.models.ReduksjonArsakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnnlatelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingIkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevantMomentTypeGodTro
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

object ForårsaketAvBrukerMapper {
    fun mapTilForårsaketAvBruker(
        vilkaarsvurderingDto: VilkaarsvurderingDto,
        tilbakekreving: Tilbakekreving,
    ): ForårsaketAvBruker {
        val logContext = SecureLog.Context.fra(tilbakekreving)

        return when (val valg = vilkaarsvurderingDto.valg) {
            is ForstoEllerBurdeForstaattDto -> when (val forståelse = valg.forståelse) {
                is ForstoDto -> Forstod(
                    begrunnelseMottakersForståelse = forståelse.begrunnelse,
                    begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseMottakersForståelse
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(forståelse.unnlatelse, logContext),
                )

                is BurdeForstaattDto -> BurdeForstått(
                    grad = Grad.BURDE_FORSTÅTT,
                    begrunnelseMottakersForståelse = forståelse.begrunnelse,
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(forståelse.unnlatelse, logContext),
                    begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseMottakersForståelse
                )
            }

            is ForaarsaketAvMottakerDto -> when (val aktsomhet = valg.aktsomhet) {
                is ForsettligDto -> Skyldgrad.Forsett(
                    begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseAktsomhet
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )

                is GrovtUaktsomtDto -> Skyldgrad.GrovUaktsomhet(
                    begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseAktsomhet
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    reduksjonSærligeGrunner = mapReduksjonSærligeGrunner(aktsomhet.erDetSærligeGrunner, logContext),
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )

                is UaktsomtDto -> Skyldgrad.Uaktsomt(
                    begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseAktsomhet
                    begrunnelseAktsomhet = aktsomhet.begrunnelse,
                    kanUnnlates4XRettsgebyr = mapUnnlatelse(aktsomhet.unnlatelse, logContext),
                    feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.IKKE_VURDERT,
                )
            }

            is GodTroDto -> {
                when (val beløpIBehold = valg.beløpIBehold) {
                    is HeleDto -> when (val reduksjon = beløpIBehold.reduksjon) {
                        is SkalIkkeReduseresDto -> GodTro(
                            beløpIBehold = HeleIBehold(
                                annetBegrunnelse = reduksjon.annetBegrunnelse,
                                begrunnelse = beløpIBehold.begrunnelse,
                                kanUnnlates4XRettsgebyr = SkalIkkeUnnlates(
                                    reduksjonMomenter = ReduksjonGodTro(
                                        begrunnelse = reduksjon.begrunnelse,
                                        grunner = reduksjon.relevans.map { mapRelevanteMomenterGodTro(it.moment, reduksjon.annetBegrunnelse) }.toSet(),
                                        skalReduseres = SkalReduseres.Nei,
                                    ),
                                ),
                            ),
                            begrunnelseForGodTro = valg.begrunnelse,
                            begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseForGodTro
                        )

                        is SkalReduseresDto -> GodTro(
                            beløpIBehold = HeleIBehold(
                                annetBegrunnelse = reduksjon.annetBegrunnelse,
                                begrunnelse = beløpIBehold.begrunnelse,
                                kanUnnlates4XRettsgebyr = SkalIkkeUnnlates(
                                    reduksjonMomenter = ReduksjonGodTro(
                                        begrunnelse = reduksjon.begrunnelse,
                                        grunner = reduksjon.relevans.map { mapRelevanteMomenterGodTro(it.moment, reduksjon.annetBegrunnelse) }.toSet(),
                                        skalReduseres = Ja(
                                            prosentdel = reduksjon.prosentReduksjon,
                                        ),
                                    ),
                                ),
                            ),
                            begrunnelseForGodTro = valg.begrunnelse,
                            begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseForGodTro
                        )

                        is JaSaerligeGrunnerDto, is NeiSaerligeGrunnerDto -> throw Feil("GodTro har ingen særlige grunner!", logContext = logContext)
                    }

                    is DelerDto -> when (val reduksjon = beløpIBehold.reduksjon) {
                        is SkalIkkeReduseresDto -> GodTro(
                            beløpIBehold = DelerIBehold(
                                beløp = beløpIBehold.beløp.toBigDecimal(),
                                annetBegrunnelse = reduksjon.annetBegrunnelse,
                                begrunnelse = beløpIBehold.begrunnelse,
                                kanUnnlates4XRettsgebyr = SkalIkkeUnnlates(
                                    reduksjonMomenter = ReduksjonGodTro(
                                        begrunnelse = reduksjon.begrunnelse,
                                        grunner = reduksjon.relevans.map { mapRelevanteMomenterGodTro(it.moment, reduksjon.annetBegrunnelse) }.toSet(),
                                        skalReduseres = SkalReduseres.Nei,
                                    ),
                                ),
                            ),
                            begrunnelseForGodTro = valg.begrunnelse,
                            begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseForGodTro
                        )

                        is SkalReduseresDto -> GodTro(
                            beløpIBehold = DelerIBehold(
                                beløp = beløpIBehold.beløp.toBigDecimal(),
                                annetBegrunnelse = reduksjon.annetBegrunnelse,
                                begrunnelse = beløpIBehold.begrunnelse,
                                kanUnnlates4XRettsgebyr = SkalIkkeUnnlates(
                                    reduksjonMomenter = ReduksjonGodTro(
                                        begrunnelse = reduksjon.begrunnelse,
                                        grunner = reduksjon.relevans.map { mapRelevanteMomenterGodTro(it.moment, reduksjon.annetBegrunnelse) }.toSet(),
                                        skalReduseres = SkalReduseres.Ja(prosentdel = reduksjon.prosentReduksjon),
                                    ),
                                ),
                            ),
                            begrunnelseForGodTro = valg.begrunnelse,
                            begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseForGodTro
                        )
                        is JaSaerligeGrunnerDto, is NeiSaerligeGrunnerDto -> throw Feil("GodTro har ingen særlige grunner!", logContext = logContext)
                    }
                    is IngentingDto -> GodTro(
                        beløpIBehold = GodTro.BeløpIBehold.Nei(begrunnelse = beløpIBehold.begrunnelse),
                        begrunnelseForGodTro = valg.begrunnelse,
                        begrunnelse = "", // TODO Denne må fjernes. krever migrering slik at eksisterende vilkårForTilbakekreving slås sammen med begrunnelseForGodTro
                    )
                }
            }

            is VilkaarsvurderingIkkeVurdertDto ->
                throw Feil("Feil vilkaarsvurderingDto: $vilkaarsvurderingDto!", logContext = logContext)
        }
    }

    private fun mapUnnlatelse(
        unnlatelse: UnnlatelseDto,
        logContext: SecureLog.Context,
    ): KanUnnlates4xRettsgebyr =
        when (unnlatelse) {
            is SkalUnnlatesDto -> Unnlates(unnlatelse.begrunnelse)
            is IkkeAktueltDto -> ErOver4xRettsgebyr(
                mapReduksjonSærligeGrunner(unnlatelse.erDetSærligeGrunner, logContext),
            )
            is SkalIkkeUnnlatesDto -> SkalIkkeUnnlates(
                mapReduksjonSærligeGrunner(unnlatelse.erDetSærligeGrunner, logContext),
            )
        }

    private fun mapReduksjonSærligeGrunner(
        dto: ReduksjonArsakerDto,
        logContext: SecureLog.Context,
    ): ReduksjonSærligeGrunner =
        when (dto) {
            is JaSaerligeGrunnerDto -> ReduksjonSærligeGrunner(
                begrunnelse = dto.begrunnelse,
                grunner = dto.særligeGrunnerFor.map { mapSærligGrunn(it.moment, dto.annetBegrunnelse) }.toSet(),
                skalReduseres = Ja(dto.prosentReduksjon),
            )

            is NeiSaerligeGrunnerDto -> ReduksjonSærligeGrunner(
                begrunnelse = dto.begrunnelse,
                grunner = dto.særligeGrunnerMot.map { mapSærligGrunn(it.moment, dto.annetBegrunnelse) }.toSet(),
                skalReduseres = SkalReduseres.Nei,
            )

            is SkalIkkeReduseresDto, is SkalReduseresDto -> throw Feil("Her kreves det SærligeGrunnerDto", logContext = logContext)
        }

    private fun mapSærligGrunn(
        moment: String,
        annetBegrunnelse: String?,
    ): SærligGrunn =
        when (enumValueOf<SærligGrunnType>(moment)) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.ANNET -> SærligGrunn.Annet(annetBegrunnelse!!)
        }

    private fun mapRelevanteMomenterGodTro(
        moment: String,
        annetBegrunnelse: String?,
    ): RelevantMomentGodTro =
        when (enumValueOf<RelevantMomentTypeGodTro>(moment)) {
            RelevantMomentTypeGodTro.STØRRELSE_BELØP -> RelevantMomentGodTro.StørrelseBeløp
            RelevantMomentTypeGodTro.TID_FRA_UTBETALING -> RelevantMomentGodTro.TidFraUtbetaling
            RelevantMomentTypeGodTro.UTBETALING_TILLIT -> RelevantMomentGodTro.UtbetalingTillit
            RelevantMomentTypeGodTro.ANNET -> RelevantMomentGodTro.Annet(annetBegrunnelse!!)
        }
}
