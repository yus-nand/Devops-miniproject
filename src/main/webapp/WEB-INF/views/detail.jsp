<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="g" value="${detail.grievance}"/>
<c:set var="pageTitle" value="${g.refNo}"/>
<%@ include file="_header.jspf" %>
<h1 id="detail-heading"><c:out value="${g.refNo}"/></h1>
<c:if test="${param.saved == '1'}">
  <p class="notice" id="saved-message">Your changes were saved.</p>
</c:if>
<c:if test="${param.updated == '1'}">
  <p class="notice" id="updated-message">The status was updated.</p>
</c:if>
<c:if test="${not empty actionError}">
  <p class="error" id="action-error" role="alert"><c:out value="${actionError}"/></p>
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
  <dd id="due-date"><c:out value="${g.dueOn}"/>
    <c:if test="${overdue}"><span class="badge-overdue" id="overdue-flag">Overdue</span></c:if></dd>
  <dt>Assigned officer</dt>
  <dd id="assigned-officer"><c:out value="${empty detail.officerName ? 'Not assigned yet' : detail.officerName}"/></dd>
  <dt>Description</dt>
  <dd id="description" class="prose"><c:out value="${g.description}"/></dd>
  <c:if test="${not empty g.resolutionNote}">
    <dt>Resolution note</dt>
    <dd id="resolution-note" class="prose"><c:out value="${g.resolutionNote}"/></dd>
  </c:if>
</dl>

<c:if test="${actions.any}">
  <section class="actions" id="actions">
    <h2>Actions</h2>
    <c:if test="${actions.canPickUp}">
      <form method="post" action="${pageContext.request.contextPath}/grievances/pickup" id="pickup-form">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <input type="hidden" name="id" value="${g.id}">
        <button type="submit" id="pickup-button">Pick up this grievance</button>
      </form>
    </c:if>
    <c:if test="${not empty actions.noteMoves}">
      <form method="post" action="${pageContext.request.contextPath}/grievances/status" id="note-form" novalidate>
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <input type="hidden" name="id" value="${g.id}">
        <label for="note"><c:out value="${actions.noteLabel}"/></label>
        <textarea id="note" name="note" rows="4" maxlength="1000"><c:out value="${noteText}"/></textarea>
        <c:forEach var="m" items="${actions.noteMoves}">
          <button type="submit" name="to" value="${m.to.code}" id="move-${m.to.code}"><c:out value="${m.label}"/></button>
        </c:forEach>
      </form>
    </c:if>
    <c:forEach var="m" items="${actions.plainMoves}">
      <form method="post" action="${pageContext.request.contextPath}/grievances/status">
        <input type="hidden" name="_csrf" value="${csrfToken}">
        <input type="hidden" name="id" value="${g.id}">
        <button type="submit" name="to" value="${m.to.code}" id="move-${m.to.code}"><c:out value="${m.label}"/></button>
      </form>
    </c:forEach>
  </section>
</c:if>

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
    <a id="back-to-queue" href="${pageContext.request.contextPath}/grievances">Back to all grievances</a>
  </c:if>
</p>
<%@ include file="_footer.jspf" %>
