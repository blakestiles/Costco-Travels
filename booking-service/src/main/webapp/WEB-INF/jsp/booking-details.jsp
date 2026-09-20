<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Booking ${booking.confirmationNumber()} - Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body>
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="page-wrap">
    <p class="breadcrumb"><a href="/account/bookings">My Bookings</a> &rsaquo; ${booking.confirmationNumber()}</p>

    <div class="panel">
      <h1>${booking.destination()}
        <span class="badge badge-${fn:toLowerCase(booking.status())}">${booking.status()}</span>
      </h1>
      <p>${booking.checkInDate()} &ndash; ${booking.checkOutDate()}</p>
      <p>Confirmation: <strong>${booking.confirmationNumber()}</strong></p>
      <p>Member: ${booking.memberName()} &mdash; ${booking.membershipType()}</p>
      <p>Total: <strong><fmt:formatNumber value="${booking.totalAmount()}" type="currency" currencySymbol="$"/> ${booking.currency()}</strong></p>
      <p>
        <a class="btn btn-primary" href="/account/bookings/${booking.confirmationNumber()}/change">Change This Booking</a>
        <a class="btn btn-secondary" href="/account/bookings">Back to My Bookings</a>
      </p>
    </div>

    <div class="panel">
      <h2>Itinerary Items</h2>
      <table class="data-table">
        <thead>
          <tr>
            <th>Type</th>
            <th>Supplier</th>
            <th>Description</th>
            <th>Supplier Confirmation</th>
            <th>Status</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="item" items="${booking.items()}">
            <tr>
              <td>${item.itemType()}</td>
              <td>${item.supplier()}</td>
              <td>${item.description()}</td>
              <td>${empty item.supplierConfirmation() ? '&mdash;' : item.supplierConfirmation()}</td>
              <td><span class="badge badge-${fn:toLowerCase(item.status())}">${item.status()}</span></td>
              <td><fmt:formatNumber value="${item.amount()}" type="currency" currencySymbol="$"/></td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </div>
</main>
<jsp:include page="fragments/footer.jsp" />
</body>
</html>
