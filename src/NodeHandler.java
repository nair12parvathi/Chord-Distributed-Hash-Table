//package edu.rit.csci759.jsonrpc.server;

/**
* Demonstration of the JSON-RPC 2.0 Server framework usage. The request
* handlers are implemented as static nested classes for convenience, but in
* real life applications may be defined as regular classes within their old
* source files.
*
* @author Vladimir Dzhuvinov
* @version 2011-03-05
*/

import java.text.DateFormat;
import java.util.*;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;

public class NodeHandler {




	 // Implements a handler for "getDate" and "getTime" JSON-RPC methods
	 // that return the current date and time
	 public static class AllRequestHandler implements RequestHandler {
        Node node;

         public AllRequestHandler(Node node) {
             this.node = node;
         }

	     // Reports the method names of the handled requests
		public String[] handledRequests() {

		    return new String[]{"downloadFile", "findSuccessor", "getFile", "goOffline", "findPointer", "updateTable", "getMyFiles", "updateFileList", "transferFiles", "downloadAll"};
		}


		// Processes the requests
		public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {
            if(req.getMethod().equals("downloadFile")){
                Map params = (Map) req.getNamedParams();
                String fileName = (String) params.get("fileName").toString().trim();
                String content = (String) params.get("content").toString().trim();
                int key = Integer.parseInt((String) params.get("key").toString().trim());
                node.downloadFile(fileName, content, key);
                return new JSONRPC2Response("successfully transferred the file to destination", req.getID());
            }

            else if(req.getMethod().equals("goOffline")){
                Map params = (Map) req.getNamedParams();

                int nodeGoingOfflineID = Integer.parseInt((String) params.get("nodeGoingOfflineID").toString().trim());
                String nodeGoingOfflineIP = (String) params.get("nodeGoingOfflineIP").toString().trim();
                int neighborID = Integer.parseInt((String) params.get("neighborID").toString().trim());
                String neighborIP = (String) params.get("neighborIP").toString().trim();
                int anchorNodeID = Integer.parseInt((String) params.get("anchorNodeID").toString().trim());
                node.updateTableOnNodeDeletion(nodeGoingOfflineID, nodeGoingOfflineIP, neighborID, neighborIP, anchorNodeID);
                return new JSONRPC2Response("successfully transferred the file to successor", req.getID());
            }
            else if(req.getMethod().equals("getFile")){
                Map params = (Map) req.getNamedParams();
                String fileName = (String) params.get("fileName").toString().trim();
                FileClass fc = node.getFileFromFileList(fileName);
                String file ="";
                if(fc == null){
                    file = "file does not exist";
                }
                else{
                    file = ""+fc.getFileName()+","+fc.getFileContent();
                }
                return new JSONRPC2Response(file, req.getID());
            }

            else if(req.getMethod().equals("getMyFiles")){

                node.sendNeighborFiles();
                String result = "Files(if any) retrieved successfully";

                return new JSONRPC2Response(result, req.getID());
            }

            else if(req.getMethod().equals("updateFileList")){

                node.updateFileList();
                String result = "Update fileList";

                return new JSONRPC2Response(result, req.getID());
            }
             else if(req.getMethod().equals("findPointer")){
                Map params = (Map) req.getNamedParams();
                String resultHashMapString="";

                int newNodeID = Integer.parseInt((String) params.get("newNodeID").toString().trim());
                int anchorNodeID = Integer.parseInt((String) params.get("anchorNodeID").toString().trim());
                HashMap<String, FingerTableRow> resultHashMap = node.findPointer(newNodeID, anchorNodeID);

                String status = null;
                FingerTableRow resultRow = null;
                for(Map.Entry<String, FingerTableRow> entry: resultHashMap.entrySet()){
                    status = entry.getKey();
                    resultRow = entry.getValue();
                }
                resultHashMapString = resultHashMapString+status+";"+resultRow.getPointingID()+";"+resultRow.getPointingIP();

                return new JSONRPC2Response(resultHashMapString, req.getID());
            }

            else if(req.getMethod().equals("updateTable")){
                Map params = (Map) req.getNamedParams();
                int newNodeID = Integer.parseInt((String) params.get("newNodeID").toString().trim());
                int anchorNodeID = Integer.parseInt((String) params.get("anchorNodeID").toString().trim());
                String newNodeIP = (String) params.get("newNodeIP").toString().trim();
                node.updateTable(newNodeID, newNodeIP, anchorNodeID);
                return new JSONRPC2Response("Updation of finger table successful", req.getID());
            }

            else if(req.getMethod().equals("findSuccessor")){
                Map params = (Map) req.getNamedParams();
                int key = Integer.parseInt((String) params.get("key").toString().trim());
                HashMap<String, FingerTableRow> resultMap = new HashMap<String, FingerTableRow>();
                resultMap =node.findSuccessor(key);

                String resultMapString ="";
                for (Map.Entry<String, FingerTableRow> entry : resultMap.entrySet()) {
                    String Key = entry.getKey();
                    FingerTableRow Value = entry.getValue();
                    resultMapString= resultMapString+Key+","+Value.getActualID()+","+Value.getPointingID()+","+Value.getPointingIP();
                }

                return new JSONRPC2Response(resultMapString, req.getID());
            }

            else if(req.getMethod().equals("transferFiles")){
                Map params = (Map) req.getNamedParams();
                String neighborIP = (String) params.get("neighborIP").toString().trim();

                node.transferFilesToNeighbor(neighborIP);

                return new JSONRPC2Response("Sccessfully transferred files to neighbor", req.getID());
            }

            else if(req.getMethod().equals("downloadAll")){
                Map params = (Map) req.getNamedParams();
                String fileListString = (String) params.get("fileListString").toString().trim();
                String nodeGoingOfflineIP = (String) params.get("nodeGoingOfflineIP").toString().trim();
                node.downloadAllFiles(fileListString, nodeGoingOfflineIP);

                return new JSONRPC2Response("Sccessfully dowloaded all files from neighbor node going offline", req.getID());
            }
            else {

		        // Method name not supported

			return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
	         }
	     }
	 }
}
