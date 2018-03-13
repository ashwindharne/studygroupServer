import java.io.IOException;
import java.sql.* ;
import java.lang.String;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.sql.Timestamp;
import java.util.TimeZone;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.Calendar;

/**
 * Servlet implementation class
 *
 */
public class StudyGroupServer extends HttpServlet {
	
	/**
	 * The Servlet constructor
	 * 
	 * @see javax.servlet.http.HttpServlet#HttpServlet()
	 */
	public StudyGroupServer() {}
	
	public Map<String, User> current_users = new HashMap<>();

	public void init() throws ServletException
	{
		/*  write any servlet initialization code here or remove this function */
	}

	public void destroy()
	{
		/*  write any servlet cleanup code here or remove this function */
	}
    
	public String getActionAndSetSessionParams(HttpServletRequest request, HttpServletResponse response)
	{
		String action = request.getParameter("action");
		if(action == null || action.isEmpty()) {
			return "";
		}
		
		return action;
	}

	/**
	 * Handles HTTP GET requests
	 * 
	 * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
	 *      HttpServletResponse response)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException 
	{
		String action = this.getActionAndSetSessionParams(request, response);
		String username = request.getParameter("username");
		if(username == null || username.isEmpty()) {
			return; // Return error
		}
		if(!action.equals("register")) {
			JsonObject responseJsonObj = new JsonObject();
			// If update location package valid (i.e. user ID exists)
			if(current_users.containsKey(currentUser.getId())) {
				current_users.replace(currentUser.getId(), currentUser);
				System.out.println("Successfully updated user to: " + currentUser.toJsonString());
				
				Map<String, User> usersInRange = findUsersInRange(currentUser, current_users, rangeKm);
				String usersInRangeStr = gson.toJson(usersInRange.values());
				System.out.println("Users in range are: " + usersInRangeStr);
				
				responseJsonObj.addProperty("usersInRange", usersInRangeStr);
				
				response.setContentType("text/html");
				PrintWriter out = response.getWriter();
				
				String responseJson = gson.toJson(responseJsonObj);
				out.println(responseJson);
			// If update location package invalid, simply ignore the packet and do not respond.
			} else {
				System.out.println("Invalid location update packet was ignored: " + packetData);
				continue;
			}
		} else {
			
		}
	}

	/**
	 * Handles HTTP POST requests
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
	 *      HttpServletResponse response)
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException 
	{
		String action = this.getActionAndSetSessionParams(request, response);
		String username = request.getParameter("username");
		if(username == null || username.isEmpty()) {
			return; // Return error
		
		JsonObject responseJsonObj = new JsonObject();
		if(action.equals("register")) {
			//If registration successful (i.e. ID not in use yet)
			if(!current_users.containsKey(currentUser.getId())) {
				responseJsonObj.addProperty("hasRegisteredSuccessfully", "true");
				current_users.put(currentUser.getId(), currentUser);
				System.out.println("Successfully registered user: " + currentUser.toJsonString());
			} else {
				responseJsonObj.addProperty("hasRegisteredSuccessfully", "false");
				System.out.println("Could not register: " + currentUser.toJsonString() + "\nUsername probably already in use.");
			}
			
			response.setContentType("text/html");
			PrintWriter out = response.getWriter();
			
			String responseJson = gson.toJson(responseJsonObj);
			out.println(responseJson);
		}
		// Register packet contents: {"isRegister": "true", "user": {"id": "X"}}
		// Register response: {"hasRegisteredSuccessfully": "true"} // if false, then id is in use already
		// Location Update Packet contents: {"isRegister": "false", "user": {"id": "X", "longitude": "X", "latitude": "X"}}
		// Location Response Packet contents: {"usersInRange": [{"id": "X", "longitude": "X", "latitude": "X"}, {"id": "X", "longitude": "X", "latitude": "X"}]}"
	}

	public static Map<String, User> findUsersInRange(User currentUser, Map<String, User> allUsers, double rangeKm) {
		Map<String, User> usersInRange = new HashMap<>();
		
		for(Map.Entry<String, User> otherUserEntry : allUsers.entrySet()) {
			User otherUser = otherUserEntry.getValue();
			if(otherUser.getId().equals(currentUser.getId())) {
				continue;
			}
			
			double distanceKm = currentUser.getDistance(otherUser);
			System.out.println("Calculated distance between user " + currentUser.getId() + " and user " +
							otherUser.getId() + " is " + distanceKm + ".");
			if(distanceKm <= rangeKm) {
				usersInRange.put(otherUserEntry.getKey(), otherUser);
			}
		}
		
		return usersInRange;
	}
}

