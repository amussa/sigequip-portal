CREATE MATERIALIZED VIEW vw_period_movements_after_20210721
TABLESPACE pg_default
AS SELECT uuid_in(md5(CONCAT(facility_code,periodstart,drug_code,reason_code)::text)::cstring) AS uuid,
periodstart,
periodend,
facility_name,
drug_name,
facility_code,
drug_code,
province_name,
province_code,
district_name,
district_code,
soh,
cmm,
reason_code,
occurrences,
total_quantity
from (SELECT
    stock_cards_soh_cmm.periodstart,
    stock_cards_soh_cmm.periodend,
    stock_cards_soh_cmm.facility_name,
    stock_cards_soh_cmm.drug_name,
    stock_cards_soh_cmm.drug_code,
    stock_cards_soh_cmm.facility_code,
    stock_cards_soh_cmm.province_name,
    stock_cards_soh_cmm.province_code,
    stock_cards_soh_cmm.district_name,
    stock_cards_soh_cmm.district_code,
    stock_cards_soh_cmm.soh,
    stock_cards_soh_cmm.cmm,
    (total_quantity_and_occurrences(stock_cards_soh_cmm.stockcardid, stock_cards_soh_cmm.periodstart::timestamp without time zone, stock_cards_soh_cmm.periodend::timestamp without time zone)).*
FROM vw_period_soh_and_cmm_after_20210721 stock_cards_soh_cmm) as vm
WITH NO DATA;

-- View indexes:
CREATE UNIQUE INDEX idx_vw_period_movements_after_20210721 ON vw_period_movements_after_20210721(uuid);
CREATE INDEX idx_vw_period_movements_after_20210721_drug_prc_dtc_fcc ON vw_period_movements_after_20210721(drug_code text_ops,province_code text_ops, district_code text_ops, facility_code text_ops);


-- Create refresh functions
CREATE OR REPLACE FUNCTION refresh_period_movements_after_20210721()
  RETURNS INT LANGUAGE plpgsql
AS $$
BEGIN
  REFRESH MATERIALIZED VIEW CONCURRENTLY vw_period_movements_after_20210721;
  RETURN 1;
END $$;