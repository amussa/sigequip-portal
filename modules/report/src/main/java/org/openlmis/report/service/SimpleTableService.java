package org.openlmis.report.service;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.repository.mapper.FacilityMapper;
import org.openlmis.report.generator.RegionLevel;
import org.openlmis.report.generator.StockOnHandStatus;
import org.openlmis.report.mapper.AppInfoMapper;
import org.openlmis.report.mapper.ProductLotInfoMapper;
import org.openlmis.report.mapper.RequisitionReportsMapper;
import org.openlmis.report.mapper.StockOnHandInfoMapper;
import org.openlmis.report.model.dto.*;
import org.openlmis.report.model.params.NonSubmittedRequisitionReportsParam;
import org.openlmis.report.model.params.StockReportParam;
import org.openlmis.report.model.params.RequisitionReportsParam;
import org.openlmis.stockmanagement.domain.CMMEntry;
import org.openlmis.stockmanagement.repository.mapper.CMMMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimpleTableService {
    @Autowired
    private RequisitionReportsMapper requisitionReportsMapper;

    @Autowired
    private ProductLotInfoMapper productLotInfoMapper;

    @Autowired
    private StockOnHandInfoMapper stockOnHandInfoMapper;

    @Autowired
    private FacilityMapper facilityMapper;

    @Autowired
    private CMMMapper cmmMapper;

    @Autowired
    private StockStatusService stockStatusService;

    @Autowired
    private AppInfoMapper appInfoMapper;

    protected static Logger logger = LoggerFactory.getLogger(SimpleTableService.class);

    public List<RequisitionDTO> getRequisitions(RequisitionReportsParam filterCriteria) {
        List<RequisitionDTO> requisitions = new ArrayList<>();
        requisitions.addAll(getSubmittedRequisitions(filterCriteria));
        requisitions.addAll(getUnSubmittedRequisitions(filterCriteria));

        for (RequisitionDTO requisitionDTO : requisitions) {
            requisitionDTO.assignType();
        }

        return requisitions;
    }

    private List<RequisitionDTO> getSubmittedRequisitions(RequisitionReportsParam filterCriteria) {
        List<RequisitionDTO> requisitions = requisitionReportsMapper.getSubmittedRequisitionList(filterCriteria);
        return requisitions;
    }

    public List<StockProductDto> getStockProductData(StockReportParam filterCriteria) {
        List<StockProductDto> stockProducts = new ArrayList<>();
        long t1 = System.currentTimeMillis();
        List<ProductLotInfo> productLotInfos = productLotInfoMapper.getProductLotInfoList(filterCriteria);
        long t2 = System.currentTimeMillis();
        logger.info("-----1:"+(t2-t1));

        if (CollectionUtils.isEmpty(productLotInfos)) {
            return stockProducts;
        }

        Map<String, StockProductDto> stockProductDtoMap
                = filterCriteria.getRegion() == RegionLevel.DISTRICT
                ? stockProductGroupByDistrict(productLotInfos) : stockProductGroupByFacility(productLotInfos);
        long t3 = System.currentTimeMillis();
        logger.info("-----2:"+(t3-t2));

        filterZeroSohLot(stockProductDtoMap);
        long t4 = System.currentTimeMillis();
        logger.info("-----3:"+(t4-t3));


        Map<String, CMMEntry> cmmEntryMap = getProductCmmMap(filterCriteria);
        long t5 = System.currentTimeMillis();
        logger.info("-----4:"+(t5-t4));


        Map<String, Integer> sohMap = sohMap(stockOnHandInfoMapper.getStockOnHandInfoList(filterCriteria));
        long t6 = System.currentTimeMillis();
        logger.info("-----5:"+(t6-t5));

        StockProductDto stockProduct;
        for (Map.Entry<String, StockProductDto> entry : stockProductDtoMap.entrySet()) {
            stockProduct = entry.getValue();
            stockProduct.setSumStockOnHand(sohMap.get(getEntryMapKey(stockProduct.getProductCode(), stockProduct.getFacilityId().toString())));
            stockProduct = calcCmmAndSoh(stockProduct, cmmEntryMap, filterCriteria);
            if (null != stockProduct) {
                stockProducts.add(stockProduct);
            }
        }
        long t7 = System.currentTimeMillis();
        logger.info("-----6:"+(t7-t6));

        return stockProducts;
    }

    private void filterZeroSohLot(Map<String, StockProductDto> stockProductDtoMap) {
        for (Map.Entry<String, StockProductDto> entry : stockProductDtoMap.entrySet()) {
            StockProductDto stockProductDto = entry.getValue();
            List<LotInfo> lotList = new ArrayList<>();
            for (LotInfo lotInfo : entry.getValue().getLotList()) {
                if (null != lotInfo.getStockOnHandOfLot() && lotInfo.getStockOnHandOfLot() > 0) {
                    lotList.add(lotInfo);
                }
            }
            if (0 == lotList.size()) {
                lotList.add(new LotInfo("-", Calendar.getInstance().getTime(), 0));
            }
            stockProductDto.setLotList(lotList);
        }
    }

    private Map<String, Integer> sohMap(List<StockOnHandDto> stockOnHandDtos) {
        Map<String, Integer> sohMap = new HashMap<>();
        for (StockOnHandDto stockOnHandDto : stockOnHandDtos) {
            sohMap.put(getEntryMapKey(stockOnHandDto.getProductCode(),stockOnHandDto.getFacilityId().toString()), stockOnHandDto.getSoh());
        }
        return sohMap;
    }

    public List<StockProductDto> getOverStockProductReport(StockReportParam filterCriteria) {
        return getStockProductsByStatus(filterCriteria, StockOnHandStatus.OVER_STOCK);
    }

    public List<StockProductDto> getStockProductsByStatus(StockReportParam filterCriteria,
                                                          StockOnHandStatus stockOnHandStatus) {
        List<StockProductDto> result = new ArrayList<>();
        List<StockProductDto> stockProducts = getStockProductData(filterCriteria);
        for (StockProductDto stockProductDto : stockProducts) {
            if (stockProductDto.getStockOnHandStatus() == stockOnHandStatus) {
                result.add(stockProductDto);
            }
        }
        return result;
    }

    private StockProductDto calcCmmAndSoh(StockProductDto stockProduct, Map<String, CMMEntry> cmmEntryMap, StockReportParam filterCriteria) {
        double cmm = -1.0f;
        CMMEntry cmmEntry = cmmEntryMap.get(getEntryMapKey(stockProduct.getProductCode(), stockProduct.getFacilityId().toString()));
        if (null != cmmEntry && null != cmmEntry.getCmmValue()) {
            cmm = cmmEntry.getCmmValue();
        }
        StockOnHandStatus status = stockStatusService.getStockOnHandStatus(cmm, stockProduct);
        if (status == StockOnHandStatus.NOT_EXIST) {
            return null;
        }
        stockProduct.setStockOnHandStatus(status);

        if (cmm < 0) {
            stockProduct.setCmm(0.0);
        } else {
            stockProduct.setCmm(cmm);
        }
        stockProduct.setMos(stockStatusService.calcMos(cmm, stockProduct));
        return stockProduct;
    }

    private Map<String, CMMEntry> getProductCmmMap(StockReportParam filterCriteria) {
        List<CMMEntry> CMMEntryList = new ArrayList<>();
        if(null != filterCriteria.getDistrictId()){
            CMMEntryList = cmmMapper.getCMMEntryByDistrictAndDay(filterCriteria.getDistrictId().longValue(), filterCriteria.getEndTime());
        }else if (null != filterCriteria.getProvinceId()){
            CMMEntryList = cmmMapper.getCMMEntryByProvinceAndDay(filterCriteria.getProvinceId().longValue(), filterCriteria.getEndTime());
        } else {
            CMMEntryList = cmmMapper.getCMMEntryByDay(filterCriteria.getEndTime());
        }

        Map<String, CMMEntry> cmmEntryMap = new HashMap<>();
        for (CMMEntry cmmEntry : CMMEntryList) {
            cmmEntryMap.put(getEntryMapKey(cmmEntry.getProductCode(), cmmEntry.getFacilityId().toString()), cmmEntry);
        }
        return cmmEntryMap;
    }

    private String getEntryMapKey(String productCode, String facilityId) {
        return String.format("%s-%s", productCode, facilityId);
    }


    private Map<String, StockProductDto> stockProductGroupByFacility(List<ProductLotInfo> productLotInfos) {
        Map<String, StockProductDto> stockProductDtoMap = new HashMap<>();
        String key;
        LotInfo lotinfo;
        for (ProductLotInfo productLotInfo : productLotInfos) {
            key = productLotInfo.getFacilityKey();
            lotinfo = new LotInfo(productLotInfo.getLotNumber(), productLotInfo.getExpiryDate(), productLotInfo.getStockOnHandOfLot());
            if (stockProductDtoMap.containsKey(key)) {
                stockProductDtoMap.get(key).getLotList().add(lotinfo);
            } else {
                StockProductDto dto = StockProductDto.of(productLotInfo);
                dto.getLotList().add(lotinfo);
                stockProductDtoMap.put(key, dto);
            }
        }
        return stockProductDtoMap;
    }

    private Map<String, StockProductDto> stockProductGroupByDistrict(List<ProductLotInfo> productLotInfos) {
        Map<String, StockProductDto> stockProductDtoMap = new HashMap<>();
        String key;
        LotInfo lotinfo;
        for (ProductLotInfo productLotInfo : productLotInfos) {
            key = productLotInfo.getDistirctKey();
            lotinfo = new LotInfo(productLotInfo.getLotNumber(), productLotInfo.getExpiryDate(), productLotInfo.getStockOnHandOfLot());
            if (stockProductDtoMap.containsKey(key)) {
                List<LotInfo> lotInfoList = stockProductDtoMap.get(key).getLotList();
                stockProductDtoMap.get(key).setLotList(addLotInfo(lotInfoList, lotinfo));
            } else {
                StockProductDto dto = StockProductDto.of(productLotInfo);
                dto.getLotList().add(lotinfo);
                stockProductDtoMap.put(key, dto);
            }
        }
        return stockProductDtoMap;
    }

    private List<LotInfo> addLotInfo(List<LotInfo> lotList, LotInfo lotInfo) {
        for(int index = 0; index < lotList.size(); index++) {
            LotInfo currentLotInfo = lotList.get(index);
            if(null != currentLotInfo.getLotNumber()
                    && null != lotInfo.getLotNumber()
                    && StringUtils.equalsIgnoreCase(currentLotInfo.getLotNumber(),lotInfo.getLotNumber())) {
                currentLotInfo.setStockOnHandOfLot(currentLotInfo.getStockOnHandOfLot() + lotInfo.getStockOnHandOfLot());
                lotList.set(index, currentLotInfo);
                return lotList;
            }
        }
        if (null != lotInfo.getLotNumber()) {
            lotList.add(lotInfo);
        }
        return lotList;
    }

    private List<RequisitionDTO> getUnSubmittedRequisitions(RequisitionReportsParam filterCriteria) {
        List<RequisitionDTO> requisitions = new ArrayList<>();
        List<Integer> facilityIds = getFacilityIds(filterCriteria);
        List<Integer> programIds = filterCriteria.getProgramIds();
        NonSubmittedRequisitionReportsParam nonSubmittedRequisitionReportsParam;
        for (Integer facilityId : facilityIds) {
            for (Integer programId : programIds) {
                nonSubmittedRequisitionReportsParam = NonSubmittedRequisitionReportsParam.builder()
                        .startTime(filterCriteria.getStartTime())
                        .endTime(filterCriteria.getEndTime())
                        .facilityId(facilityId)
                        .programId(programId)
                        .build();
                requisitions.addAll(requisitionReportsMapper.getUnSubmittedRequisitionList(nonSubmittedRequisitionReportsParam));
            }
        }

        return requisitions;
    }

    private List<Integer> getFacilityIds(RequisitionReportsParam filterCriteria) {
        List<Integer> facilityIds = new ArrayList<>();

        if (null != filterCriteria.getFacilityId()) {
            facilityIds.add(filterCriteria.getFacilityId());
            return facilityIds;
        }

        if (null != filterCriteria.getDistrictId()) {
            facilityIds = facilityMapper.getFacilityIdByDistrictId(filterCriteria.getDistrictId());
            return facilityIds;
        }

        if (null != filterCriteria.getProvinceId()) {
            facilityIds = facilityMapper.getFacilityIdByProvinceId(filterCriteria.getProvinceId());
            return facilityIds;
        }

        facilityIds = facilityMapper.getAllFacilityIds();
        return facilityIds;
    }

    public List<AppInfo> getAppInfos() {
        List<AppInfo> appInfos = appInfoMapper.queryAll();
        return convertAppVersionForV88(appInfos);
    }

    private List<AppInfo> convertAppVersionForV88(List<AppInfo> appInfos) {
        for (AppInfo appInfo : appInfos) {
            if ("1.12.88".equals(appInfo.getAppVersion())) {
                appInfo.setAppVersion("1.13.87");
            }
        }
        return appInfos;
    }
}