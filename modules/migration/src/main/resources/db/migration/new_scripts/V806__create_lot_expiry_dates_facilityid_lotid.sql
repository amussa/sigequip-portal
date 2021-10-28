DROP MATERIALIZED VIEW IF EXISTS vw_lot_expiry_dates_facilityId_lotId;

CREATE MATERIALIZED VIEW  vw_lot_expiry_dates_facilityId_lotId as
select facility_id,lot_id from vw_lot_expiry_dates group by facility_id,lot_id WITH NO DATA;

CREATE UNIQUE INDEX idx_vw_lot_expiry_dates_facilityId_lotId ON vw_lot_expiry_dates_facilityId_lotId(facility_id int4_ops,lot_id int4_ops);
