package org.openlmis.report.generator.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.report.generator.AbstractReportModelGenerator;
import org.openlmis.report.model.dto.LotInfo;
import org.openlmis.report.model.dto.StockProductDto;
import org.openlmis.report.model.params.StockReportParam;
import org.openlmis.report.service.SimpleTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(value="singleStockOnHand")
public class StockOnHandForSingleProductReportGenerator extends AbstractReportModelGenerator {

    private static Logger logger = Logger.getLogger(OverStockProductReportGenerator.class);

    private final static String KEY_QUERY_RESULT = "KEY_QUERY_RESULT";

    private final static String MOS_COLUMN = "6";

    private final static String CMM_COLUMN = "7";

    @Autowired
    private SimpleTableService simpleTableService;

    @Override
    protected Map<String, Object> getQueryResult(Map<Object, Object> paraMap) {
        StockReportParam filterCriteria = new StockReportParam();
        try {
            filterCriteria.setEndTime(DateUtil.parseDate(paraMap.get("endTime").toString()));
            if (null != paraMap.get("provinceId")
                    && StringUtils.isNotEmpty(paraMap.get("provinceId").toString().trim()))
            {
                filterCriteria.setProvinceId(Integer.parseInt(paraMap.get("provinceId").toString()));
            }
            if (null != paraMap.get("districtId")
                    && StringUtils.isNotEmpty(paraMap.get("districtId").toString().trim())) {
                filterCriteria.setDistrictId(Integer.parseInt(paraMap.get("districtId").toString()));
            }
            if (null != paraMap.get("facilityId")
                    && StringUtils.isNotEmpty(paraMap.get("facilityId").toString().trim())) {
                filterCriteria.setFacilityId(Integer.parseInt(paraMap.get("facilityId").toString().trim()));
            }
            if (null != paraMap.get("productCode")
                    && StringUtils.isNotEmpty(paraMap.get("productCode").toString().trim())) {
                filterCriteria.setProductCode(paraMap.get("productCode").toString().trim());
            }
        }
        catch (Throwable e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
        List<StockProductDto> stockProductDtoList = simpleTableService.getStockProductData(filterCriteria);

        Map<String, Object> result = new HashMap<>();
        result.put(KEY_QUERY_RESULT, stockProductDtoList);
        return result;
    }

    @Override
    protected Object getReportHeaders(Map<Object, Object> paraMap, Map<String, Object> queryResult) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("facility", getMessage("report.header.facility"));
        headers.put("drugName", getMessage("report.header.drug.name"));
        headers.put("lot", getMessage("report.header.lot"));
        headers.put("sohOfLot", getMessage("report.header.sohoflot"));
        headers.put("totalSoh", getMessage("report.header.sohoflot.total"));
        headers.put("expiryDate", getMessage("report.header.expiry.date"));
        headers.put("MoS", getMessage("report.estimated.consumption.month"));
        headers.put("cmm", getMessage("report.header.cmm"));
        headers.put("lastSyncDate", getMessage("report.header.last.sync.date"));
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
                rowMap.put("facility", dto.getFacilityName());
                rowMap.put("drugName", dto.getProductName());
                rowMap.put("lot", lotinfo.getLotNumber());
                rowMap.put("sohOfLot", lotinfo.getStockOnHandOfLot());
                rowMap.put("totalSoh", dto.getSumStockOnHand());

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

    private Date earliestExpiryDate(List<LotInfo> lotInfos) {
        Date max = null;
        for (LotInfo lotInfo : lotInfos) {
            if (null == max) {
                max = lotInfo.getExpiryDate();
            }
            if (max.getTime() < lotInfo.getExpiryDate().getTime()) {
                max = lotInfo.getExpiryDate();
            }
        }
        return max;
    }

    @Override
    protected List<Map<String, String>> getReportMergedRegions(Map<Object, Object> paraMap,
                                                               Map<String, Object> queryResult) {
        List<StockProductDto> stockProductDtoList = (List<StockProductDto>) queryResult.get(KEY_QUERY_RESULT);
        List<Map<String, String>> mergedRegions = new ArrayList<>();
        int index = 0;
        for (StockProductDto dto : stockProductDtoList) {
            int cmmSpan = dto.getLotList().size();
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "4", "4", dto.getSumStockOnHand().toString()));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "5", "5", earliestExpiryDate(dto.getLotList()).toString()));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "6", "6", getFormatDoubleValue(dto.getMos())));
            mergedRegions.add(createMergedRegion(String.valueOf(index + 1), String.valueOf(index + cmmSpan),
                    "7", "7", getFormatDoubleValue(dto.getCmm())));
            index = index + cmmSpan;
        }
        return mergedRegions;
    }

    @Override
    protected Object reportDataForFrontEnd(Map<Object, Object> paraMap) {
        return getQueryResult(paraMap).get(KEY_QUERY_RESULT);
    }
}
