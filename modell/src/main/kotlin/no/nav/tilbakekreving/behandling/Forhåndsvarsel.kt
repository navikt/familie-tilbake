package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselDto
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselErSendtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import java.time.LocalDate
import java.util.UUID

class Forhåndsvarsel(
    private var brukeruttalelse: Brukeruttalelse?,
    private var forhåndsvarselUnntak: ForhåndsvarselUnntak?,
    private var uttalelsesfrist: Uttalelsesfrist?,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FORHÅNDSVARSEL

    override val behandlingsstatus: BehandlingsstatusModell get() = if (forhåndsvarselUnntak == null && uttalelsesfrist == null) {
        BehandlingsstatusModell.TIL_FORHÅNDSVARSEL
    } else {
        BehandlingsstatusModell.TIL_BEHANDLING
    }

    override fun erFullstendig(klokke: Klokke): Boolean {
        val gjeldendeFrist = uttalelsesfrist?.hentFrist()
        return brukeruttalelse != null ||
            forhåndsvarselUnntak != null ||
            gjeldendeFrist?.isBefore(klokke.dagensDato()) == true
    }

    override fun erUnderkjent(): Boolean {
        return brukeruttalelse?.trengerNyVurdering() == true || forhåndsvarselUnntak?.trengerNyVurdering() == true
    }

    override fun underkjennSteget() {
        brukeruttalelse?.vurderPåNytt()
        forhåndsvarselUnntak?.vurderPåNytt()
    }

    override fun nullstill(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering) {}

    override fun venter(klokke: Klokke): Venter? {
        return uttalelsesfrist?.gjeldendeFrist(klokke)?.let {
            Venter(
                grunn = Venter.Grunn.BRUKERUTTALELSE,
                frist = it,
            )
        }
    }

    fun tilEntity(behandlingRef: UUID): ForhåndsvarselEntity {
        return ForhåndsvarselEntity(
            brukeruttalelseEntity = brukeruttalelse?.tilEntity(behandlingRef),
            forhåndsvarselUnntakEntity = forhåndsvarselUnntak?.tilEntity(behandlingRef),
            uttalelsesfristEntity = uttalelsesfrist?.tilEntity(behandlingRef),
        )
    }

    fun nullstillUnntakOgUttalelse() {
        forhåndsvarselUnntak = null
        brukeruttalelse = null
    }

    fun lagreUttalelse(
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: UttalelseInfo?,
        kommentar: String?,
    ) {
        brukeruttalelse = Brukeruttalelse(
            id = UUID.randomUUID(),
            uttalelseVurdering = uttalelseVurdering,
            uttalelseInfo = uttalelseInfo,
            kommentar = kommentar,
            trengerNyVurdering = false,
        )
    }

    fun lagreOpprinneligFrist(opprinneligFrist: LocalDate) {
        uttalelsesfrist = Uttalelsesfrist(
            id = UUID.randomUUID(),
            opprinneligFrist = opprinneligFrist,
            nyFrist = null,
            begrunnelse = null,
        )
    }

    fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String): UttalelsesfristDto {
        uttalelsesfrist!!.utsettFrist(nyFrist, begrunnelse)
        return uttalelsesfrist!!.nyTilFrontendDto()
    }

    fun lagreForhåndsvarselUnntak(
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
    ) {
        if (begrunnelseForUnntak != BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG) {
            brukeruttalelse = null
        }
        forhåndsvarselUnntak = ForhåndsvarselUnntak(
            id = UUID.randomUUID(),
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
            trengerNyVurdering = false,
        )
    }

    fun tilFrontendDto(varselbrev: Varselbrev?) = ForhåndsvarselDto(
        varselbrevDto = varselbrev?.tilFrontendDto(),
        brukeruttalelse = brukeruttalelse?.tilFrontendDto(),
        forhåndsvarselUnntak = forhåndsvarselUnntak?.tilFrontendDto(),
        utsettUttalelseFrist = uttalelsesfrist?.tilFrontendDto(),
    )

    override fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> {
        return brukeruttalelse?.meldingerTilSaksbehandler() ?: emptySet()
    }

    fun nyForhåndsvarselTilFrontend(varselbrev: Varselbrev?): ForhaandsvarselResponseDto = when {
        uttalelsesfrist != null -> ForhaandsvarselResponseDto(
            forhaandsvarselSteg = ForhaandsvarselErSendtDto(
                forhåndsvarselInfo = varselbrev!!.tilForhåndsvarselDto(),
                uttalelsesfrist = uttalelsesfrist!!.nyTilFrontendDto(),
            ),
            brukeruttalelse = brukeruttalelse?.nyTilFrontendDto()
                ?: UttalelseDto(harBrukerUttaltSeg = UttalelseVurderingDto.IKKE_VURDERT),
        )

        forhåndsvarselUnntak != null -> ForhaandsvarselResponseDto(
            forhaandsvarselSteg = forhåndsvarselUnntak!!.nyTilFrontendDto(),
            brukeruttalelse = brukeruttalelse?.nyTilFrontendDto(),
        )

        else -> ForhaandsvarselResponseDto(
            forhaandsvarselSteg = IkkeVurdertDto,
            brukeruttalelse = brukeruttalelse?.nyTilFrontendDto(),
        )
    }

    companion object {
        fun opprett(): Forhåndsvarsel {
            return Forhåndsvarsel(
                brukeruttalelse = null,
                forhåndsvarselUnntak = null,
                uttalelsesfrist = null,
            )
        }
    }
}
