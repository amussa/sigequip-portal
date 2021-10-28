package org.openlmis.report.model.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LotExpiryByFacilityDto {
    private String facilityName;
    private String facilityCode;
    private String districtName;
    private String districtCode;
    private String provinceName;
    private String provinceCode;
    private String drugCode;
    private String drugName;
    private Double createddate;
    private Double occurred;
    private String lotonhand;
    private Integer lotid;
    private String lotNumber;
    private String expiryDate;
}
