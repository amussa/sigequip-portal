services.factory('NosDrugsChartService', function ($http, $filter, $q, $timeout, messageService,
                                                   CubesGenerateUrlService, StockoutSingleProductZoneChartService,
                                                   CubesGenerateCutParamsService, ReportLocationConfigService,
                                                   ReportExportExcelService, DateFormatService, NosDrugStatusService,
                                                   WeeklyNosDrugExportService) {

  var DATE_FORMAT = 'yyyy,MM,dd';
  var drugCodeKey = "drug.drug_code";
  var drugNameKey = "drug.drug_name";
  var selectedDrugs = [];
  var chartDataItems;
  var allNosDrugs = [];
  var provinceGloble;
  var districtGloble;

  var status = {
    "Stock Out": "stockOut",
    "Low Stock": "lowStock",
    "Regular Stock": "regularStock",
    "Over Stock": "overStock",
  };

  function getNosDrugStockRateOnFriday(zone, friday, stockOuts, nosDrugCode, carryStartDates) {
    var stockOutsOfNosDrug = _.filter(stockOuts, function (stockOut) {
      return stockOut[drugCodeKey] === nosDrugCode;
    });
    var carryStartDatesOfNosDrug = _.filter(carryStartDates, function (carry) {
      return carry[drugCodeKey] === nosDrugCode;
    });

    return StockoutSingleProductZoneChartService.generateChartDataItemsForZone(zone, friday, friday, stockOutsOfNosDrug, carryStartDatesOfNosDrug)[0];
  }

  function getCubesRequestPromise(nosDrugs, province, district, userSelectedStartDate, userSelectedEndDate, cubesName, timeDimensionName) {
    var cutsParams = CubesGenerateCutParamsService.generateCutsParams(timeDimensionName,
      $filter('date')(userSelectedStartDate, "yyyy,MM,dd"),
      $filter('date')(userSelectedEndDate, "yyyy,MM,dd"),
      undefined, nosDrugs, province, district);

    var requestUrl = CubesGenerateUrlService.generateFactsUrl(cubesName, cutsParams);
    return $http.get(requestUrl);
  }

  function generateNosDrugDataItemForOneFriday(friday, nosDrugs, province, district, stockOuts, carryStartDates) {
    var chartDataItem = {date: friday};

    var totalPercentage = 0;
    _.forEach(nosDrugs, function (nosDrug) {
      var nosDrugCode = nosDrug[drugCodeKey];

      var fridayStockOutRate = getNosDrugStockRateOnFriday(ReportLocationConfigService.getZone(province, district), friday, stockOuts, nosDrugCode, carryStartDates);
      chartDataItem[nosDrugCode + "StockOutFacilities"] = fridayStockOutRate.stockOutFacilities;
      chartDataItem[nosDrugCode + "CarryingFacilities"] = fridayStockOutRate.carryingFacilities;
      chartDataItem[nosDrugCode] = 0;
      if (fridayStockOutRate.carryingFacilities.length > 0) {
        chartDataItem[nosDrugCode] = 100 - fridayStockOutRate.percentage;
      }

      totalPercentage += chartDataItem[nosDrugCode];
    });
    chartDataItem.average = (totalPercentage / nosDrugs.length).toFixed(0);

    return chartDataItem;
  }

  function LinkedNode() {
    var self = this;

    this.addToTail = function (elem) {
      if (self.next === undefined) {
        self.next = elem;
      } else {
        self.next.addToTail(elem);
      }
    };

    this.showSelfAndNext = function (isToggleOff, event) {
      $timeout(function () {
        if (isToggleOff) {
          event.chart.hideGraph(self.value);
        } else {
          event.chart.showGraph(self.value);
        }
        if (self.next !== undefined) {
          self.next.showSelfAndNext(isToggleOff, event);
        }
      });
    };
  }

  function toggleGraphsExclude(event, isToggleOff, excludes) {
    var firstLinkedNode = _.chain(event.chart.graphs)
      .filter(function (graph) {
        return _.every(excludes, function (exclude) {
          return graph.id != exclude;
        });
      })
      .reduce(function (node, graph) {
        var newNode = new LinkedNode();
        newNode.value = graph;

        node.addToTail(newNode);
        return node;
      }, new LinkedNode())
      .value().next;

    firstLinkedNode.showSelfAndNext(isToggleOff, event);
  }

  function generateNosDrugsChartDataItems(nosDrugs, stockOuts, carryStartDates, userSelectedStartDate, userSelectedEndDate, province, district) {
    var fridays = DateFormatService.getFridaysBetween(userSelectedStartDate, userSelectedEndDate);
    return _.chain(fridays)
      .map(function (friday) {
        return generateNosDrugDataItemForOneFriday(friday, nosDrugs, province, district, stockOuts, carryStartDates);
      }).value();
  }

  function makeNosDrugHistogram(chartDivId, nosDrugItem) {

    var formattedNosDrugItems = formatNosDrugItems(nosDrugItem);

    renderNosDrugHistogram(chartDivId, formattedNosDrugItems);

  }

  function formatNosDrugItems(nosDrugItems) {
    if (nosDrugItems.length === 0) {
      return [{
        nosData: null,
        date: '',
        lowStockValue: 0,
        overStockValue: 0,
        regularStockValue: 0,
        stockOutValue: 0
      }];
    }

    return _.map(nosDrugItems, function (nosDrugItem) {
      var dateKey = Object.keys(nosDrugItem)[0];

      return {
        nosData: nosDrugItem,
        date: dateKey + " " +messageService.get('report.tracer.week') + moment(dateKey, "YYYY-MM-DD").isoWeek(),
        lowStockValue: generateValue(nosDrugItem[dateKey], "lowStock"),
        overStockValue: generateValue(nosDrugItem[dateKey], "overStock"),
        regularStockValue: generateValue(nosDrugItem[dateKey], "regularStock"),
        stockOutValue: generateValue(nosDrugItem[dateKey], "stockOut"),
      };
    });
  }

  function generateValue(drugItem, type) {
    if (!provinceGloble) {
      return getValueWithPlaceLevel(drugItem, type, "province", false);
    } else if (!districtGloble) {
      return getValueWithPlaceLevel(drugItem, type, "district", false);
    } else {
      return getValueWithPlaceLevel(drugItem, type, "", true);
    }
  }

  function getValueWithPlaceLevel(drugItem, type, placeLevel, isFacility) {
    switch (type) {
      case "lowStock":
        return getNosDrugStatusValue(drugItem.lowStock, placeLevel, isFacility);
      case "overStock":
        return getNosDrugStatusValue(drugItem.overStock, placeLevel, isFacility);
      case "regularStock":
        return getNosDrugStatusValue(drugItem.regularStock, placeLevel, isFacility);
      case "stockOut":
        return getNosDrugStatusValue(drugItem.stockOut, placeLevel, isFacility);
      default :
        return null;
    }
  }

  function getNosDrugStatusValue(item, placeLevel, isFacility) {
    if (isFacility) {
      return item.percentage === 0 ? null : item.percentage;
    }
    return item[placeLevel].length === 0 ? null : item[placeLevel].length;
  }

  function getNosDrugItemsPromise(province, district, startTime, endTime, selectedDrugCode) {
    provinceGloble = province;
    districtGloble = district;

    selectedDrugs = [];
    selectedDrugs.push(selectedDrugCode);

    var params = {
      province: provinceGloble,
      district: districtGloble,
      startTime: $filter('date')(startTime, DATE_FORMAT),
      endTime: $filter('date')(endTime, DATE_FORMAT),
      selectedDrugs: selectedDrugs,
      reportType: "nosDrug"
    };

    return NosDrugStatusService.get(params);
  }

  function getNosDrugList() {
    return $http.get('/cubesreports/cube/products/facts?cut=is_nos:true').success(function (nosDrugs) {
      return nosDrugs;
    }).error(function () {
      return [];
    });
  }

  function generateGraphs(nosDrugs) {
    function stringToRGB(str) {
      var hash = 0;
      for (var i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash);
      }

      var c = (hash & 0x00FFFFFF)
        .toString(16)
        .toUpperCase();

      return "#" + "00000".substring(0, 6 - c.length) + c;
    }

    function makeBalloon(nosDrugName, nosDrugcode) {
      return function (item, graph) {
        var percentage = item.dataContext[nosDrugcode];
        var stockOutFacilities = item.dataContext[nosDrugcode + "StockOutFacilities"];
        var carryingFacilities = item.dataContext[nosDrugcode + "CarryingFacilities"];
        var facilitiesWithStock = carryingFacilities.filter(function (a) {
          return !~this.indexOf(a);
        }, stockOutFacilities);
        var facilitiesWithStockNames = facilitiesWithStock.join(', ');

        var informationDrug = messageService.get('report.tracer.name') + ": " + nosDrugName + "[" + nosDrugcode + "]" + "<br>" +
          messageService.get('report.tracer.percentage') + ": " + percentage + "% <br>" +
          messageService.get('report.tracer.health.facility') + ": " + (carryingFacilities.length - stockOutFacilities.length) + "% <br>";

        if (!_.isEmpty(stockOutFacilities)) {
          var stockOutFacilitiesNames = stockOutFacilities.join(', ');
          informationDrug += "<span style='color:red;'>" + stockOutFacilitiesNames + "</span><br>";
        }

        informationDrug += facilitiesWithStockNames + "<br>";

        return informationDrug;
      };
    }

    var nosDrugGraphs = _.chain(nosDrugs)
      .sortBy(function (nosDrug) {
        return nosDrug[drugNameKey];
      })
      .map(function (nosDrug) {
        var nosDrugcode = nosDrug[drugCodeKey];
        var nosDrugName = nosDrug[drugNameKey];

        return {
          lineColor: stringToRGB(nosDrugcode + nosDrugName),
          bullet: "round",
          title: nosDrugName + "[" + nosDrugcode + "]",
          valueField: nosDrugcode,
          hidden: true,
          lineThickness: 2,
          balloonFunction: makeBalloon(nosDrugName, nosDrugcode)
        };
      }).value();

    nosDrugGraphs.unshift({
      id: "average",
      lineColor: "red",
      bullet: "round",
      title: messageService.get('report.tracer.average'),
      valueField: "average",
      dashLength: 3,
      lineThickness: 6,
      balloonText: messageService.get('report.tracer.average') + ": [[average]]%"
    });
    nosDrugGraphs.push({
      id: "all",
      title: messageService.get('report.tracer.all'),
      lineColor: "black",
      hidden: true,
      legendValueText: " ",
      legendPeriodValueText: " "
    });

    return nosDrugGraphs;
  }

  function renderNosDrugHistogram(chartDivId, nosDrugItems) {
    var chart = AmCharts.makeChart(chartDivId, {
      type: "serial",
      categoryField: "date",
      startDuration: 1,
      columnWidth: 0.5,
      categoryAxis: {
        gridPosition: "start",
        minHorizontalGap: 140
      },
      chartScrollbar: {
        enabled: true,
        graphType: "line",
        offset: 40,
        oppositeAxis: false,
        scrollbarHeight: 5,
        scrollDuration: 0
      },
      chartCursor: {
        enabled: true,
        categoryBalloonEnabled: false,
        cursorAlpha: 0,
        oneBalloonOnly: false
      },
      balloon: {
        textAlign: "left",
        maxWidth: 500,
      },
      graphs: [
        {
          balloonFunction: generateBalloonInfo,
          fillAlphas: 1,
          title: "Stock Out",
          type: "column",
          valueField: "stockOutValue",
          fillColors: "#f5212d",
          legendColor: "#f5212d",
        },
        {
          balloonFunction: generateBalloonInfo,
          fillAlphas: 1,
          title: "Low Stock",
          type: "column",
          valueField: "lowStockValue",
          fillColors: "#fad74d",
          legendColor: "#fad74d",
        },
        {
          balloonFunction: generateBalloonInfo,
          fillAlphas: 1,
          title: "Regular Stock",
          type: "column",
          valueField: "regularStockValue",
          fillColors: "#4bba14",
          legendColor: "#4bba14",
        },
        {
          balloonFunction: generateBalloonInfo,
          fillAlphas: 1,
          title: "Over Stock",
          type: "column",
          valueField: "overStockValue",
          fillColors: "#6610c7",
          legendColor: "#6610c7",
        }
      ],
      legend: {
        enabled: true,
        align: "center",
        textClickEnabled: true,
        reversedOrder: true,
        useGraphSettings: true,
        valueText: ""
      },
      dataProvider: nosDrugItems
    });

    AmCharts.checkData = function (chart) {
      if (!chart.dataProvider[0].nosData) {
        chart.addLabel(0, '50%', 'The chart contains no data', 'center');

        chart.chartDiv.style.opacity = 0.5;
      }

      if (provinceGloble && districtGloble) {
        chart.valueAxes[0].stackType = "100%";
        chart.valueAxes[0].unit = "%";
        chart.valueAxes[0].color = "#999999";
        chart.chartCursor.oneBalloonOnly = true;
      }

      chart.validateNow();
    };

    AmCharts.checkData(chart);
  }

  function generateBalloonInfo(e) {
    var drugContext = e.dataContext;
    var graph = e.graph;

    if (!drugContext.nosData) {
      return null;
    }

    var originalNosDrugData = drugContext.nosData[drugContext.date.split(" ")[0]];

    if (!provinceGloble) {
      return getBalloonInfo(originalNosDrugData, graph, "province");
    } else if (!districtGloble) {
      return getBalloonInfo(originalNosDrugData, graph, "district");
    } else {
      return getBalloonInfo(originalNosDrugData, graph, "facility");
    }
  }

  function getBalloonInfo(originalNosDrugData, graph, type) {
    var tag = getTitle(graph.title);
    switch (type) {
      case "facility":
        return messageService.get('report.tracer.health.facility.number') + ": <span style='font-weight: bold'>" + originalNosDrugData[tag].facility.length + "</span><br>" +
          messageService.get('report.tracer.percentage') + ": <span style='font-weight: bold'>" + originalNosDrugData[tag].percentage + "%</span>" +
          "<hr style='margin: 0'>" + generateContent(originalNosDrugData[tag].facility);
      case "district":
        return messageService.get('report.tracer.health.district.number') + ": <span style='font-weight: bold'>" + originalNosDrugData[tag].district.length + "</span><br>" +
          "<hr style='margin: 0'>" + generateContent(originalNosDrugData[tag].district);
      case "province":
        return messageService.get('report.tracer.health.province.number') + ": <span style='font-weight: bold'>" + originalNosDrugData[tag].province.length + "</span><br>" +
          "<hr style='margin: 0'>" + generateContent(originalNosDrugData[tag].province);
    }
  }

  function getTitle(title) {
    return status[title];
  }

  function generateContent(level) {
    var names = '';
    return _.each(level, function (item) {
      names += item;
    });
  }

  function handleLegendClick(evt) {
    if (evt.type === "hideItem") {
      _.uniq(selectedDrugs);
      var index = selectedDrugs.indexOf(evt.dataItem.valueField);
      if (index > -1) {
        selectedDrugs.splice(index, 1);
      }
    } else {
      selectedDrugs.push(evt.dataItem.valueField);
    }
  }

  function exportXLSX(startTime, endTime, province, district) {
    var allNosCodes = _.map(allNosDrugs, function (drug) {
      return drug['drug.drug_code'];
    });

    WeeklyNosDrugExportService.getDataForExport(selectedDrugs, province, district, startTime, endTime, allNosCodes);
  }

  return {
    generateGraphs: generateGraphs,
    generateNosDrugsChartDataItems: generateNosDrugsChartDataItems,
    exportXLSX: exportXLSX,
    getNosDrugList: getNosDrugList,
    makeNosDrugHistogram: makeNosDrugHistogram,
    getNosDrugItemsPromise: getNosDrugItemsPromise
  };
});