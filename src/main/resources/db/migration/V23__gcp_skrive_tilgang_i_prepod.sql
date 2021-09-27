${ignoreIfProd}
DO $$
    BEGIN
        IF EXISTS
            ( SELECT 1 from pg_roles where rolname='cloudsqliamuser')
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO cloudsqliamuser;
            GRANT ALL ON ALL TABLES IN SCHEMA public TO cloudsqliamuser;
        END IF ;
    END
$$ ;