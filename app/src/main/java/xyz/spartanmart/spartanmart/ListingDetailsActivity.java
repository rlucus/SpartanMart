package xyz.spartanmart.spartanmart;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xyz.spartanmart.spartanmart.models.ChatRoomID;
import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class ListingDetailsActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = ListingDetailsActivity.class.getSimpleName();
    public static final String IS_OWNER = "isOwner";
    public static final String LISTING = "listing";

    // Views
    private TextView mSellerTV, mTitleTV, mPriceTV, mDescriptionTV;
    private Button mMessage;
    private ImageView mImage;
    private ProgressBar mProgressBar;
    private RelativeLayout mButtonContainer;

    // Firebase
    private FirebaseDatabase mDatabase;
    private DatabaseReference mListingRef;

    // internal variables
    private List<ChatRoomID> mChatRoomIDList;
    private Listing mListing;
    private boolean isOwner=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);
        getSupportActionBar().setTitle("Listing Details");
        // Firebase
        mDatabase = FirebaseDatabase.getInstance();

        // Bind Views to objects
        mMessage = (Button) findViewById(R.id.message);
        mSellerTV = (TextView) findViewById(R.id.seller);
        mTitleTV = (TextView) findViewById(R.id.title);
        mPriceTV = (TextView) findViewById(R.id.price);
        mDescriptionTV = (TextView) findViewById(R.id.description);
        mImage = (ImageView) findViewById(R.id.image);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mButtonContainer = (RelativeLayout) findViewById(R.id.button_container);

        // Fetch Listing and if owner is viewing from calling activity
        getOrigin();

        // set whether the list of interested buyers is shown or the message button
        //mViewFlipper.setDisplayedChild(isOwner ? 0 : 1);

        // when buttons are clicked, do something
        findViewById(R.id.edit).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);
        findViewById(R.id.message).setOnClickListener(this);
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.edit:
                editListing();
                break;
            case R.id.remove:
                new AlertDialog.Builder(this)
                        .setTitle("Remove Listing")
                        .setMessage("Are you sure you want to delete this listing?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                removeListing();
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .setCancelable(false)
                        .show();
                break;
            case R.id.message:
                startMessageActivity();
                break;
        }

    }

    private void editListing(){
        Intent intent = new Intent(this,CreateListingActivity.class);
        intent.putExtra("listingID",mListing.getId());
        startActivity(intent);
    }

    private void removeListing() {
        mListingRef.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if(databaseError!=null){
                    Log.e(TAG,"onComplete: ",databaseError.toException());
                }else {
                    Log.d(TAG, "onComplete: " + databaseReference);
                    finish();
                }
            }
        });
    }

    private void startMessageActivity() {
        if(!isOwner) {
            Intent intent = new Intent(this, ListingChatActivity.class);
            boolean matchFound = false;
            List<ChatRoomID> IDlist = mListing.getChatRoomIDs();
            // If there Chatroom ID's exist in database, iterate through them for matching userID
            if (IDlist != null) {
                Log.d(TAG, "startMessageActivity: mChatRoomList!=null");
                for (ChatRoomID chatRoomID : IDlist) {
                    Log.d(TAG, "startMessageActivity: chatroomBuyerID: " + chatRoomID.getBuyerID() + ", " + chatRoomID.getBuyerName());
                    if (chatRoomID.getBuyerID().equals(UserModel.uid)) {
                        Log.d(TAG, "startMessageActivity, found match");
                        matchFound = true;
                        // There is an existing chatroom, load it up
                        intent.putExtra("chatroomID", chatRoomID.getChatroomID());
                        startActivity(intent);
                        break;
                    }
                }
            } else {
                Log.d(TAG, "startMessageActivity: IDlist");
            }
            // If no chatroom ID's exist or no match is found, send the whole listing
            if (!matchFound) {
                Log.d(TAG, "startMessageActivity: no match found");
                intent.putExtra("listingID", mListing.getId());
                startActivity(intent);
            }
        }else{
            mChatRoomIDList = mListing.getChatRoomIDs();
            if(mChatRoomIDList !=null){
                ArrayList<String> list = new ArrayList<>();
                for(int i = 0; i< mChatRoomIDList.size(); i++){
                    list.add(mChatRoomIDList.get(i).getBuyerName());
                }
                CharSequence[] items = list.toArray(new CharSequence[list.size()]);
                Log.d(TAG,"createListView size: "+list.size());

                new AlertDialog.Builder(this)
                        .setTitle("ChatRooms")
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int position) {
                                String chatRoomId = mChatRoomIDList.get(position).getChatroomID();
                                Intent intent = new Intent(ListingDetailsActivity.this,ListingChatActivity.class);
                                intent.putExtra("chatroomID",chatRoomId);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel",null)
                        .show();

            }
        }
    }

    public void getOrigin() {
        Intent intent = getIntent();
        String listingID = intent.getStringExtra(LISTING);
        isOwner = intent.getBooleanExtra(IS_OWNER,false);
        Log.d(TAG,"getOrigin: "+isOwner);

        // Use Listing ID from Intent to get actual Listing
        // Passing Actual listing is too big of a transaction
        // Alternative option is store the listing as a static object in a class
        mProgressBar.setVisibility(View.VISIBLE);
        mListingRef = mDatabase.getReference().child("Listing").child(listingID);
        mListingRef.addValueEventListener(listingListener());

        //updateView();
    }

    private void updateView() {
        mTitleTV.setText(mListing.getTitle());
        mPriceTV.setText(mListing.getPrice());
        mDescriptionTV.setText(mListing.getDescription());
        mSellerTV.setText(mListing.getCreator());
        if(mListing.getPhotoUrl()!=null){
            try{
                Bitmap imageBitmap = decodeFromFirebaseBase64(mListing.getPhotoUrl());
                mImage.setImageBitmap(imageBitmap);
            }catch (IOException e){
                Log.e(TAG,"Error with bitmap",e);
            }
        }else{
            mImage.setImageDrawable(this.getResources().getDrawable(R.drawable.ic_visibility_off_black_48dp));
        }
    }

    public static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }

    private ValueEventListener listingListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,"listingListener:onDataChange "+dataSnapshot.exists());
                setProgressBar(View.GONE);
                mListing = dataSnapshot.getValue(Listing.class);
                if(isOwner){
                    mMessage.setText("Interested Buyers");
                }else{
                    mButtonContainer.setVisibility(View.GONE);
                }
                updateView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG,"listingListener:onCancelled",databaseError.toException());
                setProgressBar(View.GONE);
            }
        };
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
