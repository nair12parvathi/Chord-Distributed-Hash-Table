import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class Node {
    private InetAddress myIP;
    private InetAddress ircIP;
    private int N;
    private int myID;
    private ArrayList<FingerTableRow> fingerTable;
    private NodeSender nodeSender;
    private NodeServer nodeServer;
    private Scanner sc;
    private ArrayList<FileClass> fileList;
    private String isFirstNode;
    private int previouslyUpdatedNodeID;
    private boolean isAnchorNode;

    public Node() {
        fingerTable = new ArrayList<FingerTableRow>();
        isFirstNode = new String();
        sc = new Scanner(System.in);
        fileList = new ArrayList<FileClass>();
        previouslyUpdatedNodeID = Integer.MAX_VALUE;
        isAnchorNode = false;

    }

    /* returns IP address of the node */
    public InetAddress getMyIP() {
        return this.myIP;
    }

    /*returns ID of the node*/
    public int getMyID() {
        return this.myID;
    }

    public static void main(String[] args) {
        Node node = new Node();

        node.nodeSender = new NodeSender();
        node.nodeServer = new NodeServer();

        // start listening
        try {
            node.nodeServer.startServer(node);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean willingToBeAnchorNode = false;

        // get the ip address for IRC from CLI
        try {
            node.ircIP = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // get number of nodes in the network N from CLI
        node.N = Integer.parseInt(args[1]);

        // get self node ID
        node.myID = Integer.parseInt(args[2]);

        // assign system's IP
        node.myIP = node.findMyIP();

        System.out.println("Are you the first node in the network? (y/n)");
        node.isFirstNode = node.sc.nextLine().toLowerCase();

        if(node.isFirstNode.equals("y")){
            willingToBeAnchorNode = true;
            node.isAnchorNode = true;
        }

        else{
            System.out.println("Do you want to be an anchor node? (y/n)");
            String answer = node.sc.nextLine().toLowerCase();
            if(answer.equals("y")){
                willingToBeAnchorNode=true;
                node.isAnchorNode = true;
            }
        }

        String result = null;
        if (willingToBeAnchorNode) {
            result = node.nodeSender.registerAsAnchorNode(node, node.ircIP.getHostAddress(), "setAnchorNode");
        }


        //construct fingerTable
        node.constructFingerTable();

        boolean running = true;

        while (running) {
            System.out.println("Enter your choice:");
            System.out.println("1. Show Finger Table");
            System.out.println("2. Upload file");
            System.out.println("3. Search file");
            System.out.println("4. Show files in the node");
            System.out.println("5. shut down");
            int choice = Integer.parseInt(node.sc.nextLine());

            switch (choice) {
                case 1:
                    node.displayFingerTable();
                    break;
                case 2:
                    node.uploadFile();
                    break;
                case 3:
                    node.searchFile();
                    break;
                case 4:
                    node.displayFilesInNode();
                    break;
                case 5:
                    node.shutDownNode();
                    break;
                default:
                    System.out.println("Invalid choice. Try again");
                    break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    /*Gets its and its offline predecessor's files from neighbor once when it comes online*/
    public void updateFileList() {
        FingerTableRow neighborNodeRow = fingerTable.get(0);
        nodeSender.getMyFiles(myID, neighborNodeRow.getPointingIP(),"getMyFiles");
    }

    /*Inform IRC and transfer all files in the system to the neighbor before going offline and then shut down the node*/
    private void shutDownNode() {
        int anchorNodeID = Integer.MAX_VALUE;
        String anchorNodeIP = null;
        HashMap<Integer, String> anchorNodeList = nodeSender.getAnchorNodes(this, ircIP.getHostAddress(), "getAnchorNodes");
        if(isAnchorNode){
            if(anchorNodeList.size()-1 == 0){
                System.out.println("You are the only anchor node.\nCan't shut down until any other node volunteers to be an anchor node ! ");
                return;
            }
            else{
                isAnchorNode = false;
                anchorNodeList.remove("1");
                nodeSender.notifyIRCOfGoingOffline(this, myID, ircIP.getHostAddress(), "deleteAnchorNode");
            }
        }

        // get an anchor node from the anchor node list
        for(Map.Entry<Integer, String> entry: anchorNodeList.entrySet()){
            String anchorID = String.valueOf(entry.getKey());
            anchorNodeID = Integer.parseInt(anchorID);
            anchorNodeIP = entry.getValue();
            break;
        }

        // get neighbor information
        FingerTableRow neighborRow = fingerTable.get(0);
        int neighborID = neighborRow.getPointingID();
        String neighborIP = neighborRow.getPointingIP();

        // notify the anchor node about going offline
        nodeSender.nodeGoingOffline(this, myID, myIP.getHostAddress(), neighborID, neighborIP, anchorNodeIP, anchorNodeID, "goOffline");

        while (true) {
            System.out.println("System has gone offline");
            System.exit(0);
        }

    }

    /*Search a file*/
    private void searchFile() {
        System.out.println("Enter the file name: ");
        String fileName = sc.nextLine();

        System.out.println("Enter the file size");
        int fileSize = Integer.parseInt(sc.nextLine());

        // compute the key to discover which node has the file
        String fileNamePlusSize = fileName + fileSize;
        int key = hashAndReturnStorageID(fileNamePlusSize);

        // if file exists in the current node
        if (key == myID) {
            FileClass file = getFileFromFileList(fileName);
            displayFile(file);
        }

        // if file exists in some other node
        // nodeCredentials --> if the key in resultHashMap is nodeCredentials, the file is not in the contacted node.
        // destination --> if the key in resultHashMap is destination, the file is in the contacted node.
        String nextIP = null;
        if (myID != key) {
            HashMap<String, FingerTableRow> result = findSuccessor(key);

            if (result.containsKey("nodeCredentials")) {
                FingerTableRow r = result.get("nodeCredentials");

                if (r.getPointingID() == myID) {
                    FileClass file = getFileFromFileList(fileName);
                    displayFile(file);
                    return;
                }
            }
            if (result.containsKey("destination")) {
                FingerTableRow resultFingerTableRow = result.get("destination");

                if (resultFingerTableRow.getPointingID() == myID) {
                    FileClass file = getFileFromFileList(fileName);
                    displayFile(file);
                }

                else {
                    nextIP = resultFingerTableRow.getPointingIP();
                    nodeSender.getFile(this, fileName, nextIP, "getFile");
                }
            }
            else {
                while (result.containsKey("nodeCredentials")) {
                    FingerTableRow resultFingerTableRow = result.get("nodeCredentials");
                    nextIP = resultFingerTableRow.getPointingIP();
                    result = nodeSender.findSuccessor(this, key, nextIP, "findSuccessor");
                }

                if (result.containsKey("destination")) {
                    FingerTableRow resultftr = result.get("destination");

                    if (resultftr.getPointingID() == myID) {
                        FileClass file = getFileFromFileList(fileName);
                        displayFile(file);
                    }

                    else {
                        nextIP = resultftr.getPointingIP();
                        nodeSender.getFile(this, fileName, nextIP, "getFile");
                    }
                }
            }
        }
    }

    /*displays the file name and content*/
    public void displayFile(FileClass file) {
        System.out.println("*****************************************************************");
        System.out.println();
        if (file == null) {
            System.out.println("File doesn't exist");
            return;
        }

        System.out.println("File Name: \n " + file.getFileName());
        System.out.println("File Content:");
        System.out.println(file.getFileContent());
        System.out.println();
        System.out.println("*****************************************************************");
    }

    /* Retrieve file from the fileList*/
    public FileClass getFileFromFileList(String fileName) {
        for (FileClass fc : fileList) {
            if (fc.getFileName().equals(fileName)) {
                return fc;
            }
        }
        return null;
    }

    private void uploadFile() {
        System.out.println("Enter the file name: ");
        String fileName = sc.nextLine();

        System.out.println("Enter content");
        String content = sc.nextLine();

        // hash the filename + file length and obtain the key corresponding to the node ID where the
        // file is supposed to be stored
        String fileNamePlusSize = fileName + content.length();
        int key = hashAndReturnStorageID(fileNamePlusSize);
        createFile(fileName, content, key);
        storeFile(fileName, content, key);

    }

    /*Store the file at appropriate node where key==nodeID*/
    private void storeFile(String fileName, String content, int key) {
        if (key == myID) {
            displayFilesInNode();
            return;
        }
        String nextIP = null;
        if (myID != key) {
            HashMap<String, FingerTableRow> result = findSuccessor(key);

            for (Map.Entry<String, FingerTableRow> entry : result.entrySet()) {
                String Key = entry.getKey();
                FingerTableRow Value = entry.getValue();

            }

            //nodCredentials --> not the node to which file has to be sent
            //destination --> node to which file has to be sent
            if (result.containsKey("nodeCredentials")) {
                FingerTableRow r = result.get("nodeCredentials");
                if (r.getPointingID() == myID) {
                    displayFilesInNode();
                    return;
                }
            }
            if (result.containsKey("destination")) {
                FingerTableRow resultFingerTableRow = result.get("destination");
                if (resultFingerTableRow.getPointingID() == myID) {
                    displayFilesInNode();
                    return;
                }
                else {
                    nextIP = resultFingerTableRow.getPointingIP();
                    // send file name and content to the node(destination where key matches nodeID)
                    // with method "downloadfile"
                    nodeSender.sendFile(this, fileName, content, nextIP, key,"downloadFile");
                        deleteFile(fileName, content, key);
                }
            }

            else {
                while (result.containsKey("nodeCredentials")) {
                    FingerTableRow resultFingerTableRow = result.get("nodeCredentials");
                    nextIP = resultFingerTableRow.getPointingIP();
                    result = nodeSender.findSuccessor(this, key, nextIP, "findSuccessor");
                }
                if (result.containsKey("destination")) {
                    FingerTableRow resultftr = result.get("destination");
                    if (resultftr.getPointingID() == myID) {
                        displayFilesInNode();
                        return;
                    }
                    else {
                        nextIP = resultftr.getPointingIP();
                        // send file name and content to the node(destination where key matches nodeID)
                        // with method "downloadfile"
                        nodeSender.sendFile(this, fileName, content, nextIP,key, "downloadFile");
                        deleteFile(fileName, content, key);
                    }
                }
            }
        }
    }

    /*find the node where the file is to be stored*/
    public HashMap<String, FingerTableRow> findSuccessor(int key) {
        HashMap<String, FingerTableRow> finalResult = new HashMap<String, FingerTableRow>();
        FingerTableRow currentRow = null;
        FingerTableRow previousRow = null;
        FingerTableRow resultRow = new FingerTableRow();
        String status = "nodeCredentials";
        if (key == myID) {
            resultRow.setActualID(myID);
            resultRow.setPointingID(myID);
            resultRow.setPointingIP(myIP.getHostAddress());
            status = "destination";
            finalResult.put(status, resultRow);
            return finalResult;

        } else if (key < myID) {
            for (FingerTableRow ftr : fingerTable) {
                int v = Integer.MIN_VALUE;
                previousRow = currentRow;
                currentRow = ftr;

                if ((ftr.getActualID() > myID) && (ftr.getActualID() < N)) {
                    v = ftr.getActualID() - N;
                } else {
                    v = ftr.getActualID();
                }

                if (v < key)
                    continue;
                if (v > key) {
                    resultRow.setActualID(previousRow.getActualID());
                    resultRow.setPointingID(previousRow.getPointingID());
                    resultRow.setPointingIP(previousRow.getPointingIP());
                    // checking terminating conditions
                    int checkPointingID = resultRow.getPointingID();
                    if ((resultRow.getPointingID() > myID) && (resultRow.getPointingID() < N)) {
                        checkPointingID = resultRow.getPointingID() - N;
                    }
                    if (checkPointingID > key) {
                        status = "destination";
                    }
                    finalResult.put(status, resultRow);
                    return finalResult;
                }
            }

            resultRow.setActualID(currentRow.getActualID());
            resultRow.setPointingID(currentRow.getPointingID());
            resultRow.setPointingIP(currentRow.getPointingIP());
            int checkPointingID = resultRow.getPointingID();
            if ((resultRow.getPointingID() > myID) && (resultRow.getPointingID() < N)) {
                checkPointingID = resultRow.getPointingID() - N;
            }
            if (checkPointingID > key)
                status = "destination";
            finalResult.put(status, resultRow);
            return finalResult;

        }


        //if(key>myID(current Node ID))
        else {
            for (FingerTableRow ftr : fingerTable) {
                int v = Integer.MIN_VALUE;
                previousRow = currentRow;
                currentRow = ftr;

                if ((ftr.getActualID() < myID) && (ftr.getActualID() >= 0)) {
                    v = ftr.getActualID() + N;
                } else {
                    v = ftr.getActualID();
                }

                if (v < key)
                    continue;
                if (v > key) {
                    resultRow.setActualID(previousRow.getActualID());
                    resultRow.setPointingID(previousRow.getPointingID());
                    resultRow.setPointingIP(previousRow.getPointingIP());
                    // checking terminating conditions
                    int checkPointingID = resultRow.getPointingID();
                    if ((resultRow.getPointingID() < myID) && (resultRow.getPointingID() >= 0)) {
                        checkPointingID = resultRow.getPointingID() + N;
                    }
                    if (checkPointingID > key) {
                        status = "destination";
                    }
                    finalResult.put(status, resultRow);
                    return finalResult;
                }

            }

            resultRow.setActualID(currentRow.getActualID());
            resultRow.setPointingID(currentRow.getPointingID());
            resultRow.setPointingIP(currentRow.getPointingIP());
            int checkPointingID = resultRow.getPointingID();
            if ((resultRow.getPointingID() < myID) && (resultRow.getPointingID() >= 0)) {
                checkPointingID = resultRow.getPointingID() + N;
            }
            if (checkPointingID > key)
                status = "destination";
            finalResult.put(status, resultRow);
            return finalResult;
        }


    }

    /*create file*/
    private void createFile(String fileName, String content, int key) {
        FileClass fileClass = new FileClass();
        fileClass.setFileName(fileName);
        fileClass.setFileContent(content);
        fileClass.setFileID(key);
        fileList.add(fileClass);
    }

    /*delete file*/
    private void deleteFile(String fileName, String content, int key ) {
        Iterator<FileClass> iter = fileList.iterator();
        while (iter.hasNext()) {
            FileClass fc = iter.next();
            if (fc.getFileName().equals(fileName) && fc.getFileContent().equals(content)  && fc.getFileID()==key)
                iter.remove();
        }
    }

    /*display files in the current node*/
    private void displayFilesInNode() {
        System.out.println("*****************************************************************");
        System.out.println();
        System.out.println("Files in node: ");
        System.out.println();
        if (fileList.isEmpty()) {
            System.out.println("No files to show !");
        }

        for (FileClass fc : fileList) {
            System.out.println(fc.fileID + " ---> " + fc.fileName + ".txt");
        }
        System.out.println();
        System.out.println("*****************************************************************");
    }

    /*produces hash of file name plus size and returns key*/
    private int hashAndReturnStorageID(String fileNamePlusSize) {
        return Math.abs(fileNamePlusSize.hashCode() % N);
    }

    /*display the finger table*/
    private void displayFingerTable() {
        System.out.println("*****************************************************************");
        System.out.println();

        System.out.println("\n Finger Table \n");

        System.out.println("i" + " | " + "Actual ID" + " | " + "Pointing ID" + " | " + "Pointing IP");
        for (FingerTableRow row : fingerTable) {
            System.out.println(row.getI() + " |     " + row.getActualID() + " |      " + row.getPointingID() + " |      " + row.getPointingIP());
        }
        System.out.println();
        System.out.println("*****************************************************************");
    }

    /*construct finger table*/
    private void constructFingerTable() {

        int k = myID;
        // n is the number of entries in finger table
        int n = (int) (Math.log(N) / Math.log(2));
        int i = 0;
        while (i < n) {
            FingerTableRow row = new FingerTableRow();
            row.setI(i);
            row.setActualID((int) ((k + Math.pow(2, i)) % N));
            row.setPointingID(Integer.MAX_VALUE);
            fingerTable.add(row);
            i++;
        }
        setPointingNodeDetails();
    }

    /*set pointingID and pointingIP in the fineger table*/
    private void setPointingNodeDetails(){
        int anchorNodeID = Integer.MIN_VALUE;
        String anchorNodeIP = null;
        String nextIP = null;
        FingerTableRow resultRow = null;

        if (isFirstNode.equals("y")) {
            for (FingerTableRow row : fingerTable) {
                row.setPointingID(myID);
                row.setPointingIP(myIP.getHostAddress());
            }
        }
        else{
            HashMap<Integer, String> anchorNodeList = nodeSender.getAnchorNodes(this, ircIP.getHostAddress(), "getAnchorNodes");
            for(Map.Entry<Integer, String> entry: anchorNodeList.entrySet()){
                String anchorID = String.valueOf(entry.getKey());
                anchorNodeID = Integer.parseInt(anchorID);
                anchorNodeIP = entry.getValue();
                break;
            }
            //no_update --> not the poinitingID and IP
            //update --> correct pointingID and pointingIP received
            HashMap<String, FingerTableRow> resultHashMap = new HashMap<String, FingerTableRow>();
            String status = "no_update";
            FingerTableRow temp = new FingerTableRow();
            temp.setPointingID(anchorNodeID);
            temp.setPointingIP(anchorNodeIP);
            resultHashMap.put(status,temp);

            for(FingerTableRow f: fingerTable){
                while(!resultHashMap.containsKey("update")){
                    resultRow = resultHashMap.get("no_update");
                    nextIP = resultRow.getPointingIP();
                    resultHashMap = nodeSender.findPointer(f.actualID, nextIP, anchorNodeID, "findPointer");
                }
                resultRow = resultHashMap.get("update");
                f.pointingID = resultRow.getPointingID();
                f.pointingIP = resultRow.getPointingIP();
                resultHashMap.clear();

                status = "no_update";
                temp = new FingerTableRow();
                temp.setPointingID(anchorNodeID);
                temp.setPointingIP(anchorNodeIP);
                resultHashMap.put(status,temp);
            }
            if(anchorNodeID == myID){
                for(Map.Entry<Integer, String> entry: anchorNodeList.entrySet()){
                    String anchorID = String.valueOf(entry.getKey());
                    anchorNodeID = Integer.parseInt(anchorID);
                    anchorNodeIP = entry.getValue();
                    if(anchorNodeID != myID)
                        break;
                }
            }
            displayFingerTable();
            nodeSender.updateTable(myID, myIP.getHostAddress(), anchorNodeID, anchorNodeIP, "updateTable");
        }
    }

    //get the system's IP
    public InetAddress findMyIP() {
        InetAddress IP = null;
        try {
            IP = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return IP;
    }

    /*download file*/
    public void downloadFile(String fileName, String content, int key) {
        createFile(fileName, content, key);
        displayFilesInNode();
    }

    /*find pointingID and pointing IP to populate finger table*/
    public HashMap<String,FingerTableRow> findPointer(int newNodeID, int anchorNodeID) {
        HashMap<String, FingerTableRow> resultHashMap = new HashMap<String, FingerTableRow>();
        FingerTableRow previousRow = fingerTable.get(0);
        FingerTableRow currentRow = fingerTable.get(0);

        for(FingerTableRow f: fingerTable){
            if((f.getActualID() == newNodeID) || (f.getPointingID() == newNodeID)){
                FingerTableRow resultRow = new FingerTableRow();
                resultRow.setPointingID(f.getPointingID());
                resultRow.setPointingIP(f.getPointingIP());
                resultHashMap.put("update", resultRow);
                return resultHashMap;
            }
        }

        for(int i=1;i<fingerTable.size();i++){
            previousRow = currentRow;
            currentRow = fingerTable.get(i);
            if(isInBetween(previousRow.getActualID(), newNodeID, currentRow.getActualID())){

                if(isInBetween(previousRow.getActualID(), newNodeID, previousRow.getPointingID())){
                    FingerTableRow resultRow = new FingerTableRow();
                    resultRow.setPointingID(previousRow.getPointingID());
                    resultRow.setPointingIP(previousRow.getPointingIP());
                    resultHashMap.put("update", resultRow);
                    return resultHashMap;
                }
                else{
                    FingerTableRow resultRow = new FingerTableRow();
                    resultRow.setPointingID(previousRow.getPointingID());
                    resultRow.setPointingIP(previousRow.getPointingIP());
                    resultHashMap.put("no_update", resultRow);
                    return resultHashMap;
                }

            }
        }

        if(isInBetween(currentRow.getActualID(), newNodeID, currentRow.getPointingID())){
            FingerTableRow resultRow = new FingerTableRow();
            resultRow.setPointingID(currentRow.getPointingID());
            resultRow.setPointingIP(currentRow.getPointingIP());
            resultHashMap.put("update", resultRow);
            return resultHashMap;
        }
        else{
            FingerTableRow resultRow = new FingerTableRow();
            resultRow.setPointingID(currentRow.getPointingID());
            resultRow.setPointingIP(currentRow.getPointingIP());
            resultHashMap.put("no_update", resultRow);
            return resultHashMap;
        }


    }


    /*tweek if numbers are not in natural order and then compare the numbers*/
    public boolean isInBetween(int start, int mid, int end){
        if(mid<start){
            mid=mid+N;
        }
        if(end<start){
            end=end+N;
        }
        if(mid>start && mid<end){
            return true;
        }
        else{
            return false;
        }

    }

    /*update finger table*/
    public void updateTable(int newNodeID, String newNodeIP, int anchorNodeID) {

        if(anchorNodeID == myID){
            if(previouslyUpdatedNodeID == newNodeID){
                previouslyUpdatedNodeID = Integer.MIN_VALUE;
                nodeSender.notifyNewNode(newNodeIP, "updateFileList");
                return;
            }
            else{
                previouslyUpdatedNodeID = newNodeID;
            }
        }


        for(FingerTableRow f: fingerTable){

            if((f.getActualID() == newNodeID) || isInBetween(f.getActualID(), newNodeID, f.getPointingID())){
                f.setPointingID(newNodeID);
                f.setPointingIP(newNodeIP);
            }

        }


        FingerTableRow neighborRow = fingerTable.get(0);
        nodeSender.updateTable(newNodeID, newNodeIP, anchorNodeID, neighborRow.getPointingIP(), "updateTable");

    }

    /*transfer files belonging to the neighbor node that newly joined the network*/
    public void sendNeighborFiles() {
        Iterator<FileClass> iter = fileList.iterator();
        int size = fileList.size();
        for(int i=0;i<size;i++){
            FileClass f = fileList.get(i);

            storeFile(f.getFileName(), f.getFileContent(), f.getFileID());
            if(fileList.size()<size) {
                size = fileList.size();
                i--;
            }
        }
    }

    /*update finger table when any node in the network goes offline*/
    public void updateTableOnNodeDeletion(int nodeGoingOfflineID, String nodeGoingOfflineIP, int neighborID, String neighborIP, int anchorNodeID) {
        if(anchorNodeID == myID){
            if(previouslyUpdatedNodeID == nodeGoingOfflineID){
                previouslyUpdatedNodeID = Integer.MIN_VALUE;
                nodeSender.notifyNodeGoingOffline(nodeGoingOfflineIP, neighborIP, "transferFiles");
                return;
            }
            else{
                previouslyUpdatedNodeID = nodeGoingOfflineID;
            }
        }

        for(FingerTableRow f: fingerTable){
            if(f.getActualID() == nodeGoingOfflineID || f.getPointingID()==nodeGoingOfflineID){
                f.setPointingID(neighborID);
                f.setPointingIP(neighborIP);
            }
        }

        FingerTableRow neighborRow = fingerTable.get(0);
        String nextIP = neighborRow.getPointingIP();
        nodeSender.nodeGoingOffline(this, nodeGoingOfflineID, nodeGoingOfflineIP, neighborID, neighborIP, nextIP, anchorNodeID, "goOffline");
    }

    /*transfer files to neighbor/successor before going offline*/
    public void transferFilesToNeighbor(String neighborIP) {
        String fileListString = "";
        for(FileClass f: fileList){
            fileListString = fileListString+f.getFileID()+","+f.getFileName()+","+f.getFileContent()+";";
        }
        fileList.clear();
        nodeSender.transferAll(this, myIP.getHostAddress(), fileListString,  neighborIP,"downloadAll");

    }

    /*download all files from the neighbor after joining the network*/
    public void downloadAllFiles(String fileListString, String nodeGoingOfflineIP) {
        if(fileListString.isEmpty())
            return;
        String[] files = fileListString.split(";");
        for(int i=0; i<files.length;i++){
            FileClass f = new FileClass();
            String[] fileParams = files[i].split(",");
            f.setFileID(Integer.parseInt(fileParams[0]));
            f.setFileName(fileParams[1]);
            f.setFileContent(fileParams[2]);
            fileList.add(f);
        }
    }
}
