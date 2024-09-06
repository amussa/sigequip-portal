DO $$
DECLARE
r RECORD;
BEGIN
    -- Iterar sobre todas as tabelas no schema público e ativar todos os triggers
FOR r IN
SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ALTER TABLE ' || r.tablename || ' ENABLE TRIGGER ALL';
RAISE NOTICE 'Triggers ativados para a tabela: %', r.tablename;
END LOOP;
END $$;
