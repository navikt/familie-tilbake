package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Forhåndsvarsel
import java.time.LocalDate

data class ForhåndsvarselEntity(
    val brukeruttalelseEntity: BrukeruttalelseEntity?,
    val forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?,
    val fristUtsettelseEntity: List<FristUtsettelseEntity>,
) {
    fun fraEntity(opprinneligFrist: LocalDate?): Forhåndsvarsel = Forhåndsvarsel(
        brukeruttalelse = brukeruttalelseEntity?.fraEntity(),
        forhåndsvarselUnntak = forhåndsvarselUnntakEntity?.fraEntity(),
        utsattFrist = fristUtsettelseEntity.map { it.fraEntity() }.toMutableList(),
        opprinneligFrist = opprinneligFrist,
    )
}
