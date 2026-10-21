<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Edit grievance"/>
<%@ include file="_header.jspf" %>
<h1 id="edit-heading">Edit <c:out value="${grievance.refNo}"/></h1>
<p class="hint">You can edit this grievance until an officer starts reviewing it. The reference ID and due date do not change.</p>
<form method="post" action="${pageContext.request.contextPath}/grievances/edit" id="grievance-form" novalidate>
  <input type="hidden" name="_csrf" value="${csrfToken}">
  <input type="hidden" name="id" value="${grievance.id}">

  <%@ include file="_grievance-fields.jspf" %>

  <button type="submit" id="save-grievance">Save changes</button>
  <a id="cancel-link" href="${pageContext.request.contextPath}/grievances/view?id=${grievance.id}">Cancel</a>
</form>
<%@ include file="_footer.jspf" %>
