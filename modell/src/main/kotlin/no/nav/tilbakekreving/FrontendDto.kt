package no.nav.tilbakekreving

fun interface FrontendDto<Dto> {
    fun tilFrontendDto(lesContext: LesContext): Dto
}
