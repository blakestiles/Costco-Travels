<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Operations - Costco Travel Smart Rebook</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/static/css/main.css">
</head>
<body>
<jsp:include page="fragments/header.jsp" />
<main>
  <div class="page-wrap">
    <h1>Internal Operations <span class="badge badge-pending">Demo</span></h1>

    <div class="panel">
      <h2>Supplier Reliability</h2>
      <c:choose>
        <c:when test="${empty reliability}">
          <p class="empty-state">No supplier reliability data yet.</p>
        </c:when>
        <c:otherwise>
          <table class="data-table">
            <thead>
              <tr>
                <th>Supplier</th>
                <th>Attempts</th>
                <th>Successful</th>
                <th>Failed</th>
                <th>Success Rate</th>
                <th>Avg Latency</th>
                <th>P95 Latency</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="r" items="${reliability}">
                <tr>
                  <td>${r.supplierName()}</td>
                  <td>${r.bookingAttempts()}</td>
                  <td>${r.successCount()}</td>
                  <td>${r.failureCount()}</td>
                  <td><fmt:formatNumber value="${r.successRate()}" maxFractionDigits="2" minFractionDigits="2"/>%</td>
                  <td>${r.averageLatencyMs()} ms</td>
                  <td>${r.p95LatencyMs()} ms</td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </c:otherwise>
      </c:choose>
    </div>

    <div class="panel">
      <h2>Recent Transactions</h2>
      <c:choose>
        <c:when test="${empty transactions}">
          <p class="empty-state">No transactions recorded yet.</p>
        </c:when>
        <c:otherwise>
          <table class="data-table">
            <thead>
              <tr>
                <th>Correlation ID</th>
                <th>Booking</th>
                <th>Member</th>
                <th>Operation</th>
                <th>Status</th>
                <th>Reconciliation</th>
                <th>Started</th>
                <th>Duration</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach var="t" items="${transactions}">
                <tr>
                  <td>${t.correlationId()}</td>
                  <td>${t.bookingConfirmation()}</td>
                  <td>${t.memberName()}</td>
                  <td>${t.operation()}</td>
                  <td><span class="badge badge-${fn:toLowerCase(t.status())}">${t.status()}</span></td>
                  <td><span class="badge badge-${fn:toLowerCase(t.reconciliationStatus())}">${t.reconciliationStatus()}</span></td>
                  <td>${t.startedAt()}</td>
                  <td>${t.durationMs()} ms</td>
                  <td><a class="btn btn-secondary" href="/ops/transactions/${t.correlationId()}">View</a></td>
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
