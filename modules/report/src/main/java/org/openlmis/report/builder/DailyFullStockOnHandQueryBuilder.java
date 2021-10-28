package org.openlmis.report.builder;

import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.jdbc.SqlBuilder;
import org.openlmis.report.model.params.StockReportParam;

import java.util.Map;

import static org.apache.ibatis.jdbc.SqlBuilder.BEGIN;
import static org.apache.ibatis.jdbc.SqlBuilder.FROM;
import static org.apache.ibatis.jdbc.SqlBuilder.JOIN;
import static org.apache.ibatis.jdbc.SqlBuilder.SELECT;
import static org.apache.ibatis.jdbc.SqlBuilder.WHERE;

public class DailyFullStockOnHandQueryBuilder {

  public static String get(Map params) {
    BEGIN();
    return new SQL()
        .SELECT("facilityId, productCode, soh, occurred")
        .FROM("(" + getStockOnHandInfo(params) + ") as tmp")
        .WHERE("tmp.rank = 1").toString();
  }

  public static String getStockOnHandInfo(Map params) {

    StockReportParam filter = (StockReportParam) params.get("filterCriteria");

    SELECT(
        "stock_cards.facilityid as facilityId, products.code as productCode,stock_card_entries_soh.valuecolumn as soh, stock_card_entries_soh.occurred");
    SELECT(
        "row_number() over (partition by stock_card_entries_soh.stockcardid order by stock_card_entries_soh.occurred desc) rank");
    FROM("stock_card_entries_soh");
    JOIN("stock_cards ON stock_card_entries_soh.stockcardid = stock_cards.id");
    JOIN("products ON stock_cards.productid = products.id");
    JOIN("facilities ON facilities.id = stock_cards.facilityid");
    JOIN("geographic_zones zone on facilities.geographiczoneid = zone.id");
    JOIN("geographic_zones parent_zone on zone.parentid = parent_zone.id");
    writePredicates(filter);
    return SqlBuilder.SQL();
  }

  private static void writePredicates(StockReportParam filter) {
    if (null != filter) {
      WHERE("stock_card_entries_soh.occurred <= #{filterCriteria.endTime}");

      if (null != filter.getProvinceCode()) {
        WHERE("parent_zone.code = #{filterCriteria.provinceCode}");
      }
      if (null != filter.getDistrictCode()) {
        WHERE("zone.code = #{filterCriteria.districtCode}");
      }
      if (null != filter.getFacilityId()) {
        WHERE("stock_cards.facilityid = #{filterCriteria.facilityId}");
      }
      if (null != filter.getProductCode()) {
        WHERE("products.code = #{filterCriteria.productCode}");
      }
    }
  }
}
