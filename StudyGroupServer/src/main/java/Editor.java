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


import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Servlet implementation class
 *
 */
public class Editor extends HttpServlet {
	private static final String DB_DRIVER = "com.mysql.jdbc.Driver";
	private static final String DB_CONNECTION = "jdbc:mysql://localhost:3306/CS144";
	private static final String DB_USER = "cs144";
	private static final String DB_PASSWORD = "";
	
    /**
     * The Servlet constructor
     * 
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public Editor() {}

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
    	
    	String username = request.getParameter("username");
    	if(username != null && !username.isEmpty())
    	{
    		HttpSession session = request.getSession();
            session.setAttribute("username", username);
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
    	if( action.isEmpty()) {
    		return; // Here we could also pull up an error page.
    	}
    	
    	if(action.equals("list")) {
    		showListPage(request, response);
    	}
    	else if (action.equals("edit") || action.equals("open")) {
    		showEditPage(request, response);
    	}
    	else if (action.equals("preview")) {
            showPreviewPage(request, response);

        }

    }
    
    public void showListPage(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException
    {
    	String username = (String) request.getSession().getAttribute("username");
		if(username != null && !username.isEmpty()) {
			ArrayList<HashMap<String, String>> resultList = getPostsFromMySQL(username);
    		request.setAttribute("listResult", resultList);
    		request.getRequestDispatcher("/list.jsp").forward(request, response);
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

    	if(request.getParameter("deleteBtn") != null) {
    		int postId;
    		try {
    			postId = Integer.parseInt(request.getParameter("deleteBtn"));
    			if(postId > 0) {
    				deletePostFromMySQL(postId);
    			}
				showListPage(request, response);
    		} catch (NumberFormatException e) {
    			return; // Here we could also pull up an error page.
    		}
    	} 
    	else if(request.getParameter("openBtn") != null) {
    		int postId;
    		try {
    			postId = Integer.parseInt(request.getParameter("openBtn"));
    			if(postId > 0) {
    				showEditPage(request, response, postId);
        		}
    		} catch (NumberFormatException e) {
    			return; // Here we could also pull up an error page.
    		}
    	}
    	else if(action.equals("list") || request.getParameter("closeBtn") != null)
        {
            showListPage(request, response);
        }
    	else if (action.equals("edit") || action.equals("open")){
    		showEditPage(request, response);
    	}
        else if (action.equals("back")){
            request.setAttribute("postTitle", request.getParameter("title"));
            request.setAttribute("postBody", request.getParameter("body"));
            backEditPage(request, response);

        }
    	else if(action.equals("save") || request.getParameter("saveBtn") != null)
        {
            Connection dbConnection = null;
            PreparedStatement preparedStatement = null;
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            java.sql.Timestamp date = new java.sql.Timestamp(calendar.getTime().getTime());
            
            String username = (String) request.getSession().getAttribute("username");
            String title = request.getParameter("title");
            String body = request.getParameter("body");
            
            int id = Integer.parseInt(request.getParameter("postid"));
			
            try {
                String query = "insert into Posts (username, postid, title, body, modified, created)" + " values (?, ?, ?, ?, ?, ?)";
                String Updatequery = "update Posts set title=?, body=?, modified=?";

                // update
                if (id > 0) {
                    Updatequery += " where username=? and postid = ?";
                    dbConnection = getDBConnection();
                    preparedStatement = dbConnection.prepareStatement(Updatequery);
                    preparedStatement.setString(1, title);
                    preparedStatement.setString(2, body);
                    preparedStatement.setTimestamp(3, date);
                    preparedStatement.setString(4, username);
                    preparedStatement.setInt(5, id);
                    preparedStatement.executeUpdate();
                }
                else {
    				ArrayList<HashMap<String, String>> postDataList = getPostsFromMySQL(username);
                	if(postDataList != null && postDataList.size() > 0){
                		String lastUsedIdStr = postDataList.get(postDataList.size()-1).get("postid");
                		id = Integer.parseInt(lastUsedIdStr) + 1;
                	} else {
                		id = 1;
                	}
    				
					dbConnection = getDBConnection();
					preparedStatement = dbConnection.prepareStatement(query);
					preparedStatement.setString(1, username);
					preparedStatement.setInt(2, id);
					preparedStatement.setString(3, title);
					preparedStatement.setString(4, body);
					preparedStatement.setTimestamp(5, date);
					preparedStatement.setTimestamp(6, date);
					preparedStatement.executeUpdate();
				}

	        } catch (SQLException e) {
	        	System.out.println(e.getMessage());
	        } finally {
				try { preparedStatement.close(); } catch (Exception e) { /* ignored */ }
				try { dbConnection.close(); } catch (Exception e) { /* ignored */ }
			}
            this.showListPage(request, response);

        }
    	else if(action.isEmpty()) {
    		return; // Here we could also pull up an error page.
    	}
    }


    public boolean deletePostFromMySQL(int postId) {
    	Connection dbConnection = null;
    	PreparedStatement preparedStatement = null;
        try {
        	String queryStr = "DELETE FROM Posts WHERE postid = ?";
        	dbConnection = getDBConnection();
    		preparedStatement = dbConnection.prepareStatement(queryStr);
            preparedStatement.setInt(1, postId);

            int noRowsAffected = preparedStatement.executeUpdate();
            if(noRowsAffected == 1) {
            	return true;
            }
        } catch (SQLException e) {
			System.out.println(e.getMessage());
        } finally {
		    try { preparedStatement.close(); } catch (Exception e) { /* ignored */ }
		    try { dbConnection.close(); } catch (Exception e) { /* ignored */ }
		}
        
        return false;
    }
    
    public void showEditPage(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException
    {
    	String postidStr = (String) request.getParameter("postid");
    	if(postidStr == null || postidStr.isEmpty()) {
    		postidStr = "-1"; // TODO: update here when the save button functionality is in.
    	}

    	int postid = Integer.parseInt(postidStr);
    	
    	showEditPage(request, response, postid);
    }
    
    public void backEditPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        String postidStr = (String) request.getParameter("postid");
        if(postidStr == null || postidStr.isEmpty()) {
            postidStr = "-1"; // TODO: update here when the save button functionality is in.
        }
        int postid = Integer.parseInt(postidStr);
        
        HttpSession session = request.getSession();
        request.setAttribute("postid", session.getAttribute("postId"));
        request.setAttribute("postTitle", session.getAttribute("title"));
        request.setAttribute("postBody", session.getAttribute("body"));
        session.removeAttribute("postId");
        session.removeAttribute("postTitle");
        session.removeAttribute("postBody");

        request.getRequestDispatcher("/edit.jsp").forward(request, response);
    }
    
    public void showEditPage(HttpServletRequest request, HttpServletResponse response, int postid) 
    		throws ServletException, IOException
    {
    	String username = (String) request.getSession().getAttribute("username");
    	if(username == null || username.isEmpty()) {
    		username = (String) request.getAttribute("username");
    	}
    
    	if(postid > 0)
    	{
			ArrayList<HashMap<String, String>> postsList = this.getPostsFromMySQL(username, postid); 
			if(postsList.size() > 0) {
				HashMap<String, String> postData = this.getPostsFromMySQL(username, postid).get(0);
				request.setAttribute("postuser", postData.get("username"));
				request.setAttribute("postTitle", postData.get("title"));
				request.setAttribute("postBody", postData.get("body"));
			}
	    }
    	request.setAttribute("postid", postid);
    	
    	request.getRequestDispatcher("/edit.jsp").forward(request, response);
    }
    
    public void showPreviewPage(HttpServletRequest request, HttpServletResponse response) 
    		throws ServletException, IOException
    {
    	String postidStr = (String) request.getParameter("postid");
    	if(postidStr == null || postidStr.isEmpty()) {
    		postidStr = "-1"; // TODO: update here when the save button functionality is in.
    	}

    	int postid = Integer.parseInt(postidStr);
    	String postTitle = (String) request.getParameter("title");
    	String postBody = (String) request.getParameter("body");
    	
    	HttpSession session = request.getSession();
    	session.setAttribute("postId", postid);
        session.setAttribute("title", postTitle);
        session.setAttribute("body", postBody);
    	
    	String renderedTitle = parseMarkdownToHTML(postTitle);
        String renderedBody = parseMarkdownToHTML(postBody);
        request.setAttribute("rtitle", renderedTitle);
        request.setAttribute("rbody", renderedBody);
        
    	request.getRequestDispatcher("/preview.jsp").forward(request, response);
    }
    
    public ArrayList<HashMap<String, String>> getPostsFromMySQL(String username, int singlePostID) {
    	ArrayList<HashMap<String, String>> listData = new ArrayList<HashMap<String, String>>();
        Connection dbConnection = null;
    	PreparedStatement preparedStatement = null;
    	ResultSet rs = null;
        try {
        	String queryStr = "SELECT * FROM Posts WHERE username = ?";
        	if(singlePostID > 0) {
        		queryStr += " AND postid = ?";
        	}
        	queryStr += " ORDER BY postid ASC";
        	
        	dbConnection = getDBConnection();
    		preparedStatement = dbConnection.prepareStatement(queryStr);
            preparedStatement.setString(1, username);
            if(singlePostID > 0) {
        		preparedStatement.setInt(2, singlePostID);
        	}

            rs = preparedStatement.executeQuery();

            while (rs.next()) {
            	HashMap<String, String> post = new HashMap<String, String>();
            	post.put("username", rs.getString("username"));
            	post.put("postid", rs.getString("postid"));
            	post.put("title", rs.getString("title"));
            	post.put("body", rs.getString("body"));
            	post.put("modified", rs.getString("modified"));
            	post.put("created", rs.getString("created"));
            	listData.add(post);
            }

        } catch (SQLException e) {
			System.out.println(e.getMessage());
        } finally {
		    try { rs.close(); } catch (Exception e) { /* ignored */ }
		    try { preparedStatement.close(); } catch (Exception e) { /* ignored */ }
		    try { dbConnection.close(); } catch (Exception e) { /* ignored */ }
		}
        
        return listData;
    }
    
    public ArrayList<HashMap<String, String>> getPostsFromMySQL(String username) {
    	return this.getPostsFromMySQL(username, -1);
    }
    
    private static Connection getDBConnection() {
		Connection dbConnection = null;

		try {
			Class.forName(DB_DRIVER);
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		
		try {
			dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
			return dbConnection;
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}

		return dbConnection;
	}
    
    public String parseMarkdownToHTML(String markdownStr) {
    	Parser parser = Parser.builder().build();
    	HtmlRenderer renderer = HtmlRenderer.builder().build();
    	
    	String htmlStr = renderer.render(parser.parse(markdownStr));  
    	
    	return htmlStr;
    }
}

