<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>My Bookings - Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body>
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="page-wrap">
    <h1>My Bookings</h1>

    <div class="panel">
      <h2>Current Bookings</h2>
      <c:choose>
        <c:when test="${empty currentBookings}">
          <p class="empty-state">No current bookings.</p>
        </c:when>
        <c:otherwise>
          <table class="data-table">
            <thead>
              <tr>
                <th>Confirmation</th>
                <th>Destination</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>Status</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="b" items="${currentBookings}">
                <tr>
                  <td>${b.confirmationNumber()}</td>
                  <td>${b.destination()}</td>
                  <td>${b.checkInDate()}</td>
                  <td>${b.checkOutDate()}</td>
                  <td><span class="badge badge-${fn:toLowerCase(b.status())}">${b.status()}</span></td>
                  <td><fmt:formatNumber value="${b.totalAmount()}" type="currency" currencySymbol="$"/></td>
                  <td><a class="btn btn-secondary" href="/account/bookings/${b.confirmationNumber()}">View</a></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </c:otherwise>
      </c:choose>
    </div>

    <div class="panel">
      <h2>Past Bookings</h2>
      <c:choose>
        <c:when test="${empty pastBookings}">
          <p class="empty-state">No past bookings.</p>
        </c:when>
        <c:otherwise>
          <table class="data-table">
            <thead>
              <tr>
                <th>Confirmation</th>
                <th>Destination</th>
                <th>Check-in</th>
                <th>Check-out</th>
                <th>Status</th>
                <th>Total</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="b" items="${pastBookings}">
                <tr>
                  <td>${b.confirmationNumber()}</td>
                  <td>${b.destination()}</td>
                  <td>${b.checkInDate()}</td>
                  <td>${b.checkOutDate()}</td>
                  <td><span class="badge badge-${fn:toLowerCase(b.status())}">${b.status()}</span></td>
                  <td><fmt:formatNumber value="${b.totalAmount()}" type="currency" currencySymbol="$"/></td>
                  <td><a class="btn btn-secondary" href="/account/bookings/${b.confirmationNumber()}">View</a></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </c:otherwise>
      </c:choose>
    </div>
  </div>
</main>
<jsp:include page="fragments/footer.jsp" />
</body>
</html>
