<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>Grievance Portal - setup check</title>
</head>
<body>
  <h1>Policyholder Grievance Portal</h1>
  <p id="setup-status">Local setup OK</p>
  <ul>
    <li>Server: <%= application.getServerInfo() %></li>
    <li>Java: <%= System.getProperty("java.version") %></li>
    <li>Servlet API: <%= application.getMajorVersion() %>.<%= application.getMinorVersion() %></li>
  </ul>
</body>
</html>