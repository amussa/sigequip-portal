package org.openlmis.report.model.params;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.report.annotations.RequiredParam;
import org.openlmis.report.generator.RegionLevel;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockReportParam {

    private static Logger logger = LogManager.getLogger(StockReportParam.class);
    @RequiredParam
    private Date endTime;

    private Integer provinceId;
    private Integer districtId;
    private Integer facilityId;
    private String productCode;
    private String provinceCode;
    private String districtCode;

    private FilterCondition filterCondition;
    @Setter
    private RegionLevel regionLevel;

    public void setValue(Map<Object, Object> paraMap) {
        try {
            setEndTime(DateUtil.parseDate(paraMap.get("endTime").toString()));
            if (isValidParam(paraMap,"provinceId"))
            {
                setProvinceId(Integer.parseInt(paraMap.get("provinceId").toString()));
            }
            if (isValidParam(paraMap, "districtId")) {
                setDistrictId(Integer.parseInt(paraMap.get("districtId").toString()));
            }
            if (isValidParam(paraMap,"facilityId")) {
                setFacilityId(Integer.parseInt(paraMap.get("facilityId").toString().trim()));
            }
            if (isValidParam(paraMap, "productCode")) {
                setProductCode(paraMap.get("productCode").toString().trim());
            }
            if (isValidParam(paraMap, "regionLevel")) {
                setRegionLevel((RegionLevel)paraMap.get("regionLevel"));
            }
        }
        catch (Throwable e) {
            logger.error(ExceptionUtils.getStackTrace(e));
        }
    }


    private boolean isValidParam(Map<Object, Object> paraMap, String key) {
        return null != paraMap.get(key)
                && StringUtils.isNotEmpty(paraMap.get(key).toString().trim());
    }

    public interface FilterCondition {
        String getCondition();
    }

    public RegionLevel getRegion() {
        if (null != regionLevel) {
            return regionLevel;
        }
        if (null == districtId) {
            return RegionLevel.DISTRICT;
        }
        return RegionLevel.FACILITY;
    }
}