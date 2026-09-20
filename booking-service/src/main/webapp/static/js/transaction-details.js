(function () {
  'use strict';

  var changeRequestId = document.body.getAttribute('data-change-request-id');
  var reconcileBtn = document.getElementById('reconcile-btn');
  var incidentBtn = document.getElementById('incident-btn');
  var actionResult = document.getElementById('action-result');

  function setBtnLoading(btn, loadingText, isLoading) {
    if (isLoading) {
      btn.dataset.originalText = btn.dataset.originalText || btn.textContent;
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span>' + loadingText;
    } else {
      btn.disabled = false;
      btn.textContent = btn.dataset.originalText || btn.textContent;
    }
  }

  if (reconcileBtn) {
    reconcileBtn.addEventListener('click', function () {
      setBtnLoading(reconcileBtn, 'Reconciling...', true);
      fetch('/api/change-requests/' + encodeURIComponent(changeRequestId) + '/reconcile', { method: 'POST' })
        .then(function (res) {
          if (!res.ok) { throw new Error('Reconcile request failed.'); }
          return res.json();
        })
        .then(function () {
          window.location.reload();
        })
        .catch(function (err) {
          actionResult.textContent = err.message;
        })
        .finally(function () {
          setBtnLoading(reconcileBtn, '', false);
        });
    });
  }

  if (incidentBtn) {
    incidentBtn.addEventListener('click', function () {
      setBtnLoading(incidentBtn, 'Creating Incident...', true);
      fetch('/api/change-requests/' + encodeURIComponent(changeRequestId) + '/incident', { method: 'POST' })
        .then(function (res) {
          if (!res.ok) { throw new Error('Incident creation failed.'); }
          return res.json();
        })
        .then(function (data) {
          actionResult.textContent = 'Incident created: ' + data.externalReference + ' (' + data.status + ')';
        })
        .catch(function (err) {
          actionResult.textContent = err.message;
        })
        .finally(function () {
          setBtnLoading(incidentBtn, '', false);
        });
    });
  }
})();
