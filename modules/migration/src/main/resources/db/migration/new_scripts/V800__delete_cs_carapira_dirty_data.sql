--CMM
DELETE FROM cmm_entries WHERE stockcardid IN (SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617));

--deleteStockCardEntryKeyValues(sceIds)
DELETE from stock_card_entry_key_values where stockcardentryid in (SELECT id FROM stock_card_entries WHERE stockcardid IN ( SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617)));

--deleteStockCardEntryLotItemsKeyValues(sceIds)
DELETE from stock_card_entry_lot_items_key_values where stockcardentrylotitemid IN (SELECT id FROM stock_card_entry_lot_items WHERE stockcardentryid in (SELECT id FROM stock_card_entries WHERE stockcardid IN ( SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617))));

--deleteStockCardEntryLotItems(sceIds)
DELETE from stock_card_entry_lot_items where stockcardentryid in (SELECT id FROM stock_card_entries WHERE stockcardid IN ( SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617)));

--deleteLotsOnHand(scIds)
DELETE FROM lots_on_hand WHERE stockcardid in (SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617));

--deleteStockCardEntryByStockCardIds(scIds)
DELETE from stock_card_entries where stockcardid in (SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617));

--deleteStockCards(scIds)
DELETE from stock_cards where id in (SELECT id FROM stock_cards WHERE stock_cards.facilityid = 819 AND stock_cards.productid in (26,62,180,397,1671,610,721,964,1013,1116,1762,1604,1692,1619,1695,1617));

--deleteArchivedProductListByFacilityIdAndProductCode
DELETE FROM archived_products WHERE facilityId = 819 and productcode in ('29A01','20A0X','01F01','29A02Y','27A03ZZ','22A15','17C04','08S17','08A10Z','04F06W','24W025','08L14','10A06Z','29A03','02D01','15G01');