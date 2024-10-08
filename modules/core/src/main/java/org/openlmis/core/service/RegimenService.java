/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.core.service;

import org.openlmis.core.domain.*;
import org.openlmis.core.repository.RegimenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Exposes the services for handling Regimen entity.
 */

@Service
public class RegimenService {

  @Autowired
  RegimenRepository repository;

  @Autowired
  ProgramService programService;

  public void save(List<Regimen> regimens, Long userId) {
    repository.save(regimens, userId);
  }

  public void save(Regimen regimen, Long userId) {
    repository.save(regimen, userId);
  }

  public List<Regimen> getByProgram(Long programId) {
    return repository.getByProgram(programId);
  }

  public List<RegimenCategory> getAllRegimenCategories() {
    return repository.getAllRegimenCategories();
  }

  public Regimen getById(Long id){return repository.getById(id);}

  public List getRegimensByCategory(RegimenCategory category) {
    return repository.getRegimensByCategory(category);
  }

  public RegimenCategory queryRegimenCategoryByName(String name) {
    return repository.getRegimenCategoryByName(name);
  }

  public Regimen getRegimensByCategoryIdAndName(Long categoryId, String name) {
    return repository.getRegimensByCategoryIdAndName(categoryId, name);
  }
  public Regimen getRegimensByCategoryIdAndNameAndVersion(Long categoryId, String name, Long versionCode) {
    return repository.getRegimensByCategoryIdAndNameAndVersion(categoryId, name,versionCode);
  }

  public List<Regimen> listAll() {
     return repository.getAllRegimens();
  }

  public List<Regimen> getRegimensByProgramAndIsCustom(Long programId, boolean isCustom) {
    return repository.getRegimensByProgramAndIsCustom(programId, isCustom);
  }
}
