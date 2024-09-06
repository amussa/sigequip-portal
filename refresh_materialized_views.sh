#!/bin/bash

export PATH=/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin

LOGFILE="/tmp/sigequip-refresh.log"

{
    echo "Iniciando o refresh das materialized views: $(date)"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "SELECT refresh_stock_card_entries_soh();"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_carry_start_dates;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_stockouts;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_daily_full_soh;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_cmm_entries;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_lot_expiry_dates;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_period_movements;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_weekly_nos_soh;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_weekly_tracer_soh;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_lot_expiry_dates;"
    /usr/bin/psql -U postgres -d open_lmis -p5433 -c "REFRESH MATERIALIZED VIEW public.vw_lot_expiry_dates_facilityid_lotid;"
    echo "Finalizando o refresh das materialized views: $(date)"
} >> "$LOGFILE" 2>&1
