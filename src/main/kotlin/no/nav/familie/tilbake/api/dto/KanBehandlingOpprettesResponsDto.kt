package no.nav.familie.tilbake.api.dto

data class KanBehandlingOpprettesResponsDto(val kanBehandlingOpprettes: Boolean,
                                            val melding: String,
                                            val kravgrunnlagsreferanse: String? = null)
