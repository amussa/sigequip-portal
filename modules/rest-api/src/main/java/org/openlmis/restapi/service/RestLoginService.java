package org.openlmis.restapi.service;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.LmisThreadLocalUtils;
import org.openlmis.authentication.domain.UserToken;
import org.openlmis.authentication.service.UserAuthenticationService;
import org.openlmis.core.domain.ProgramSupported;
import org.openlmis.core.domain.User;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.service.FacilityService;
import org.openlmis.core.service.MessageService;
import org.openlmis.core.service.ProgramSupportedService;
import org.openlmis.core.service.UserService;
import org.openlmis.report.model.dto.AppInfo;
import org.openlmis.restapi.domain.FacilitySupportedProgram;
import org.openlmis.restapi.domain.LoginInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestLoginService {

    MessageService messageService = MessageService.getRequestInstance();
    @Autowired
    private UserAuthenticationService userAuthenticationService;
    @Autowired
    private UserService userService;
    @Autowired
    private FacilityService facilityService;
    @Autowired
    private ProgramSupportedService programSupportedService;
    @Autowired
    private RestAppInfoService restAppInfoService;

    public LoginInformation login(String username, String password) {
        authenticateUser(username, password);
        LoginInformation loginInformation = getLoginInformation(username);
        String uniqueId = LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_UNIQUE_ID);
        AppInfo appInfo = restAppInfoService.searchAppInfoByFacilityId(loginInformation.getFacilityId());
        if(appInfo != null && !StringUtils.equals(uniqueId, appInfo.getUniqueId())){
            throw new DataException("One facility can only sign in on one device. Your facility has signed in on another device.");
        }
        return loginInformation;
    }

    private UserToken authenticateUser(String username, String password) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        String userName = (String) authenticationToken.getPrincipal();
        String pass = (String) authenticationToken.getCredentials();

        User user = new User();
        user.setUserName(userName);
        user.setPassword(pass);

        UserToken userToken = userAuthenticationService.authenticateUser(user);

        if (userToken.isAuthenticated()) {
            return userToken;
        } else {
            throw new BadCredentialsException(messageService.message("error.authentication.failed"));
        }
    }

    private LoginInformation getLoginInformation(String username) {
        User user = userService.getByUserName(username);
        if (!user.isMobileUser()) {
            throw new DataException("error.invalid.mobile.user");
        }
        Long facilityId = user.getFacilityId();
        List<String> programs = FluentIterable.from(getProgramsSupportedByFacilityId(facilityId)).transform(new Function<ProgramSupported, String>() {
            @Override
            public String apply(ProgramSupported input) {
                return input.getProgram().getCode();
            }
        }).toList();
        return LoginInformation.prepareForREST(user, facilityService.getById(facilityId), programs);
    }

    private List<ProgramSupported> getProgramsSupportedByFacilityId(Long facilityId) {
        if (facilityId != null) {
            return programSupportedService.getActiveByFacilityId(facilityId);
        } else {
            throw new DataException("error.facility.unknown");
        }
    }

    public List<FacilitySupportedProgram> getFacilitySupportedPrograms(Long facilityId) {
        return FluentIterable.from(getProgramsSupportedByFacilityId(facilityId)).transform(new Function<ProgramSupported, FacilitySupportedProgram>() {
            @Override
            public FacilitySupportedProgram apply(ProgramSupported input) {
                FacilitySupportedProgram program = new FacilitySupportedProgram();
                program.setProgramCode(input.getProgram().getCode());
                program.setProgramName(input.getProgram().getName());
                program.setIsSupportEmergency(input.getProgram().getIsSupportEmergency());
                if (input.getProgram().getParent() != null) {
                    program.setParentCode(input.getProgram().getParent().getCode());
                }
                return program;
            }
        }).toList();
    }
}
