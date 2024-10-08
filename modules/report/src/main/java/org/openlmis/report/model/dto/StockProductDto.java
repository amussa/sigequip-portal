package org.openlmis.report.model.dto;

import lombok.*;
import org.openlmis.report.generator.StockOnHandStatus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockProductDto {
    private Integer provinceId;
    private String provinceName;
    private Integer districtId;
    private String districtName;
    private Integer facilityId;
    private String facilityName;
    private String facilityCode;
    private Integer productId;
    private String productCode;
    private String productName;
    List<LotInfo> lotList = new ArrayList<>();
    private Double cmm;
    private Double mos;
    private Boolean isHiv;
    private Integer sumStockOnHand;
    private StockOnHandStatus stockOnHandStatus;
    private String lastSyncUpDate;
    private Date syncDate;
    private Double price;

    public static StockProductDto of(ProductLotInfo lotInfo){
        StockProductDto dto = new StockProductDto();

        dto.setProvinceId(lotInfo.getProvinceId());
        dto.setProvinceName(lotInfo.getProvinceName());
        dto.setDistrictId(lotInfo.getDistrictId());
        dto.setDistrictName(lotInfo.getDistrictName());
        dto.setFacilityId(lotInfo.getFacilityId());
        dto.setFacilityName(lotInfo.getFacilityName());
        dto.setFacilityCode(lotInfo.getFacilityCode());
        dto.setProductId(lotInfo.getProductId());
        dto.setProductCode(lotInfo.getProductCode());
        dto.setProductName(lotInfo.getProductName());
        dto.setIsHiv(lotInfo.getIsHiv());
        dto.setLastSyncUpDate(lotInfo.getLastSyncDateString());
        dto.setSyncDate(lotInfo.getLastSyncDate());
        dto.setPrice(lotInfo.getPrice());
        return dto;
    }
}
