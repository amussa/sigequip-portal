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

import java.util.Date;
import java.util.List;
import lombok.NoArgsConstructor;
import org.openlmis.email.domain.EmailFailSentLog;
import org.openlmis.email.domain.EmailFailSentType;
import org.openlmis.email.service.EmailService;
import org.openlmis.rnr.domain.Rnr;
import org.openlmis.rnr.service.RequisitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service exposes methods for creating, approving a requisition.
 */

@Service
@NoArgsConstructor
public class RestEmailService {
  @Autowired
  private EmailService emailService;

  @Autowired
  private RequisitionService requisitionService;

  @Transactional
  public void emailResentByDate(Date startDate,Date endDate){
    List<EmailFailSentLog> emailFailSentLogList = emailService.queryEmailFailSentLogByCreatedDate(startDate,endDate);
    if(emailFailSentLogList.isEmpty()){
      return;
    }
    for(EmailFailSentLog emailFailSentLog : emailFailSentLogList){
      handleEmailFailSentLog(emailFailSentLog);
    }
  }

  public void emailResentById(Long failSentLogId){
    EmailFailSentLog emailFailSentLog = emailService.queryEmailFailSentLogById(failSentLogId);
    handleEmailFailSentLog(emailFailSentLog);
  }

  private void handleEmailFailSentLog(EmailFailSentLog emailFailSentLog){
    if(emailFailSentLog == null){
      return;
    }
    if(emailFailSentLog.getType() == EmailFailSentType.SENT_FAIL){
      emailService.updateEmailNotificationsSentFalse(emailFailSentLog.getEmailId());
    }else{
      Rnr rnr = requisitionService.getByIdWhetherExists(emailFailSentLog.getRequisitionId());
      requisitionService.emailResentNotify(rnr);
    }
    emailService.updateEmailSentFailLogManualSent(emailFailSentLog.getId());
  }

}
