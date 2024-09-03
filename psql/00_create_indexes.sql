CREATE INDEX idx_lotid_stock_card_entry_lot_items ON stock_card_entry_lot_items (lotid);
CREATE INDEX idx_lotid_stock_movement_lots ON stock_movement_lots (lotid);
CREATE INDEX idx_lotid_stock_movement_line_items ON stock_movement_line_items (lotid);
CREATE INDEX idx_lotid_lots_on_hand ON lots_on_hand (lotid);
CREATE INDEX idx_lotid_lot_conflicts ON lot_conflicts (lotid);
CREATE INDEX idx_productcode_requisition_line_items ON requisition_line_items (productcode);
CREATE INDEX idx_productcode_cmm_entries ON cmm_entries (productcode);

