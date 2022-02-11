/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.restapi.controller;

import static org.openlmis.restapi.response.RestResponse.response;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import com.wordnik.swagger.annotations.Api;
import java.util.Date;
import lombok.NoArgsConstructor;
import org.openlmis.restapi.response.RestResponse;
import org.openlmis.restapi.service.RestEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This controller is responsible for handling API endpoint to create/approve a requisition.
 * A user of any external integrated system can submit a request to this endpoint triggering actions like create or approve.
 * The system responds with the requisition Number on success and specific error messages on failure.
 * It also acts as an end point to get requisition details.
 */

@Controller
@NoArgsConstructor
@Api(value = "Email", description = "Email sent", position = 6)
public class RestEmailController extends BaseController {

  public static final String EMAIL = "EMAIL";

  @Autowired
  private RestEmailService restEmailService;

  @RequestMapping(value = "/rest-api/email/resentByDate", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity<RestResponse> emailResentByDate(@RequestParam(value = "startDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final Date startDate,
      @RequestParam(value = "endDate") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") final Date endDate) {
    restEmailService.emailResentByDate(startDate,endDate);
    return response(EMAIL, 0L, OK);
  }

  @RequestMapping(value = "/rest-api/email/resentById/{failSentLogId}", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity<RestResponse> emailResentByDate(@PathVariable Long failSentLogId) {
    restEmailService.emailResentById(failSentLogId);
    return response(EMAIL, failSentLogId, OK);
  }
}
