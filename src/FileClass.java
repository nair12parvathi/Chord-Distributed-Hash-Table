import java.io.Serializable;
public class FileClass implements Serializable{
    String fileName;
    String fileContent;

    int fileID;

    public int getFileID() {
        return this.fileID;
    }

    public void setFileID(int fileID) {
        this.fileID = fileID;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }




}
