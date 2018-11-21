class Item
{
	private int startingPrice;
	private int highestBid;
	private String name;
	private boolean biddedFor;
	private int highestBidder;
	private String highestBidderName;

	public Item(int startingPrice, String name) {
		this.startingPrice = startingPrice;
		highestBid = startingPrice;
		this.name = name;
		biddedFor = false;
		highestBidder = 0;
		highestBidderName = "";
	}
	
	public int getStartingPrice() {
		return startingPrice;
	}
	
	public int getHighestBid() {
		return highestBid;
	}
	
	public String getHighestBidderName() {
		return highestBidderName;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isBiddedFor() {
		return biddedFor;
	}
	
	public int getHighestBidder() {
		return highestBidder;
	}
	
	public void setHighestBid(int highestBid) {
		this.highestBid = highestBid;
	}
	
	public void setHighestBidderName(String highestBidderName) {
		this.highestBidderName = highestBidderName;
	}
	
	public void setBiddedFor(boolean biddedFor) {
		this.biddedFor = biddedFor;
	}
	
	public void setHighestBidder(int ID) {
		this.highestBidder = ID;
	}
	
}