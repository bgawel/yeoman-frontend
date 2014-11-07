'use strict';

angular.module('frontendApp')
  .controller('MainCtrl', function ($scope, $http) {
    $http.get('test/awesomeThings.json').then(function(result) {
      $scope.awesomeThings = [
        'HTML5 Boilerplate',
        'Angular JS',
        'Karma'
      ].concat(result.data);
    });
  });
