(function() {
  'use strict';

  angular
    .module('lunchboxWebapp')
    .controller('DaySelectorController', DaySelectorController);

  function DaySelectorController($scope, $filter, _) {
    // TODO: Wie passen "controller as" & isolated scope zusammen? Wie können bidirektional Daten synchronisiert werden?

    $scope.prevDay = function() {
      return _.chain($scope.days)
          .filter(function(day) { return day < $scope.selectedDay; })
          .last().value();
    };

    $scope.curDay = function() {
      return _.chain($scope.days)
          .filter(function(day) { return day == new Date(); })
          .first().value()
    }

    $scope.nextDay = function() {
      return _.chain($scope.days)
          .filter(function(day) { return day > $scope.selectedDay; })
          .first().value();
    };

    $scope.goPrevDay = function() {
      var prevDay = $scope.prevDay();
      if (angular.isDefined(prevDay)) {
        $scope.selectedDay = prevDay;
      }
    };

    $scope.goCurDay = function() {
      var curDay = $scope.curDay();
      if (angular.isDefined(curDay)) {
        $scope.selectedDay = curDay;
      }
    };

    $scope.goNextDay = function() {
      var nextDay = $scope.nextDay();
      if (angular.isDefined(nextDay)) {
        $scope.selectedDay = nextDay;
      }
    };

    $scope.hasPrevDay = function() {
      return angular.isDefined($scope.prevDay());
    };

    $scope.hasNextDay = function() {
      return angular.isDefined($scope.nextDay());
    };
  }

})();
