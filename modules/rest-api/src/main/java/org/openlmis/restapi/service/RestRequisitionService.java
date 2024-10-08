/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org.
 */

package org.openlmis.restapi.service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.joda.time.DateTime;
import org.openlmis.LmisThreadLocalUtils;
import org.openlmis.core.domain.*;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.repository.SyncUpHashRepository;
import org.openlmis.core.service.*;
import org.openlmis.order.service.OrderService;
import org.openlmis.restapi.domain.RegimenLineItemForRest;
import org.openlmis.restapi.domain.ReplenishmentDTO;
import org.openlmis.restapi.domain.Report;
import org.openlmis.rnr.domain.*;
import org.openlmis.rnr.repository.mapper.RegimenLineItemMapper;
import org.openlmis.rnr.search.criteria.RequisitionSearchCriteria;
import org.openlmis.rnr.service.RequisitionService;
import org.openlmis.rnr.service.RnrTemplateService;
import org.openlmis.core.utils.MessageKeyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.find;
import static org.openlmis.restapi.domain.ReplenishmentDTO.prepareForREST;

/**
 * This service exposes methods for creating, approving a requisition.
 */

@Service
@NoArgsConstructor
public class RestRequisitionService {

  public static final boolean EMERGENCY = false;
  private static final String RAPID_TEST_PROGRAM_CODE = "TEST_KIT";
  private static final String MMIA_PROGRAM_CODE = "MMIA";
  private static final Logger logger = LoggerFactory.getLogger(RestRequisitionService.class);
  @Autowired
  private RequisitionService requisitionService;
  @Autowired
  private OrderService orderService;
  @Autowired
  private FacilityService facilityService;
  @Autowired
  private ProgramService programService;
  @Autowired
  private ProgramSupportedService programSupportedService;
  @Autowired
  private RnrTemplateService rnrTemplateService;
  @Autowired
  private RestRequisitionCalculator restRequisitionCalculator;
  @Autowired
  private ProcessingPeriodService processingPeriodService;
  @Autowired
  private StaticReferenceDataService staticReferenceDataService;
  @Autowired
  private FacilityApprovedProductService facilityApprovedProductService;
  private List<FacilityTypeApprovedProduct> nonFullSupplyFacilityApprovedProductByFacilityAndProgram;
  @Autowired
  private SyncUpHashRepository syncUpHashRepository;
  @Autowired
  private RegimenService regimenService;
  @Autowired
  private RegimenLineItemMapper regimenLineItemMapper;
  @Autowired
  private ProcessingScheduleService processingScheduleService;

  @Transactional
  public Rnr submitReport(Report report, Long userId) {
    if (syncUpHashRepository.hashExists(report.getSyncUpHash())) {
      return null;
    }

    report.validate();

    Facility reportingFacility = facilityService.getOperativeFacilityByCode(report.getAgentCode());
    Program reportingProgram = programService.getValidatedProgramByCode(report.getProgramCode());

    validReportDate(report.getActualPeriodEndDate(), reportingFacility.getId(), reportingProgram.getId());

    if (staticReferenceDataService.getBoolean("toggle.skip.initial.requisition.validation")) {
      Rnr lastRegularRequisition = requisitionService.getLastRegularRequisitionByReportDate(reportingFacility, reportingProgram);
      if (lastRegularRequisition == null) {
        programSupportedService.updateProgramSupportedReportStartDate(reportingFacility.getId(), reportingProgram.getId(), getDateOf21(report.getActualPeriodStartDate()));
      }
    }

    if (!report.getProgramCode().equals(RAPID_TEST_PROGRAM_CODE)) {
      restRequisitionCalculator.validatePeriod(reportingFacility, reportingProgram, report.getActualPeriodStartDate(), report.getActualPeriodEndDate());
    }

    ProcessingPeriod proposedPeriod = null;
    if (!reportingFacility.getVirtualFacility()) {
       proposedPeriod =
          findReportPeriod(report.getActualPeriodStartDate(), report.getActualPeriodEndDate(), reportingFacility.getId(), reportingProgram.getId());
    }
    Rnr rnr = requisitionService.initiate(reportingFacility, reportingProgram, userId, EMERGENCY, proposedPeriod, report.getServiceLineItems());

    restRequisitionCalculator.validateProducts(report.getProducts(), rnr);

    markSkippedLineItems(rnr, report);

    if (reportingFacility.getVirtualFacility()) {
      restRequisitionCalculator.setDefaultValues(rnr);
    }

    if (staticReferenceDataService.getBoolean("toggle.mmia.custom.regimen")) {
      List<RegimenLineItem> regimenLineItems = checkAndInsertCustomRegimenItems(report, rnr, userId, reportingProgram.getId());
      regimenLineItems.addAll(0, rnr.getRegimenLineItems());
      rnr.setRegimenLineItems(regimenLineItems);
    }

    copyRegimens(rnr, report);

    requisitionService.save(rnr);

    updateClientFields(report, rnr);
    insertPatientQuantificationLineItems(report, rnr);
    insertTherapeuticLinesItem(report, rnr);

    insertRnrSignatures(report, rnr, userId);

    rnr.setProgramDataFormId(report.getProgramDataFormId());
    rnr = requisitionService.submit(rnr);

    Rnr authorize = requisitionService.authorize(rnr);

    syncUpHashRepository.save(report.getSyncUpHash());

    return authorize;
  }

  private void validReportDate(Date actualPeriodEndDate, Long facilityId, Long programId) {
    Date reportStartDate = programService.getReportStartDate(facilityId, programId);
    if (null == reportStartDate) {
      logger.error("facilityId {} programId {}, report support date is invalid", facilityId,
          programId);
      throw new DataException(MessageKeyUtils.RNR_REPORT_SUPPORTED_DATE_INVALID);
    }
    if (actualPeriodEndDate.before(reportStartDate)) {
      logger
          .error("facilityId {} programId {}, report start date is invalid", facilityId, programId);
      throw new DataException(MessageKeyUtils.RNR_REPORT_START_DATE_INVALID);
    }
  }

  public void notifySubmittedEvent(Rnr rnr) {
    requisitionService.logStatusChangeAndNotify(rnr, true, null);
  }

  private void updateClientFields(Report report, Rnr rnr) {
    Date clientSubmittedTime = report.getClientSubmittedTime();
    rnr.setClientSubmittedTime(clientSubmittedTime);

    String clientSubmittedNotes = report.getClientSubmittedNotes();
    rnr.setClientSubmittedNotes(clientSubmittedNotes);

    requisitionService.updateClientFields(rnr);

    if (staticReferenceDataService.getBoolean("toggle.sync.period.date.for.rnr")) {
      rnr.setActualPeriodStartDate(report.getActualPeriodStartDate());
      rnr.setActualPeriodEndDate(report.getActualPeriodEndDate());
      requisitionService.saveClientPeriod(rnr);
    }
  }

  @Transactional
  public Rnr submitSdpReport(Report report, Long userId) {
    if (syncUpHashRepository.hashExists(report.getSyncUpHash())) {
      return null;
    }

    report.validate();

    Facility reportingFacility = facilityService.getOperativeFacilityByCode(report.getAgentCode());
    Program reportingProgram = programService.getValidatedProgramByCode(report.getProgramCode());
    ProcessingPeriod period = processingPeriodService.getById(report.getPeriodId());


    Rnr rnr;
    List<Rnr> rnrs = null;

    RequisitionSearchCriteria searchCriteria = new RequisitionSearchCriteria();
    searchCriteria.setProgramId(reportingProgram.getId());
    searchCriteria.setFacilityId(reportingFacility.getId());
    searchCriteria.setWithoutLineItems(true);
    searchCriteria.setUserId(userId);

    if (report.getPeriodId() != null) {
      //check if the requisition has already been initiated / submitted / authorized.
      restRequisitionCalculator.validateCustomPeriod(reportingFacility, reportingProgram, period, userId);
      rnrs = requisitionService.getRequisitionsFor(searchCriteria, asList(period));
    }


    if (rnrs != null && rnrs.size() > 0) {
      rnr = requisitionService.getFullRequisitionById(rnrs.get(0).getId());

    } else {
      rnr = requisitionService.initiate(reportingFacility, reportingProgram, userId, report.getEmergency(), period, null);
    }

    List<RnrLineItem> fullSupplyProducts = new ArrayList<>();
    List<RnrLineItem> nonFullSupplyProducts = new ArrayList<>();
    Iterator<RnrLineItem> iterator = report.getProducts().iterator();
    nonFullSupplyFacilityApprovedProductByFacilityAndProgram = facilityApprovedProductService.getNonFullSupplyFacilityApprovedProductByFacilityAndProgram(reportingFacility.getId(), reportingProgram.getId());

    // differentiate between full supply and non full supply products
    while (iterator.hasNext()) {
      final RnrLineItem lineItem = iterator.next();

      //default to full supply products
      if (lineItem.getFullSupply() == null || lineItem.getFullSupply()) {
        fullSupplyProducts.add(lineItem);
      } else {

        setNonFullSupplyCreatorFields(lineItem);
        nonFullSupplyProducts.add(lineItem);
      }
    }
    report.setProducts(fullSupplyProducts);
    report.setNonFullSupplyProducts(nonFullSupplyProducts);
    restRequisitionCalculator.validateProducts(report.getProducts(), rnr);

    markSkippedLineItems(rnr, report);

    copyRegimens(rnr, report);
    // if you have come this far, then do it, it is your day. make the submission.
    // i cannot believe we do all of these three at the same time.
    // but then this is what zambia specifically asked.
    requisitionService.save(rnr);

    updateClientFields(report, rnr);
    insertPatientQuantificationLineItems(report, rnr);

    insertRnrSignatures(report, rnr, userId);

    rnr = requisitionService.submit(rnr);
    rnr = requisitionService.authorize(rnr);

    syncUpHashRepository.save(report.getSyncUpHash());

    return rnr;
  }

  private void setNonFullSupplyCreatorFields(final RnrLineItem lineItem) {

    FacilityTypeApprovedProduct p = (FacilityTypeApprovedProduct) find(nonFullSupplyFacilityApprovedProductByFacilityAndProgram, new Predicate() {
      @Override
      public boolean evaluate(Object product) {
        return ((FacilityTypeApprovedProduct) product).getProgramProduct().getProduct().getCode().equals(lineItem.getProductCode());
      }
    });
    if (p == null) {
      return;
    }
    lineItem.setDispensingUnit(p.getProgramProduct().getProduct().getDispensingUnit());
    lineItem.setMaxMonthsOfStock(p.getMaxMonthsOfStock());
    lineItem.setDosesPerMonth(p.getProgramProduct().getDosesPerMonth());
    lineItem.setDosesPerDispensingUnit(p.getProgramProduct().getProduct().getDosesPerDispensingUnit());
    lineItem.setPackSize(p.getProgramProduct().getProduct().getPackSize());
    lineItem.setRoundToZero(p.getProgramProduct().getProduct().getRoundToZero());
    lineItem.setPackRoundingThreshold(p.getProgramProduct().getProduct().getPackRoundingThreshold());
    lineItem.setPrice(p.getProgramProduct().getCurrentPrice());
  }


  private void copyRegimens(Rnr rnr, Report report) {
    if (report.getRegimens() != null) {
      for (RegimenLineItem regimenLineItem : report.getRegimens()) {
        RegimenLineItem correspondingRegimenLineItem = rnr.findCorrespondingRegimenLineItem(regimenLineItem);
        if (correspondingRegimenLineItem == null) {
          throw new DataException("error.invalid.regimen");
        }
        correspondingRegimenLineItem.populate(regimenLineItem);
      }
    }
  }

  private List<RegimenLineItem> checkAndInsertCustomRegimenItems(Report report, Rnr rnr, Long userId, Long programId) {
    List<RegimenLineItem> customRegimenItems = new ArrayList<>();
    if (report.getRegimens() != null) {
      for (RegimenLineItemForRest regimenLineItemForRest : report.getRegimens()) {
        RegimenCategory regimenCategory = regimenService.queryRegimenCategoryByName(regimenLineItemForRest.getCategoryName());
        regimenLineItemForRest.setCategory(regimenCategory);
        String versionCodeFromHeader = LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_VERSION_CODE);
        String code = versionCodeFromHeader == null || Integer.valueOf(versionCodeFromHeader) < 87  ? LmisThreadLocalUtils.STR_VERSION_86 : LmisThreadLocalUtils.STR_VERSION_87;
        Regimen regimen = null;
        if (MMIA_PROGRAM_CODE.equals(report.getProgramCode())) {
          regimen = regimenService.getRegimensByCategoryIdAndNameAndVersion(regimenCategory.getId(), regimenLineItemForRest.getName(), Long.valueOf(code));
        } else {
          regimen = regimenService.getRegimensByCategoryIdAndName(regimenCategory.getId(), regimenLineItemForRest.getName());
        }
        if (regimen == null) {
          String regimenCode = null != regimenLineItemForRest.getCode() ? regimenLineItemForRest.getCode() : String.format("%03d", regimenService.listAll().size() + 1);
          regimen = new Regimen(regimenLineItemForRest.getName(), regimenCode, programId, true, regimenCategory, regimenService.getRegimensByCategory(regimenCategory).size(), true, false, Long.valueOf(code));
          regimenService.save(regimen, userId);
        }

        if (regimen.isCustom()) {
          regimenLineItemForRest.setCode(regimen.getCode());
          RegimenLineItem regimenLineItem = new RegimenLineItem(rnr.getId(), regimenCategory, userId, userId);
          regimenLineItem.setCode(regimen.getCode());
          regimenLineItem.setName(regimen.getName());
          regimenLineItem.setRegimenDisplayOrder(regimen.getDisplayOrder());
          regimenLineItem.setSkipped(regimen.isSkipped());
          regimenLineItemMapper.insert(regimenLineItem);
          customRegimenItems.add(regimenLineItem);
        }
      }
    }
    return customRegimenItems;
  }

  private ProcessingPeriod findReportPeriod(Date actualPeriodStartDate, Date actualPeriodEndDate, Long facilityId, Long programId) {
    ProcessingPeriod processingPeriod = processingScheduleService.getPeriodByDate(actualPeriodStartDate, actualPeriodEndDate, facilityId, programId);
    if (null == processingPeriod) {
      throw new DataException("error.schedule.period.configuration.missing");
    }
    return processingPeriod;
  }

  private void insertPatientQuantificationLineItems(Report report, Rnr rnr) {
    if (report.getPatientQuantifications() != null) {
      rnr.setPatientQuantifications(report.getPatientQuantifications());
      requisitionService.insertPatientQuantificationLineItems(rnr);
    }
  }

  private void insertTherapeuticLinesItem(Report report, Rnr rnr) {
    if (report.getTherapeuticLines() != null) {
      rnr.setTherapeuticLines(report.getTherapeuticLines());
      requisitionService.insertTherapeuticLinesItem(rnr);
    }
  }

  private void insertRnrSignatures(Report report, Rnr rnr, final Long userId) {
    if (report.getRnrSignatures() != null) {

      List<Signature> rnrSignatures = new ArrayList(CollectionUtils.collect(report.getRnrSignatures(), new Transformer() {
        @Override
        public Object transform(Object input) {
          ((Signature) input).setCreatedBy(userId);
          ((Signature) input).setModifiedBy(userId);
          return input;
        }
      }));
      rnr.setRnrSignatures(rnrSignatures);
      requisitionService.insertRnrSignatures(rnr);
    }
  }

  @Transactional
  public void approve(Report report, Long requisitionId, Long userId) {
    Rnr requisition = report.getRequisition(requisitionId, userId);

    Rnr savedRequisition = requisitionService.getFullRequisitionById(requisition.getId());

    if (!savedRequisition.getFacility().getVirtualFacility()) {
      throw new DataException("error.approval.not.allowed");
    }

    if (savedRequisition.getNonSkippedLineItems().size() != report.getProducts().size()) {
      throw new DataException("error.number.of.line.items.mismatch");
    }

    restRequisitionCalculator.validateProducts(report.getProducts(), savedRequisition);

    requisitionService.save(requisition);
    requisitionService.approve(requisition, report.getApproverName());
  }

  public ReplenishmentDTO getReplenishmentDetails(Long id) {
    Rnr requisition = requisitionService.getFullRequisitionById(id);
    return prepareForREST(requisition, orderService.getOrder(id));
  }


  private void markSkippedLineItems(Rnr rnr, Report report) {

    ProgramRnrTemplate rnrTemplate = rnrTemplateService.fetchProgramTemplateForRequisition(rnr.getProgram().getId());

    List<RnrLineItem> savedLineItems = rnr.getFullSupplyLineItems();
    List<RnrLineItem> reportedProducts = report.getProducts();

    for (final RnrLineItem savedLineItem : savedLineItems) {
      RnrLineItem reportedLineItem = (RnrLineItem) find(reportedProducts, new Predicate() {
        @Override
        public boolean evaluate(Object product) {
          return ((RnrLineItem) product).getProductCode().equals(savedLineItem.getProductCode());
        }
      });

      copyInto(savedLineItem, reportedLineItem, rnrTemplate);
    }


    savedLineItems = rnr.getNonFullSupplyLineItems();
    reportedProducts = report.getNonFullSupplyProducts();
    if (reportedProducts != null) {
      for (final RnrLineItem reportedLineItem : reportedProducts) {
        RnrLineItem savedLineItem = (RnrLineItem) find(savedLineItems, new Predicate() {
          @Override
          public boolean evaluate(Object product) {
            return ((RnrLineItem) product).getProductCode().equals(reportedLineItem.getProductCode());
          }
        });
        if (savedLineItem == null && reportedLineItem != null) {
          rnr.getNonFullSupplyLineItems().add(reportedLineItem);
        } else {
          copyInto(savedLineItem, reportedLineItem, rnrTemplate);
        }
      }
    }

  }

  private void copyInto(RnrLineItem savedLineItem, RnrLineItem reportedLineItem, ProgramRnrTemplate rnrTemplate) {
    if (reportedLineItem == null) {
      savedLineItem.setSkipped(true);
      return;
    }
    savedLineItem.setServiceItems(reportedLineItem.getServiceItems());
    savedLineItem.setTotalServiceQuantity(reportedLineItem.getTotalServiceQuantity());

    for (Column column : rnrTemplate.getColumns()) {
      if (!column.getVisible() || !rnrTemplate.columnsUserInput(column.getName())) {
        continue;
      }
      try {
        Field field = RnrLineItem.class.getDeclaredField(column.getName());
        field.setAccessible(true);

        Object reportedValue = field.get(reportedLineItem);
        Object toBeSavedValue = (reportedValue != null ? reportedValue : field.get(savedLineItem));
        field.set(savedLineItem, toBeSavedValue);
      } catch (Exception e) {
        logger.error("could not copy field: " + column.getName());
      }
    }
  }

  public List<Report> getRequisitionsByFacility(String facilityCode, Date startDate) {
    Facility facility = facilityService.getFacilityByCode(facilityCode);
    if (facility == null) {
      throw new DataException("error.facility.unknown");
    }

    List<Rnr> rnrList = requisitionService.getRequisitionsByFacility(facility, startDate);

    return FluentIterable.from(rnrList).transform(new Function<Rnr, Report>() {
      @Override
      public Report apply(Rnr rnr) {
        return Report.prepareForREST(rnr);
      }
    }).toList();
  }

  private Date getDateOf21(Date date) {
    final int DAY_OF_PERIOD_START = 21;
    DateTime dateTime = new DateTime(date);
    return new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), DAY_OF_PERIOD_START, 0, 0).toDate();
  }
}
