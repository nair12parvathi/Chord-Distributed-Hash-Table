import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class LookupServer {
    private int id;
    private String ip;
    //maintains the IDs and IPs of all nodes that are anchor nodes in a hash map
    private HashMap<Integer, String> anchorNodeList = new HashMap<Integer, String>();
    private InetAddress myIP;


    public static void main(String[] args){
        LookupServer lookupServer = new LookupServer();

        // assign system's IP
        lookupServer.myIP = lookupServer.getMyIP();

        //Display IP of LookupServer for new nodes to know who to connect to and ask for anchor nodes
        System.out.println("MY IP: " + lookupServer.myIP);

        // start the LookupServer Server which has functions for registering the anchor nodes
        // and to send the anchor node details
        LookupServerListener lookupServerListener = new LookupServerListener();
        try {
            lookupServerListener.startServer(lookupServer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //get the system's IP
    public InetAddress getMyIP(){
        InetAddress IP =null;
        try {
            IP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return IP;
    }

    public void addNodeToAnchorNodeList(int nodeID, String nodeIP){
        anchorNodeList.put(nodeID, nodeIP);
    }

    public HashMap<Integer, String> getAnchorNodeList(){
        return this.anchorNodeList;
    }

    public void deleteAnchorNode(int nodeGoingOffline) {
        anchorNodeList.remove(nodeGoingOffline);
    }
}
