
-- Desativar os triggers em todas as tabelas
DO $$
DECLARE
r RECORD;
BEGIN
    -- Iterar sobre todas as tabelas no schema público e desativar todos os triggers
FOR r IN
SELECT tablename
FROM pg_tables
WHERE schemaname = 'public'
    LOOP
        EXECUTE 'ALTER TABLE ' || r.tablename || ' DISABLE TRIGGER ALL';
RAISE NOTICE 'Triggers desativados para a tabela: %', r.tablename;
END LOOP;
END $$;

-- verificar estado triggers
SELECT
    tgname AS trigger_name,
    tgrelid::regclass AS table_name,
        CASE
            WHEN tgenabled = 'D' THEN 'DISABLED'
            ELSE 'ENABLED'
            END AS trigger_state
FROM
    pg_trigger
WHERE
        tgenabled = 'D'
ORDER BY
    tgrelid::regclass, tgname;


