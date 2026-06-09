package no.nav.tilbakekreving.api

import jakarta.validation.Valid
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.brev.varselbrev.ForhåndsvarselService
import no.nav.tilbakekreving.brev.vedtaksbrev.NyVedtaksbrevService
import no.nav.tilbakekreving.dokumentHåndtering.saf.SafService
import no.nav.tilbakekreving.kontrakter.frontend.apis.BehandlingApi
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentInfoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentTypeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.FaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SendForhaandsvarselDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SplittPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UpdateUttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsunntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import java.util.UUID

@Component
@ProtectedWithClaims(issuer = "azuread")
class BehandlingApiController(
    private val tilbakekrevingService: TilbakekrevingService,
    private val nyVedtaksbrevService: NyVedtaksbrevService,
    private val forhåndsvarselService: ForhåndsvarselService,
    private val safService: SafService,
) : BehandlingApi {
    override fun behandlingFakta(behandlingId: String): ResponseEntity<FaktaOmFeilutbetalingDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(UUID.fromString(behandlingId)),
            valideringContext = ValideringContext.HentFaktaFeilutbetaling,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekreving.tilFeilutbetalingFrontendDto(UUID.fromString(behandlingId), SystemKlokke))
    }

    @Validated
    override fun behandlingOppdaterFakta(
        behandlingId: String,
        @Valid oppdaterFaktaOmFeilutbetalingDto: OppdaterFaktaOmFeilutbetalingDto,
    ): ResponseEntity<FaktaOmFeilutbetalingDto> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(UUID.fromString(behandlingId)),
            valideringContext = ValideringContext.OppdaterFakta,
        ) { tilbakekreving, context ->
            tilbakekreving.vurderFakta(
                behandlingId = UUID.fromString(behandlingId),
                sideeffektContext = context,
                oppdaget = oppdaterFaktaOmFeilutbetalingDto.vurdering?.oppdaget,
                årsak = oppdaterFaktaOmFeilutbetalingDto.vurdering?.årsak,
                perioder = oppdaterFaktaOmFeilutbetalingDto.perioder,
            )
            ResponseEntity.ok(tilbakekreving.tilFeilutbetalingFrontendDto(UUID.fromString(behandlingId), SystemKlokke))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingHentVedtaksbrev(behandlingId: String): ResponseEntity<VedtaksbrevDataDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(UUID.fromString(behandlingId)),
            valideringContext = ValideringContext.HentVedtaksbrevData,
        ) ?: return ResponseEntity.notFound().build()
        val beslutter = ContextService.hentBehandler(SecureLog.Context.fra(tilbakekreving))

        return ResponseEntity.ok(
            nyVedtaksbrevService.hentVedtaksbrevData(
                UUID.fromString(behandlingId),
                tilbakekreving.hentVedtaksbrevInfo(UUID.fromString(behandlingId)),
                beslutter,
            ),
        )
    }

    override fun behandlingForeslaaVedtak(behandlingId: UUID): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.ForeslåVedtak,
        ) { tilbakekreving, context ->
            tilbakekreving.håndterForeslåVedtak(behandlingId, context)
            ResponseEntity.ok(Unit)
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingOppdaterVedtaksbrev(behandlingId: UUID, vedtaksbrevRedigerbareDataUpdateDto: VedtaksbrevRedigerbareDataUpdateDto): ResponseEntity<VedtaksbrevRedigerbareDataDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.OppdaterVedtaksbrev,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            nyVedtaksbrevService.oppdaterVedtaksbrevData(
                behandlingId = behandlingId,
                data = vedtaksbrevRedigerbareDataUpdateDto,
                info = tilbakekreving.hentVedtaksbrevInfo(behandlingId),
            ),
        )
    }

    override fun behandlingHentVedtaksresultat(behandlingId: UUID): ResponseEntity<BeregningsresultatDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.HentVedtaksresultat,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekreving.hentBehandling(behandlingId).hentVedtaksresultatForFrontend())
    }

    override fun behandlingBehandlingslogg(behandlingId: UUID): ResponseEntity<List<LogginnslagDto>> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.HentBehandlingslogg,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekrevingService.hentHistorikk(tilbakekreving.id))
    }

    override fun behandlingForhandsvarsel(behandlingId: UUID): ResponseEntity<ForhaandsvarselResponseDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.HentForhåndsvarsel,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekreving.nyHentForhåndsvarselFrontendDto(behandlingId))
    }

    override fun behandlingSendVarselbrev(behandlingId: UUID, sendForhaandsvarselDto: SendForhaandsvarselDto): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.SendVarselbrev,
        ) { tilbakekreving, context ->
            ResponseEntity.ok(tilbakekreving.sendVarselbrev(behandlingId, sendForhaandsvarselDto.tekstFraSaksbehandler, context))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingSplittPeriode(behandlingId: UUID, splittPeriode: SplittPeriodeDto): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.SplitteVilkårsvurderingsperiode,
        ) { tilbakekreving, context ->
            ResponseEntity.ok(tilbakekreving.splitteVilkårsvurderingsperioder(behandlingId, context, splittPeriode.splittFra))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingVilkaarsvurderingsperioder(behandlingId: UUID): ResponseEntity<List<PeriodeDto>> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.Vilkårsvurderingsperioder,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekreving.hentVilkårsvurderingsperioder(behandlingId))
    }

    override fun behandlingLagreBrukersuttalelse(behandlingId: UUID, uttalelseDto: UttalelseDto): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.LagreBrukersuttalelse,
        ) { tilbakekreving, context ->
            ResponseEntity.ok(forhåndsvarselService.nyLagreUttalelse(behandlingId, tilbakekreving, uttalelseDto, context))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingUtsettUttalelsesfrist(behandlingId: UUID, updateUttalelsesfristDto: UpdateUttalelsesfristDto): ResponseEntity<UttalelsesfristDto> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.UtsettUttalelsesfrist,
        ) { tilbakekreving, context ->
            ResponseEntity.ok(tilbakekreving.lagreFristUtsettelse(behandlingId, updateUttalelsesfristDto.nyFrist!!, updateUttalelsesfristDto.begrunnelse!!, context))
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingLagreForhaandsvarselUnntak(behandlingId: UUID, unntakDto: UnntakDto): ResponseEntity<Unit> {
        return tilbakekrevingService.endreTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.LagreForhåndsvarselUnntak,
        ) { tilbakekreving, context ->
            ResponseEntity.ok(
                tilbakekreving.lagreForhåndsvarselUnntak(
                    behandlingId = behandlingId,
                    begrunnelseForUnntak = when (unntakDto.begrunnelseForUnntak) {
                        VarslingsunntakDto.IKKE_PRAKTISK_MULIG -> BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG
                        VarslingsunntakDto.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING -> BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                        VarslingsunntakDto.ÅPENBART_UNØDVENDIG -> BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG
                        VarslingsunntakDto.ALLEREDE_UTTALET_SEG -> BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG
                    },
                    beskrivelse = unntakDto.beskrivelse,
                    sideeffektContext = context,
                ),
            )
        } ?: ResponseEntity.notFound().build()
    }

    override fun behandlingHentDokumentInfo(behandlingId: UUID, dokumentType: DokumentTypeDto): ResponseEntity<DokumentInfoDto> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.HentDokumentInfo,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(tilbakekreving.hentDokumentInfo(dokumentType))
    }

    override fun behandlingHentDokument(behandlingId: UUID, journalpostId: String, dokumentInfoId: String): ResponseEntity<Any> {
        val tilbakekreving = tilbakekrevingService.lesTilbakekreving(
            filter = TilbakekrevingFilter.behandling(behandlingId),
            valideringContext = ValideringContext.HentDokument,
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            safService.hentDokument(
                behandlingId = behandlingId,
                journalpostId = journalpostId,
                dokumentInfoId = dokumentInfoId,
                tilbakekreving.eksternFagsak.eksternId,
            ),
        )
    }
}
