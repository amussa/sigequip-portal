package org.openlmis.rnr.service;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openlmis.core.domain.ConfigurationSetting;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.Program;
import org.openlmis.core.domain.User;
import org.openlmis.core.service.ApproverService;
import org.openlmis.core.service.ConfigurationSettingService;
import org.openlmis.core.service.StaticReferenceDataService;
import org.openlmis.db.categories.UnitTests;
import org.openlmis.email.domain.EmailFailSentLog;
import org.openlmis.email.service.EmailService;
import org.openlmis.rnr.domain.Rnr;
import org.openlmis.rnr.domain.RnrStatus;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.openlmis.rnr.builder.RequisitionBuilder.defaultRequisition;

import java.util.ArrayList;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Category(UnitTests.class)
@RunWith(MockitoJUnitRunner.class)
public class NotificationServicesTest {


  private NotificationServices notificationServices;

  @Mock
  private ConfigurationSettingService configService;

  @Mock
  private EmailService emailService;

  @Mock
  private ApproverService approverService;

  @Mock
  private RequisitionEmailServiceForSIMAM requisitionEmailServiceForSIMAM;

  @Mock
  private StaticReferenceDataService staticReferenceDataService;

  @Before
  public void setUp() throws Exception {
    notificationServices = new NotificationServices("emailBaseURL", configService, emailService, approverService, requisitionEmailServiceForSIMAM, staticReferenceDataService);
  }

  @Test
  public void shouldSendEmailWhenRnRStatusIsInApproval() throws Exception {


    Rnr rnr = make(a(defaultRequisition));
    rnr.setId(1L);
    rnr.setProgram(new Program(1L, "VIA_ESS", "VIA_ESS", "", false, false));
    rnr.setStatus(RnrStatus.AUTHORIZED);

    Facility facility = new Facility();
    facility.setName("abc");
    rnr.setFacility(facility);


    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setValue("abc");
    when(configService.getByKey(anyString())).thenReturn(setting);

    final User user1 = new User();
    final User user2 = new User();
    ArrayList<User> userList = new ArrayList<User>(){{
      add(user1); add(user2);
    }};
    when(approverService.getNextApprovers(1L)).thenReturn(userList);
    when(staticReferenceDataService.getBoolean("toggle.email.attachment.simam")).thenReturn(true);

    notificationServices.notifyStatusChange(rnr);

    verify(requisitionEmailServiceForSIMAM).queueRequisitionEmailWithAttachment(rnr, userList);
  }

  @Test
  public void shouldNotSendEmailWhenToggleOff(){

    Rnr rnr = make(a(defaultRequisition));
    rnr.setId(1L);
    rnr.setProgram(new Program(1L, "VIA_ESS", "VIA_ESS", "", false, false));
    rnr.setStatus(RnrStatus.AUTHORIZED);

    Facility facility = new Facility();
    facility.setName("abc");
    rnr.setFacility(facility);


    ConfigurationSetting setting = new ConfigurationSetting();
    setting.setValue("abc");
    when(configService.getByKey(anyString())).thenReturn(setting);

    final User user1 = new User();
    final User user2 = new User();
    ArrayList<User> userList = new ArrayList<User>(){{
      add(user1); add(user2);
    }};
    when(approverService.getNextApprovers(1L)).thenReturn(userList);
    when(staticReferenceDataService.getBoolean("toggle.email.attachment.simam")).thenReturn(false);

    notificationServices.notifyStatusChange(rnr);

    verify(requisitionEmailServiceForSIMAM,never()).queueRequisitionEmailWithAttachment(rnr, userList);
  }

//  @Test
//  public void shouldInsertEmailFailLogsWhenSuperiorUserIsNull() {
//    // given
//    Rnr rnr = make(a(defaultRequisition));
//    rnr.setId(1L);
//    rnr.setStatus(RnrStatus.AUTHORIZED);
//
//    EmailFailSentLog emailFailSentLog = new EmailFailSentLog();
//    emailFailSentLog.setRequisitionId(rnr.getId());
//    emailFailSentLog.setErrorMsg("User is null");
//
//    when(approverService.getNextApprovers(1L)).thenReturn(Collections.emptyList());
//    when(staticReferenceDataService.getBoolean("toggle.email.attachment.simam")).thenReturn(true);
//
//    // when
//    notificationServices.notifyStatusChange(rnr);
//
//    //then
//    verify(emailService).insertEmailFailSentLog(emailFailSentLog);
//  }

  @Test
  public void shouldInsertEmailFailLogsWhenQueueEmailWithAttachmentThrowException() {
    // given
    Rnr rnr = make(a(defaultRequisition));
    rnr.setId(1L);
    rnr.setStatus(RnrStatus.AUTHORIZED);

    final User user1 = new User();
    final User user2 = new User();
    ArrayList<User> userList = new ArrayList<User>(){{
      add(user1); add(user2);
    }};

    EmailFailSentLog emailFailSentLog = new EmailFailSentLog();
    emailFailSentLog.setRequisitionId(rnr.getId());
    emailFailSentLog.setErrorMsg("User is null");

    when(approverService.getNextApprovers(1L)).thenReturn(userList);
    when(staticReferenceDataService.getBoolean("toggle.email.attachment.simam")).thenReturn(true);
    doThrow(new NullPointerException()).when(requisitionEmailServiceForSIMAM).queueRequisitionEmailWithAttachment(rnr,userList);

    try {
      // when
      notificationServices.notifyStatusChange(rnr);
    } catch (Exception e) {
      //then
      verify(emailService).insertEmailFailSentLog(emailFailSentLog);
    }
  }
}
