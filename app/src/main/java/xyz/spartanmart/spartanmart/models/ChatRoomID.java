package xyz.spartanmart.spartanmart.models;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatRoomID implements Parcelable {

    private String chatroomID = "";
    private String buyerName = "";
    private String buyerID = "";

    public ChatRoomID() {
    }

    public ChatRoomID(String chatroomID, String buyerName, String buyerID) {
        this.chatroomID = chatroomID;
        this.buyerName = buyerName;
        this.buyerID = buyerID;
    }

    public String getChatroomID() {
        return chatroomID;
    }

    public void setChatroomID(String chatroomID) {
        this.chatroomID = chatroomID;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerID() {
        return buyerID;
    }

    public void setBuyerID(String buyerID) {
        this.buyerID = buyerID;
    }

    protected ChatRoomID(Parcel in) {
        chatroomID = in.readString();
        buyerName = in.readString();
        buyerID = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(chatroomID);
        dest.writeString(buyerName);
        dest.writeString(buyerID);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ChatRoomID> CREATOR = new Parcelable.Creator<ChatRoomID>() {
        @Override
        public ChatRoomID createFromParcel(Parcel in) {
            return new ChatRoomID(in);
        }

        @Override
        public ChatRoomID[] newArray(int size) {
            return new ChatRoomID[size];
        }
    };
}
