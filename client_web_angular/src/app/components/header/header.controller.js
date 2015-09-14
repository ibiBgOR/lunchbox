(function() {
  'use strict';

  var app = angular.module('lunchboxWebapp');

  app.controller('HeaderCtrl', function ($scope, $route, $location, $rootScope, LunchModel, Settings) {
    $scope.header = {
      routes: []
    };
    $scope.model = LunchModel;
    $scope.settings = Settings;

    var init = function () {
      // Initialisierung der Route
      angular.forEach($route.routes, function (route, path) {
        if (route.navbarName) {
          $scope.header.routes.push({
            path: path,
            name: route.navbarName
          });
        }
      });
      // Initialisierung der Location
      if (!Settings.location) {
        if (LunchModel.locations.length > 0) {
          Settings.setLocation(LunchModel.locations[0]);
        }
      }
    };
    init();


    // Aktiv-Status für Tabs abfragen
    $scope.header.activeRoute = function (route) {
      return route.path === $location.path();
  //    return $location.path().indexOf(route.path) === 0;
    };

    // Setzen der Location aus Dropdown
    $scope.header.selectLocation = function(location) {
      Settings.setLocation(location);
    };

  });

})();
