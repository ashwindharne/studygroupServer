import java.io.IOException;
import java.sql.* ;
import java.lang.String;
import java.util.*;
import java.sql.Timestamp;
import com.google.gson.*;
import java.io.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.Calendar;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;

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
	public Gson gson;
	
	public HashMap<String, User> current_users = new HashMap<String, User>();
	public HashMap<String, String> logged_in_users = new HashMap<String, String>();

	public void init() throws ServletException
	{
		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();
	}

	public void destroy()
	{
		/*  write any servlet cleanup code here or remove this function */
	}
    
	public String getAction(HttpServletRequest request, HttpServletResponse response)
	{
		String requestURLStr = request.getRequestURL().toString();
		String[] reqURLParts = requestURLStr.split("/");
		return reqURLParts[reqURLParts.length - 1];
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
		String action = this.getAction(request, response);

		JsonObject responseJsonObj = new JsonObject();
		if(action.equals("search")) {
			String lat = request.getParameter("lat");
			String lon = request.getParameter("lon");
			String range = request.getParameter("range");
			if(lat == null || lat.isEmpty() || lon == null || lon.isEmpty() || range == null || range.isEmpty()) {
				this.writeError(response); // Return error
				return;
			}
			double rangeKm = Integer.parseInt(range);
			
			// If update location package valid (i.e. user ID exists)
			if(logged_in_users.containsKey(request.getRemoteAddr())) {
				User currentUser = current_users.get(logged_in_users.get(request.getRemoteAddr()));
				currentUser.setLatitude(Integer.parseInt(lat));
				currentUser.setLongitude(Integer.parseInt(lon));
				
				current_users.put(currentUser.getUsername(), currentUser);
				System.out.println("Successfully updated user to: " + currentUser.toJsonString());
				
				Map<String, User> usersInRange = findUsersInRange(currentUser, current_users, rangeKm);
				String usersInRangeStr = gson.toJson(usersInRange.values());
				System.out.println("Users in range are: " + usersInRangeStr);
				
				if (!usersInRange.isEmpty()){
					ServletContext context = getServletContext();
					String cert_path = context.getRealPath("/Certificates.p12");
					String password = "qwerty11"; // TODO: DROP PASSWORD HERE
					ApnsService service = APNS.newService().withCert(cert_path, password).withSandboxDestination().build();

					String payload = APNS.newPayload()
							.alertBody("Someone is in range!")
							.badge(1).sound("default").build();
					String deviceToken = "d13382f02dda1063d7f58b413c9874148981917bb79c94f5d3a9a0590bacfff6"; //TODO: DROP HARD-CODED DEVICE TOKEN HERE
					service.push(deviceToken, payload);
				}
				
				responseJsonObj.addProperty("usersInRange", usersInRangeStr);
			// If update location package invalid, simply ignore the packet and do not respond.
			} else {
				System.out.println("Invalid location update packet was ignored.");
				responseJsonObj.addProperty("usersInRange", "");
			}
		}
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		String responseJson = gson.toJson(responseJsonObj);
		out.println(responseJson);
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
		String action = this.getAction(request, response);
		
		StringBuffer jb = new StringBuffer();
		String line = null;
		try {
			BufferedReader reader = request.getReader();
			while ((line = reader.readLine()) != null)
			  jb.append(line);
		} catch (Exception e) { /*report an error*/ }
		
		JsonUser user;
		try {
			user = gson.fromJson(jb.toString(), JsonUser.class);
		} catch (Exception e) {
			// crash and burn
			throw new IOException("Error parsing JSON request string");
		}
		String username = user.username;
		String password = user.password;
		if(username == null || username.isEmpty() || password == null || password.isEmpty()) {
			this.writeError(response); // Return error
			return;
		}
		
		User currentUser = new User(request.getRemoteAddr(), username);
		
		JsonObject responseJsonObj = new JsonObject();
		if(action.equals("signup") || action.equals("login")) {
			//If registration successful (i.e. ID not in use yet)
			if(!current_users.containsKey(currentUser.getUsername())) {
				if(action.equals("login")){
					this.writeError(response);
					return;
				}
				responseJsonObj.addProperty("hasRegisteredSuccessfully", "true");
				current_users.put(currentUser.getUsername(), currentUser);
				System.out.println("Successfully registered user: " + currentUser.toJsonString());
			} else {
				responseJsonObj.addProperty("hasLoggedInSuccessfully", "true");
			}
			logged_in_users.put(currentUser.getId(), currentUser.getUsername());
			responseJsonObj.addProperty("username", username);
			responseJsonObj.addProperty("remoteAddr", request.getRemoteAddr());
		}
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		String responseJson = gson.toJson(responseJsonObj);
		out.println(responseJson);
		// Register packet contents: {"isRegister": "true", "user": {"id": "X"}}
		// Register response: {"hasRegisteredSuccessfully": "true"} // if false, then id is in use already
		// Location Update Packet contents: {"isRegister": "false", "user": {"id": "X", "longitude": "X", "latitude": "X"}}
		// Location Response Packet contents: {"usersInRange": [{"id": "X", "longitude": "X", "latitude": "X"}, {"id": "X", "longitude": "X", "latitude": "X"}]}"
	}
	
	public void writeError(HttpServletResponse response) throws IOException{
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		
		out.println("Internal Error occurred.");
	}

	private class JsonUser{
		String username;
		String password;
	}
	
	public static Map<String, User> findUsersInRange(User currentUser, Map<String, User> allUsers, double rangeKm) {
		Map<String, User> usersInRange = new HashMap<>();
		
		for(Map.Entry<String, User> otherUserEntry : allUsers.entrySet()) {
			User otherUser = otherUserEntry.getValue();
			if(otherUser.getUsername().equals(currentUser.getUsername())) {
				continue;
			}
			
			double distanceKm = currentUser.getDistance(otherUser);
			System.out.println("Calculated distance between user " + currentUser.getUsername() + " and user " +
							otherUser.getUsername() + " is " + distanceKm + ".");
			if(distanceKm <= rangeKm) {
				usersInRange.put(otherUserEntry.getKey(), otherUser);
			}
		}
		
		return usersInRange;
	}
}

