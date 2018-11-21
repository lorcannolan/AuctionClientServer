import java.net.*;
import java.io.*;



public class BidderClientThread extends Thread
{  private Socket           socket   = null;
   private BidderClient       client   = null;
   private DataInputStream  streamIn = null;

	public BidderClientThread(BidderClient _client, Socket _socket)
	{
		client   = _client;
		socket   = _socket;
		open();
		start();
	}
	
	public void open()
	{  
		try {
			// Step 2: set up input stream
			streamIn = new DataInputStream(socket.getInputStream());
		}
		catch(IOException ioe) {
			System.out.println("Error getting input stream: " + ioe);
			client.stop();
		}
	}
	
	public void close() {
		try {
			// Step 4: Close the connection to the socket
			if (streamIn != null) streamIn.close();
		}
		catch(IOException ioe) {
			System.out.println("Error closing input stream: " + ioe);
		}
	}

	public void run()
	{
		while(true && client!= null) {
			try {
				// Step 3: receive data
				String message = streamIn.readUTF();
				client.display(message);
			}
			catch(IOException ioe) {
				client = null;
				System.out.println("Listening error: " + ioe.getMessage());
			}
		}
	}
}



