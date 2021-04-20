package org.openlmis.restapi.controller;

import com.wordnik.swagger.annotations.Api;
import lombok.NoArgsConstructor;
import org.openlmis.core.exception.DataException;
import org.openlmis.restapi.domain.StockCardDTO;
import org.openlmis.restapi.response.RestResponse;
import org.openlmis.restapi.service.RestStockCardService;
import org.openlmis.restapi.utils.KitProductFilterUtils;
import org.openlmis.stockmanagement.domain.StockCardEntry;
import org.openlmis.stockmanagement.dto.StockCardDeleteDTO;
import org.openlmis.stockmanagement.dto.StockEvent;
import org.openlmis.stockmanagement.service.StockCardService;
import org.openlmis.stockmanagement.util.JsonUtils;
import org.openlmis.stockmanagement.util.StockCardLockConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.openlmis.restapi.response.RestResponse.error;
import static org.openlmis.restapi.response.RestResponse.response;
import static org.openlmis.restapi.utils.KitProductFilterUtils.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@NoArgsConstructor
@Api(value = "Stock Card", description = "Stock card update", position = 0)
public class RestStockCardController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(StockCardEntry.class);

  @Autowired
  private RestStockCardService restStockCardService;
  @Autowired
  private StockCardService stockCardService;

  @RequestMapping(value = "/rest-api/facilities/{facilityId}/stockCards", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity adjustStock(@PathVariable long facilityId,
                                    @RequestHeader(value = "VersionCode", required = false) String versionCode,
                                    @RequestBody List<StockEvent> events,
                                    Principal principal) {
    List<StockEvent> filterStockEvents;
    if (KitProductFilterUtils.isBiggerThanThresholdVersion(versionCode, FILTER_THRESHOLD_VERSION)) {
      filterStockEvents = restStockCardService
          .filterStockEventsList(events, WRONG_KIT_PRODUCTS_SET);
    } else {
      filterStockEvents = restStockCardService
          .filterStockEventsList(events, ALL_FILTER_KIT_PRODUCTS_SET);
    }

    try {
      restStockCardService.adjustStock(facilityId, filterStockEvents, loggedInUserId(principal));
    } catch (DataException e) {
      logger.error(e.getOpenLmisMessage().toString());
      return error(e.getOpenLmisMessage(), BAD_REQUEST);
    } catch (Exception e) {
      logger.error(e.getMessage());
      return error(e.getMessage(), BAD_REQUEST);
    }

    return RestResponse.success("msg.stockmanagement.adjuststocksuccess");
  }

  @RequestMapping(value = "/rest-api/facilities/split/{facilityId}/stockCards", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity adjustStock(@PathVariable long facilityId,
      @RequestBody List<StockEvent> events, Principal principal) {
    if (!restStockCardService.validFacility(facilityId)) {
      throw new DataException("error.facility.unknown");
    }
    List<StockEvent> correctEventList = restStockCardService
        .filterStockEventsList(events, WRONG_KIT_PRODUCTS_SET);
    Map<String, List<StockEvent>> productStockEventMap = restStockCardService
        .groupByProduct(correctEventList);
    List<String> errorProductCodes = new ArrayList<>();
    Long userId = loggedInUserId(principal);
    for (Map.Entry<String, List<StockEvent>> entry : productStockEventMap.entrySet()) {
      try {
        if (stockCardService.tryLock(facilityId, entry.getKey(), StockCardLockConstants.UPDATE)) {
          restStockCardService.adjustStockSpilt(facilityId, entry.getValue(), userId);
        } else {
          errorProductCodes.add(entry.getKey());
        }
      } catch (DataException e) {
        errorProductCodes.add(entry.getKey());
        logger.error("facilityId {} productCode {} sync error", facilityId, entry.getKey());
      } catch (Exception e) {
        logger.error(e.getMessage());
        return error(e.getMessage(), BAD_REQUEST);
      } finally {
        stockCardService.release(facilityId, entry.getKey(), StockCardLockConstants.UPDATE);
        productStockEventMap.put(entry.getKey(), null);
      }
    }
    return response("errorProductCodes", errorProductCodes);
  }

  @RequestMapping(value = "/rest-api/facilities/{facilityId}/stockCards", method = GET, headers = ACCEPT_JSON)
  public ResponseEntity getStockMovements(@PathVariable long facilityId,
                                          @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date startTime,
                                          @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") final Date endTime) {
    List<StockCardDTO> stockCards;
    try {
      stockCards = restStockCardService.queryStockCardByOccurred(facilityId, startTime, endTime);
    } catch (DataException e) {
      return error(e.getOpenLmisMessage(), BAD_REQUEST);
    }
    return response("stockCards", stockCards);
  }

  @RequestMapping(value = "/rest-api/facilities/{facilityId}/unSyncedStockCards", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity updateStockCardsUpdatedTime(@PathVariable long facilityId, @RequestBody(required = true) List<String> unsyncedStockCardProductCodes) {
    try {
      restStockCardService.updateStockCardSyncTime(facilityId, unsyncedStockCardProductCodes);
    } catch (DataException e) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(null);
  }


  @RequestMapping(value = "/rest-api/facilities/{facilityId}/deleteStockCards", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity deleteStockCards(@PathVariable long facilityId,
      @RequestBody List<StockCardDeleteDTO> stockCardDeleteDTOs, Principal principal) {
    Long userId = loggedInUserId(principal);
    try {
      restStockCardService.deleteStockCards(facilityId, stockCardDeleteDTOs, userId);
      return ResponseEntity.ok(null);
    } catch (DataException e) {
      logger.error("Failed to delete stock cards, facilityId {}, stockCardDeleteDTOs is {}", facilityId, JsonUtils.toJsonString(stockCardDeleteDTOs));
      return ResponseEntity.badRequest().build();
    }
  }
}
