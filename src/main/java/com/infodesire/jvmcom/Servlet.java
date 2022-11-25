package com.infodesire.jvmcom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Enumeration;

public class Servlet extends HttpServlet {

  private static Logger logger = LoggerFactory.getLogger( "Server" );

  public void init() {
    logger.info( "Servlet init" );
  }

  public void doGet( HttpServletRequest request, HttpServletResponse response)
    throws IOException {

    // Set response content type
    response.setContentType("text/html");

    // Actual logic goes here.
    PrintWriter out = response.getWriter();
    out.println("<h1>Hello World</h1>");
    out.println("Sent from servlet");

    ServletContext context = getServletContext();

    out.println( "<h1>" + context.getContextPath() + "</h1>");

    out.println( "<h2>Attributes</h2>" );

    for( Enumeration<String> i = context.getAttributeNames(); i.hasMoreElements(); ) {
      String name = i.nextElement();
      out.println( "<b>" + name + "</b>: " + context.getAttribute( name ) + "<br>" );
    }

    out.println( "<h2>Init Parameters</h2>" );

    for( Enumeration<String> i = context.getInitParameterNames(); i.hasMoreElements(); ) {
      String name = i.nextElement();
      out.println( "<b>" + name + "</b>: " + context.getInitParameter( name ) + "<br>" );
    }

  }

  public void doPost( HttpServletRequest request, HttpServletResponse response)
    throws IOException {

    String portParameter = request.getParameter( "port" );
    String hostParameter = request.getParameter( "host" );
    String serverPortParameter = request.getParameter( "serverport" );

    if( portParameter != null ) {
      WebAppServer.setPort( Integer.parseInt( portParameter ) );
    }
    if( hostParameter != null ) {
      WebAppServer.setHost( hostParameter );
    }
    if( serverPortParameter != null ) {
      WebAppServer.setServerPort( Integer.parseInt( serverPortParameter ) );
    }

    if( request.getRequestURI().endsWith( "/server/start" ) ) {
      WebAppServer.startServer();
    }
    else if( request.getRequestURI().endsWith( "/server/stop" ) ) {
      WebAppServer.stopServer();
    }
    else if( request.getRequestURI().endsWith( "/client/connect" ) ) {
      WebAppServer.connectClient();
    }
    else if( request.getRequestURI().endsWith( "/client/disconnect" ) ) {
      WebAppServer.disconnectClient();
    }

    response.sendRedirect( "/jvmcom" );

  }

  public void destroy() {
    logger.info( "Servlet destroy" );
  }

}
