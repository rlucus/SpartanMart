package xyz.spartanmart.spartanmart.models;


/**
 * Created by stefan on 12/9/2016.
 */

public class ChatRoom {

    private static final String TAG = ChatRoom.class.getSimpleName();

    //private List<Message> messages;
    private String id = "";
    private String sellerID = "";
    private String sellerName = "";
    private String buyerID = "";
    private String buyerName = "";
    private String listingID = "";
    private String listingName = "";
    private String price="";
    private String offer="";
    private String offerBy="";
    private boolean isActive;
    private boolean buyerAgree=false;
    private boolean sellerAgree=false;

    public ChatRoom() {
    }

    public ChatRoom(String buyerName,String buyerID, String sellerID, String sellerName, String listingName,String listingID) {
        this.buyerName = buyerName;
        this.buyerID = buyerID;
        this.listingName = listingName;
        this.listingID = listingID;
        this.sellerID = sellerID;
        this.sellerName = sellerName;
    }

    /*public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }*/

    public String getSellerID() {
        return sellerID;
    }

    public void setSellerID(String sellerID) {
        this.sellerID = sellerID;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(String buyerID) {
        this.buyerID = buyerID;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getListingID() {
        return listingID;
    }

    public void setListingID(String listingID) {
        this.listingID = listingID;
    }

    public String getListingName() {
        return listingName;
    }

    public void setListingName(String listingName) {
        this.listingName = listingName;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getOffer() {
        return offer;
    }

    public void setOffer(String offer) {
        this.offer = offer;
    }

    public String getOfferBy() {
        return offerBy;
    }

    public void setOfferBy(String offerBy) {
        this.offerBy = offerBy;
    }

    public boolean isBuyerAgree() {
        return buyerAgree;
    }

    public void setBuyerAgree(boolean buyerAgree) {
        this.buyerAgree = buyerAgree;
    }

    public boolean isSellerAgree() {
        return sellerAgree;
    }

    public void setSellerAgree(boolean sellerAgree) {
        this.sellerAgree = sellerAgree;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean active) {
        isActive = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setListing(Listing mListing) {
        setListingID(mListing.getId());
        setListingName(mListing.getTitle());
        setPrice(mListing.getPrice());

        setSellerID(mListing.getCreatorId());
        setSellerName(mListing.getCreator());

        setBuyerID(UserModel.uid);
        setBuyerName(UserModel.username);
        setOffer(mListing.getPrice());
        setOfferBy(mListing.getCreatorId());

        setIsActive(true);
        setBuyerAgree(false);
        setSellerAgree(false);
    }
}