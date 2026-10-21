<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My grievances"/>
<%@ include file="_header.jspf" %>
<h1 id="mine-heading">My grievances</h1>
<c:choose>
  <c:when test="${empty rows}">
    <p class="notice" id="empty-message">You have not submitted any grievances yet.</p>
  </c:when>
  <c:otherwise>
    <table class="grid" id="grievance-table">
      <thead>
        <tr>
          <th>Reference ID</th>
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
<p><a id="new-grievance-link" href="${pageContext.request.contextPath}/grievances/new">Submit a grievance</a></p>
<%@ include file="_footer.jspf" %>
