package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel

data class ForhåndsvarselEntity(
    val brukeruttalelseEntity: BrukeruttalelseEntity?,
    val forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
    val fristUtsettelseEntity: List<FristUtsettelseEntity>,
) {
    fun fraEntity(): Forhåndsvarsel = Forhåndsvarsel(
        brukeruttalelse = brukeruttalelseEntity?.fraEntity(),
        forhåndsvarselUnntak = forhåndsvarselUnntakEntity?.fraEntity(),
        utsattFrist = fristUtsettelseEntity.map { it.fraEntity() }.toMutableList(),
    )
}
