package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelseDto
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.time.LocalDate
import java.util.UUID

class Forhåndsvarsel(
    private var brukeruttalelse: Brukeruttalelse?,
    private var forhåndsvarselUnntak: ForhåndsvarselUnntak?,
    private var utsattFrist: UtsettFrist?,
    private var opprinneligFrist: LocalDate?,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FORHÅNDSVARSEL
    override var erUnderkjent: Boolean = false

    override fun erFullstendig(): Boolean {
        val gjeldendeFrist = utsattFrist?.hentFrist() ?: opprinneligFrist
        return !erUnderkjent &&
            (
                brukeruttalelse != null ||
                    forhåndsvarselUnntak != null ||
                    gjeldendeFrist?.isBefore(LocalDate.now()) == true
            )
    }

    override fun underkjennSteget() {
        this.erUnderkjent = true
    }


    override fun nullstill(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering) {}

    fun tilEntity(behandlingRef: UUID): ForhåndsvarselEntity {
        return ForhåndsvarselEntity(
            brukeruttalelseEntity = brukeruttalelse?.tilEntity(behandlingRef),
            forhåndsvarselUnntakEntity = forhåndsvarselUnntak?.tilEntity(behandlingRef),
            fristUtsettelseEntity = utsattFrist?.tilEntity(behandlingRef),
        )
    }

    fun nullstillUnntakOgUttalelse() {
        forhåndsvarselUnntak = null
        brukeruttalelse = null
    }

    fun lagreUttalelse(
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: List<UttalelseInfo>,
        kommentar: String?,
    ) {
        brukeruttalelse = Brukeruttalelse(
            id = UUID.randomUUID(),
            uttalelseVurdering = uttalelseVurdering,
            uttalelseInfo = uttalelseInfo,
            kommentar = kommentar,
        )
    }

    fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String) {
        utsattFrist = UtsettFrist(
            id = UUID.randomUUID(),
            nyFrist = nyFrist,
            begrunnelse = begrunnelse,
        )
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
        )
    }

    fun brukeruttaleserTilFrontendDto(): BrukeruttalelseDto? {
        return brukeruttalelse?.tilFrontendDto()
    }

    fun utsettUttalelseFristTilFrontendDto(): FristUtsettelseDto? {
        return utsattFrist?.tilFrontendDto()
    }

    fun forhåndsvarselUnntakTilFrontendDto(): ForhåndsvarselUnntakDto? {
        return forhåndsvarselUnntak?.tilFrontendDto()
    }
}
