import java.net.*;
import java.io.*;


public class BidderClient implements Runnable
{  private Socket socket = null;
   private Thread thread = null;
   private BufferedReader console = null;
   private DataOutputStream streamOut = null;
   private BidderClientThread client = null;
   private String clientName;
   private boolean isNewItemName = false;
   private boolean isNewItemValue = false;

   
	public BidderClient(String serverName, int serverPort, String name)
	{
		System.out.println("Establishing connection. Please wait ...");

		this.clientName = name;
		try {
			// Establish server connection and provide client with welcome message
			socket = new Socket(serverName, serverPort);
			System.out.println("Welcome to the Auction!");
			System.out.println("To bid enter a numeric value greater than the current bid.");
			System.out.println("To leave auction, enter QUIT.");
			System.out.println("To add a new item enter NEW.");
			start();
		}
		catch(UnknownHostException uhe) {
			System.out.println("Host unknown: " + uhe.getMessage());
		}
		catch(IOException ioe)	{
			System.out.println("Unexpected exception: " + ioe.getMessage());
		}
	}

	public void run()
	{
		while (thread != null) {
			// Send data to server
			try {
				// If regular bid is to be entered
				String input = console.readLine();
				if (!isNewItemName & !isNewItemValue) {
					try {
						if (!input.equals("NEW")) {
							Integer.parseInt(input);
						}
						streamOut.writeUTF(input + " " + clientName);
						streamOut.flush();
					}
					catch (NumberFormatException nfe) {
						if (input.equals("QUIT")) {
							stop();
						}
						else {
							System.out.println("Must enter a number");
						}
					}
				}
				// If client is entering value of new item name
				else if (isNewItemName && !isNewItemValue) {
					if (input.equals("QUIT")) {
						stop();
					}
					// Attach unique string to inform server that a new item name is being sent
					streamOut.writeUTF(input + " ***Name***");
					streamOut.flush();
				}
				// If client is entering value of new item value
				else if (!isNewItemName && isNewItemValue) {
					try {
						Integer.parseInt(input);
						// Attach unique string to inform server that a new item value is being sent
						streamOut.writeUTF(input + " ***Value***");
						streamOut.flush();
					}
					catch (NumberFormatException nfe) {
						if (input.equals("QUIT")) {
							stop();
						}
						else {
							System.out.println("Must enter a number");
						}
					}
				}
			}
			catch(IOException ioe) {
				System.out.println("Sending error: " + ioe.getMessage());
				stop();
			}
		}
	}

	/*
	This method displays messages sent to client from the server.
	If server requires name or value of new item, update flags to 
	ensure correct unique string is attached.
	*/
	public void display(String message) {
		if (message.equals("\nEnter Name of new item:\t")) {
			isNewItemName = true;
			isNewItemValue = false;
		}
		else if (message.equals("\nEnter Value of new item:\t")) {
			isNewItemName = false;
			isNewItemValue = true;
		}
		else {
			isNewItemName = false;
			isNewItemValue = false;
		}
		System.out.print(message);
	}
	
	public void start() throws IOException
	{
		console = new BufferedReader(new InputStreamReader(System.in));

		// Set up output stream
		streamOut = new DataOutputStream(socket.getOutputStream());
		if (thread == null) {
			// creating client thread
			client = new BidderClientThread(this, socket);
			thread = new Thread(this);
			thread.start();
		}
	}

	public void stop()
	{
		try {
			if (console != null) {
				console.close();
			}
			if (streamOut != null) {
				streamOut.close();
			}
			if (socket != null) {
				socket.close();
			}
		}
		catch(IOException ioe) {
			System.out.println("Error closing ...");
		}
		// Close the Connection
		client.close();
		thread = null;
	}


	public static void main(String args[])
	{  
		BidderClient client = null;
		if (args.length != 3) {
			System.out.println("Usage: java BidderClient host port name");
		}
		else {
			client = new BidderClient(args[0], Integer.parseInt(args[1]), args[2]);
		}
	}
}
