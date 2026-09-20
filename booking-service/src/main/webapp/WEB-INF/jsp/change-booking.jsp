<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Change Booking ${booking.confirmationNumber()} - Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body data-confirmation="${booking.confirmationNumber()}">
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="page-wrap">
    <p class="breadcrumb">
      <a href="/account/bookings">My Bookings</a> &rsaquo;
      <a href="/account/bookings/${booking.confirmationNumber()}">${booking.confirmationNumber()}</a> &rsaquo;
      Change
    </p>

    <h1>Change Your Reservation</h1>

    <div class="panel">
      <h2>Current Reservation</h2>
      <p>${booking.destination()} &mdash; ${booking.checkInDate()} to ${booking.checkOutDate()}</p>
      <p>Status: <span class="badge badge-${fn:toLowerCase(booking.status())}">${booking.status()}</span></p>
      <p>Total: <strong><fmt:formatNumber value="${booking.totalAmount()}" type="currency" currencySymbol="$"/></strong></p>
    </div>

    <div class="panel">
      <h2>New Travel Dates</h2>
      <form id="date-form">
        <div class="field-row">
          <div class="field">
            <label for="new-checkin">New Check-in</label>
            <input type="date" id="new-checkin" name="newCheckIn" value="2027-03-15">
          </div>
          <div class="field">
            <label for="new-checkout">New Check-out</label>
            <input type="date" id="new-checkout" name="newCheckOut" value="2027-03-20">
          </div>
        </div>
        <button type="button" id="check-availability-btn" class="btn btn-primary">Check Availability</button>
        <p class="loading-note" id="availability-loading" hidden>Checking availability with suppliers&hellip; this can take a moment.</p>
        <p class="loading-note" id="availability-error" hidden></p>
      </form>
    </div>

    <div class="panel" id="comparison-panel" hidden>
      <h2>Current Reservation vs. Proposed Reservation</h2>
      <div class="compare-grid">
        <div class="compare-col current">
          <h3>Current</h3>
          <dl>
            <dt>Dates</dt><dd id="cmp-current-dates"></dd>
            <dt>Hotel</dt><dd id="cmp-current-hotel"></dd>
            <dt>Car</dt><dd id="cmp-current-car"></dd>
            <dt>Taxes &amp; Fees</dt><dd id="cmp-current-fees"></dd>
            <dt>Total</dt><dd id="cmp-current-total"></dd>
          </dl>
        </div>
        <div class="compare-col proposed">
          <h3>Proposed</h3>
          <dl>
            <dt>Dates</dt><dd id="cmp-proposed-dates"></dd>
            <dt>Hotel</dt><dd id="cmp-proposed-hotel"></dd>
            <dt>Car</dt><dd id="cmp-proposed-car"></dd>
            <dt>Taxes &amp; Fees</dt><dd id="cmp-proposed-fees"></dd>
            <dt>Total</dt><dd id="cmp-proposed-total"></dd>
          </dl>
        </div>
      </div>

      <p>Price difference: <span id="price-delta" class="price-delta"></span></p>

      <p class="check-line" id="hotel-avail-line"></p>
      <p class="check-line" id="car-avail-line"></p>
      <p class="check-line"><span class="check-ok">Member benefit preserved &#10003;</span> &mdash; <span id="member-benefit-note"></span></p>
      <p class="check-line">Cancellation implications: No penalty in this demonstration scenario.</p>

      <button type="button" id="replace-btn" class="btn btn-primary">Replace Reservation</button>
    </div>

    <div id="result-container"></div>

    <details class="demo-controls">
      <summary><span class="demo-label">Demo</span>Demo Controls</summary>
      <p class="note">Demo controls simulate external supplier behavior.</p>
      <div class="scenario-options">
        <label><input type="radio" name="scenario" value="NORMAL" checked> Normal</label>
        <label><input type="radio" name="scenario" value="CAR_TIMEOUT"> Car Timeout</label>
        <label><input type="radio" name="scenario" value="HOTEL_FAILURE"> Hotel Failure</label>
        <label><input type="radio" name="scenario" value="CAR_AMBIGUOUS"> Ambiguous Car Response</label>
      </div>
      <div class="demo-actions">
        <button type="button" id="reset-demo-btn" class="btn btn-plain">Reset Demo</button>
      </div>
      <div id="tech-details-container"></div>
    </details>
  </div>
</main>

<div class="modal-overlay" id="confirm-modal" hidden>
  <div class="modal-box" role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title">
    <h3 id="confirm-modal-title">Confirm Replacement</h3>
    <p>Your existing reservation will remain active until the replacement is successfully secured.</p>
    <div class="modal-actions">
      <button type="button" id="cancel-replace-btn" class="btn btn-plain">Cancel</button>
      <button type="button" id="confirm-replace-btn" class="btn btn-primary">Confirm Replacement</button>
    </div>
  </div>
</div>

<jsp:include page="fragments/footer.jsp" />
<script src="/static/js/change-booking.js"></script>
</body>
</html>
