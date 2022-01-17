package no.nav.familie.tilbake.common

import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.tilbake.config.PropertyName

fun ITask.fagsystem(): String = this.metadata.getProperty(PropertyName.FAGSYSTEM, "UKJENT")