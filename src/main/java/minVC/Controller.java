/**
 * Copyright (C) 2014 Simon Hartcher.
 */
package minvc;

/**
 * Main class.
 *
 * @version 1.0.0
 */

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;

/**
 * Minimal model view controller.
 */
public abstract class Controller extends HttpServlet {

  /**
   * Logger for Controller class.
   */
  private static final Logger LOGGER =
    Logger.getLogger(Controller.class.getName());

  /**
   * Path to layout.jsp.
   */
  public static final String LAYOUT_PATH = "/views/shared/layout.jsp";

  /**
   * Attribute to set when inserting body.
   */
  private static final String BODY_ATTRIBUTE = "body";

  /**
   * Attribute to set when inserting scripts.
   */
  private static final String SCRIPTS_ATTRIBUTE = "scripts";

  /**
   * Attribute to set when inserting styles.
   */
  private static final String STYLES_ATTRIBUTE = "styles";

  /**
   * Attribute to set when inserting title.
   */
  private static final String TITLE_ATTRIBUTE = "title";

  /**
   * Gets the action method name from the URI.
   * @param requestUri The uri requested
   * @return The action name of the controller action to run
   */
  private static String getAction(final URI requestUri) {
    String[] paths = requestUri.getPath().toLowerCase().split("/");
    String actionName = paths[paths.length - 1];
    LOGGER.info("Returning action: " + actionName);

    return actionName;
  }

  /**
   * Gets the action method name from a request string.
   * @param requestString The request path
   * @return The action name of the controller action to run
   */
  private static String getAction(final String requestString) {
    try {
      return getAction(new URI(requestString));
    }
    catch (URISyntaxException e) {
      LOGGER.log(Level.SEVERE, "URI Error", e);
      return null;
    }
  }

  /**
   * Performs a HTTP GET method.
   * @param req Request object
   * @param res Response object
   */
  protected final void doGet(
    final HttpServletRequest req,
    final HttpServletResponse res) {
    this.routeRequest(req, res);
  }

  /**
   * Performs a HTTP POST method.
   * @param req Request object
   * @param res Response object
   */
  protected final void doPost(
    final HttpServletRequest req,
    final HttpServletResponse res) {
    this.routeRequest(req, res);
  }

  /**
   * Gets the action method to be run based on the name of the action.
   * @param actionName The name of the action to run
   * @return The method to run
   */
  private Method getActionMethod(final String actionName) {
    Method[] methods = this.getClass().getDeclaredMethods();
    for (Method m: methods) {
      if (m.getName().equalsIgnoreCase(actionName + "Action")) {
        return m;
      }
    }

    return null;
  }

  /**
   * Renders a view with supplied view data.
   * @param req A HTTP Request
   * @param res A HTTP Response
   * @param viewPath The path of the view
   * @param viewData The data to be displayed on the view
   */
  protected final void view(
    final HttpServletRequest req,
    final HttpServletResponse res,
    final String viewPath,
    final Map<String, Object> viewData) {

    //Create request parameters for each entry in the view data
    for (Map.Entry<String, Object> entry : viewData.entrySet()) {
      req.setAttribute(entry.getKey(), entry.getValue());
    }

    this.view(req, res, viewPath);
  }

  /**
   * Renders the layout for a view.
   * @param req A HTTP Request
   * @param res A HTTP Response
   * @param viewPath The path of the view
   */
  protected final void view(
    final HttpServletRequest req,
    final HttpServletResponse res,
    final String viewPath) {
    req.setAttribute("partialViewMain", viewPath);

    this.renderLayout(req, res);
  }

  /**
   * Determines the route to be invoked by the controller.
   * @param req A HTTP Request
   * @param res A HTTP Response
   */
  private void routeRequest(
    final HttpServletRequest req,
    final HttpServletResponse res) {
    try {
      //get action name
      String actionName = Controller.getAction(req.getRequestURL().toString());

      //find action method on class
      Method actionMethod = this.getActionMethod(actionName);

      //no action found
      if (actionMethod == null) {
        //404 then return
        this.httpNotFound(req, res);
        return;
      }

      actionMethod.invoke(this, req, res);
    }
    catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Action Method Error", e);
      this.httpNotFound(req, res);
    }
  }

  /**
   * Displays a HTTP 404 Page Not Found error page.
   * @param req A HTTP Request
   * @param res A HTTP Response
   */
  protected final void httpNotFound(
    final HttpServletRequest req,
    final HttpServletResponse res) {
    Map<String, Object> viewData = new HashMap<>();
    viewData.put("title", "Http Not Found");

    this.view(req, res, "/views/shared/HttpNotFound.jsp", viewData);
  }

  /**
   * Redirects the response to the given view path.
   * @param req A HTTP Request
   * @param res A HTTP Response
   * @param path The view path
   */
  protected static void redirectToLocal(
    final HttpServletRequest req,
    final HttpServletResponse res,
    final String path) {
    try {
      res.sendRedirect(req.getContextPath() + path);
      return;
    }
    catch (java.io.IOException e) {
      LOGGER.log(Level.SEVERE, "Redirection Error", e);
    }
  }

  /**
   * Renders the layout for the page and display the page.
   * @param req A HTTP Request
   * @param res A HTTP Response
   */
  private void renderLayout(
    final HttpServletRequest req,
    final HttpServletResponse res) {
    try {
      RequestDispatcher rd = req.getRequestDispatcher(LAYOUT_PATH);
      rd.forward(req, res);
      return;
    }
    catch (ServletException e) {
      LOGGER.log(Level.SEVERE, "Servlet Error", e);
    }
    catch (IOException e) {
      LOGGER.log(Level.SEVERE, "IO Error", e);
    }
  }

  /**
   * Gets the filename from a request part.
   *
   * @param part The request part to process
   * @return The file name of the part
   */
  public final String getFileName(final Part part) {
    for (String cd : part.getHeader("content-disposition").split(";")) {
      if (cd.trim().startsWith("filename")) {
        return cd.substring(cd.indexOf('=') + 1).trim()
            .replace("\"", "");
      }
    }
    return null;
  }
}
