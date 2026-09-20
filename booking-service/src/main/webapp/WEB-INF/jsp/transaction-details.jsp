<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Transaction ${transaction.correlationId()} - Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body data-change-request-id="${transaction.id()}" data-correlation-id="${transaction.correlationId()}">
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="page-wrap">
    <p class="breadcrumb"><a href="/ops">Operations</a> &rsaquo; ${transaction.correlationId()}</p>

    <h1>Transaction ${transaction.correlationId()}</h1>

    <div class="panel" id="summary-panel">
      <h2>Summary</h2>
      <p>Status: <span class="badge badge-${fn:toLowerCase(transaction.status())}">${transaction.status()}</span></p>
      <p>Reconciliation: <span class="badge badge-${fn:toLowerCase(transaction.reconciliationStatus())}" id="reconciliation-badge">${transaction.reconciliationStatus()}</span></p>
      <p>Old Confirmation: ${transaction.oldConfirmationNumber()} &rarr;
        New Confirmation: ${empty transaction.newConfirmationNumber() ? '&mdash;' : transaction.newConfirmationNumber()}</p>
      <p>Old Total: <fmt:formatNumber value="${transaction.oldTotal()}" type="currency" currencySymbol="$"/> &mdash;
        New Total: <c:choose><c:when test="${empty transaction.newTotal()}">&mdash;</c:when><c:otherwise><fmt:formatNumber value="${transaction.newTotal()}" type="currency" currencySymbol="$"/></c:otherwise></c:choose></p>
      <p>Price Difference: <c:choose><c:when test="${empty transaction.priceDifference()}">&mdash;</c:when><c:otherwise><fmt:formatNumber value="${transaction.priceDifference()}" type="currency" currencySymbol="$"/></c:otherwise></c:choose></p>
      <p>${transaction.memberHeadline()}</p>
      <p>${transaction.memberDetail()}</p>

      <c:if test="${transaction.reconciliationStatus() == 'REQUIRED'}">
        <div id="reconcile-actions">
          <button type="button" id="reconcile-btn" class="btn btn-primary">Reconcile</button>
          <button type="button" id="incident-btn" class="btn btn-secondary">Create Incident</button>
        </div>
        <div id="action-result"></div>
      </c:if>
    </div>

    <div class="panel">
      <h2>Event Timeline</h2>
      <c:choose>
        <c:when test="${empty transaction.events()}">
          <p class="empty-state">No events recorded.</p>
        </c:when>
        <c:otherwise>
          <table class="data-table" id="events-table">
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>Event Type</th>
                <th>Supplier</th>
                <th>Status</th>
                <th>Message</th>
                <th>Latency</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="ev" items="${transaction.events()}">
                <tr>
                  <td>${ev.createdAt()}</td>
                  <td>${ev.eventType()}</td>
                  <td>${empty ev.supplier() ? '&mdash;' : ev.supplier()}</td>
                  <td>${empty ev.status() ? '&mdash;' : ev.status()}</td>
                  <td>${ev.message()}</td>
                  <td><c:choose><c:when test="${empty ev.latencyMs()}">&mdash;</c:when><c:otherwise>${ev.latencyMs()} ms</c:otherwise></c:choose></td>
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
<script src="/static/js/transaction-details.js"></script>
</body>
</html>
