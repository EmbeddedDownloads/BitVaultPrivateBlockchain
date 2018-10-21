<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="/WEB-INF/taglib.tld" prefix="t"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<!-- <meta http-equiv="refresh" content="1"> -->
<title>PBC Logger</title>
</head>

<script>
	function autoScrolling() {
		window.scrollTo(0, document.body.scrollHeight);
	}

	setInterval(autoScrolling, 1000);
</script>

<body>

	<t:tail
		file="/home/linchpinub4/Documents/apache-tomcat-8.0.33/logs/pbc_reports.log"
		count="50" id="S">
		<br><%=S%>
	</t:tail>

	<!--Script for Scroll to bottom -->
	<script type="text/javascript">
		setInterval(function() {
		}, 1000);
	</script>

</body>
</html>