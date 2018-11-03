import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Request;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import com.thetransactioncompany.jsonrpc2.server.MessageContext;
import com.thetransactioncompany.jsonrpc2.server.RequestHandler;
import java.util.Map;

public class IRCHandler {

    // Implements a handler for "getAnchorNodes", "setAnchorNode" and "deleteAnchorNode" JSON-RPC methods
    // that return the anchor node list and registers a new anchor node respectively
    public static class GetSetHandler implements RequestHandler {
        IRC irc;

        public GetSetHandler(IRC irc) {
            this.irc = irc;
        }

        // Reports the method names of the handled requests
        public String[] handledRequests() {
            return new String[]{"setAnchorNode", "getAnchorNodes", "deleteAnchorNode"};
        }


        // Processes the requests
        public JSONRPC2Response process(JSONRPC2Request req, MessageContext ctx) {


            if (req.getMethod().equals("setAnchorNode")) {
                Map params = (Map) req.getNamedParams();
                int nodeID = Integer.parseInt((String) params.get("myID").toString().trim());
                String nodeIP = (String) params.get("myIP").toString().trim();
                irc.addNodeToAnchorNodeList(nodeID, nodeIP);
                String result ="Successfully registered as anchor node";
                return new JSONRPC2Response(result, req.getID());

            }


            else if (req.getMethod().equals("getAnchorNodes")) {
                return new JSONRPC2Response(irc.getAnchorNodeList(), req.getID());
            }

            else if (req.getMethod().equals("deleteAnchorNode")) {
                Map params = (Map) req.getNamedParams();
                int nodeGoingOffline = Integer.parseInt((String) params.get("nodeGoingOffline").toString().trim());
                irc.deleteAnchorNode(nodeGoingOffline);
                String result ="Notified IRC of unavailability";
                return new JSONRPC2Response(result, req.getID());
            }


            else {
                // Method name not supported
                return new JSONRPC2Response(JSONRPC2Error.METHOD_NOT_FOUND, req.getID());
            }
        }
    }

}
