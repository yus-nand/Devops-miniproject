<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Log in"/>
<%@ include file="_header.jspf" %>
<h1>Log in</h1>
<c:if test="${not empty error}">
  <p class="error" id="login-error" role="alert"><c:out value="${error}"/></p>
</c:if>
<form method="post" action="${pageContext.request.contextPath}/login" id="login-form">
  <input type="hidden" name="_csrf" value="${csrfToken}">
  <label for="username">Username</label>
  <input type="text" id="username" name="username" value="<c:out value='${username}'/>" required autofocus>
  <label for="password">Password</label>
  <input type="password" id="password" name="password" required>
  <button type="submit" id="login-button">Log in</button>
</form>
<p class="hint">Demo accounts (password <code>demo123</code>): asha and ravi are policyholders, officer1 is an officer, admin1 is an admin.</p>
<%@ include file="_footer.jspf" %>
