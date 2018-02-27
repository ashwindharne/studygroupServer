import java.io.*;
import java.net.*;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.util.HashMap;
class StudyGroupServer
{
    public static void main(String args[]) throws Exception
    {
        double rangeKm = 1;
        
        Map<String, User> current_users = new HashMap<>();
        
        DatagramSocket serverSocket = new DatagramSocket(5000);
        byte[] receiveData = new byte[1024]; // TODO: this is prone to buffer overflow, no?
        byte[] sendData = new byte[1024];
        
        GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
        
        while(true)
        {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            
            InetAddress IPAddress = receivePacket.getAddress();
            int port = receivePacket.getPort();
            
            // Register packet contents: {"isRegister": "true", "user": {"id": "X"}}
            // Register response: {"hasRegisteredSuccessfully": "true"} // if false, then id is in use already
            // Location Update Packet contents: {"isRegister": "false", "user": {"id": "X", "longitude": "X", "latitude": "X"}}
            // Location Response Packet contents: {"usersInRange": [{"id": "X", "longitude": "X", "latitude": "X"}, {"id": "X", "longitude": "X", "latitude": "X"}]}"
            String packetData = new String(receivePacket.getData());
            System.out.println("Received packet data: " + packetData);
            
    		JsonParser parser = new JsonParser();
    		JsonReader reader = new JsonReader(new StringReader(packetData));
    		reader.setLenient(true);
    		JsonObject rootObj = parser.parse(reader).getAsJsonObject();
    		boolean isRegister = rootObj.get("isRegister").getAsBoolean();
			User currentUser = gson.fromJson(rootObj.get("user").getAsJsonObject(), User.class);

			String responseJson = "";
			JsonObject responseJsonObj = new JsonObject();
			
			// If register packet arrived
    		if(isRegister) {
				//If registration successful (i.e. ID not in use yet)
    			if(!current_users.containsKey(currentUser.getId())) {
    				responseJsonObj.addProperty("hasRegisteredSuccessfully", "true");
    				current_users.put(currentUser.getId(), currentUser);
    				System.out.println("Successfully registered user: " + currentUser.toJsonString());
    			} else {
    				responseJsonObj.addProperty("hasRegisteredSuccessfully", "false");
    				System.out.println("Could not register: " + currentUser.toJsonString() + "\nUsername probably already in use.");
    			}
    		// If update location packet arrived
    		} else if (!isRegister) {
    			// If update location package valid (i.e. user ID exists)
    			if(current_users.containsKey(currentUser.getId())) {
    				current_users.replace(currentUser.getId(), currentUser);
    				System.out.println("Successfully updated user to: " + currentUser.toJsonString());
    				
    				Map<String, User> usersInRange = findUsersInRange(currentUser, current_users, rangeKm);
    				String usersInRangeStr = gson.toJson(usersInRange.values());
    				System.out.println("Users in range are: " + usersInRangeStr);
    				
    				responseJsonObj.addProperty("usersInRange", usersInRangeStr);
    			// If update location package invalid, simply ignore the packet and do not respond.
    			} else {
    				System.out.println("Invalid location update packet was ignored: " + packetData);
    			}
    		}
    		
    		responseJson = gson.toJson(responseJsonObj);
    		sendData = responseJson.getBytes();
            DatagramPacket sendPacket =
                    new DatagramPacket(sendData, sendData.length, IPAddress, port);
            serverSocket.send(sendPacket);
        }
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