function StockMovementReportController($scope, $routeParams, Facility, $http, CubesGenerateUrlService, DateFormatService,
                                       $cacheFactory, $filter, ReportExportExcelService, messageService) {
  var currentDate = new Date(),
    DATE_FORMAT = 'yyyy,MM,dd';
  
  var thisPeriodStartDate = currentDate.getDate() < 21 ?
    new Date(currentDate.getFullYear(), currentDate.getMonth() - 1, 21) :
    new Date(currentDate.getFullYear(), currentDate.getMonth(), 21);
  
  var threePeriodsBeforeIncludingCurrent = new Date(thisPeriodStartDate.getFullYear(), thisPeriodStartDate.getMonth() - 2, 21);
  if (thisPeriodStartDate.getMonth() - 2 > currentDate.getMonth()) {
    threePeriodsBeforeIncludingCurrent = new Date(threePeriodsBeforeIncludingCurrent).setYear(currentDate.getFullYear() - 1);
  }
  
  var twelvePeriodsBeforeIncludingCurrent = new Date(thisPeriodStartDate.getFullYear(), thisPeriodStartDate.getMonth() - 11, 21);
  if (thisPeriodStartDate.getMonth() - 11 > currentDate.getMonth()) {
    twelvePeriodsBeforeIncludingCurrent = new Date(twelvePeriodsBeforeIncludingCurrent).setYear(currentDate.getFullYear() - 2);
  } else {
    twelvePeriodsBeforeIncludingCurrent = new Date(twelvePeriodsBeforeIncludingCurrent).setYear(currentDate.getFullYear() - 1);
  }
  
  var periodOptions = {
    'period': thisPeriodStartDate,
    '3periods': threePeriodsBeforeIncludingCurrent,
    'year': twelvePeriodsBeforeIncludingCurrent
  };
  
  $scope.periodTags = Object.keys(periodOptions);
  $scope.selectedPeriodTag = '';
  $scope.isSelectDateRange = false;
  $scope.dateRange = {
    startTime: null,
    endTime: null
  };
  
  $scope.loadFacilityAndStockMovements = function () {
    if ($cacheFactory.get('keepHistoryInStockOnHandPage') !== undefined) {
      $cacheFactory.get('keepHistoryInStockOnHandPage').put('saveDataOfStockOnHand', "yes");
    }
    if ($cacheFactory.get('stockOutReportParams') !== undefined) {
      $cacheFactory.get('stockOutReportParams').put('shouldLoadStockOutReportSingleProductFromCache', "yes");
    }
    
    loadStockMovements();
  };
  
  $scope.changePeriodOption = function (periodTag) {
    $scope.selectedPeriodTag = periodTag;
    $scope.dateRange = {
      startTime: DateFormatService.formatDateWithStartDayOfPeriod(new Date(periodOptions[periodTag])),
      endTime: DateFormatService.formatDateWithEndDayOfPeriod(currentDate)
    };
    
    loadStockMovements();
  };
  
  $scope.$on('messagesPopulated', function () {
    $scope.formatDate();
  });
  
  $scope.formatDate = function (dateString) {
    return DateFormatService.formatDateWithLocale(dateString);
  };
  
  $scope.selectedDateRange = function () {
    var dateRange = $scope.dateRange;
    if (dateRange.startTime && dateRange.endTime) {
      loadStockMovements();
    }
  };
  
  $scope.$on('$viewContentLoaded', function () {
    $scope.productCode = $routeParams.productCode;
    $scope.facilityCode = $routeParams.facilityCode;
    
    $scope.loadFacilityAndStockMovements();
  });
  
  $scope.exportXLSX = function () {
    var data = {
      reportTitles: generateReportTitle(),
      reportHeaders: generateReportHeader(),
      reportContent: generateReportContent()
    };
    
    if ($scope.stockMovements) {
      ReportExportExcelService.exportAsXlsx(data, messageService.get('report.file.stock.movements.report'));
    }
  };
  
  function generateReportTitle() {
    var dateString = '';
    var dateRange = $scope.dateRange;
    if (dateRange.startTime && dateRange.endTime) {
      dateString = DateFormatService.formatDateWithDateMonthYear(dateRange.startTime) + ' - ' +
        DateFormatService.formatDateWithDateMonthYear(dateRange.endTime);
    } else {
      var firstItem = $scope.stockMovements[0];
      var lastItem = $scope.stockMovements.pop();
      dateString = DateFormatService.formatDateWithDateMonthYearForString(lastItem['movement.date']) + ' - ' +
        DateFormatService.formatDateWithDateMonthYearForString(firstItem['movement.date']);
    }
    
    return [
      [$scope.productCode, $scope.productName, $scope.facilityName, $scope.district, $scope.province],
      [messageService.get('report.header.generated.for'), dateString]
    ];
  }
  
  function generateReportHeader() {
    return {
      date: messageService.get('stock.movement.date'),
      reason: messageService.get('stock.movement.reason'),
      documentNumber: messageService.get('stock.movement.document.number'),
      entries: messageService.get('stock.movement.entries'),
      negativeAdjustment: messageService.get('stock.movement.negative.adjustment'),
      positiveAdjustment: messageService.get('stock.movement.positive.adjustment'),
      issues: messageService.get('stock.movement.issues'),
      soh: messageService.get('stock.movement.soh'),
      requestedQuantity: messageService.get('stock.movement.requestedquantity'),
      signature: messageService.get('stock.movement.signature')
    };
  }
  
  function generateReportContent() {
    var reportContent = [];
    if ($scope.stockMovements) {
      $scope.stockMovements.forEach(function (stockMovement) {
        var requisitionContent = {
          date: {
            value: stockMovement['movement.date'],
            dataType: 'date',
            style: {
              dataPattern: 'yyyy-MM-dd',
              excelDataPattern: 'd/m/yy'
            }
          },
          reason: messageService.get('stock.movement.' + stockMovement['movement.reason']),
          documentNumber: stockMovement['movement.documentnumber'],
          entries: {value: stockMovement.entries, dataType: 'integer'},
          negativeAdjustment: {value: stockMovement.negativeAdjustment, dataType: 'integer'},
          positiveAdjustment: {value: stockMovement.positiveAdjustment, dataType: 'integer'},
          issues: {value: stockMovement.issues, dataType: 'integer'},
          soh: {value: stockMovement['movement.soh'], dataType: 'integer'},
          requestedQuantity: {value: stockMovement['movement.requestedquantity'], dataType: 'integer'},
          signature: stockMovement['movement.signature']
        };
        
        reportContent.push(requisitionContent);
      });
    }
    
    return reportContent;
  }
  
  function loadStockMovements() {
    var cuts = [
      {dimension: "movement", values: [$scope.productCode]},
      {dimension: "facility", values: [$scope.facilityCode]}
    ];
    
    if ($scope.selectedPeriodTag ||
      ($scope.dateRange.startTime && $scope.dateRange.endTime)) {
      cuts.push({
        dimension: "movementdate",
        values: [$filter('date')($scope.dateRange.startTime, DATE_FORMAT) + '-' +
        $filter('date')($scope.dateRange.endTime, DATE_FORMAT)],
        skipEscape: true
      });
    }
    
    getStockMovementsAPI(cuts);
  }
  
  function getStockMovementsAPI(cuts) {
    $http.get(CubesGenerateUrlService.generateFactsUrl('vw_stock_movements', cuts)).success(function (data) {
      if (!data.length) {
        $scope.stockMovements = [];
        return;
      }
      
      var firstEntry = data[0];
      $scope.facilityName = firstEntry['facility.facility_name'];
      $scope.district = firstEntry['location.district_name'];
      $scope.province = firstEntry['location.province_name'];
      $scope.productName = firstEntry['movement.productname'];
      
      $scope.stockMovements = [];
      _.each(data, function (item) {
        setQuantityByType(item);
        $scope.stockMovements.push(item);
      });
      
      $scope.stockMovements = _.sortBy($scope.stockMovements, function (item) {
        return [item["movement.date"], item["movement.id"]].join("_");
      });
      $scope.stockMovements.reverse();
    });
  }
  
  function setQuantityByType(item) {
    var quantity = Math.abs(item["movement.quantity"]);
    switch (item["movement.type"]) {
      case 'RECEIVE' :
        item.entries = quantity;
        break;
      case 'ISSUE':
        item.issues = quantity;
        break;
      case 'NEGATIVE_ADJUST':
        item.negativeAdjustment = quantity;
        break;
      case 'POSITIVE_ADJUST':
        item.positiveAdjustment = quantity;
        break;
    }
  }
}