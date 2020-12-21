/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author PC
 */
import java.io.*;
import java.net.*;
import java.util.*;


public final class serverweb implements Runnable {
    final static String CarrLine = "\r\n";
    Socket clientSocket;

    // A list of files that have been moved.
    // Even indexes (0, 2, 4, ...) are the original file names.
    // Odd indexes (1, 3, 5, ...) are where the files of previous indexes moved to.
    static String movedFiles[] = {"index5.html", "index.html", "page.html", "homepage.html"};

    // This sets the Httprequest object socket equal to
    // the socket the client comes in through
    public serverweb(Socket socket) throws Exception {
        this.clientSocket = socket;
    }

    // Here we define a new method that overwrites the
    // previous method in the Runnables class. This is done
    // so that when an Http request is attempted, and
    // something goes wrong, our whole web server will
    // not fail and crash.
    @Override
    public void run(){
        try {

            // This is where the method to actually start the Http request starts.
            requestProcessing();

        } catch (Exception ex) { System.out.print(ex); }
    } 


    // This is our main processing method to take in out Http request
    // and spit out a reponse header along with the requested data,
    // if there is any.
    void requestProcessing() throws Exception {
        Boolean fileExists = false;

        String CarrLine = "\r\n";
        String statusCode = null;
        String responseHeader = "HTTP/1.1 ";
        String fileName, line = null;
        String clientSentence = null;

        ArrayList<String> records = new ArrayList<String>();
        FileInputStream requestedFileStream = null;
        File requestedFile;

        // Starts input from client and establishes filters
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Starts output stream for output to client through socket
        DataOutputStream outToClient = new DataOutputStream(clientSocket.getOutputStream());

        /*
        // Reads in GET from client BufferedReader
        while ( (line = inFromClient.readLine()) != null){
            records.add(line);
            break;
        }*/
        clientSentence = inFromClient.readLine();

        // Parses and stores file name the client wants in a string
        fileName = parseGET(clientSentence);


        if (!existingFile(fileName)){

            // Here is where the 301 response message is generated and
            // retrieve the correct filename.
            if (hasMoved(fileName) != -1){
                statusCode = "301";
                responseHeader = responseHeader + statusCode + " Moved Permanently\n";
                responseHeader = responseHeader + "Location: localhost:9012/" 
                        + movedFiles[hasMoved(fileName)] + CarrLine;

            }

            // This generates the response header for the client
            // if the file the client is looking for is not there (404).
            else {
                statusCode = "404";
                responseHeader = responseHeader + statusCode + " Not Found: \n";
                responseHeader = responseHeader + "Content-Type: text/html" + CarrLine;
            }

        }

        // This generates the 200 status code response header
        // to send to the client saying the file was found.
        if (existingFile(fileName)) {
            statusCode = "200";
            responseHeader = responseHeader + statusCode + " OK: \n";
            responseHeader = responseHeader + "Content-Type: " + fileType(fileName) + CarrLine;
            requestedFileStream = openFileStream(fileName);
        }

        // Outputs the response message to the client through a data stream
        outToClient.writeBytes(responseHeader);
        outToClient.writeBytes(CarrLine);

        // If the file the client is requesting exists,
        // begin writing file out to client.
        if (existingFile(fileName)){
            fileWriteOut(requestedFileStream, outToClient);
            requestedFileStream.close();
        }

        else if(hasMoved(fileName) != -1){
            outToClient.writeBytes("File Moved");
        }

        // If the file the client is requesting does not exist,
        // return a 404 message.
        else {
            outToClient.writeBytes("404: File not found!");
        }

        // Closes all open streams and sockets to the client.
        inFromClient.close();
        outToClient.close();
        clientSocket.close();
    }

    // This parses the GET line from the client to get the filename the client is requesting
    String parseGET(String clientString){

        String temp[] = clientString.split(" /");
        temp = temp[1].split(" ");
        return temp[0];
    }

    // This is used to find the file the client is requesting.
    // It will return null if no file was found/opened.
    FileInputStream openFileStream(String file){
        FileInputStream fileStream = null;

        // Opening the file stream is in a try catch statment so that
        // incase there was no file, the program doesn't crash
        // and it'll alert the user on the console.
        try {
            fileStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.out.println(e);
            return null;
            }

        return fileStream;
    }

    // Determines the file type that is being sent to the client
    // and returns the appropriate string
    String fileType(String clientRequestFile){

        // If the file ends in .html or .htm, it will return "text/html"
        // so that it can be added to the response message.
        if (clientRequestFile.endsWith(".html") || clientRequestFile.endsWith(".htm")){
            return "text/html";
        }

        // If the file ends in .jpg, it will return "text/jpeg"
        // so that it can be added to the response message.
        if (clientRequestFile.endsWith(".jpg")){
            return "text/jpg";
        }

        // If the file ends in .css, it will return "text/css"
        // so that it can be added to the response message.
        if (clientRequestFile.endsWith(".css")){
            return "text/css";
        }
        
        // If the file ends in .json, it will return "text/json"
        // so that it can be added to the response message.
        if (clientRequestFile.endsWith(".json")){
            return "text/json";
        }

        // Returns this by default, if none of the above.
        return "application/octet-stream";
    }

    // This creates a 2k buffer and writes out
    // requested filed to the client.
    static void fileWriteOut(FileInputStream clientStream, OutputStream toClient) throws Exception{
        byte[] buffer = new byte[2048];
        int bytes = 0;

        while ((bytes = clientStream.read(buffer)) != -1){
            toClient.write(buffer, 0, bytes);
        }

    }

    // This determines whether or not a file that
    // the client has requested exists or not.
    // Returns a Boolean value.
    static Boolean existingFile(String fileName){
        File file = new File(fileName);

        if (file.exists() && !file.isDirectory()){
            return true;
        }
        return false;
    }


    // Determines if a file has been moved and if so,
    // returns the index of the NEW file. Else it
    // returns -1.
    static int hasMoved(String fileName){
        int i = 0;

        for (i = 0; i < movedFiles.length; i=i+2){
            if (movedFiles[i].equals(fileName)){
                return i+1;
            }
        }
        return -1;
    }
}