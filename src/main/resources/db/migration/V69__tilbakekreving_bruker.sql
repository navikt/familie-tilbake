CREATE TABLE tilbakekreving_bruker(
    id UUID NOT NULL PRIMARY KEY,
    tilbakekreving_ref INT NOT NULL REFERENCES tilbakekreving(id) ON DELETE CASCADE,
    ident VARCHAR (20) NOT NULL ,
    aktør_type VARCHAR (20) NOT NULL ,
    språkkode VARCHAR(2),
    navn VARCHAR (64),
    fødselsdato DATE,
    kjønn VARCHAR (10),
    dødsdato DATE
);
