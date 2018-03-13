import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class User {
    private double latitude;
    private double longitude;
    private String id;
	private String passwordHash;

    public User(String auth_id){
        id=auth_id;
    }
    
    public User(String auth_id, double lat, double lon){
        id=auth_id;
        latitude=lat;
        longitude=lon;
    }//constructor, if necessary give it an ID and initial lat/lon.
    
    public String getId(){//getters and setters
        return id;
    }
    
    public double getLatitude(){
        return latitude;
    }
    
    public double getLongitude(){
        return longitude;
    }
    
    public void setLatitude(double new_lat){
        latitude=new_lat;
    }
    
    public void setLongitude(double new_long){
        longitude=new_long;
    }
	
	public void setPassword(String passwordStr) {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(passwordStr.getBytes());
		this.passwordHash = new String(messageDigest.digest());
	}
	
	public boolean checkPassword(String passwordStr) {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
		messageDigest.update(passwordStr.getBytes());
		return this.passwordHash.equals(new String(messageDigest.digest()));
	}
	
	private void getPassword() {
		return this.passwordHash;
	}
    
    public double getDistance(User other){//gets the distance between this user and another user, using lat&long
        double R = 6371;
        double lat_diff=deg2rad(latitude-other.getLatitude());
        double lon_diff=deg2rad(longitude-other.getLongitude());
        double a = Math.sin(lat_diff / 2) * Math.sin(lat_diff / 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.getLatitude()))
                * Math.sin(lon_diff / 2) * Math.sin(lon_diff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R*c;
    }
    
    private double deg2rad(double deg){
        return deg*(Math.PI/180);
    }
    
    public String toJsonString(){
    	GsonBuilder builder = new GsonBuilder();
		Gson gson = builder.create();
		
    	return gson.toJson(this);
    }
}
