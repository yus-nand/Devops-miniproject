<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="g" value="${detail.grievance}"/>
<c:set var="pageTitle" value="${g.refNo}"/>
<%@ include file="_header.jspf" %>
<h1 id="detail-heading"><c:out value="${g.refNo}"/></h1>
<c:if test="${param.saved == '1'}">
  <p class="notice" id="saved-message">Your changes were saved.</p>
</c:if>
<dl class="summary">
  <dt>Status</dt>
  <dd id="status"><c:out value="${g.status.label}"/></dd>
  <dt>Title</dt>
  <dd id="title"><c:out value="${g.title}"/></dd>
  <dt>Category</dt>
  <dd id="category"><c:out value="${g.category.label}"/></dd>
  <dt>Policy number</dt>
  <dd id="policy-number"><c:out value="${g.policyNumber}"/></dd>
  <dt>Submitted by</dt>
  <dd id="submitted-by"><c:out value="${detail.policyholderName}"/></dd>
  <dt>Submitted on</dt>
  <dd id="created-on"><c:out value="${g.createdOn}"/></dd>
  <dt>Due date</dt>
  <dd id="due-date"><c:out value="${g.dueOn}"/></dd>
  <dt>Assigned officer</dt>
  <dd id="assigned-officer"><c:out value="${empty detail.officerName ? 'Not assigned yet' : detail.officerName}"/></dd>
  <dt>Description</dt>
  <dd id="description" class="prose"><c:out value="${g.description}"/></dd>
  <c:if test="${not empty g.resolutionNote}">
    <dt>Resolution note</dt>
    <dd id="resolution-note" class="prose"><c:out value="${g.resolutionNote}"/></dd>
  </c:if>
</dl>

<h2>Status history</h2>
<table class="grid" id="history-table">
  <thead>
    <tr>
      <th>When</th>
      <th>Change</th>
      <th>By</th>
      <th>Note</th>
    </tr>
  </thead>
  <tbody>
    <c:forEach var="h" items="${detail.history}">
      <tr class="history-row">
        <td class="col-when"><c:out value="${h.changedAtText}"/></td>
        <td class="col-change"><c:out value="${empty h.fromStatus ? 'New' : h.fromStatus.label}"/> to <c:out value="${h.toStatus.label}"/></td>
        <td class="col-by"><c:out value="${h.changedByName}"/> (<c:out value="${h.changedByRole}"/>)</td>
        <td class="col-note"><c:out value="${h.note}"/></td>
      </tr>
    </c:forEach>
  </tbody>
</table>

<p>
  <c:if test="${detail.editable}">
    <a id="edit-link" href="${pageContext.request.contextPath}/grievances/edit?id=${g.id}">Edit this grievance</a>
    &middot;
  </c:if>
  <c:if test="${sessionScope.user.policyholder}">
    <a id="back-to-list" href="${pageContext.request.contextPath}/grievances/mine">Back to my grievances</a>
  </c:if>
  <c:if test="${not sessionScope.user.policyholder}">
    <a id="back-home" href="${pageContext.request.contextPath}/home">Back to home</a>
  </c:if>
</p>
<%@ include file="_footer.jspf" %>
