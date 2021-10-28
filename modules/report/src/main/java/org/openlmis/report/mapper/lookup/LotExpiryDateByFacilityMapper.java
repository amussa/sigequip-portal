/*
 * Electronic Logistics Management Information System (eLMIS) is a supply chain management system for health commodities in a developing country setting.
 *
 * Copyright (C) 2015 Clinton Health Access Initiative (CHAI). This program was produced for the U.S. Agency for International Development (USAID). It was prepared under the USAID | DELIVER PROJECT, Task Order 4.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openlmis.report.mapper.lookup;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.openlmis.report.model.dto.LotExpiryByFacilityDto;
import org.springframework.stereotype.Repository;

@Repository
public interface LotExpiryDateByFacilityMapper {

  @Select("select "
      + "uuid,"
      + "facility_code as facilityCode,"
      + "facility_name as facilityName,"
      + "province_code as provinceCode,"
      + "province_name as provinceName,"
      + "district_code as districtCode,"
      + "district_name as districtName,"
      + "drug_name as drugName,"
      + "drug_code as drugCode,"
      + "occurred,"
      + "lot_number as lotNumber,"
      + "expiration_date AS expiryDate,"
      + "lot_on_hand AS lotonhand,"
      + "createddate "
      + " from loop_select_vw_lot_expiry_dates(#{occurred},#{provinceId},#{districtId},#{facilityId})")
    List<LotExpiryByFacilityDto> getLotExpiryByFacilityData(@Param(value = "occurred") Long occurred,
      @Param(value = "provinceId") Integer provinceId, @Param(value = "districtId") Integer districtId,
      @Param(value = "facilityId") Integer facilityId);


}
