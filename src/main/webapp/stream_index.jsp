<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta http-equiv="X-UA-Compatible" content="IE=edge">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
        <title>EAP Cumulative Patch Releases ${title}</title>

        <!-- Bootstrap -->
        <link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet">
        <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
        <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
        <!--[if lt IE 9]>
            <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
            <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
        <![endif]-->
    </head>

    <body>
        <div class="container">
            <div class="row">
                <div class="col-md-12"><h1>EAP Cumulative Patch Releases ${title}</h1></div>
            </div>
            <div class="payload">
                <a href="../">Home</a>
                <ul>
                <c:forEach var="i" items="${streamMap}">
                    <h4>
                        <li>
                            ${i.key}
                            <ul>
                            <c:forEach var="j" items="${i.value}">
                                <li>
                                    <a href="${first}/${i.key}/${second}/${j}" id=${j}>${j}</a>
                                </li>
                            </c:forEach>
                            </ul>
                        </li>
                    </h4>
                </c:forEach>
                </ul>
            </div>
        </div>
    </body>
</html>
