<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Grievance received"/>
<%@ include file="_header.jspf" %>
<h1 id="confirmation-heading">Grievance received</h1>
<p>Your grievance has been recorded. Keep the reference ID below.</p>
<dl class="summary">
  <dt>Reference ID</dt>
  <dd id="ref-no"><c:out value="${grievance.refNo}"/></dd>
  <dt>Status</dt>
  <dd id="status"><c:out value="${grievance.status.label}"/></dd>
  <dt>Due date</dt>
  <dd id="due-date"><c:out value="${grievance.dueOn}"/></dd>
  <dt>Category</dt>
  <dd><c:out value="${grievance.category.label}"/></dd>
  <dt>Title</dt>
  <dd><c:out value="${grievance.title}"/></dd>
</dl>
<p>We aim to resolve your grievance by the due date, which is 14 days after submission.</p>
<p><a id="back-home" href="${pageContext.request.contextPath}/home">Back to home</a></p>
<%@ include file="_footer.jspf" %>
