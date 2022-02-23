create function refresh_period_movements_after_20220121() returns integer
    language plpgsql
as
$$
BEGIN
  REFRESH MATERIALIZED VIEW CONCURRENTLY vw_period_movements_after_20220121;
RETURN 1;
END
$$;

alter function refresh_period_movements_after_20220121() owner to openlmis;

