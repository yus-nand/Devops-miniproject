<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Submit a grievance"/>
<%@ include file="_header.jspf" %>
<h1>Submit a grievance</h1>
<form method="post" action="${pageContext.request.contextPath}/grievances/new" id="grievance-form" novalidate>
  <input type="hidden" name="_csrf" value="${csrfToken}">

  <%@ include file="_grievance-fields.jspf" %>

  <button type="submit" id="submit-grievance">Submit grievance</button>
</form>
<%@ include file="_footer.jspf" %>
