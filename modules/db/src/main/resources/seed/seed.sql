-- This program is part of the OpenLMIS logistics management information system platform software.
-- Copyright © 2013 VillageReach
--
-- This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
--  
-- This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
-- You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
--

-- Generated by pg_dump -a -T *schema_version -T dosage_units* -T facility_operators* -T facility_types* -T geographic_levels* -T geographic_zones* -T product_forms* -T product_groups* -T programs -T programs_id_seq -T regimen_categories* --column-inserts open_lmis > modules/db/src/main/resources/seed/baseline_seed.sql
-- after seed task is finished
\i src/main/resources/seed/baseline_seed.sql
-- Generated by pg_dump -a -t dosage_units -t facility_operators -t facility_types -t geographic_levels -t geographic_zones -t product_forms -t product_groups -t programs -t regimen_categories --column-inserts open_lmis > modules/db/src/main/resources/seed/extra_seed.sql
-- after seed task is finished
\i src/main/resources/seed/extra_seed.sql
\i src/main/resources/seed/toggle_data.sql
