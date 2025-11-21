package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import java.util.UUID

class Forhåndsvarsel(
    private var brukeruttalelse: Brukeruttalelse?,
    private var forhåndsvarselUnntak: ForhåndsvarselUnntak?,
) {
    fun tilEntity(behandlingRef: UUID): ForhåndsvarselEntity? {
        if (brukeruttalelse != null || forhåndsvarselUnntak != null) {
            return ForhåndsvarselEntity(
                brukeruttalelseEntity = brukeruttalelse?.tilEntity(behandlingRef),
                forhåndsvarselUnntakEntity = forhåndsvarselUnntak?.tilEntity(behandlingRef),
            )
        }
        return null
    }

    fun lagreUttalelse(
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: List<UttalelseInfo>,
        kommentar: String?,
        utsettFrist: List<UtsettFristInfo>,
    ) {
        brukeruttalelse = Brukeruttalelse(
            id = UUID.randomUUID(),
            uttalelseVurdering = uttalelseVurdering,
            uttalelseInfo = uttalelseInfo,
            kommentar = kommentar,
            utsettUttalselsFrist = utsettFrist,
        )
    }

    fun lagreForhåndsvarselUnntak(
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
        uttalelseInfo: List<UttalelseInfo>,
    ) {
        forhåndsvarselUnntak = ForhåndsvarselUnntak(
            id = UUID.randomUUID(),
            begrunnelseForUnntak = begrunnelseForUnntak,
            beskrivelse = beskrivelse,
        )
        if (uttalelseInfo.isNotEmpty()) {
            brukeruttalelse = Brukeruttalelse(
                id = UUID.randomUUID(),
                uttalelseVurdering = UttalelseVurdering.valueOf(begrunnelseForUnntak.name),
                uttalelseInfo = uttalelseInfo,
                kommentar = null,
                utsettUttalselsFrist = listOf(),
            )
        }
    }

    fun brukeruttaleserTilFrontendDto(): BrukeruttalelseDto? {
        return brukeruttalelse?.tilFrontendDto()
    }

    fun forhåndsvarselUnntakTilFrontendDto(): ForhåndsvarselUnntakDto? {
        return forhåndsvarselUnntak?.tilFrontendDto()
    }
}
