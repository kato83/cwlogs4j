<%@ page contentType="application/json; charset=UTF-8" %><%
%><%@ page import="com.pu10g.cwlogs4j.JsLogger" %><%
    request.setCharacterEncoding("UTF-8");
    try {
    JsLogger.sendLog(request, response);
    } catch (Exception e) {
        e.printStackTrace();
    }
%>