(function () {
  'use strict';

  var confirmation = document.body.getAttribute('data-confirmation');

  var checkAvailabilityBtn = document.getElementById('check-availability-btn');
  var availabilityLoading = document.getElementById('availability-loading');
  var availabilityError = document.getElementById('availability-error');
  var comparisonPanel = document.getElementById('comparison-panel');
  var replaceBtn = document.getElementById('replace-btn');
  var resultContainer = document.getElementById('result-container');
  var confirmModal = document.getElementById('confirm-modal');
  var confirmReplaceBtn = document.getElementById('confirm-replace-btn');
  var cancelReplaceBtn = document.getElementById('cancel-replace-btn');
  var resetDemoBtn = document.getElementById('reset-demo-btn');
  var techDetailsContainer = document.getElementById('tech-details-container');

  var currentAvailability = null;
  var currentDates = null;
  var idempotencyKey = null;
  var lastAttemptDates = null;

  function money(v) {
    if (v === null || v === undefined) { return '&mdash;'; }
    return Number(v).toLocaleString('en-US', { style: 'currency', currency: 'USD' });
  }

  function formatDate(iso) {
    if (!iso) { return ''; }
    var parts = iso.split('-');
    var d = new Date(Date.UTC(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])));
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' });
  }

  function setBtnLoading(btn, loadingText, isLoading, originalText) {
    if (isLoading) {
      btn.dataset.originalText = btn.dataset.originalText || originalText || btn.textContent;
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner"></span>' + loadingText;
    } else {
      btn.disabled = false;
      btn.textContent = btn.dataset.originalText || originalText || btn.textContent;
    }
  }

  checkAvailabilityBtn.addEventListener('click', function () {
    var newCheckIn = document.getElementById('new-checkin').value;
    var newCheckOut = document.getElementById('new-checkout').value;

    if (!newCheckIn || !newCheckOut) {
      availabilityError.hidden = false;
      availabilityError.textContent = 'Please select both a check-in and check-out date.';
      return;
    }

    availabilityError.hidden = true;
    comparisonPanel.hidden = true;
    resultContainer.innerHTML = '';
    availabilityLoading.hidden = false;
    setBtnLoading(checkAvailabilityBtn, 'Checking...', true, 'Check Availability');

    // A genuinely new date selection means a genuinely new change attempt.
    if (!lastAttemptDates || lastAttemptDates.newCheckIn !== newCheckIn || lastAttemptDates.newCheckOut !== newCheckOut) {
      idempotencyKey = null;
    }
    lastAttemptDates = { newCheckIn: newCheckIn, newCheckOut: newCheckOut };

    fetch('/api/bookings/' + encodeURIComponent(confirmation) + '/change/availability', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ newCheckIn: newCheckIn, newCheckOut: newCheckOut })
    })
      .then(function (res) {
        if (!res.ok) {
          return res.json().catch(function () { return {}; }).then(function (body) {
            throw new Error(body.message || 'Unable to check availability right now.');
          });
        }
        return res.json();
      })
      .then(function (data) {
        currentAvailability = data;
        currentDates = { newCheckIn: newCheckIn, newCheckOut: newCheckOut };
        renderComparison(data);
      })
      .catch(function (err) {
        availabilityError.hidden = false;
        availabilityError.textContent = err.message || 'Something went wrong checking availability.';
      })
      .finally(function () {
        availabilityLoading.hidden = true;
        setBtnLoading(checkAvailabilityBtn, '', false, 'Check Availability');
      });
  });

  function renderComparison(data) {
    document.getElementById('cmp-current-dates').textContent = formatDate(data.current.checkIn) + ' - ' + formatDate(data.current.checkOut);
    document.getElementById('cmp-current-hotel').innerHTML = money(data.current.hotelTotal);
    document.getElementById('cmp-current-car').innerHTML = money(data.current.carTotal);
    document.getElementById('cmp-current-fees').innerHTML = money(data.current.taxesAndFees);
    document.getElementById('cmp-current-total').innerHTML = money(data.current.total);

    document.getElementById('cmp-proposed-dates').textContent = formatDate(data.proposed.checkIn) + ' - ' + formatDate(data.proposed.checkOut);
    document.getElementById('cmp-proposed-hotel').innerHTML = money(data.proposed.hotelTotal);
    document.getElementById('cmp-proposed-car').innerHTML = money(data.proposed.carTotal);
    document.getElementById('cmp-proposed-fees').innerHTML = money(data.proposed.taxesAndFees);
    document.getElementById('cmp-proposed-total').innerHTML = money(data.proposed.total);

    var deltaEl = document.getElementById('price-delta');
    var diff = Number(data.priceDifference);
    var sign = diff > 0 ? '+' : '';
    deltaEl.textContent = sign + money(diff);
    deltaEl.className = 'price-delta' + (diff < 0 ? ' neg' : '');

    var hotelLine = document.getElementById('hotel-avail-line');
    var carLine = document.getElementById('car-avail-line');
    if (data.hotelAvailable) {
      hotelLine.innerHTML = '<span class="check-ok">Hotel availability confirmed &#10003;</span>';
    } else {
      hotelLine.innerHTML = '<span class="check-fail">Hotel is not available for these dates.</span>';
    }
    if (data.carAvailable) {
      carLine.innerHTML = '<span class="check-ok">Rental car availability confirmed &#10003;</span>';
    } else {
      carLine.innerHTML = '<span class="check-fail">Rental car is not available for these dates.</span>';
    }

    document.getElementById('member-benefit-note').textContent = data.memberBenefitNote || '$200 Digital Costco Shop Card preserved';

    replaceBtn.hidden = !(data.hotelAvailable && data.carAvailable);
    comparisonPanel.hidden = false;
  }

  replaceBtn.addEventListener('click', function () {
    if (!idempotencyKey) {
      idempotencyKey = crypto.randomUUID();
    }
    confirmModal.hidden = false;
  });

  cancelReplaceBtn.addEventListener('click', function () {
    confirmModal.hidden = true;
  });

  confirmReplaceBtn.addEventListener('click', function () {
    if (!currentDates) { return; }
    setBtnLoading(confirmReplaceBtn, 'Replacing your reservation...', true, 'Confirm Replacement');
    cancelReplaceBtn.disabled = true;

    fetch('/api/bookings/' + encodeURIComponent(confirmation) + '/change', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': idempotencyKey
      },
      body: JSON.stringify(currentDates)
    })
      .then(function (res) {
        if (!res.ok) {
          return res.json().catch(function () { return {}; }).then(function (body) {
            var err = new Error(plainLanguageError(res.status, body.code));
            throw err;
          });
        }
        return res.json();
      })
      .then(function (data) {
        confirmModal.hidden = true;
        renderResult(data);
      })
      .catch(function (err) {
        confirmModal.hidden = true;
        renderErrorResult(err.message);
      })
      .finally(function () {
        setBtnLoading(confirmReplaceBtn, '', false, 'Confirm Replacement');
        cancelReplaceBtn.disabled = false;
      });
  });

  function plainLanguageError(status, code) {
    if (status === 404 || code === 'BOOKING_NOT_FOUND') {
      return "We couldn't find this booking. It may have already been changed or removed.";
    }
    if (code === 'CHANGE_IN_PROGRESS') {
      return 'A change is already in progress for this booking. Please wait for it to finish before trying again.';
    }
    if (code === 'INVALID_BOOKING_STATE') {
      return 'This booking is no longer eligible to be changed.';
    }
    if (code === 'IDEMPOTENCY_KEY_CONFLICT') {
      return 'This change request was already submitted. Please refresh the page and try again if needed.';
    }
    return "We couldn't complete your requested change. Please try again.";
  }

  function renderResult(data) {
    resultContainer.innerHTML = '';
    var panel = document.createElement('div');

    if (data.success) {
      panel.className = 'result-panel success';
      panel.innerHTML =
        '<h3>' + escapeHtml(data.memberHeadline) + '</h3>' +
        '<p>' + escapeHtml(data.memberDetail) + '</p>' +
        '<p>Previous confirmation: <strong>' + escapeHtml(data.oldConfirmationNumber) + '</strong> &rarr; ' +
        'New confirmation: <strong>' + escapeHtml(data.newConfirmationNumber || '') + '</strong></p>' +
        '<div class="actions"><a class="btn btn-primary" href="/account/bookings">Return to My Bookings</a></div>';
      // A successful attempt is finished; a future attempt needs a fresh key.
      idempotencyKey = null;
      lastAttemptDates = null;
    } else {
      panel.className = 'result-panel failure';
      panel.innerHTML =
        '<h3>' + escapeHtml(data.memberHeadline) + '</h3>' +
        '<p>' + escapeHtml(data.memberDetail) + '</p>' +
        '<div class="actions">' +
        '<button type="button" class="btn btn-primary" id="try-again-btn">Try Again</button>' +
        '<a class="btn btn-secondary" href="/account/bookings/' + encodeURIComponent(confirmation) + '">Return to Booking</a>' +
        '</div>';
    }

    resultContainer.appendChild(panel);

    if (!data.success) {
      document.getElementById('try-again-btn').addEventListener('click', function () {
        resultContainer.innerHTML = '';
      });
    }

    renderTechDetails(data);
  }

  function renderErrorResult(message) {
    resultContainer.innerHTML = '';
    var panel = document.createElement('div');
    panel.className = 'result-panel failure';
    panel.innerHTML =
      "<h3>We couldn't complete your requested change</h3>" +
      '<p>' + escapeHtml(message) + '</p>' +
      '<div class="actions">' +
      '<button type="button" class="btn btn-primary" id="try-again-btn">Try Again</button>' +
      '<a class="btn btn-secondary" href="/account/bookings/' + encodeURIComponent(confirmation) + '">Return to Booking</a>' +
      '</div>';
    resultContainer.appendChild(panel);
    document.getElementById('try-again-btn').addEventListener('click', function () {
      resultContainer.innerHTML = '';
    });
  }

  function renderTechDetails(data) {
    var eventsHtml = (data.events || []).map(function (ev) {
      return escapeHtml(JSON.stringify(ev));
    }).join('\n');

    techDetailsContainer.innerHTML =
      '<details class="tech-details">' +
      '<summary>View Technical Details</summary>' +
      '<p>Correlation ID: ' + escapeHtml(data.correlationId) + '</p>' +
      '<p>Status: ' + escapeHtml(data.status) + '</p>' +
      '<p>Reconciliation Status: ' + escapeHtml(data.reconciliationStatus) + '</p>' +
      '<pre>' + eventsHtml + '</pre>' +
      '</details>';
  }

  function escapeHtml(str) {
    if (str === null || str === undefined) { return ''; }
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  document.querySelectorAll('input[name="scenario"]').forEach(function (radio) {
    radio.addEventListener('change', function () {
      if (!radio.checked) { return; }
      fetch('/api/demo/scenario', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scenario: radio.value })
      }).catch(function () { /* best-effort demo control */ });
    });
  });

  resetDemoBtn.addEventListener('click', function () {
    if (!confirm('Reset all demo data?')) { return; }
    fetch('/api/demo/reset', { method: 'POST' })
      .then(function () {
        window.location.reload();
      })
      .catch(function () {
        alert('Failed to reset demo data.');
      });
  });
})();
