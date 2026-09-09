package no.nav.tilbakekreving.kravgrunnlag

interface EndretKravgrunnlagObservatør {
    fun periodeEndret(forskjell: KravgrunnlagSammenligning.Forskjell.EndretPeriode)

    fun nyPeriode(periode: KravgrunnlagSammenligning.Forskjell.NyPeriode) {}

    fun periodeFjernet(periode: KravgrunnlagSammenligning.Forskjell.FjernetPeriode) {}
}
