<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="All grievances"/>
<%@ include file="_header.jspf" %>
<h1 id="queue-heading">All grievances</h1>
<form class="filters" id="filter-form" method="get" action="${pageContext.request.contextPath}/grievances">
  <label for="q">Reference ID or policy number</label>
  <input type="text" id="q" name="q" maxlength="60" value="<c:out value="${filter.query}"/>">
  <div class="filter-row">
    <div>
      <label for="status">Status</label>
      <select id="status" name="status">
        <option value="">All statuses</option>
        <c:forEach var="s" items="${statuses}">
          <option value="${s.code}" ${filter.statusCode == s.code ? 'selected' : ''}><c:out value="${s.label}"/></option>
        </c:forEach>
      </select>
    </div>
    <div>
      <label for="category">Category</label>
      <select id="category" name="category">
        <option value="">All categories</option>
        <c:forEach var="cat" items="${categories}">
          <option value="${cat.code}" ${filter.categoryCode == cat.code ? 'selected' : ''}><c:out value="${cat.label}"/></option>
        </c:forEach>
      </select>
    </div>
  </div>
  <label class="check"><input type="checkbox" id="overdue" name="overdue" value="1" ${filter.overdueOnly ? 'checked' : ''}> Overdue only</label>
  <button type="submit" id="search-button">Search</button>
  <c:if test="${filter.active}">
    <a id="clear-filters" href="${pageContext.request.contextPath}/grievances">Clear</a>
  </c:if>
</form>
<c:choose>
  <c:when test="${empty rows}">
    <c:choose>
      <c:when test="${filter.active}">
        <p class="notice" id="empty-message">No grievances match your search. Try a different reference ID, policy number or filter.</p>
      </c:when>
      <c:otherwise>
        <p class="notice" id="empty-message">There are no grievances yet.</p>
      </c:otherwise>
    </c:choose>
  </c:when>
  <c:otherwise>
    <p class="hint" id="result-count"><c:out value="${rows.size()}"/> grievance(s), earliest due date first.</p>
    <table class="grid" id="queue-table">
      <thead>
        <tr>
          <th>Reference ID</th>
          <th>Policy number</th>
          <th>Category</th>
          <th>Title</th>
          <th>Status</th>
          <th>Due date</th>
        </tr>
      </thead>
      <tbody>
        <c:forEach var="row" items="${rows}">
          <tr class="grievance-row${row.overdue ? ' overdue-row' : ''}">
            <td class="col-ref"><a class="ref-link" href="${pageContext.request.contextPath}/grievances/view?id=${row.grievance.id}"><c:out value="${row.grievance.refNo}"/></a></td>
            <td class="col-policy"><c:out value="${row.grievance.policyNumber}"/></td>
            <td class="col-category"><c:out value="${row.grievance.category.label}"/></td>
            <td class="col-title"><c:out value="${row.grievance.title}"/></td>
            <td class="col-status"><c:out value="${row.grievance.status.label}"/></td>
            <td class="col-due"><c:out value="${row.grievance.dueOn}"/>
              <c:if test="${row.overdue}"><span class="badge-overdue">Overdue</span></c:if></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>
<%@ include file="_footer.jspf" %>
