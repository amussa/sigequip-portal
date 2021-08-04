package org.openlmis.restapi.service;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.openlmis.LmisThreadLocalUtils;
import org.openlmis.core.domain.KitProduct;
import org.openlmis.core.domain.Product;
import org.openlmis.core.domain.ProgramProduct;
import org.openlmis.core.domain.ProgramSupported;
import org.openlmis.core.service.*;
import org.openlmis.restapi.domain.ProductResponse;
import org.openlmis.restapi.domain.ProgramProductResponse;
import org.openlmis.restapi.utils.KitProductFilterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.openlmis.restapi.utils.KitProductFilterUtils.*;

@Service
public class RestProductService {

  @Autowired
  ArchivedProductService archivedProductService;
  @Autowired
  StaticReferenceDataService staticReferenceDataService;
  @Autowired
  private ProductService productService;
  @Autowired
  private ProgramProductService programProductSevice;
  @Autowired
  private UserService userService;
  @Autowired
  private ProgramSupportedService programSupportedService;

  @Transactional
  public Product buildAndSave(Product product) {
    productService.save(buildProduct(product), true);
    return product;
  }

  private Product buildProduct(Product product) {
    product.setDefaultValuesForMandatoryFieldsIfNotExist();

    if (product.getKitProductList() != null) {
      for (KitProduct kitProduct : product.getKitProductList()) {
        kitProduct.setKitCode(product.getCode());
      }
    }
    return product;
  }

  public List<ProductResponse> getLatestProductsAfterUpdatedTime(Date afterUpdatedTime, String versionCode, Long userId) {
    Long facilityId = userService.getById(userId).getFacilityId();

    List<Product> latestProducts = getLatestProducts(afterUpdatedTime, versionCode, facilityId);

    List<ProgramProduct> latestProgramProduct = programProductSevice.getLatestUpdatedProgramProduct(afterUpdatedTime);
    for (ProgramProduct programProduct : latestProgramProduct) {
      if (!isContainedInLatestProduct(latestProducts, programProduct)) {
        latestProducts.add(programProduct.getProduct());
      }
    }

    List<String> allSupportedPrograms = getSupportedProgramsByFacility(facilityId);

    return prepareProductsBasedOnFacilitySupportedPrograms(latestProducts, allSupportedPrograms);
  }

  public List<ProductResponse> getTemp86KitChangeProducts(Long userId) {
    Long facilityId = userService.getById(userId).getFacilityId();
    List<Product> kitChangesProducts = getKitChangeProducts();
    List<String> allSupportedPrograms = getSupportedProgramsByFacility(facilityId);

    return prepareProductsBasedOnFacilitySupportedPrograms(kitChangesProducts, allSupportedPrograms);
  }

  private List<Product> getKitChangeProducts() {
    List<Product> lists = new ArrayList<>();
    for (String productCode : ALL_FILTER_KIT_PRODUCTS_SET) {
      Product product = productService.getProductByCode(productCode);
      if (product != null) {
        lists.add(product);
      }
    }
    return filterProductFromGetProductsAfterUpdatedDate(lists);
  }

  private boolean isContainedInLatestProduct(List<Product> latestProducts, ProgramProduct programProduct) {
    boolean isContainedInLatestProduct = false;
    for (Product latestProduct : latestProducts) {
      if (latestProduct.getCode().equals(programProduct.getProduct().getCode()))
        isContainedInLatestProduct = true;
    }
    return isContainedInLatestProduct;
  }

  private List<Product> getLatestProducts(Date afterUpdatedTime, String versionCode,
      Long facilityId) {
    if (afterUpdatedTime == null) {
      final List<String> archivedProductCodes = archivedProductService
          .getAllArchivedProducts(facilityId);

      return FluentIterable.from(productService.getAllProducts())
          .transform(new Function<Product, Product>() {
            @Override
            public Product apply(Product product) {
              product.setArchived(archivedProductCodes.contains(product.getCode()));
              return product;
            }
          }).toList();
    } else {
      List<Product> products = productService.getProductsAfterUpdatedDate(afterUpdatedTime);
      if (KitProductFilterUtils
          .isBiggerThanThresholdVersion(versionCode, KIT_CODE_CHANGE_VERSION)) {
        return filterProductFromGetProductsAfterUpdatedDate(products);
      }
      return products;
    }
  }

  private List<Product> filterProductFromGetProductsAfterUpdatedDate(List<Product> products) {
    return new ArrayList<Product>(FluentIterable.from(products).transform(new Function<Product, Product>() {
      @Override
      public Product apply(Product product) {
        if (WRONG_KIT_PRODUCTS_SET.contains(product.getCode())) {
          product.setArchived(false);
          product.setIsKit(false);
        }
        if (RIGHT_KIT_PRODUCTS_SET.contains(product.getCode())) {
          product.setIsKit(true);
        }
        return product;
      }
    }).toList());
  }

  private List<String> getSupportedProgramsByFacility(Long facilityId) {
    List<ProgramSupported> programSupportedList = programSupportedService.getAllByFacilityId(facilityId);
    return FluentIterable.from(programSupportedList).transform(new Function<ProgramSupported, String>() {
      @Override
      public String apply(ProgramSupported programSupported) {
        return programSupported.getProgram().getCode();
      }
    }).toList();
  }

  private List<ProductResponse> prepareProductsBasedOnFacilitySupportedPrograms(List<Product> latestProducts, final List<String> programs) {
    List<ProductResponse> productResponseList = new ArrayList<>();
    String versionCodeFromHeader = LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_VERSION_CODE);
    final String versioncode = versionCodeFromHeader == null || Integer.valueOf(versionCodeFromHeader) < 87  ? LmisThreadLocalUtils.STR_VERSION_86 : LmisThreadLocalUtils.STR_VERSION_87;
    for (final Product product : latestProducts) {
      List<ProgramProduct> productPrograms = programProductSevice.getByProductCode(product.getCode());
      List<ProgramProductResponse> programsResponse = FluentIterable.from(productPrograms).filter(new Predicate<ProgramProduct>() {
        @Override
        public boolean apply(ProgramProduct programProduct) {
          return programs.contains(programProduct.getProgram().getCode());
        }
      }).filter(new Predicate<ProgramProduct>() {
        @Override
        public boolean apply(ProgramProduct programProduct) {
          return (!"MMIA".equals(programProduct.getProgram().getCode()) ||
                  versioncode.equals(String.valueOf(programProduct.getVersionCode())));
        }
      }).transform(new Function<ProgramProduct, ProgramProductResponse>() {
        @Override
        public ProgramProductResponse apply(ProgramProduct programProduct) {
          return new ProgramProductResponse(programProduct.getProgram().getCode(), product.getCode(),
                  programProduct.getActive(), programProduct.getProductCategory().getName(),programProduct.getVersionCode());
        }
      }).toList();

      if (!programsResponse.isEmpty() || versioncode.equals(LmisThreadLocalUtils.getHeader(LmisThreadLocalUtils.HEADER_VERSION_CODE))) {
        productResponseList.add(new ProductResponse(product, programsResponse));
      }
    }
    return productResponseList;
  }

}
