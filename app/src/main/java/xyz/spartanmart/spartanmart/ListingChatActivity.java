package xyz.spartanmart.spartanmart;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import xyz.spartanmart.spartanmart.adapters.ChatRecyclerAdapter;
import xyz.spartanmart.spartanmart.models.ChatRoom;
import xyz.spartanmart.spartanmart.models.ChatRoomID;
import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.Message;
import xyz.spartanmart.spartanmart.models.UserModel;

public class ListingChatActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = ListingChatActivity.class.getSimpleName();

    // Make Views
    private TextView mItem, mPrice, mOffer;
    private EditText mMessage, mCounterOffer;
    private Button mAcceptOffer;
    private RecyclerView mRecycler;
    private ChatRecyclerAdapter mAdapter;
    private ProgressBar mProgressBar;

    // Internal member variables
    private FirebaseDatabase mDatabase;
    private DatabaseReference mChatRoomRef;
    private DatabaseReference mMessagesRef;
    private DatabaseReference mSellerRef;
    private DatabaseReference mBuyerRef;
    private ChatRoom mChatRoom;
    private Listing mListing;
    private List<Message> mMessagesList;

    private double mSellerBank;
    private double mBuyerBank;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        mDatabase = FirebaseDatabase.getInstance();

        // Bind views to objects
        mItem = (TextView) findViewById(R.id.item);
        mPrice = (TextView) findViewById(R.id.price);
        mOffer = (TextView) findViewById(R.id.offer);
        mAcceptOffer = (Button) findViewById(R.id.accept_offer);
        mMessage = (EditText) findViewById(R.id.text_submission);
        mCounterOffer = (EditText) findViewById(R.id.counter_offer);
        mRecycler = (RecyclerView) findViewById(R.id.recyclerView);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mMessagesList = new ArrayList<>();

        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ChatRecyclerAdapter(mMessagesList,this,UserModel.username);
        mRecycler.setAdapter(mAdapter);

        changeAcceptButton(true);

        getOrigin();

        // Button Click Listeners
        findViewById(R.id.send).setOnClickListener(this);
        findViewById(R.id.make_offer).setOnClickListener(this);
        findViewById(R.id.accept_offer).setOnClickListener(this);
    }

    private void getOrigin() {
        Intent intent = getIntent();
        if(intent.hasExtra("chatroomID")) {
            String chatroomID = intent.getStringExtra("chatroomID");
            Log.d(TAG,"getOrigin:if "+chatroomID);

            // Download ChatRoom and Messages from Firebase
            fetchFirebaseData(chatroomID);

        }else if(intent.hasExtra("listingID")){
            Log.d(TAG,"getOrigin:else ");
            String listingID =  intent.getStringExtra("listingID");

            setProgressBar(View.VISIBLE);
            DatabaseReference listingRef = mDatabase.getReference().child("Listing").child(listingID);
            listingRef.addListenerForSingleValueEvent(listingListener());
        }
    }

    private void changeAcceptButton(boolean isLocked){
        if(!isLocked){
            mAcceptOffer.setBackgroundColor(Color.GREEN);
            mAcceptOffer.setClickable(true);
            mAcceptOffer.setAlpha(1);
            mAcceptOffer.setText("Accept Offer");
        }else{
            mAcceptOffer.setBackgroundColor(Color.GRAY);
            mAcceptOffer.setClickable(false);
            mAcceptOffer.setAlpha(0.5f);
            mAcceptOffer.setText("Waiting for Response...");
        }
    }

    private void fetchFirebaseData(String chatroomID) {
        // Fetch ChatRoom Data
        setProgressBar(View.VISIBLE);
        mChatRoomRef = mDatabase.getReference().child("Chatrooms").child(chatroomID);
        Log.d(TAG,"fetchFirebaseData: "+chatroomID+", "+mChatRoomRef);
        mChatRoomRef.addValueEventListener(chatRoomEventListener());

        // Fetch Messages, use Child Listener because list is hashmap
        mMessagesRef = mChatRoomRef.child("messages");
        mMessagesRef.addChildEventListener(messagesChildListener());
    }

    private void updateUI(String item_input,String price_input, String offer_input){
        Log.d(TAG,"updateUI: "+item_input+", "+price_input+", "+offer_input);
        String item = "Item: "+item_input;
        String price = "Price: "+price_input;
        String offer = "Offer: "+offer_input;

        mItem.setText(item);
        mPrice.setText(price);
        mOffer.setText(offer);
    }

    private ValueEventListener listingListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,"listingListener:onDataChange "+dataSnapshot.exists());
                setProgressBar(View.GONE);
                mListing = dataSnapshot.getValue(Listing.class);
                updateUI(mListing.getTitle(),mListing.getPrice(),"0.00");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,"listingListener:onCancelled",databaseError.toException());
                setProgressBar(View.GONE);
            }
        };
    }

    private ValueEventListener chatRoomEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,  "getOrigin:onDataChange: " + dataSnapshot.exists()+", "+dataSnapshot);
                setProgressBar(View.GONE);
                mChatRoom = new ChatRoom();
                try {
                    mChatRoom.setBuyerAgree((boolean) dataSnapshot.child("buyerAgree").getValue());
                    mChatRoom.setBuyerID((String) dataSnapshot.child("buyerID").getValue());
                    mChatRoom.setBuyerName((String) dataSnapshot.child("buyerName").getValue());
                    mChatRoom.setListingID((String) dataSnapshot.child("listingID").getValue());
                    mChatRoom.setListingName((String) dataSnapshot.child("listingName").getValue());
                    mChatRoom.setOffer((String) dataSnapshot.child("offer").getValue());
                    mChatRoom.setOfferBy((String) dataSnapshot.child("offerBy").getValue());
                    mChatRoom.setPrice((String) dataSnapshot.child("price").getValue());
                    mChatRoom.setSellerAgree((boolean) dataSnapshot.child("sellerAgree").getValue());
                    mChatRoom.setSellerID((String) dataSnapshot.child("sellerID").getValue());
                    mChatRoom.setSellerName((String) dataSnapshot.child("sellerName").getValue());

                    boolean isLocked = mChatRoom.getOfferBy().equals(UserModel.uid);
                    changeAcceptButton(isLocked);


                    setBanks();
                    updateUI(mChatRoom.getListingName(), mChatRoom.getPrice(), mChatRoom.getOffer());
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
                //updateMessages();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "getOrigin:onCancelled: ", databaseError.toException());
                setProgressBar(View.GONE);
            }
        };
    }

    private ChildEventListener messagesChildListener(){
        return new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG,"messagesChildListener:onChildAdded: "+dataSnapshot.exists()+", string: "+s+", dataSnapShot: "+dataSnapshot);
                Message message = dataSnapshot.getValue(Message.class);
                mAdapter.addMessage(message);
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG,"messagesChildListener:onChildChanged: "+dataSnapshot.exists()+", string: "+s);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG,"messagesChildListener:onChildRemoved: "+dataSnapshot.exists());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                Log.d(TAG,"messagesChildListener:onChildMoved: "+dataSnapshot.exists()+", string: "+s);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG,"messagesChildListener:onCancelled",databaseError.toException());
            }
        };
    }

    private void updateMessages() {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.send:
                String message = mMessage.getText().toString().trim();
                if(!message.isEmpty()){
                    checkChatRoomExists(message);
                }
                break;
            case R.id.make_offer:
                String counterOffer = mCounterOffer.getText().toString().trim();
                mCounterOffer.setText("");
                if(counterOffer!=null && !counterOffer.equals("")){
                    counterOffer = formatOffer(counterOffer);
                    String dialogMessage = "Would you like to make an offer of "+counterOffer+"?";
                    final String finalOffer = counterOffer;
                    new AlertDialog.Builder(this)
                            .setTitle("Make Offer")
                            .setMessage(dialogMessage)
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    makeOffer(finalOffer);
                                }
                            })
                            .setNegativeButton("Cancel",null)
                            .show();
                }else{
                    Toast.makeText(this, "There was a format issue with your input", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.accept_offer:
                final String offer = mChatRoom.getOffer();
                String dialogMessage = "Please Confirm that you are accepting the offer of "+offer;
                new AlertDialog.Builder(this)
                        .setTitle("Accept Offer?")
                        .setMessage(dialogMessage)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                acceptOffer(offer);
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .show();
                break;
        }
    }

    private void setBanks(){
        mSellerRef = mDatabase.getReference().child("Users").child(mChatRoom.getSellerID()).child("bank");
        mBuyerRef = mDatabase.getReference().child("Users").child(mChatRoom.getBuyerID()).child("bank");
        mSellerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Number number = (Number) dataSnapshot.getValue();
                mSellerBank = number.doubleValue();
                Log.d(TAG,"onDataChange: "+mSellerBank+", "+number);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
        mBuyerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Number number = (Number) dataSnapshot.getValue();
                mBuyerBank = number.doubleValue();
                Log.d(TAG,"onDataChange: "+mBuyerBank+", "+number);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void acceptOffer(String offer) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        try {
            double offer_d = nf.parse(offer).doubleValue();
            Log.d(TAG,"acceptOffer: offer_d: "+offer_d+", "+mBuyerBank);
            if(offer_d<mBuyerBank){
                // User has enough money to buy
                mBuyerBank = mBuyerBank-offer_d;
                mSellerBank = mSellerBank+ offer_d;

                mBuyerRef.setValue(mBuyerBank);
                mSellerRef.setValue(mSellerBank);
                mChatRoomRef.child("getIsActive").setValue(false);
                mDatabase.getReference().child("Listing").child(mChatRoom.getListingID()).child("isActive").setValue(false);

                new AlertDialog.Builder(this)
                        .setTitle("Offer Accepted!")
                        .setMessage("Congratulations on your purchase of "+mChatRoom.getListingName())
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(ListingChatActivity.this,MainDrawerActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                startActivity(intent);
                            }
                        })
                        .setCancelable(false)
                        .show();
            }else{
                new AlertDialog.Builder(this)
                        .setTitle("Sorry!")
                        .setMessage("It appears you have insufficient funds to make this purchase")
                        .setPositiveButton("Ok",null)
                        .show();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void makeOffer(String counterOffer) {
        mChatRoomRef.child("offer").setValue(counterOffer);
        mChatRoomRef.child("offerBy").setValue(UserModel.uid);
    }

    private String formatOffer(String counterOffer) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        String newOffer = counterOffer;
        try {
            newOffer = nf.format(Double.valueOf(counterOffer));
        }catch (NumberFormatException ex){
            ex.printStackTrace();
        }
        return newOffer;
    }

    private void checkChatRoomExists(String submission) {
        if(mChatRoomRef==null){
            Log.d(TAG,"checkChatRoomExists: ChatRoomRef is null... ");
            // ChatRoom doesn't exist, create a new ChatRoom
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setListing(mListing);
            mChatRoomRef = mDatabase.getReference().child("Chatrooms").push();
            String key = mChatRoomRef.getKey();
            mChatRoomRef.setValue(chatRoom);

            // Add Event Listener, this enables live chat
            mChatRoomRef.addValueEventListener(chatRoomEventListener());

            // Initialize messages listener for Live Chat
            mMessagesRef = mChatRoomRef.child("messages");
            mMessagesRef.addChildEventListener(messagesChildListener());
            //Query messageQuery = mMessagesRef.limitToFirst(50);
            //messageQuery.addChildEventListener(messagesChildListener());

            // Add new Chatroom reference to existing Listing,
            int pos = mListing.getChatRoomIDs() != null ? mListing.getChatRoomIDs().size() : 0;
            DatabaseReference listingRef = mDatabase.getReference().child("Listing").child(mListing.getId()).child("ChatRoomIDs").child(String.valueOf(pos));
            ChatRoomID chatRoomID = new ChatRoomID(key, UserModel.username, UserModel.uid);
            listingRef.setValue(chatRoomID);

        }
        sendMessage(submission);
    }

    private void sendMessage(String submission){
        if(mChatRoomRef!=null) {
            Log.d(TAG,"sendMessage: "+submission);
            Message message = new Message(submission, UserModel.username);
            mMessage.setText("");

            // Reference just the Messages child from the ChatRoom Object and push new message
            DatabaseReference messageRef = mMessagesRef.push();
            messageRef.setValue(message);
        }else{
            Log.d(TAG,"sendMessage: chatroom ref is null...");
            Toast.makeText(this, "the chatroom doens't exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void setProgressBar(final int visibility){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(visibility);
            }
        });
    }

}
