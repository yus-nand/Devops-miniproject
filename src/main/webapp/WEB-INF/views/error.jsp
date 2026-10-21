<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${errorTitle}"/>
<%@ include file="_header.jspf" %>
<h1 id="error-heading"><c:out value="${errorTitle}"/></h1>
<p id="error-message"><c:out value="${errorMessage}"/></p>
<p class="hint" id="error-code">Error <c:out value="${errorStatus}"/></p>
<p><a id="error-home" href="${pageContext.request.contextPath}/home">Back to home</a></p>
<%@ include file="_footer.jspf" %>
