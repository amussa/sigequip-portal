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

import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openlmis.core.domain.*;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.repository.ProductGroupRepository;
import org.openlmis.core.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Exposes the services for handling Product entity.
 */

@Service
@NoArgsConstructor
public class ProductService {

  @Autowired
  private ProductRepository repository;

  @Autowired
  private ProductGroupRepository productGroupRepository;

  @Autowired
  private ProgramProductService programProductService;

  @Autowired
  private ProductGroupService productGroupService;

  @Autowired
  private ProgramService programService;

  @Autowired
  private ProductFormService productFormService;


  protected static Logger logger = LoggerFactory.getLogger(ProductService.class);

  @Transactional
  public void save(Product product, boolean updateKitProductList) {

    product.validate();

    ProductGroup productGroup = productGroupService.validateAndReturn(product.getProductGroup());
    product.setProductGroup(productGroup);

    ProductForm productForm = productFormService.validateAndReturn(product.getForm());
    product.setForm(productForm);

    DosageUnit dosageUnit = validateAndReturnDosageUnit(product.getDosageUnit());
    product.setDosageUnit(dosageUnit);


    if (product.getId() == null) {
      repository.insert(product);
      List<KitProduct> oldKitProductList = new ArrayList<>();
      repository.updateLastModifieddate(oldKitProductList,product.getKitProductList());
      return;
    }

    validateKitProductParam(product);
    repository.update(product, updateKitProductList);
    List<ProgramProduct> existingProgramProducts = programProductService.getByProductCode(product.getCode());
    notifyProgramCatalogChange(product, existingProgramProducts);
  }

  private DosageUnit validateAndReturnDosageUnit(DosageUnit dosageUnit) {
    if (dosageUnit == null) return null;

    String dosageUnitCode = dosageUnit.getCode();
    if (dosageUnitCode == null || dosageUnitCode.isEmpty()) return null;

    dosageUnit = repository.getDosageUnitByCode(dosageUnitCode);
    if (dosageUnit == null)
      throw new DataException("error.reference.data.invalid.dosage.unit");

    return dosageUnit;
  }

  private void notifyProgramCatalogChange(Product product, List<ProgramProduct> existingProgramProducts) {
    for (ProgramProduct existingProgramProduct : existingProgramProducts) {
      if (existingProgramProduct.getActive() && (existingProgramProduct.getProduct().getActive() != product.getActive())) {
        programService.setFeedSendFlag(existingProgramProduct.getProgram(), true);
      }
    }
  }

  private void validateKitProductParam(Product product){
    List<KitProduct> kitProductList =  product.getKitProductList();
    if(CollectionUtils.isEmpty(kitProductList)){
      return;
    }

    for (KitProduct kitProduct : kitProductList) {
      if(StringUtils.isBlank(kitProduct.getKitCode()) || StringUtils.isBlank(kitProduct.getProductCode())
              || !product.getCode().equals(kitProduct.getProductCode())){
        throw new DataException("error.reference.data.invalid.kitProduct.code");
      }

      if(!(null != kitProduct.getQuantity() && kitProduct.getQuantity().intValue() > 0)){
        throw new DataException("error.reference.data.invalid.kitProduct.quantity");
      }
    }
  }

  public Long getIdForCode(String code) {
    return repository.getIdByCode(code);
  }

  public Product getByCode(String code) {
    return repository.getByCode(code);
  }

  public boolean isActive(String code) {
    return repository.isActive(code);
  }

  public Integer getTotalSearchResultCount(String searchParam) {
    return repository.getTotalSearchResultCount(searchParam);
  }

  public List<DosageUnit> getAllDosageUnits() {
    return repository.getAllDosageUnits();
  }

  public Product getById(Long id) {
    return repository.getById(id);
  }

  public Product getExisting(Product product) {
        return repository.getByCode(product.getCode());
    }

  public List<Product> getAllProducts(){
    return repository.getAllProducts();
  }

  public List<Product> getProductsAfterUpdatedDate(Date afterUpdatedTime) {
    return repository.getProductsAfterUpdatedTime(afterUpdatedTime);
  }

  public Product getProductByCode(String productCode){
    return repository.getProductByCode(productCode);
  }


  public List<Product> getProductsForUpdateStatus(){
    return repository.getProductCodeForUpdateStatus();
  }

  public void updateProductStatus(boolean active, long id){
    repository.updateProductStatus(active, id);
  }

  public List<KitProduct> getKitProductsByProductCode(String productCode){
    return repository.getKitProductsByProductCode(productCode);
  }
}
