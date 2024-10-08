/*
 * Electronic Logistics Management Information System (eLMIS) is a supply chain management system for health commodities in a developing country setting.
 *
 * Copyright (C) 2015  John Snow, Inc (JSI). This program was produced for the U.S. Agency for International Development (USAID). It was prepared under the USAID | DELIVER PROJECT, Task Order 4.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openlmis.report.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.RowBounds;
import org.openlmis.report.builder.MailingLabelReportQueryBuilder;
import org.openlmis.report.model.ReportParameter;
import org.openlmis.report.model.report.MailingLabelReport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailingLabelReportMapper {

  @SelectProvider(type = MailingLabelReportQueryBuilder.class, method = "getQuery")
  @Options(resultSetType = ResultSetType.SCROLL_SENSITIVE, fetchSize = 10, timeout = 0, useCache = true, flushCache = Options.FlushCachePolicy.TRUE)

  @Results(value = {
    @Result(column = "code", property = "code"),
    @Result(column = "name", property = "facilityName"),
    @Result(column = "active", property = "active"),
    @Result(column = "facilityType", property = "facilityType"),
    @Result(column = "region", property = "region"),
    @Result(column = "owner", property = "owner"),
    @Result(column = "gpsCoordinates", property = "gpsCoordinates"),
    @Result(column = "phoneNumber", property = "phoneNumber"),
    @Result(column = "fax", property = "fax")
  })
  public List<MailingLabelReport> SelectFilteredSortedPagedFacilities(
    @Param("filterCriteria") ReportParameter filterCriteria,
    @Param("RowBounds") RowBounds rowBounds
  );
}
