import java.net.*;
import java.io.*;


public class HttpServerDemo{
  
  public static void main(String args[])throws IOException {

	int port = 8000;
       ServerSocket serversocket = new ServerSocket(port);
     System.out.println("Server is running: http://localhost:" +port);
	
	while(true){
	Socket clientSocket = serversocket.accept();
	System.err.println("Client connected");

	
//	BufferedReader in =  new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//	OutputStream clientOutput = clientSocket.getOutputStream();
  	
	//creating a new thread tohandle this client
	new Thread(() -> {
	try{
	handleClient(clientSocket);
    }catch(IOException e) {
	e.printStackTrace();
}
	}).start();
   }

}
	

	private static void handleClient(Socket clientSocket) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream clientOut = clientSocket.getOutputStream();


	String s;
	while((s = in.readLine())!=null ){
	System.out.println(s);
	if(s.isEmpty()){

	  break;
 	

          }
 
        }
	clientOut.write("HTTP/1.1 200 OK\r\n".getBytes());
        clientOut.write("\r\n".getBytes());
	 clientOut.write("<b>Welcome To my id!</b>".getBytes());
	 clientOut.write("\r\n\r\n".getBytes());
	clientOut.flush();


	System.err.println("Client connection closed!");

	in.close();
	clientOut.close();
	clientSocket.close();		
	
      }	
	   

   }	



