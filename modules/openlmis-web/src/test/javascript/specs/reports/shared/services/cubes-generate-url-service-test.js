describe("cubes generate url service test", function () {

    var cubesGenerateUrlService;
    beforeEach(module('openlmis'));

    beforeEach(function () {
        inject(function (CubesGenerateUrlService) {
            cubesGenerateUrlService = CubesGenerateUrlService;
        })
    });

    it("should generate drill down and cuts parameter for url", function () {
        var cubesName = "xxx";
        var drillDown = ["drug", "overlapped_month"];
        var cuts = [{dimension: "facility", values: ["HF1"]},
            {dimension: "drug", values: ["01C01", "08A07"]},
            {dimension: "location", values: [["provinceCode1", "districtCode1"], ["provinceCode2", "districtCode2"]]},
            {dimension: "date", values: ["2015,12,3-2015,12,21"], skipEscape: true}];

        expect(cubesGenerateUrlService.generateAggregateUrl(cubesName, drillDown, cuts))
            .toEqual("/cubesreports/cube/xxx/aggregate?drilldown=drug%7Coverlapped_month&cut=facility%3AHF1%7Cdrug%3A01C01%3B08A07%7Clocation%3AprovinceCode1%2CdistrictCode1%3BprovinceCode2%2CdistrictCode2%7Cdate%3A2015%2C12%2C3-2015%2C12%2C21");
        expect(cubesGenerateUrlService.generateFactsUrl(cubesName, cuts))
            .toEqual("/cubesreports/cube/xxx/facts?cut=facility%3AHF1%7Cdrug%3A01C01%3B08A07%7Clocation%3AprovinceCode1%2CdistrictCode1%3BprovinceCode2%2CdistrictCode2%7Cdate%3A2015%2C12%2C3-2015%2C12%2C21")
    });

    it("should generate aggregate url without dirll down", function () {
        var cubesName = "xxx";
        var drillDown = ["drug", "overlapped_month"];
        var cuts = [{dimension: "facility", values: ["HF1"]},
            {dimension: "drug", values: ["01C01", "08A07"]},
            {dimension: "location", values: [["provinceCode1", "districtCode1"], ["provinceCode2", "districtCode2"]]},
            {dimension: "date", values: ["2015,12,3-2015,12,21"], skipEscape: true}];

        expect(cubesGenerateUrlService.generateAggregateUrl(cubesName, [], cuts))
            .toEqual("/cubesreports/cube/xxx/aggregate?cut=facility%3AHF1%7Cdrug%3A01C01%3B08A07%7Clocation%3AprovinceCode1%2CdistrictCode1%3BprovinceCode2%2CdistrictCode2%7Cdate%3A2015%2C12%2C3-2015%2C12%2C21");
    });

    it("should generate url for csv downloading by cuts params and fields params", function () {
        var cubesName = "vw_weekly_tracer_soh";
        var params = [{
            name: "fields",
            value: ["facility.facility_name", "drug.drug_name", "date", "soh"]
        }, {name: "format", value: ["csv"]}];
        var cuts = [{
            dimension: "location",
            values: [["provinceCode1", "districtCode1"], ["provinceCode2", "districtCode2"]]
        },
            {dimension: "cutDate", values: ["2015,12,3-2016,03,21"], skipEscape: true}];

        expect(cubesGenerateUrlService.generateFactsUrlWithParams(cubesName, cuts, params))
            .toEqual("/cubesreports/cube/vw_weekly_tracer_soh/facts?cut=location%3AprovinceCode1%2CdistrictCode1%3BprovinceCode2%2CdistrictCode2%7CcutDate%3A2015%2C12%2C3-2016%2C03%2C21&fields=facility.facility_name%2Cdrug.drug_name%2Cdate%2Csoh&format=csv");
    });

    it("should generate url for facts", function () {
        var cubesName = "vw_stock_movements";
        var cuts = [{dimension: "movement", values: [['facilitycode'], ['productcode']]},
            {dimension: "stock", values: [['facilitycode1'], ['productcode1']]}];
        expect(cubesGenerateUrlService.generateFactsUrl(cubesName, cuts))
            .toEqual("/cubesreports/cube/vw_stock_movements/facts?cut=movement%3Afacilitycode%3Bproductcode%7Cstock%3Afacilitycode1%3Bproductcode1");
    });

    it("should escape special characters in url parameters", function () {
        var cubesName = "vw_stock_movements";
        var cuts = [{dimension: "movement", values: [['facility#+code'], ['product-&code']]},
            {dimension: "stock", values: [['facility,|code1'], ['product;code1']]}];
        expect(cubesGenerateUrlService.generateFactsUrl(cubesName, cuts))
            .toEqual("/cubesreports/cube/vw_stock_movements/facts?cut=movement%3Afacility%2523%252Bcode%3Bproduct%255C-%2526code%7Cstock%3Afacility%255C%252C%255C%257Ccode1%3Bproduct%255C%253Bcode1");
    });
});