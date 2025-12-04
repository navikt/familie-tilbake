package no.nav.tilbakekreving.integrasjoner.felles

import org.apache.commons.lang3.StringUtils
import org.springframework.core.io.ClassPathResource

fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))

fun graphqlQuery(path: String) = ClassPathResource(path).url.readText().graphqlCompatible()
