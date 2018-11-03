import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class IRC {
    private int id;
    private String ip;
    //maintains the IDs and IPs of all nodes that are anchor nodes in a hash map
    private HashMap<Integer, String> anchorNodeList = new HashMap<Integer, String>();
    private InetAddress myIP;


    public static void main(String[] args){
        IRC irc = new IRC();

        // assign system's IP
        irc.myIP = irc.getMyIP();

        //Display IP of IRC for new nodes to know who to connect to and ask for anchor nodes
        System.out.println("MY IP: " + irc.myIP);

        // start the IRC Server which has functions for registering the anchor nodes
        // and to send the anchor node details
        IRCServer ircServer = new IRCServer();
        try {
            ircServer.startServer(irc);
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
