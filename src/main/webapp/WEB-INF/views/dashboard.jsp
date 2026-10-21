<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Dashboard"/>
<%@ include file="_header.jspf" %>
<h1 id="dashboard-heading">Dashboard</h1>
<p class="hint">Click a number to see exactly those grievances in the list.</p>
<div class="tiles">
  <div class="tile">
    <span class="tile-label">All grievances</span>
    <a class="tile-number" id="total-count" href="${pageContext.request.contextPath}/grievances"><c:out value="${summary.total}"/></a>
  </div>
  <div class="tile${summary.overdue > 0 ? ' tile-alert' : ''}">
    <span class="tile-label">Overdue</span>
    <a class="tile-number" id="overdue-count" href="${pageContext.request.contextPath}/grievances?overdue=1"><c:out value="${summary.overdue}"/></a>
  </div>
</div>

<h2>By status</h2>
<table class="grid" id="status-table">
  <thead>
    <tr><th>Status</th><th>Grievances</th></tr>
  </thead>
  <tbody>
    <c:forEach var="entry" items="${summary.byStatus}">
      <tr class="status-row" id="status-${entry.key.code}">
        <td><c:out value="${entry.key.label}"/></td>
        <td class="col-count"><a href="${pageContext.request.contextPath}/grievances?status=${entry.key.code}"><c:out value="${entry.value}"/></a></td>
      </tr>
    </c:forEach>
  </tbody>
</table>

<h2>By category</h2>
<table class="grid" id="category-table">
  <thead>
    <tr><th>Category</th><th>Grievances</th></tr>
  </thead>
  <tbody>
    <c:forEach var="entry" items="${summary.byCategory}">
      <tr class="category-row" id="category-${entry.key.code}">
        <td><c:out value="${entry.key.label}"/></td>
        <td class="col-count"><a href="${pageContext.request.contextPath}/grievances?category=${entry.key.code}"><c:out value="${entry.value}"/></a></td>
      </tr>
    </c:forEach>
  </tbody>
</table>
<p><a id="back-home" href="${pageContext.request.contextPath}/home">Back to home</a></p>
<%@ include file="_footer.jspf" %>
