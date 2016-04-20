<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
		<title>EAP Cumulative Patch Releases Payload Tracker List</title>
	</head>
	<body>
		<h3>Following Payload tracker lists are configured to review</h3>
		<div class="payload">
			<ul>
				<c:forEach var="i" items="${payloadMap}">
					<h4>
						<li>
							<a href="payloadoverview?payloadName=${i}" id=${i}}> <c:out value="${i}" /></a>
						</li>
					</h4>
				</c:forEach>
			</ul>
		</div>
	</body>
</html>