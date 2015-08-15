(function() {
  'use strict';

  // WebApp-Modul abrufen ...
  var app = angular.module('lunchboxWebapp');

  // ... und Controller für Offers-View erzeugen
  app.controller('OffersCtrl', function ($scope, _, LunchProviderStore, LunchOfferStore) {
    function today() {
      var localNow = new Date();
      return new Date(Date.UTC(localNow.getFullYear(), localNow.getMonth(), localNow.getDate()));
    }

    $scope.day = today();
    $scope.location = 'Neubrandenburg';

    var LoadStatusEnum = Object.freeze({LOADING: 0, LOADED: 1, FAILED: 2});

    var loadStatus = {
      providers: LoadStatusEnum.LOADING,
      offers: LoadStatusEnum.LOADING
    };

    $scope.providers = LunchProviderStore.query(
      function() { // on success
        loadStatus.providers = LoadStatusEnum.FINISHED;
        if ( $scope.isLoadFinished() ) {
          refreshVisibleOffers();
        }
      }, function() { // on error
        loadStatus.providers = LoadStatusEnum.FAILED;
      }
    );

    $scope.offers = LunchOfferStore.query(
      function() { // on success
        loadStatus.offers = LoadStatusEnum.FINISHED;
        if ( $scope.isLoadFinished() ) {
          refreshVisibleOffers();
        }
      }, function() { // on error
        loadStatus.offers = LoadStatusEnum.FAILED;
      }
    );

    $scope.visibleOffers = {};

    function refreshVisibleOffers() {
      var providerIdsForLocation = _.chain($scope.providers)
                .filter(function(p) { return p.location === $scope.location; })
                .map(function(p) { return p.id; })
                .value();
      var offersForDayAndLocation = $scope.offers.filter( function(o){
          return Date.parse(o.day) === $scope.day.getTime() &&
                 _.contains(providerIdsForLocation, o.provider);
        });
      $scope.visibleOffers = _.groupBy(offersForDayAndLocation, function(o) { return o.provider; });
    }

    $scope.hasVisibleOffers = function(providerId) {
      if (!providerId) {
        return $scope.visibleOffers && !angular.equals($scope.visibleOffers, {});
      } else {
        return $scope.visibleOffers[providerId] && $scope.visibleOffers[providerId].length > 0;
      }
    };

    $scope.daysInOffers = function() {
      return _.chain($scope.offers)
          .map(function(offer) { return new Date(Date.parse(offer.day)); })
          .uniq(false, function(offer) { return offer.getTime(); })
          .sortBy(function(offer) { return offer.getTime(); })
          .value();
    };

    $scope.prevDay = function() {
      return _.chain($scope.daysInOffers())
          .filter(function(day) { return day < $scope.day; })
          .last().value();
    };

    $scope.nextDay = function() {
      return _.chain($scope.daysInOffers())
          .filter(function(day) { return day > $scope.day; })
          .first().value();
    };

    $scope.goPrevDay = function() {
      $scope.day = $scope.prevDay();
      refreshVisibleOffers();
    };

    $scope.goNextDay = function() {
      $scope.day = $scope.nextDay();
      refreshVisibleOffers();
    };

    $scope.hasPrevDay = function() {
      return $scope.prevDay() !== undefined;
    };

    $scope.hasNextDay = function() {
      return $scope.nextDay() !== undefined;
    };

    $scope.isLoadFinishedWithNoVisibleOffers = function() {
      return $scope.isLoadFinished() && !$scope.hasVisibleOffers();
    };

    $scope.isLoading = function() {
      var statusArray = _.map(loadStatus);
      return _.some(statusArray, function(value) {
        return value === LoadStatusEnum.LOADING;
      });
    };

    $scope.isLoadFinished = function() {
      var statusArray = _.map(loadStatus);
      return _.every(statusArray, function(value) {
        return value === LoadStatusEnum.FINISHED;
      });
    };

    $scope.isLoadFailed = function() {
      var statusArray = _.map(loadStatus);
      return _.some(statusArray, function(value) {
        return value === LoadStatusEnum.FAILED;
      });
    };

  });

})();
