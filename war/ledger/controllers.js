var ledgerControllers = angular.module('ledgerControllers', ['ngRoute']);

ledgerControllers.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/accounts', {
        templateUrl: '/ledger/accounts.ng',
        controller: 'LedgerAccountsCtrl'
      }).
      when('/account/:accountId/transactions', {
        templateUrl: '/ledger/transactions.ng',
        controller: 'LedgerTransactionsCtrl'
      });
  }]);

ledgerControllers.controller('LedgerAccountsCtrl', ['$scope', '$http', '$window',
  function ($scope, $http, $window) {
    $scope.bank_account = {};
    $scope.bank_account_types = [
      {id: 'operating', title: 'Operating'},
      {id: 'reserve', title: 'Reserve'}
    ];
    $scope.createBankAccountFormVisible = false;
    $scope.updateBankAccountFormVisible = false;

    $scope.showCreateBankAccountForm = function() {
      $scope.bank_account = {
        'title': 'New bank account',
      };
      $scope.createBankAccountFormVisible = true;
    }

    $scope.hideCreateBankAccountForm = function() {
      $scope.createBankAccountFormVisible = false;
      $scope.bank_account = {};
    }

    $scope.showUpdateBankAccountForm = function(bank_account) {
      $scope.bank_account = {
        'id': bank_account.id,
        'title': bank_account.attributes.title,
        'account_code_id': bank_account.attributes.account_code_id,
        'bank_account_type': bank_account.attributes.bank_account_type,
      };
      $scope.updateBankAccountFormVisible = true;
    }

    $scope.hideUpdateBankAccountForm = function() {
      $scope.updateBankAccountFormVisible = false;
      $scope.bank_account = {};
    }

    $scope.createBankAccount = function() {
      var createRequest = {
        'title': $scope.bank_account.title,
        'account_code_id': $scope.bank_account.account_code_id,
        'bank_account_type': $scope.bank_account.bank_account_type,
      };
      $http.post('/ledger/bank_account/create', createRequest)
          .success(function(data) {
            $scope.hideCreateBankAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.updateBankAccount = function() {
      var updateRequest = {
        'title': $scope.bank_account.title,
        'account_code_id': $scope.bank_account.account_code_id,
        'bank_account_type': $scope.bank_account.bank_account_type,
      };
      $http.post('/ledger/bank_account/' + $scope.bank_account.id + '/update', updateRequest)
          .success(function(data) {
            $scope.hideUpdateBankAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.revenue_account = {};
    $scope.createRevenueAccountFormVisible = false;
    $scope.updateRevenueAccountFormVisible = false;

    $scope.showCreateRevenueAccountForm = function() {
      $scope.loadBankAccounts(function() {
        $scope.revenue_account = {
          'title': 'New revenue account',
        };
        $scope.createRevenueAccountFormVisible = true;
      });
    }

    $scope.hideCreateRevenueAccountForm = function() {
      $scope.createRevenueAccountFormVisible = false;
      $scope.revenue_account = {};
    }

    $scope.showUpdateRevenueAccountForm = function(revenue_account) {
      $scope.loadBankAccounts(function() {
        $scope.revenue_account = {
          'id': revenue_account.id,
          'title': revenue_account.attributes.title,
          'account_code_id': revenue_account.attributes.account_code_id,
          'default_deposit_account_id': revenue_account.attributes.default_deposit_account_id,
        };
        $scope.updateRevenueAccountFormVisible = true;
      });
    }

    $scope.hideUpdateRevenueAccountForm = function() {
      $scope.updateRevenueAccountFormVisible = false;
      $scope.revenue_account = {};
    }

    $scope.createRevenueAccount = function() {
      var createRequest = {
        'title': $scope.revenue_account.title,
        'account_code_id': $scope.revenue_account.account_code_id,
        'default_deposit_account_id': $scope.revenue_account.default_deposit_account_id,
      };
      $http.post('/ledger/revenue_account/create', createRequest)
          .success(function(data) {
            $scope.hideCreateRevenueAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.updateRevenueAccount = function() {
      var updateRequest = {
        'title': $scope.revenue_account.title,
        'account_code_id': $scope.revenue_account.account_code_id,
        'default_deposit_account_id': $scope.revenue_account.default_deposit_account_id,
      };
      $http.post('/ledger/revenue_account/' + $scope.revenue_account.id + '/update', updateRequest)
          .success(function(data) {
            $scope.hideUpdateRevenueAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.expense_account = {};
    $scope.createExpenseAccountFormVisible = false;
    $scope.updateExpenseAccountFormVisible = false;

    $scope.showCreateExpenseAccountForm = function() {
      $scope.loadBankAccounts(function() {
        $scope.expense_account = {
          'title': 'New expense account',
        };
        $scope.createExpenseAccountFormVisible = true;
      });
    }

    $scope.hideCreateExpenseAccountForm = function() {
      $scope.createExpenseAccountFormVisible = false;
      $scope.expense_account = {};
    }

    $scope.showUpdateExpenseAccountForm = function(expense_account) {
      $scope.loadBankAccounts(function() {
        $scope.expense_account = {
          'id': expense_account.id,
          'title': expense_account.attributes.title,
          'account_code_id': expense_account.attributes.account_code_id,
          'default_source_account_id': expense_account.attributes.default_source_account_id,
        };
        $scope.updateExpenseAccountFormVisible = true;
      });
    }

    $scope.hideUpdateExpenseAccountForm = function() {
      $scope.updateExpenseAccountFormVisible = false;
      $scope.expense_account = {};
    }

    $scope.createExpenseAccount = function() {
      var createRequest = {
        'title': $scope.expense_account.title,
        'account_code_id': $scope.expense_account.account_code_id,
        'default_source_account_id': $scope.expense_account.default_source_account_id,
      };
      $http.post('/ledger/expense_account/create', createRequest)
          .success(function(data) {
            $scope.hideCreateExpenseAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.updateExpenseAccount = function() {
      var updateRequest = {
        'title': $scope.expense_account.title,
        'account_code_id': $scope.expense_account.account_code_id,
        'default_source_account_id': $scope.expense_account.default_source_account_id,
      };
      $http.post('/ledger/expense_account/' + $scope.expense_account.id + '/update', updateRequest)
          .success(function(data) {
            $scope.hideUpdateExpenseAccountForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.showUpdateForm = function(account) {
      if (account.type == 'bank_account') {
        $scope.showUpdateBankAccountForm(account);
      } else if (account.type == 'revenue_account') {
        $scope.showUpdateRevenueAccountForm(account);
      } else if (account.type == 'expense_account') {
        $scope.showUpdateExpenseAccountForm(account);
      } else {
        alert('Account type ' + account.type + ' is not supported.');
      }
    }

    // load account codes
    $scope.account_codes = [];
    $scope.account_codes_by_id = {};
    var account_codes_future = $http.get('/account/account_codes')
        .success(function(data) {
          $scope.account_codes = data;
          for (i = 0; i < data.length; i++) {
            var account_code = data[i];
            $scope.account_codes_by_id[account_code.id] = account_code;
          }
          // Load the main table only after loading the account codes.
          $scope.refreshTable();
        })
        .error(function(data, status, headers) {
          handleRequestErrors($window, data, status, headers);
        });

    $scope.refreshTable = function() {
      // refresh the accounts table
      $http.get('/ledger/accounts')
          .success(function(data) {
            $scope.accounts = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.loadBankAccounts = function(postLoadFunction) {
      // refresh the accounts table
      $http.get('/ledger/bank_accounts')
          .success(function(data) {
            $scope.bank_accounts = data;
            postLoadFunction();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.accountCodeTitle = function(accountCodeId) {
      return accountCodeId + ' (' + $scope.account_codes_by_id[accountCodeId].attributes.title + ')';
    }

    $scope.accountType = function(type) {
      switch (type) {
        case 'bank_account':
          return 'Bank';
        case 'expense_account':
          return 'Expense';
        case 'revenue_account':
          return 'Revenue';
        default:
          return 'Other';
      }
    }
  }]);

ledgerControllers.controller('LedgerTransactionsCtrl', ['$scope', '$http', '$routeParams', '$window',
  function ($scope, $http, $routeParams, $window) {
    // Bind the common formatDate function.
    $scope.formatDate = formatDate;
    // load the account
    $http.get('/ledger/account/' + $routeParams.accountId)
        .success(function(data) {
          $scope.account = data;
        })
        .error(function(data, status, headers) {
          handleRequestErrors($window, data, status, headers);
        });

    $scope.account_transaction = {};
    $scope.createAccountTransactionFormVisible = false;

    $scope.showCreateAccountTransactionForm = function() {
      $scope.account_transaction = {
        'date': new Date(),
        'amount': 0,
      };
      $scope.createAccountTransactionFormVisible = true;
    }

    $scope.hideCreateAccountTransactionForm = function() {
      $scope.createAccountTransactionFormVisible = false;
      $scope.account_transaction = {};
    }

    $scope.createAccountTransaction = function() {
      var createRequest = {
        'date': $scope.account_transaction.date,
        'amount': $scope.account_transaction.amount,
        'comment': $scope.account_transaction.comment,
      };
      $http.post('/ledger/account/' + $routeParams.accountId + '/create_transaction', createRequest)
          .success(function(data) {
            $scope.hideCreateAccountTransactionForm();
            $scope.refreshTable();
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable = function() {
      // refresh the accounts table
      $http.get('/ledger/account/' + $routeParams.accountId + '/transactions')
          .success(function(data) {
            $scope.transactions = data;
          })
          .error(function(data, status, headers) {
            handleRequestErrors($window, data, status, headers);
          });
    }

    $scope.refreshTable();
  }]);
