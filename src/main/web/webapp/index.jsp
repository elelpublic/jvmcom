<html>
	<head>
		<title>jvmcom</title>
		<link rel="stylesheet" href="index.css">
	</head>
	<body>

	    <a href=servlet>Go to servlet</a>

	    <p>

            <%@ page import="java.util.Enumeration" %>

            <%
                ServletContext context = getServletContext();

                out.println( "<h1>" + context.getContextPath() + "</h1>");

                /*
                out.println( "<h2>Attributes</h2>" );

                for( Enumeration<String> i = context.getAttributeNames(); i.hasMoreElements(); ) {
                  String name = i.nextElement();
                  out.println( "<b>" + name + "</b>: " + context.getAttribute( name ) + "<br>" );
                }
                */

                out.println( "<h2>Init Parameters</h2>" );

                for( Enumeration<String> i = context.getInitParameterNames(); i.hasMoreElements(); ) {
                  String name = i.nextElement();
                  out.println( "<b>" + name + "</b>: " + context.getInitParameter( name ) + "<br>" );
                }
            %>

	    </p>

	    <%@ page import="com.infodesire.jvmcom.WebAppServer" %>

		<h1>jvmcom</h1>

		<p>
		    Communication between JVM instances.
		</p>

		<h2>Status</h2>

		<table>
		    <tr>
		        <td>Server running</td>
		        <td><%= WebAppServer.isServerRunning() %></td>
		    </tr>
		    <tr>
		        <td>Client connected</td>
		        <td><%= WebAppServer.isClientConnected() %></td>
		    </tr>

		</table>

		<h2>Server</h2>

        <form method=post action="servlet/server/start">

            <label for=port>port</label><br>
            <input type=text name=port id=port value=44000 /><br>

            <button type=submit>Start server</button>

        </form>

        <form method=post action="servlet/server/stop">

            <button type=submit>Stop server</button>

        </form>

        <h2>Client</h2>

        <form method=post action="servlet/client/connect">

            <label for=port>host</label><br>
            <input type=text name=host id=host value=localhost /><br>

            <label for=serverport>server port</label><br>
            <input type=text name=serverport id=serverport value=44000 /><br>

            <button type=submit>Connect to server</button>

        </form>

        <form method=post action="servlet/client/disconnect">

            <button type=submit>Disconnect from server</button>

        </form>

	</body>
</html>