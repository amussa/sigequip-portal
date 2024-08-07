function LotExpiryDatesReportController($scope, $controller, $http, $q, CubesGenerateUrlService, messageService, CubesGenerateCutParamsService, $routeParams, DateFormatService, ReportExportExcelService, EntryLotOnHands) {
  $controller('BaseProductReportController', {$scope: $scope});

  $scope.populateOptions = function () {
    if(!_.isEmpty($routeParams)) {
      $scope.reportParams.endTime = $routeParams.date;
      if($routeParams.facilityId) {
        $scope.reportParams.facilityId = $routeParams.facilityId;
      }
      if($routeParams.districtId) {
        $scope.reportParams.districtId = $routeParams.districtId;
      }
      if($routeParams.provinceId) {
        $scope.reportParams.provinceId = $routeParams.provinceId;
      }
      $scope.cursor = $routeParams.drug_code;
      $scope.loadReport();
    }
  };

  $scope.$on('$viewContentLoaded', function () {
    $scope.loadProducts();
    $scope.loadHealthFacilities();
  });

  $scope.loadReport = function () {
    if ($scope.validateFacility()) {
      generateReportTitle();
      queryLotExpiryDatesReportDataFromCubes();
    }
  };

  $scope.highlightDate = function (date) {
    var selectedDate = new Date($scope.reportParams.endTime);
    var threeMonthsFromOccurred = new Date(selectedDate.getFullYear(), selectedDate.getMonth() + 3, selectedDate.getDate());
    return date <= threeMonthsFromOccurred;
  };

  function queryLotExpiryDatesReportDataFromCubes() {
    var selectedTime = new Date($scope.reportParams.endTime).setHours(23, 59, 59, 999);
    var provinceId = null;
    if ($scope.expiryDatesReportParams.selectedProvince) {
      provinceId = $scope.expiryDatesReportParams.selectedProvince.id;
    }
    var districtId = null;
    if ($scope.expiryDatesReportParams.selectedDistrict) {
      districtId = $scope.expiryDatesReportParams.selectedDistrict.id;
    }
    var facilityId = null;
    if ($scope.expiryDatesReportParams.selectedFacility) {
      facilityId = $scope.expiryDatesReportParams.selectedFacility.id;
    }
    EntryLotOnHands.get({occurred:selectedTime,"provinceId":provinceId,"districtId":districtId,"facilityId":facilityId},function(loadData){
      generateReportData(loadData.data);
    });
  }

  function generateReportData(data) {
    $scope.reportData = [];

    var drugHash = {};

    _.forEach(_.groupBy(data, 'facilityCode'), function (item) {
      var expiryDatesForTheFacility = getExpiryDatesBeforeOccurredForFacility(item);

      _.forEach(expiryDatesForTheFacility, function (expiryDateItem, drugCode) {
        if (drugHash[drugCode]) {
          _.forEach(_.keys(expiryDateItem.lot_expiry_dates), function(lotExpiryKey) {
            if (drugHash[drugCode].lot_expiry_dates[lotExpiryKey] !== undefined) {
              drugHash[drugCode].lot_expiry_dates[lotExpiryKey] += expiryDateItem.lot_expiry_dates[lotExpiryKey];
            } else {
              drugHash[drugCode].lot_expiry_dates[lotExpiryKey] = expiryDateItem.lot_expiry_dates[lotExpiryKey];
            }
          });
        } else {
          drugHash[drugCode] = expiryDateItem;
        }
      });
    });

    _.forEach(_.values(drugHash), function (drug) {
      var expiryDateArray = [];
      _.forEach(_.keys(drug.lot_expiry_dates), function (oneLotExpiryDate) {
        if (drug.lot_expiry_dates[oneLotExpiryDate] > 0) {
          var lotNumber = oneLotExpiryDate.split(" - ")[0];
          var formattedExpiryDate = formatExpiryDate(oneLotExpiryDate.split(" - ")[1]);
          var lotExpiryDateObj = {
            'lot_number': lotNumber,
            'lot_expiry': lotNumber + " - " + formattedExpiryDate,
            'expiry_date': new Date(oneLotExpiryDate.split(" - ")[1]),
            'formatted_expiry_date': formattedExpiryDate,
            'lot_on_hand': drug.lot_expiry_dates[oneLotExpiryDate]
          };
          expiryDateArray.push(lotExpiryDateObj);
        }
      });
      drug.lot_expiry_dates = _.sortBy(_.uniq(expiryDateArray), 'expiry_date');
      if(drug.lot_expiry_dates.length > 0) {
        drug.first_expiry_date = drug.lot_expiry_dates[0].expiry_date;
        $scope.reportData.push(drug);
      }
    });
  }

  function formatExpiryDate(expiryDate) {
    var options = {year: 'numeric', month: 'short'};
    return new Date(expiryDate).toLocaleString(locale, options);
  }

  function getExpiryDatesBeforeOccurredForFacility(dataForOneFacility) {
    var drugOccurredHash = {};
    _.forEach(dataForOneFacility, function (item) {
      var createdDate = item.createddate;
      var occurredDate = item.occurred;
      var drugCode = item.drugCode;

      if (drugOccurredHash[drugCode]) {
          drugOccurredHash[drugCode].occurred_date = occurredDate;
          drugOccurredHash[drugCode].createddate = createdDate;
          drugOccurredHash[drugCode].lot_expiry_dates[item.lotNumber + " - " + item.expiryDate] = item.lotonhand;
      } else {
        drugOccurredHash[drugCode] = {
          code: drugCode,
          name: item.drugName,
          createddate: createdDate,
          occurred_date: occurredDate,
          lot_expiry_dates: {},
          facility_code: item.facilityCode,
          province_name: item.provinceName,
          facility_name: item.facilityName,
          district_name: item.districtName
        };
        drugOccurredHash[drugCode].lot_expiry_dates[item.lotNumber + " - " + item.expiryDate] = item.lotonhand;
      }
    });
    return drugOccurredHash;
  }

  function getExpiryDateReportsParams() {
    var params = {};
    var dividingDateTime = new Date('2021-10-01').setHours(23, 59, 59, 999);
    var selectedTime = new Date($scope.reportParams.endTime).setHours(23, 59, 59, 999);
    params.combineMvs=[];
    if (selectedTime <= dividingDateTime ) {
      params.combineMvs.push({
        endTime:selectedTime,
        mv:"vw_lot_expiry_dates_before_20211001"
      });
    } else { // combine
      params.combineMvs.push(
          {
            endTime: dividingDateTime,
            mv: "vw_lot_expiry_dates_before_20211001"
          },
          {
            endTime: selectedTime,
            mv: "vw_lot_expiry_dates_after_20211001"
          }
      );
    }

    $scope.locationIdToCode(params);
    return params;
  }

  function generateReportTitle() {
    $scope.expiryDatesReportParams = getExpiryDateReportsParams();
    var reportTitle = "";
    if ($scope.expiryDatesReportParams.selectedProvince) {
      reportTitle = $scope.expiryDatesReportParams.selectedProvince.name;
    }
    if ($scope.expiryDatesReportParams.selectedDistrict) {
      reportTitle = $scope.expiryDatesReportParams.selectedDistrict.name;
    }
    if ($scope.expiryDatesReportParams.selectedFacility) {
      reportTitle = $scope.expiryDatesReportParams.selectedFacility.name;
    }
    $scope.reportParams.reportTitle = reportTitle || messageService.get("label.all");
  }

  $scope.partialPropertiesFilter = function(searchValue) {
    return function(entry) {
      var regex = new RegExp(searchValue, "gi");

      return regex.test(entry.code) ||
          regex.test(entry.name) ||
          regex.test(_.pluck(entry.lot_expiry_dates, 'lot_expiry').join(' '));
    };
  };

  $scope.exportXLSX = function() {
    var data = {
      reportHeaders: {
        drugCode: messageService.get('report.header.drug.code'),
        drugName: messageService.get('report.header.drug.name'),
        province: messageService.get('report.header.province'),
        district: messageService.get('report.header.district'),
        facility: messageService.get('report.header.facility'),
        lot: messageService.get('report.header.lot'),
        expiryDate: messageService.get('report.header.expiry.date'),
        soh: messageService.get('report.header.stock.on.hand'),
        reportGenerateDate: messageService.get('report.header.generated.for')
      },
      reportContent: []
    };

      if($scope.reportData) {
      $scope.reportData.forEach(function (expiryDateReportData) {
        expiryDateReportData.lot_expiry_dates.forEach(function(lot) {
          var expiryDateReportContent = {};
          expiryDateReportContent.drugCode = expiryDateReportData.code;
          expiryDateReportContent.drugName = expiryDateReportData.name;
          expiryDateReportContent.province = expiryDateReportData.province_name;
          expiryDateReportContent.district = expiryDateReportData.district_name;
          expiryDateReportContent.facility = expiryDateReportData.facility_name;
          expiryDateReportContent.lot = lot.lot_number;
          expiryDateReportContent.expiryDate = lot.formatted_expiry_date;
          expiryDateReportContent.soh = lot.lot_on_hand;
          expiryDateReportContent.reportGenerateDate = DateFormatService.formatDateWithDateMonthYear($scope.expiryDatesReportParams.endTime);
          data.reportContent.push(expiryDateReportContent);
        });
      });

      ReportExportExcelService.exportAsXlsx(data, messageService.get('report.file.expiry.dates.report'));
    }

  };

  $scope.$on('ngRepeatFinished', function () {
    if (!_.isEmpty($routeParams)) {
      $(".content-table")[0].scrollTop += ($("#" + $routeParams.drugCode).offset().top - $(".content-table").offset().top - 10);
      $("#" + $routeParams.drugCode).parent().parent().addClass("highlight");
    }
  });
}