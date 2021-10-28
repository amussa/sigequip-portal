DROP MATERIALIZED VIEW IF EXISTS vw_lot_expiry_dates;

CREATE MATERIALIZED VIEW vw_lot_expiry_dates AS
SELECT uuid_in(md5(random()::text || now()::text)::cstring) AS uuid,
       lots.lotnumber                                       AS lot_number,
       lots.expirationdate                                  AS expiration_date,
       stock_entry_loh.stock_card_entry_id,
       stock_entry_loh.facility_name,
       stock_entry_loh.facility_code,
       stock_entry_loh.facility_id,
       stock_entry_loh.district_name,
       stock_entry_loh.district_code,
       stock_entry_loh.province_name,
       stock_entry_loh.province_code,
       stock_entry_loh.drug_code,
       stock_entry_loh.drug_name,
       stock_entry_loh.createddate,
       stock_entry_loh.occurred,
       stock_entry_loh.lot_id,
       stock_entry_loh.lot_on_hand
FROM (SELECT stock_card_entries.id AS stock_card_entry_id,
             facilities.name       AS facility_name,
             facilities.code       AS facility_code,
             facilities.id         AS facility_id,
             zone.name             AS district_name,
             zone.code             AS district_code,
             parent_zone.name      AS province_name,
             parent_zone.code      AS province_code,
             products.code         AS drug_code,
             products.primaryname  AS drug_name,
             date_part('epoch'::text, stock_card_entries.createddate) * 1000::double precision AS createddate,
            date_part('epoch'::text, stock_card_entries.occurred) * 1000::double precision AS occurred,
            stock_card_entry_lot_items.lotid AS lot_id,
            NULLIF(stock_card_entry_lot_items_key_values.valuecolumn, ''::text)::integer AS lot_on_hand
      FROM facilities
          JOIN geographic_zones zone
      ON facilities.geographiczoneid = zone.id
          JOIN geographic_zones parent_zone ON zone.parentid = parent_zone.id
          JOIN stock_cards ON facilities.id = stock_cards.facilityid
          JOIN products ON stock_cards.productid = products.id
          JOIN stock_card_entries ON stock_cards.id = stock_card_entries.stockcardid
          JOIN stock_card_entry_lot_items ON stock_card_entry_lot_items.stockcardentryid = stock_card_entries.id
          JOIN stock_card_entry_lot_items_key_values ON stock_card_entry_lot_items_key_values.stockcardentrylotitemid = stock_card_entry_lot_items.id
      ORDER BY facilities.code, products.code, date_part('epoch'::text, stock_card_entries.occurred) * 1000:: double precision, stock_card_entries.id DESC) stock_entry_loh
         JOIN lots ON stock_entry_loh.lot_id = lots.id WITH NO DATA;

DROP INDEX IF EXISTS idx_vw_lot_expiry_dates_facilityId_lotId_occurred;
CREATE INDEX idx_vw_lot_expiry_dates_facilityId_lotId_occurred ON vw_lot_expiry_dates(facility_id int4_ops,lot_id int4_ops,occurred float8_ops desc);
