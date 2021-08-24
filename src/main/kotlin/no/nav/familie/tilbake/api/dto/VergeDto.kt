package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype

data class VergeDto(val ident: String? = null,
                    val orgNr: String? = null,
                    val type: Vergetype,
                    val navn: String,
                    val kilde: String,
                    val begrunnelse: String?)
