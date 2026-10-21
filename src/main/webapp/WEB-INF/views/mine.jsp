<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My grievances"/>
<%@ include file="_header.jspf" %>
<h1 id="mine-heading">My grievances</h1>
<c:choose>
  <c:when test="${empty grievances}">
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
        <c:forEach var="g" items="${grievances}">
          <tr class="grievance-row">
            <td class="col-ref"><c:out value="${g.refNo}"/></td>
            <td class="col-category"><c:out value="${g.category.label}"/></td>
            <td class="col-title"><c:out value="${g.title}"/></td>
            <td class="col-status"><c:out value="${g.status.label}"/></td>
            <td class="col-due"><c:out value="${g.dueOn}"/></td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </c:otherwise>
</c:choose>
<p><a id="new-grievance-link" href="${pageContext.request.contextPath}/grievances/new">Submit a grievance</a></p>
<%@ include file="_footer.jspf" %>
