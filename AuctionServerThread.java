import java.net.*;
import java.io.*;



public class AuctionServerThread extends Thread
{  private AuctionServer       server    = null;
   private Socket           socket    = null;
   private int              ID        = -1;
   private DataInputStream  streamIn  =  null;
   private DataOutputStream streamOut = null;
   private Thread thread;

   public AuctionServerThread(AuctionServer _server, Socket _socket)
   {
	  super();
      server = _server;
      socket = _socket;
      ID     = socket.getPort();

   }
   
   // This method sends data to client or clients
	public void send(String msg)
	{
		try {
			streamOut.writeUTF(msg);
			streamOut.flush();
        }
		catch(IOException ioe) {
			System.out.println(ID + " ERROR sending: " + ioe.getMessage());
			server.remove(ID);
			thread=null;
		}
	}
	
	// This method returns the port number of a socket connected to current client
	public int getID() {
		return ID;
	}

	public void run()
	{
		System.out.println("Server Thread " + ID + " running.");
		// Display the current item on auction when a client first joins
		send("\n\nThe current item on auction is:\t" + server.getCurrentItem().getName());
		if (server.getCurrentItem().isBiddedFor()) {
			send("\n" + server.getCurrentItem().getHighestBidderName()
					+ " has bid " + server.getCurrentItem().getHighestBid()
					+ " for " + server.getCurrentItem().getName() + "\n\n"
					+ "Enter Bid>\t");
		}
		else {
			send("\nThe starting price for this item is:\t" 
					+ server.getCurrentItem().getStartingPrice() + "\n\n"
					+ "Enter Bid>\t");
		}
		thread = new Thread(this);
		while (true) {
			try {
				// Receive data and pass it to server to handle
				server.getInput(ID, streamIn.readUTF());
				
				int pause = (int)(Math.random()*3000);
				Thread.sleep(pause);
			}
			catch (InterruptedException e) {
				System.out.println(e);
			}
			catch(IOException ioe) {
				server.remove(ID);
				thread = null;
			}
		}
	}

	public void open() throws IOException
	{
		// Set up input and output streams 
		streamIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		streamOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	public void close() throws IOException {
		if (socket != null) {
			socket.close();
		}

		if (streamIn != null) {
			streamIn.close();
		}

		if (streamOut != null) {
			streamOut.close();
		}
	}
}