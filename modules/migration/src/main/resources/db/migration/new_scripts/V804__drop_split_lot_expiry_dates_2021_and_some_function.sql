DROP MATERIALIZED VIEW IF EXISTS public.vw_lot_expiry_dates_before_20211001;
DROP MATERIALIZED VIEW IF EXISTS public.vw_lot_expiry_dates_after_20211001;
DROP FUNCTION refresh_vw_lot_expiry_dates_before_20211001();
DROP FUNCTION refresh_vw_lot_expiry_dates_after_20211001();
DROP FUNCTION refresh_period_movements_before_20210721();
DROP FUNCTION refresh_period_soh_and_cmm_after_20210721();
DROP FUNCTION refresh_period_movements_after_20210721();