/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.core.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.databind.annotation.JsonSerialize.Inclusion.NON_EMPTY;

/**
 * Regimen contains information about Regimen for a given program.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(include = NON_EMPTY)
public class Regimen extends BaseModel {

  private String name;
  private String code;
  private Long programId;
  private Boolean active;
  private RegimenCategory category;
  private Integer displayOrder;
  private boolean isCustom;

  //if skipped is true, will not send this regimen to SIMMAN
  private boolean skipped;

  private Long versionCode;

  @JsonIgnore
  public int isEqualForFCRegimen(Regimen regimen) {
    if (regimen == null) return 0;
    if (!this.code.equals(regimen.code)) return -1;
    if (!this.toStringForEqual().equals(regimen.toStringForEqual())) {
      return 1;
    }
    return 0;
  }

  private String toStringForEqual() {
    return this.name + this.code + this.active + this.programId
            + this.category != null ? this.getId() + "" : "" + this.isCustom;
  }
}
