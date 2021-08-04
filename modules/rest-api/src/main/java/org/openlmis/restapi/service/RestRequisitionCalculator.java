/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright Â© 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Â 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.Â  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.Â  If not, see http://www.gnu.org/licenses. Â For additional information contact info@OpenLMIS.org.Â 
 */

package org.openlmis.restapi.service;

import org.joda.time.DateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import org.openlmis.LmisThreadLocalUtils;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.ProcessingPeriod;
import org.openlmis.core.domain.Program;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.service.ProcessingScheduleService;
import org.openlmis.core.service.StaticReferenceDataService;
import org.openlmis.pod.domain.OrderPODLineItem;
import org.openlmis.pod.service.PODService;
import org.openlmis.rnr.domain.Rnr;
import org.openlmis.rnr.domain.RnrLineItem;
import org.openlmis.rnr.search.criteria.RequisitionSearchCriteria;
import org.openlmis.rnr.service.RequisitionService;
import org.openlmis.core.utils.MessageKeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class acts as helper class exposing methods to validate requisition attributes,
 * also has methods to compute attributes like quantity received, beginning balance.
 */

@Component
public class RestRequisitionCalculator {

  @Autowired
  private RequisitionService requisitionService;

  @Autowired
  private PODService podService;

  @Autowired
  private ProcessingScheduleService processingScheduleService;

  @Autowired
  private StaticReferenceDataService staticReferenceDataService;

  private static final Logger LOGGER = LoggerFactory.getLogger(RestRequisitionCalculator.class);

  public void validatePeriod(Facility reportingFacility, Program reportingProgram, Date periodStartDate, Date periodEndDate) {

    if (!reportingFacility.getVirtualFacility()) {

      RequisitionSearchCriteria searchCriteria = new RequisitionSearchCriteria();
      searchCriteria.setProgramId(reportingProgram.getId());
      searchCriteria.setFacilityId(reportingFacility.getId());

      if (staticReferenceDataService.getBoolean("toggle.requisitions.allow.previous")) {
        validatePeriodBasedOnActualPeriodDates(reportingFacility, reportingProgram, periodStartDate, periodEndDate);
      } else {

        if (requisitionService.getCurrentPeriod(searchCriteria) != null && !requisitionService.getCurrentPeriod(searchCriteria).getId().equals
            (requisitionService.getPeriodForInitiating(reportingFacility, reportingProgram).getId())) {
          throw new DataException("error.rnr.previous.not.filled");
        }
      }
    }
  }

  private void validatePeriodBasedOnActualPeriodDates(Facility reportingFacility, Program reportingProgram, Date periodStartDate, Date periodEndDate) {
    ProcessingPeriod periodForInitialize = requisitionService.getPeriodForInitiating(reportingFacility, reportingProgram);

    if (periodForInitialize == null) {
      throw new DataException("error.program.configuration.missing");
    }

    List<Rnr> rnrs = requisitionService.getNormalRnrsByPeriodAndProgram(periodStartDate, periodEndDate, reportingProgram.getId(), reportingFacility.getId());

    DateTime actualStart = new DateTime(periodStartDate);
    DateTime actualEnd = new DateTime(periodEndDate);
    if (rnrs != null && !rnrs.isEmpty()) {
      LOGGER.error("facilityId {} programId {}, {}-{} has been submitted",
          actualStart.toString("yyyy-MM"), actualEnd.toString("yyyy-MM"),
          LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_FACILITY_ID),
          reportingProgram.getId());
      throw new DataException(MessageKeyUtils.RNR_PERIOD_DUPLICATE);
    }
    if (periodStartDate != null) {
      DateTime initStart = new DateTime(periodForInitialize.getStartDate());
      DateTime initEnd = new DateTime(periodForInitialize.getEndDate());
      
      if (verifyExpectedPeriod(initStart, initEnd, actualStart, actualEnd)) {
          LOGGER.error("facilityId {} programId {}, expected period is {}-{}, but submit is {}-{}, ",
              LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_FACILITY_ID),
              reportingProgram.getId(), initStart.toString("yyyy-MM"), initEnd.toString("yyyy-MM"),
              actualStart.toString("yyyy-MM"), actualEnd.toString("yyyy-MM"));
        throw new DataException(MessageKeyUtils.RNR_ERROR_RNR_PERIOD_INVALID, initStart.toString("yyyy-MM"),
            initEnd.toString("yyyy-MM"));
      }
    }
  }
  
  private boolean verifyExpectedPeriod(DateTime initStart, DateTime initEnd, DateTime actualStart, DateTime actualEnd) {
    if (calculateDateMonthOffset(initStart.toDate(), new Date()) > 12) {
      return false;
    }
    return initStart.getYear() != actualStart.getYear()
        || initStart.getMonthOfYear() != actualStart.getMonthOfYear()
        || initEnd.getYear() != actualEnd.getYear()
        || initEnd.getMonthOfYear() != actualEnd.getMonthOfYear();
        
  }

  public void validateCustomPeriod(Facility reportingFacility, Program reportingProgram, ProcessingPeriod period, Long userId) {

    if (period == null) {
      throw new DataException("error.rnr.period.provided.is.invalid");
    }

    RequisitionSearchCriteria searchCriteria = new RequisitionSearchCriteria();
    searchCriteria.setProgramId(reportingProgram.getId());
    searchCriteria.setFacilityId(reportingFacility.getId());

    List<ProcessingPeriod> periods = new ArrayList<ProcessingPeriod>();
    periods.add(period);

    searchCriteria.setWithoutLineItems(true);
    searchCriteria.setUserId(userId);
    List<Rnr> list = requisitionService.getRequisitionsFor(searchCriteria, periods);
    if (list != null && !list.isEmpty() && !list.get(0).preAuthorize()) {
      throw new DataException("error.rnr.already.submitted.for.this.period");
    }
  }

  public void validateProducts(List<RnrLineItem> products, Rnr savedRequisition) {
    if (products == null) {
      return;
    }

    List<String> invalidProductCodes = new ArrayList<>();
    for (final RnrLineItem product : products) {
      RnrLineItem correspondingLineItem = savedRequisition.findCorrespondingLineItem(product);
      if (correspondingLineItem == null) {
        invalidProductCodes.add(product.getProductCode());
      }
    }
    if (!invalidProductCodes.isEmpty()) {
      LOGGER.error(String.format("invalid product code : %s", invalidProductCodes.toString()));
//      throw new DataException("invalid.product.codes", invalidProductCodes.toString());
    }
  }

  public Rnr setDefaultValues(Rnr requisition) {
    Integer M = processingScheduleService.findM(requisition.getPeriod());

    List<ProcessingPeriod> nPreviousPeriods = processingScheduleService.getNPreviousPeriodsInDescOrder(requisition.getPeriod(), 2);
    Date trackingDate = requisition.getPeriod().getStartDate();

    if (!nPreviousPeriods.isEmpty()) {
      trackingDate = M >= 3 ? nPreviousPeriods.get(0).getStartDate() : nPreviousPeriods.get(nPreviousPeriods.size() - 1).getStartDate();
    }

    for (RnrLineItem rnrLineItem : requisition.getNonSkippedLineItems()) {
      setBeginningBalance(rnrLineItem, requisition, trackingDate);
      setQuantityReceived(rnrLineItem, requisition, trackingDate);
    }
    return requisition;
  }

  private void setQuantityReceived(RnrLineItem rnrLineItem, Rnr requisition, Date trackingDate) {
    if (rnrLineItem.getQuantityReceived() != null)
      return;

    List<OrderPODLineItem> nOrderPodLineItems = podService.getNPreviousOrderPodLineItems(rnrLineItem.getProductCode(), requisition, 1, trackingDate);

    Integer quantityReceived = !nOrderPodLineItems.isEmpty() ? nOrderPodLineItems.get(0).getQuantityReceived() : 0;

    rnrLineItem.setQuantityReceived(quantityReceived);
  }

  private void setBeginningBalance(RnrLineItem rnrLineItem, Rnr requisition, Date trackingDate) {
    List<RnrLineItem> nRnrLineItems = requisitionService.getNRnrLineItems(rnrLineItem.getProductCode(), requisition, 1, trackingDate);

    if (!nRnrLineItems.isEmpty()) {
      if (rnrLineItem.getBeginningBalance() != null) {
        rnrLineItem.setPreviousStockInHand(nRnrLineItems.get(0).getStockInHand());
      } else {
        rnrLineItem.setBeginningBalance(nRnrLineItems.get(0).getStockInHand());
        rnrLineItem.setPreviousStockInHand(nRnrLineItems.get(0).getStockInHand());
      }
      return;
    } else {
      if (rnrLineItem.getBeginningBalance() != null)
        return;
    }

    Integer beginningBalance = rnrLineItem.getStockInHand() != null ? rnrLineItem.getStockInHand() : 0;
    rnrLineItem.setBeginningBalance(beginningBalance);
  }

  public static int calculateDateMonthOffset(Date earlierDate, Date laterDate) {
    Calendar startCalendar = new GregorianCalendar();
    startCalendar.setTime(earlierDate);
    Calendar endCalendar = new GregorianCalendar();
    endCalendar.setTime(laterDate);

    int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
    return diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
  }
}
