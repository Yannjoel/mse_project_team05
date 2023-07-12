<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<body>
<h2>Results for ${query}</h2>
</a>
<form action= "/search">
    <label for="query">Your search query:</label><br>
    <input type="text" id="query" name="query"><br>
    <input type="submit" value="Submit">
</form>
<br>
<table>
    <c:forEach items="${results}" var="result">
        <tr>
            <td>Url: <c:out value="${result.website.url}"/></td>
            <td>Score: <c:out value="${result.ranking}"/></td>
        </tr>
    </c:forEach>
<table>
</body>
</html>
