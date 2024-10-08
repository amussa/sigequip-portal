package org.openlmis.restapi.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.openlmis.stockmanagement.domain.StockCardEntryKV;
import org.openlmis.stockmanagement.domain.StockCardEntryLotItem;
import org.openlmis.stockmanagement.domain.StockCardEntryType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_EMPTY;
import static java.lang.Math.abs;

@Data
@NoArgsConstructor
@JsonSerialize(include = NON_EMPTY)
public class StockCardMovementDTO {

    String documentNumber;

    long movementQuantity;

    String reason;

    StockCardEntryType type;

    HashMap<String,String> extensions = new HashMap<>();

    Date createdDate;

    String occurred;

    Long requested;

    List<LotMovementDTO> lotMovementItems;

    public StockCardMovementDTO(StockCardEntry stockCardEntry) {
        this.documentNumber = stockCardEntry.getReferenceNumber();
        this.movementQuantity = abs(stockCardEntry.getQuantity());
        this.reason = stockCardEntry.getAdjustmentReason().getName();
        this.initCustomProps(stockCardEntry);
        this.createdDate = stockCardEntry.getCreatedDate();
        if (stockCardEntry.getOccurred() != null) {
            this.occurred = new SimpleDateFormat(DateUtil.FORMAT_DATE).format(stockCardEntry.getOccurred());
        }
        this.type = stockCardEntry.getType();
        this.requested = stockCardEntry.getRequestedQuantity();
        this.lotMovementItems = FluentIterable.from(stockCardEntry.getStockCardEntryLotItems()).transform(new Function<StockCardEntryLotItem, LotMovementDTO>() {
          @Override
          public LotMovementDTO apply(StockCardEntryLotItem stockCardEntryLotItem) {
            return new LotMovementDTO(stockCardEntryLotItem);
          }
        }).toList();
    }

    private void initCustomProps(StockCardEntry stockCardEntry) {
        for (StockCardEntryKV stockCardEntryKV :stockCardEntry.getExtensions()){
            this.extensions.put(stockCardEntryKV.getKey(), stockCardEntryKV.getValue());
        }
    }
}
