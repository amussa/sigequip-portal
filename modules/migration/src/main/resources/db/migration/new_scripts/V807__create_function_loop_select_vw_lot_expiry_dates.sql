create function loop_select_vw_lot_expiry_dates(occured bigint, search_province_id integer, search_district_id integer, search_facility_id integer) returns SETOF vw_lot_expiry_dates
    language plpgsql
as
$$
DECLARE
facilityId int;
    lotId      int;
    querySql   VARCHAR;
begin
    querySql := 'select vledfl.* from vw_lot_expiry_dates_facilityId_lotId vledfl
       left join facilities on facilities.id = vledfl.facility_id
       left join geographic_zones zone ON facilities.geographiczoneid = zone.id
       left join geographic_zones parent_zone ON zone.parentid = parent_zone.id ';
    if (search_province_id is not null) and (search_district_id is not null) and (search_facility_id is not null)
    then
        querySql := 'select vledfl.* from vw_lot_expiry_dates_facilityId_lotId vledfl
       left join facilities on facilities.id = vledfl.facility_id
       left join geographic_zones zone ON facilities.geographiczoneid = zone.id
       left join geographic_zones parent_zone ON zone.parentid = parent_zone.id
       where parent_zone.id =' || search_province_id || ' and zone.id = ' ||
                    search_district_id || ' and facilities.id = ' || search_facility_id || ' ';

    elseif (search_province_id is not null) and (search_district_id is not null)
    then
        querySql := 'select vledfl.* from vw_lot_expiry_dates_facilityId_lotId vledfl
       left join facilities on facilities.id = vledfl.facility_id
       left join geographic_zones zone ON facilities.geographiczoneid = zone.id
       left join geographic_zones parent_zone ON zone.parentid = parent_zone.id
       where parent_zone.id =' || search_province_id || ' and zone.id = ' ||
                    search_district_id || ' ';

    elseif (search_province_id is not null)
    then
        querySql := 'select vledfl.* from vw_lot_expiry_dates_facilityId_lotId vledfl
       left join facilities on facilities.id = vledfl.facility_id
       left join geographic_zones zone ON facilities.geographiczoneid = zone.id
       left join geographic_zones parent_zone ON zone.parentid = parent_zone.id
       where parent_zone.id =' || search_province_id || ' ';

else
        querySql := 'select * from vw_lot_expiry_dates_facilityId_lotId';
end if;


for facilityId,lotId IN EXECUTE querySql
        loop
            return query select *
                         from vw_lot_expiry_dates
                         where facility_id = facilityId
                           and lot_id = lotId
                           and occurred <= occured
                         order by occurred desc
                             limit 1;
end loop;
    return;

END;
$$;

alter function loop_select_vw_lot_expiry_dates(bigint, integer, integer, integer) owner to postgres;

