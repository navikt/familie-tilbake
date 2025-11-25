package no.nav.tilbakekreving.api.v1.dto

data class ForhåndsvarselDto(
    val varselbrevDto: VarselbrevDto?,
    val brukeruttalelse: BrukeruttalelseDto?,
    val utsettUttalelseFrist: List<FristUtsettelseDto>,
    val forhåndsvarselUnntak: ForhåndsvarselUnntakDto?,
)
