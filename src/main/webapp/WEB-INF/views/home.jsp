<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Home"/>
<%@ include file="_header.jspf" %>
<h1 id="welcome">Welcome, <c:out value="${sessionScope.user.fullName}"/></h1>
<p>You are signed in as <strong><c:out value="${sessionScope.user.role}"/></strong>.</p>
<%@ include file="_footer.jspf" %>
