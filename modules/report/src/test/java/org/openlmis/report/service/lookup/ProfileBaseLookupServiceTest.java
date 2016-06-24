package org.openlmis.report.service.lookup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.FacilityType;
import org.openlmis.core.domain.GeographicZone;
import org.openlmis.core.domain.User;
import org.openlmis.core.repository.FacilityRepository;
import org.openlmis.core.repository.UserRepository;
import org.openlmis.report.mapper.lookup.GeographicZoneReportMapper;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class ProfileBaseLookupServiceTest {

    @Mock
    private FacilityRepository facilityRepository;
    @Mock
    private GeographicZoneReportMapper zoneMapper;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileBaseLookupService profileBaseLookupService;

    private long parentZoneId = 123L;
    private long zoneId = 456L;

    @Before
    public void setUp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(userRepository.getById(anyLong())).thenReturn(new User());
    }

    @Test
    public void shouldGetOneProvinceAndAllDistrictsUnderItForDPMUser() throws Exception {
        //given
        Facility facility = createFacilityWithTypeCode("DPM");
        when(facilityRepository.getById(anyLong())).thenReturn(facility);

        //when
        profileBaseLookupService.getAllZones();

        //then
        verify(zoneMapper).getZoneAndChildren(parentZoneId);
    }

    @Test
    public void shouldGetOneDistrictAndItsParentProvinceForNonDPMUser() throws Exception {
        //given
        Facility facility = createFacilityWithTypeCode("whatever");
        when(facilityRepository.getById(anyLong())).thenReturn(facility);

        //when
        profileBaseLookupService.getAllZones();

        //then
        verify(zoneMapper).getZoneAndParent(zoneId);
    }

    private Facility createFacilityWithTypeCode(String facilityTypeCode) {
        GeographicZone parent = new GeographicZone();
        parent.setId(parentZoneId);

        GeographicZone geographicZone = new GeographicZone();
        geographicZone.setId(zoneId);
        geographicZone.setParent(parent);

        FacilityType facilityType = new FacilityType();
        facilityType.setCode(facilityTypeCode);

        Facility facility = new Facility();
        facility.setGeographicZone(geographicZone);
        facility.setFacilityType(facilityType);
        return facility;
    }
}