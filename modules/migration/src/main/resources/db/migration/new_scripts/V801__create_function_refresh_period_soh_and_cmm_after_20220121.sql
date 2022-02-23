create function refresh_period_soh_and_cmm_after_20220121() returns integer
    language plpgsql
as
$$
BEGIN
  REFRESH MATERIALIZED VIEW vw_period_soh_and_cmm_after_20220121;
RETURN 1;
END
$$;

alter function refresh_period_soh_and_cmm_after_20220121() owner to openlmis;

