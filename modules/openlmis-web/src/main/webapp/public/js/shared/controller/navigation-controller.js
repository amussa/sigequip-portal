/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

function NavigationController($scope, ConfigSettingsByKey, localStorageService, Locales, $location, $window, FeatureToggleService, AppPropertiesService, HomeFacilityService) {

  ConfigSettingsByKey.get({key: 'LOGIN_SUCCESS_DEFAULT_LANDING_PAGE'}, function (data){
    $scope.homePage =  data.settings.value;
  });

  var viewPropertiesKey = {key: "app.environment"};
  AppPropertiesService.get(viewPropertiesKey, function (result) {
    $scope.env=result.key;
  });

   $scope.loadToggle = function () {
    var updateProductToggleKey = {key: 'update.product.view'};
    FeatureToggleService.get(updateProductToggleKey, function (result) {
      $scope.isUpdateProductsToggleOn = result.key;
    });
    var stockOutReportToggleKey = {key: 'stock.out.report'};
    FeatureToggleService.get(stockOutReportToggleKey, function (result) {
      $scope.isNewStockReportToggleOn = result.key;
    });
    FeatureToggleService.get({key: 'expiry.dates.report'}, function (result) {
      $scope.isExpiryDatesToggleOn = result.key;
    });
    FeatureToggleService.get({key: 'tracer.drugs.report'}, function (result) {
      $scope.isTracerDrugsReportToggleOn = result.key;
    });
    FeatureToggleService.get({key: 'nos.drugs.report'}, function (result) {
      $scope.isNosDrugsReportToggleOn = result.key;
    });
    FeatureToggleService.get({key: 'consumption.movements.report'}, function (result) {
     $scope.isConsumptionMovementsReportOn = result.key;
    });
    FeatureToggleService.get({key: 'adjustment.occurrences.report'}, function (result) {
     $scope.isAdjustmentOccurrencesReportOn = result.key;
    });
    FeatureToggleService.get({key: 'last.sync.time.report'}, function (result) {
     $scope.isLastSyncTimeReportOn = result.key;
    });
    FeatureToggleService.get({key: 'lot.expiry.dates.report'}, function (result) {
      $scope.isLotExpiryDatesToggleOn = result.key;
    });
    FeatureToggleService.get({key: 'rapid.test.report'}, function (result) {
      $scope.isRapidTestReportOn = result.key;
    });
   }();

  $scope.loadRights = function () {
    $scope.rights = localStorageService.get(localStorageKeys.RIGHT);

    $(".navigation > ul").show();
  }();

  $scope.showSubmenu = function () {
    $(".navigation li:not(.navgroup)").on("click", function () {
      $(this).children("ul").show();
    });
  }();

  $scope.hasReportingPermission = function () {
    if ($scope.rights !== undefined && $scope.rights !== null) {
      var rights = JSON.parse($scope.rights);
      var rightTypes = _.pluck(rights, 'type');
      return rightTypes.indexOf('REPORTING') > -1;
    }
    return false;
  };

  $scope.homeLinkClicked=function(){
    $window.location.href= $scope.homePage;
  };

  $scope.hasPermission = function (permission) {
    if ($scope.rights !== undefined && $scope.rights !== null) {
      var rights = JSON.parse($scope.rights);
      var rightNames = _.pluck(rights, 'name');
      return rightNames.indexOf(permission) > -1;
    }
    return false;
  };

  $scope.goOnline = function () {
    Locales.get({}, function (data) {
      if (data.locales) {
        var currentURI = $location.absUrl();
        if (currentURI.endsWith('offline.html')) {
          $window.location = currentURI.replace('public/pages/offline.html', '');
        }
        else {
          $window.location = currentURI.replace('offline.html', 'index.html').replace('#/list', '#/manage');
        }
        $scope.showNetworkError = false;
        return;
      }
      $scope.showNetworkError = true;
    }, {});
  };

  $scope.loadHomeFacility = function() {
    HomeFacilityService.get({}, function (data) {
          if (!data['home-facility']) {
            $scope.canViewUpdateReport = false;
          } else if (data['home-facility'].facilityType.code === 'CSRUR-I' ||
              data['home-facility'].facilityType.code === 'CSRUR-II' ) {
            $scope.canViewUpdateReport = false;
          } else {
            $scope.canViewUpdateReport = true;
          }
        }
    );
  }();
}
