package com.mycompany.server_web;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Leonardo
 */
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.mycompany.server_web.PuntiVendita;
import java.math.BigDecimal;
import java.io.*;
import java.net.*;
import java.util.*;

public class serverwebmain implements Runnable{ 	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final int PORT = 8080;
	static final boolean verbose = true;
	private Socket connect;
        
	public serverwebmain(Socket c) {
		connect = c;
	}
	
	public static void main(String[] args) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
			while (true) {
				serverwebmain myServer = new serverwebmain(serverConnect.accept());
				if (verbose) {
					System.out.println("Connecton opened. (" + new Date() + ")");
				}
				Thread thread = new Thread(myServer);
				thread.start();
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}
        
	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		String fileRequested = null;
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			String input = in.readLine();
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase();
			fileRequested = parse.nextToken().toLowerCase();
			if (!method.equals("GET")  &&  !method.equals("HEAD")) {
                            if (verbose) {
                                    System.out.println("501 Not Implemented : " + method + " method.");
                            }
                            File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                            int fileLength = (int) file.length();
                            String contentMimeType = "text/html";
                            byte[] fileData = readFileData(file, fileLength);
                            out.println("HTTP/1.1 501 Not Implemented");
                            out.println("Server: Java HTTP Server from SSaurel : 1.0");
                            out.println("Date: " + new Date());
                            out.println("Content-type: " + contentMimeType);
                            out.println("Content-length: " + fileLength);
                            out.println();
                            out.flush();
                            dataOut.write(fileData, 0, fileLength);
                            dataOut.flush();
                        } else if(fileRequested.contains("punti-vendita.xml")){
                            ObjectMapper mapper = new ObjectMapper();
                            PuntiVendita p = mapper.readValue(new File("DESErializzazione/puntiVendita.json"), PuntiVendita.class);
                            XmlMapper xmlMapper = new XmlMapper();
                            xmlMapper.writeValue(new File("puntiVendita.xml"), p);
                            File fileinxml = new File("puntiVendita.xml");
                            File file = new File(WEB_ROOT, FILE_NOT_FOUND);
                            int fileLength = (int) file.length();
                            String content = "application/xml";
                            byte[] fileData = readFileData(file, fileLength);
                            out.println("HTTP/1.1 200 OK");
                            out.println("Server: Java HTTP Server from SSaurel : 1.0");
                            out.println("Date: " + new Date());
                            out.println("Content-type: " + content);
                            out.println("Content-length: " + fileLength);
                            out.println(); 
                            out.flush(); 
                            dataOut.write(fileData, 0, fileLength);
                            dataOut.flush();
                            if (verbose) {
                                    System.out.println("File " + fileRequested + " not found");
                            }
			} else {
                            // GET or HEAD method
                            if (fileRequested.endsWith("/")) {
                                    fileRequested += DEFAULT_FILE;
                            }
                            File file = new File(WEB_ROOT, fileRequested);
                            int fileLength = (int) file.length();
                            String content = getContentType(fileRequested);
                            if (method.equals("GET")) { // GET method so we return content
                                    byte[] fileData = readFileData(file, fileLength);
                                    out.println("HTTP/1.1 200 OK");
                                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                                    out.println("Date: " + new Date());
                                    out.println("Content-type: " + content);
                                    out.println("Content-length: " + fileLength);
                                    out.println(); 
                                    out.flush(); 
                                    dataOut.write(fileData, 0, fileLength);
                                    dataOut.flush();
                            }
                            if (verbose) {
                                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                            }	
			}	
		} catch (FileNotFoundException fnfe) {
			try {
				fileNotFound(out, dataOut, fileRequested);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
		} catch (IOException ioe) {
			System.err.println("Server error : " + ioe);
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
        }
        
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		return fileData;
	}
	private String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
			return "text/html";
		else
			return "text/plain";
	}
	
	private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
            if(fileRequested.contains("punti-vendita.xml")){
                ObjectMapper mapper = new ObjectMapper();
                List<PuntiVendita> p = mapper.readValue(new File("DESErializzazione/puntiVendita.json"), new TypeReference<List<PuntiVendita>>(){});
                XmlMapper xmlMapper = new XmlMapper();
                xmlMapper.writeValue(new File("simple_bean.xml"), p);
                File file = new File("simple_bean.xml");
                int fileLength = (int) file.length();
                String content = "application/xml";
                byte[] fileData = readFileData(file, fileLength);
                out.println("HTTP/1.1 200 OK");
                out.println("Server: Java HTTP Server from SSaurel : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + content);
                out.println("Content-length: " + fileLength);
                out.println("Location: " + fileRequested);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
                if (verbose) {
                        System.out.println("File " + fileRequested + " not found");
                } 
            }else{
                File file = new File(WEB_ROOT, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);
		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server from SSaurel : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println(); 
		out.flush(); 
		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();
		if (verbose) {
			System.out.println("File " + fileRequested + " not found");
		}
            }
	}
}
