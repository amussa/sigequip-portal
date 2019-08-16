function NosDrugsReportController($scope, $controller, NosDrugsChartService) {
  $controller('BaseProductReportController', {$scope: $scope});
  $scope.reportLoaded = false;
  $scope.selectedDrugCode = '';
  $scope.buttonDisplay = false;
  $scope.exportAllbtnDisplay = false;

  init();

  function getReportLoaded() {
    $scope.reportLoaded = true;
    NosDrugsChartService.getNosDrugItemsPromise(getSelectedProvince(), getSelectedDistrict(),
      $scope.reportParams.startTime, $scope.reportParams.endTime, $scope.selectedDrugCode, "nosDrug")
      .$promise.then(function (result) {
      $scope.buttonDisplay = result.data.length > 0;
      $scope.exportAllbtnDisplay = result.data.length > 0;
      NosDrugsChartService.makeNosDrugHistogram('tracer-report', result.data);
    });
  }

  $scope.loadReport = function () {
    if ($scope.validateProvince() && $scope.validateDistrict()) {
      getReportLoaded();
    }
  };

  $scope.exportXLSX = function () {
    NosDrugsChartService.exportXLSX($scope.reportParams.startTime, $scope.reportParams.endTime, getSelectedProvince(), getSelectedDistrict(), "nosDrug");
  };



  $scope.exportXLSXAll = function () {
    console.log("click Export All");
    // NosDrugsChartService.exportXLSX($scope.reportParams.startTime, $scope.reportParams.endTime, getSelectedProvince(), getSelectedDistrict(), "nosDrug");
  };

  $scope.exportXLSXAll = function () {
    console.log("click Export All");
    // NosDrugsChartService.exportXLSX($scope.reportParams.startTime, $scope.reportParams.endTime, getSelectedProvince(), getSelectedDistrict(), "nosDrug");
  };

  $scope.onSelectCodeToExport = function () {
    console.log("onSelectCodeToExport");
  };

  function init() {
    var nosDrugListPromis = NosDrugsChartService.getNosDrugList();

    nosDrugListPromis.then(function (nosDrugListResult) {
      $scope.nosDrugList = nosDrugListResult.data;
      $scope.selectedDrugCode = nosDrugListResult.data[0]['drug.drug_code'];
    });
  }

  function getSelectedProvince() {
    return $scope.getGeographicZoneById($scope.provinces, $scope.reportParams.provinceId);
  }

  function getSelectedDistrict() {
    return $scope.getGeographicZoneById($scope.districts, $scope.reportParams.districtId);
  }
}