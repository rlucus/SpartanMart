package xyz.spartanmart.spartanmart;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import xyz.spartanmart.spartanmart.adapters.InboxRecyclerAdapter;
import xyz.spartanmart.spartanmart.models.ChatRoom;
import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class AccountActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private static final String TAG = AccountActivity.class.getSimpleName();

    // Views
    private TextView mUsername, mEmail, mNoContent;
    private RecyclerView mRecycler;
    private InboxRecyclerAdapter mAdapter;
    private SearchView mSearchView;
    private ProgressBar mProgressBar;

    // Firebase
    private FirebaseDatabase mDatabase;
    private DatabaseReference mChatRoomsRef;

    // Internal Variables
    private List<ChatRoom> mChatRoomList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        getSupportActionBar().setTitle("Account");

        // Bind Views
        mUsername = (TextView) findViewById(R.id.username);
        mEmail = (TextView) findViewById(R.id.email);
        mNoContent = (TextView) findViewById(R.id.no_content);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mRecycler = (RecyclerView) findViewById(R.id.recyclerView);
        mSearchView = (SearchView) findViewById(R.id.searchView);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));

        // Set Views
        mUsername.setText(UserModel.username);
        mEmail.setText(UserModel.email);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance();
        mChatRoomsRef = mDatabase.getReference().child("Chatrooms");
        fetchChatRooms();

        mSearchView.setOnQueryTextListener(this);
    }

    private void fetchChatRooms() {
        mChatRoomList = new ArrayList<>();
        Log.d(TAG,"fetchChatRooms");
        mProgressBar.setVisibility(View.VISIBLE);
        Query query = mChatRoomsRef.limitToFirst(50);
        Log.d(TAG,"reference: "+mChatRoomsRef);
        query.addListenerForSingleValueEvent(inboxEventListener());
    }

    private void setProgressBar(final int visibility){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(visibility);
            }
        });
    }

    private ValueEventListener inboxEventListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,"listingValueListener:onDataChanged: "+dataSnapshot);
                setProgressBar(View.GONE);
                // Create List to hold all the listings to pass to the recycler
                // Iterate through the dataSnapshots children
                // DataSnapshot looks liek a json array
                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                if(snapshotIterator!=null) {
                    for (DataSnapshot aSnapshotIterator : snapshotIterator) {
                        ChatRoom chatRoom = new ChatRoom();

                        try {
                            // Download all key:value pairs for chatroom, there are more efficient ways to do this
                            chatRoom.setId(aSnapshotIterator.getKey());
                            chatRoom.setBuyerAgree((boolean) aSnapshotIterator.child("buyerAgree").getValue());
                            chatRoom.setBuyerID((String) aSnapshotIterator.child("buyerID").getValue());
                            chatRoom.setBuyerName((String) aSnapshotIterator.child("buyerName").getValue());
                            chatRoom.setListingID((String) aSnapshotIterator.child("listingID").getValue());
                            chatRoom.setListingName((String) aSnapshotIterator.child("listingName").getValue());
                            chatRoom.setOffer((String) aSnapshotIterator.child("offer").getValue());
                            chatRoom.setOfferBy((String) aSnapshotIterator.child("offerBy").getValue());
                            chatRoom.setPrice((String) aSnapshotIterator.child("price").getValue());
                            chatRoom.setSellerAgree((boolean) aSnapshotIterator.child("sellerAgree").getValue());
                            chatRoom.setSellerID((String) aSnapshotIterator.child("sellerID").getValue());
                            chatRoom.setSellerName((String) aSnapshotIterator.child("sellerName").getValue());
                            chatRoom.setIsActive((boolean) aSnapshotIterator.child("isActive").getValue());
                            Log.d(TAG, ":listingValueListener:onDataChange: \nwordListName: " + chatRoom.getListingName()+
                                    "\nbuyer: "+chatRoom.getBuyerID()+
                                    "\nseller: "+chatRoom.getSellerID()+
                                    "\nUserID: "+UserModel.uid);
                            if (chatRoom.isActive()) {
                                Log.d(TAG, "getIsActive: true");
                                if (chatRoom.getSellerID().equals(UserModel.uid) | chatRoom.getBuyerID().equals(UserModel.uid)) {
                                    if (!isDuplicate(chatRoom)) mChatRoomList.add(chatRoom);
                                } else {
                                    Log.d(TAG, "no match with seller or buyer id");
                                }
                            } else {
                                Log.d(TAG, "getIsActive: false");
                            }
                        }catch (NullPointerException ex){
                            ex.printStackTrace();
                        }
                    }

                    // Because this is in a network callback, i set this in the UI thread
                    // errors arise due to Views being changed on separate thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!mChatRoomList.isEmpty()) {
                                Log.d(TAG,"inboxEventListener:onDataChange mChatRoomList: "+mChatRoomList.size());
                                mNoContent.setVisibility(View.GONE);
                                mRecycler.setVisibility(View.VISIBLE);
                                mAdapter = new InboxRecyclerAdapter(AccountActivity.this, mChatRoomList);
                                mRecycler.setAdapter(mAdapter);
                            }else{
                                Log.d(TAG,"inboxEventListener:onDataChange empty!");
                                mNoContent.setVisibility(View.VISIBLE);
                                mRecycler.setVisibility(View.GONE);
                            }
                        }
                    });

                }else{
                    Log.d(TAG,":listingValueListener:onDataChange: empty snapshotIterator");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    /** Check for Duplicates before adding them to list in Recycler */
    private boolean isDuplicate(ChatRoom input_chatRoom) {
        for(ChatRoom chatRoom: mChatRoomList){
            if(chatRoom.getId().equals(input_chatRoom.getId())){
                Log.d(TAG,"isDuplicate: Duplicate Found!");
                return true;
            }
        }
        return false;
    }

    /**
     * Creates 'Refresh' button in top right corner of actionbar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_account,menu);
        return true;
    }

    /**
     * When user presses the refresh button, download all the chatrooms
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_refresh:
                fetchChatRooms();
                break;
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if(mAdapter!=null) {
            mAdapter = new InboxRecyclerAdapter(this, filter(mChatRoomList, query));
            mRecycler.setAdapter(mAdapter);
        }
        return true;
    }

    /**
     * Not a very efficient way of filtering through a recycler list but it works
     * @param list  : list of all words currently in inbox
     * @param query : query user entered in
     * @return
     */
    private List<ChatRoom> filter(List<ChatRoom> list, String query){
        query = query.toLowerCase();
        final List<ChatRoom> filteredList = new ArrayList<>();
        for(ChatRoom chatroom: list){
            final String text = chatroom.getListingName().toLowerCase();
            if(text.contains(query)){
                filteredList.add(chatroom);
            }
        }
        return filteredList;
    }
}
