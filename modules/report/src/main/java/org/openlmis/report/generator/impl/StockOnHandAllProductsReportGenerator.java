package org.openlmis.report.generator.impl;

import org.openlmis.core.utils.DateUtil;
import org.openlmis.report.model.dto.LotInfo;
import org.openlmis.report.model.dto.StockProductDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(value = "stockOnHandAll")
public class StockOnHandAllProductsReportGenerator extends StockOnHandForSingleProductReportGenerator {

    @Override
    protected Object getReportHeaders(Map<Object, Object> paraMap, Map<String, Object> queryResult) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("drugName", getMessage("report.header.drug.name"));
        headers.put("lot", getMessage("report.header.lot"));
        headers.put("sohOfLot", getMessage("report.header.sohoflot"));
        headers.put("totalSoh", getMessage("report.header.sohoflot.total"));
        headers.put("status", getMessage("report.header.status"));
        headers.put("expiryDate", getMessage("report.soonest.expiry.date"));
        headers.put("MoS", getMessage("report.estimated.consumption.month"));
        headers.put("cmm", getMessage("report.header.cmm"));
        headers.put("lastSyncDate", getMessage("report.header.last.update.from.tablet"));
        return headers;
    }

    @Override
    protected Object getReportContent(Map<Object, Object> paraMap, Map<String, Object> queryResult) {
        List<StockProductDto> stockProductDtoList = (List<StockProductDto>) queryResult.get(KEY_QUERY_RESULT);
        List<Map<String, Object>> content = new ArrayList<>();
        for (StockProductDto dto : stockProductDtoList) {
            Date date = earliestExpiryDate(dto.getLotList());
            for (LotInfo lotinfo : dto.getLotList()) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("drugName", dto.getProductName());
                rowMap.put("lot", lotinfo.getLotNumber());
                rowMap.put("sohOfLot", lotinfo.getStockOnHandOfLot());
                rowMap.put("totalSoh", dto.getSumStockOnHand());
                rowMap.put("status", getMessage(dto.getStockOnHandStatus().getMessageKey()));

                Map<String, Object> tmpValue = new HashMap<>();
                tmpValue.put("value", DateUtil.formatDate(date));
                Map<String, Object> styleMap = new HashMap<>();
                styleMap.put("dataPattern", DateUtil.FORMAT_DATE_TIME);
                styleMap.put("excelDataPattern", "m/d/yy");
                tmpValue.put("style", styleMap);
                tmpValue.put("dataType", "date");
                rowMap.put("expiryDate", tmpValue);

                rowMap.put("MoS", getFormatDoubleValue(dto.getMos()));
                rowMap.put("cmm", getFormatDoubleValue(dto.getCmm()));


                Map<String, Object> tmpValue2 = new HashMap<>();
                tmpValue2.put("value", DateUtil.formatDate(dto.getSyncDate()));
                Map<String, Object> styleMap2 = new HashMap<>();
                styleMap2.put("dataPattern", DateUtil.FORMAT_DATE_TIME);
                styleMap2.put("excelDataPattern", "m/d/yy");
                tmpValue2.put("style", styleMap2);
                tmpValue2.put("dataType", "date");
                rowMap.put("lastSyncDate", tmpValue2);

                content.add(rowMap);
            }
        }

        return content;
    }

    @Override
    protected List<Map<String, String>> getReportMergedRegions(Map<Object, Object> paraMap,
                                                               Map<String, Object> queryResult) {
        List<StockProductDto> stockProductDtoList = (List<StockProductDto>) queryResult.get(KEY_QUERY_RESULT);
        List<Map<String, String>> mergedRegions = new ArrayList<>();
        int index = 1;
        for (StockProductDto dto : stockProductDtoList) {
            int cmmSpan = dto.getLotList().size();
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "3", "3", dto.getSumStockOnHand().toString()));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "4", "4", earliestExpiryDate(dto.getLotList()).toString()));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "5", "5", getFormatDoubleValue(dto.getMos())));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "6", "6", getFormatDoubleValue(dto.getCmm())));
            index = index + cmmSpan;
        }
        return mergedRegions;
    }
}
