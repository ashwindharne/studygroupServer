import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.HashMap;
class StudyGroupServer
{
    public static void main(String args[]) throws Exception
    {
        int range = 1; //range in km
        Map<String, User> current_users = new HashMap<>();
        DatagramSocket serverSocket = new DatagramSocket(5000);
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
        while(true)
        {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            if(!current_users.containsKey(receivePacket.getAddress().getHostAddress())){
                String ip = receivePacket.getAddress().getHostAddress();
                User new_user;
                //logic to add a new user to the system
                //add user to system, assign location using given methods in user class.
            }
            else{
                //received a UDP packet from a user, process it and send back a packet to the same address
                //containing the updated list of users within a certain range.
            }
            //the following is code just to show how to get data and send data.
//            String a = new String(receivePacket.getData());
//            InetAddress IPAddress = receivePacket.getAddress();
//            int port = receivePacket.getPort();
//            sendData = a.getBytes();
//            DatagramPacket sendPacket =
//                    new DatagramPacket(sendData, sendData.length, IPAddress, port);
//            serverSocket.send(sendPacket);
        }
    }
}