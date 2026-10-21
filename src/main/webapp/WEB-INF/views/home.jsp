<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Home"/>
<%@ include file="_header.jspf" %>
<h1 id="welcome">Welcome, <c:out value="${sessionScope.user.fullName}"/></h1>
<p>You are signed in as <strong><c:out value="${sessionScope.user.role}"/></strong>.</p>
<c:if test="${sessionScope.user.officer or sessionScope.user.admin}">
  <p><a id="queue-link" href="${pageContext.request.contextPath}/grievances">All grievances</a></p>
</c:if>
<c:if test="${sessionScope.user.policyholder}">
  <p class="hint" id="home-tip">Open a grievance from My grievances to see its full status history.</p>
</c:if>
<c:if test="${sessionScope.user.policyholder}">
  <p><a id="submit-link" href="${pageContext.request.contextPath}/grievances/new">Submit a grievance</a></p>
  <p><a id="mine-link" href="${pageContext.request.contextPath}/grievances/mine">My grievances</a></p>
</c:if>
<%@ include file="_footer.jspf" %>
