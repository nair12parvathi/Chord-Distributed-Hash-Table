//The Client sessions package
import com.thetransactioncompany.jsonrpc2.client.*;

//The Base package for representing JSON-RPC 2.0 messages
import com.thetransactioncompany.jsonrpc2.*;

//The JSON Smart package for JSON encoding/decoding (optional)

//For creating URLs
import java.net.*;
import java.util.HashMap;
import java.util.Map;


public class NodeSender {
    int requestID = 0;

    public HashMap<String, FingerTableRow> findSuccessor(Node node, int key, String nextIP, String method){

        HashMap<String, FingerTableRow> resultMap = new HashMap<String, FingerTableRow>();
        JSONRPC2Request request = null;
        Map params = new HashMap();
        params.put("key", key);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            String resultMapString = (String)response.getResult();
            String[] arr = resultMapString.split(",");
            FingerTableRow row = new FingerTableRow();
            row.setActualID(Integer.parseInt(arr[1]));
            row.setPointingID(Integer.parseInt(arr[2]));
            row.setPointingIP(arr[3]);
            resultMap.put(arr[0], row);
            return resultMap;
        }
        else {
            System.out.println(response.getError().getMessage());
            return null;
        }


    }

    public HashMap<Integer, String> getAnchorNodes(Node node, String IP, String method){
        // Once the client session object is created, you can use to send a series
        // of JSON-RPC 2.0 requests and notifications to it.

        // Construct new request
        JSONRPC2Request request = null;
        request = new JSONRPC2Request(method, requestID++);

        JSONRPC2Response response = communicate(IP, method, request);
        // Print response result / error


        if (response.indicatesSuccess()) {
            return (HashMap<Integer, String>) response.getResult();
        }
        else {
            System.out.println(response.getError().getMessage());
            return null;
        }

    }

    public String registerAsAnchorNode(Node node, String IP, String method){
        // Once the client session object is created, you can use to send a series
        // of JSON-RPC 2.0 requests and notifications to it.

        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("myID", node.getMyID());
        params.put("myIP", node.getMyIP().getHostAddress());
        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(IP, method, request);
        // Print response result / error
        if (response.indicatesSuccess())
            return (String) response.getResult();
        else {
            System.out.println(response.getError().getMessage());
            return "Request Unsuccessful";
        }


    }


    public void sendFile(Node node, String fileName, String content, String nextIP, int key, String method) {
        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("fileName", fileName);
        params.put("content", content);
        params.put("key", key);
        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess())
            System.out.println((String) response.getResult());
        else {
            System.out.println(response.getError().getMessage());
        }
    }

    public void getFile(Node node, String fileName, String nextIP, String method) {
        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("fileName", fileName);
        //params.put("content", content);
        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            String file = ((String) response.getResult());
            if(file.equals("file does not exist")){
                System.out.println(file);
                return;
            }
            String[] fileArr = file.split(",");
            FileClass fc = new FileClass();
            fc.setFileName(fileArr[0]);
            fc.setFileContent(fileArr[1]);
            node.displayFile(fc);
        }
        else {
            System.out.println(response.getError().getMessage());
        }
    }

    public void nodeGoingOffline(Node node, int nodeGoingOfflineID, String nodeGoingOfflineIP, int neighborID, String neighborIP, String nextIP, int anchorNodeID, String method) {
        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("nodeGoingOfflineID", nodeGoingOfflineID);
        params.put("nodeGoingOfflineIP", nodeGoingOfflineIP);
        params.put("neighborID", neighborID);
        params.put("neighborIP", neighborIP);
        params.put("anchorNodeID", anchorNodeID);
        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());
        }
    }

    public HashMap<String,FingerTableRow> findPointer(int actualID, String nextIP, int anchorNodeID, String method) {
        // Construct new request
        JSONRPC2Request request = null;

//        System.out.println("inside sender findPointer "+ " actualID becomes newNodeID");
        Map params = new HashMap();
        params.put("newNodeID", actualID);
        params.put("anchorNodeID", anchorNodeID);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            String resultHashMapString = (String) response.getResult();
            //update; pointingid; pointingIP
            String[] values = resultHashMapString.split(";");
            FingerTableRow tempRow = new FingerTableRow();
            tempRow.setPointingID(Integer.parseInt(values[1]));
            tempRow.setPointingIP(values[2]);
            HashMap<String,FingerTableRow> resultHashMap = new HashMap<String,FingerTableRow>();
            resultHashMap.put(values[0], tempRow);
            return resultHashMap;
        }
        else {
            System.out.println(response.getError().getMessage());
            return null;
        }
    }

    public void updateTable(int newNodeID, String newNodeIP, int anchorNodeID, String nextIP, String method) {
        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("newNodeID", newNodeID);
        params.put("newNodeIP", newNodeIP);
        params.put("anchorNodeID", anchorNodeID);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nextIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());

        }
        else {
            System.out.println(response.getError().getMessage());

        }
    }

    public void getMyFiles(int newNodeID, String neighborIP, String method) {

        // Construct new request
        JSONRPC2Request request = null;


        request = new JSONRPC2Request(method, requestID++);

        JSONRPC2Response response = communicate(neighborIP, method, request);
        // Print response result / error


        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());

        }


    }

    public void notifyNewNode(String newNodeIP, String method) {

        // Construct new request
        JSONRPC2Request request = null;


        request = new JSONRPC2Request(method, requestID++);

        JSONRPC2Response response = communicate(newNodeIP, method, request);
        // Print response result / error


        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());

        }

    }

    public void notifyLookupServerOfGoingOffline(Node node, int nodeGoingOffline, String lookupServerIP, String method) {
        // Construct new request
        JSONRPC2Request request = null;

        Map params = new HashMap();
        params.put("nodeGoingOffline", nodeGoingOffline);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(lookupServerIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());

        }
    }


    public void notifyNodeGoingOffline(String nodeGoingOfflineIP, String neighborIP, String method) {
        // Construct new request
        JSONRPC2Request request = null;
        Map params = new HashMap();
        params.put("neighborIP", neighborIP);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(nodeGoingOfflineIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());

        }
    }

    public void transferAll(Node node, String nodeGoingOfflineIP,  String fileListString, String neighborIP, String method) {
        // Construct new request
        JSONRPC2Request request = null;
        Map params = new HashMap();
        params.put("nodeGoingOfflineIP", nodeGoingOfflineIP);
        params.put("fileListString", fileListString);

        request = new JSONRPC2Request(method, params, requestID++);

        JSONRPC2Response response = communicate(neighborIP, method, request);
        // Print response result / error
        if (response.indicatesSuccess()){
            System.out.println((String) response.getResult());
        }
        else {
            System.out.println(response.getError().getMessage());

        }
    }

    public JSONRPC2Response communicate(String IP, String method, JSONRPC2Request request) {
        // Creating a new session to a JSON-RPC 2.0 web service at a specified URL
        // The JSON-RPC 2.0 server URL
        URL serverURL = null;

        try {
            //serverURL = new URL("http://127.0.0.1:8080");
            serverURL = new URL("http://"+IP+":8080");

        } catch (MalformedURLException e) {
            // handle exception...
        }

        // Create new JSON-RPC 2.0 client session
        JSONRPC2Session mySession = new JSONRPC2Session(serverURL);

        // Send request
        JSONRPC2Response response = null;

        try {
            response = mySession.send(request);

        } catch (JSONRPC2SessionException e) {

            System.err.println(e.getMessage());
            // handle exception...
        }

        return response;

    }


}