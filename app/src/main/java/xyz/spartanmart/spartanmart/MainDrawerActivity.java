package xyz.spartanmart.spartanmart;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xyz.spartanmart.spartanmart.adapters.ListingRecyclerAdapter;
import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class MainDrawerActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,SearchView.OnQueryTextListener {

    private static final String TAG = MainDrawerActivity.class.getSimpleName();

    // member variables
    private FirebaseDatabase mDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Views
    private NavigationView mNavView;
    private DrawerLayout mDrawer;
    private RecyclerView mRecycler;
    private ListingRecyclerAdapter mAdapter;
    private ProgressBar mProgressBar;

    // Used to hold position in list of categories
    private int mCategoryPos=0;
    private List<Listing> mAllListingsList = new ArrayList<>();
    private List<Listing> mCategoryList = new ArrayList<>();

    private boolean isBrowsingAll = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_drawer);
        //getSupportActionBar().setTitle("SpartanMart");
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("SpartanMart");


        // Bind Views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavView = (NavigationView) findViewById(R.id.nav_view);
        mRecycler = (RecyclerView) findViewById(R.id.recycleView);


        // Initialize Firebase Auth + Listener & check if use is logged in, if not, sent to login
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = listener();

        // Drawer is the physical drawer that slides in and out, Nav View is inside
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.setDrawerListener(toggle);
        toggle.syncState();

        // Nav View holds all the items + header
        mNavView.setNavigationItemSelectedListener(this);

        // This will store the recycle view (list of listings)
        // Layout manager you have options of Linear(list), Grid(netflix-sih), & Staggered Grid
        // I set the adapter after downloadListings()
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        downloadListings();
    }

    /** handles when user presses physical back button when drawer is open*/
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    /** called after onCreate()
     *  add the Auth Listener AFTER both mAuth & mAuthListener have been initialized && !=null
     * */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
        mAuth.addAuthStateListener(mAuthListener);
    }

    /**this is default (bottom of activity stack),
     * only called when user exits app or clears stack
     */
    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener!=null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    /**
     * Add and Inflate menu to the toolbar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        if(searchItem!=null){
            SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setOnQueryTextListener(this);
        }
        return true;
    }

    /**
     * when a user presses a menu item (currently only action_signout), handle it
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_signout:
                signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the button clicks for when a user presses a navigation item
     * @param item
     * @return
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id){
            case R.id.nav_browse:
                if(!isBrowsingAll) {
                    mAdapter = new ListingRecyclerAdapter(this, mAllListingsList);
                    mRecycler.setAdapter(mAdapter);
                }
                isBrowsingAll=true;
                break;
            case R.id.nav_categories:
                isBrowsingAll=false;
                showCategoryDialog();
                break;
            case R.id.nav_post_listing:
                startActivity(new Intent(MainDrawerActivity.this,CreateListingActivity.class));
                break;
            case R.id.nav_my_listings:
                showMyListings();
                break;
        }

        // close the drawer when user presses item
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showCategoryDialog() {
        // Create a list of the array stored in the R.array.categories list
        // Create a pop-up dialog displaying the category items and handle button clicks
        final List<String> categoryList = Arrays.asList(getResources().getStringArray(R.array.categories));
        new AlertDialog.Builder(this)
                .setTitle("Pick a Category")
                .setSingleChoiceItems(R.array.categories, mCategoryPos, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        // When user clicks a category item, save the position to find in list
                        mCategoryPos = position;
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User picked a category position, handle it
                        handleCategorySelection(categoryList.get(mCategoryPos));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .show();

    }

    /**
     * sort the Firebase Query in order of those items who's category is 'category'
     * Ceate a new query (1st 50-100), populate it, sort it, then add the SingleEventListener
     * You could do addEventListener, but you don't want it updating whenever a listing is added
     * because that's a lot of traffic (if you have lots of users)
     * @param category
     */
    private void handleCategorySelection(String category) {
        Log.d(TAG,"handleCategorySelection: "+category);
        mCategoryList = new ArrayList<>();
        for(Listing listing: mAllListingsList){
            if(listing.getCategory().equals(category)){
                mCategoryList.add(listing);
            }
        }

        mAdapter = new ListingRecyclerAdapter(this,mCategoryList);
        mRecycler.setAdapter(mAdapter);


    }

    private void showMyListings(){
        List<Listing> myListings = new ArrayList<>();
        for(Listing listing: mAllListingsList){
            if(listing.getCreatorId().equals(UserModel.uid)){
                myListings.add(listing);
            }
        }
        mAdapter = new ListingRecyclerAdapter(this,myListings);
        mRecycler.setAdapter(mAdapter);
    }


    /**
     * Sign out user from Auth and navigate to login screen
     */
    private void signOut() {
        mAuth.signOut();
        navigateToLogin();
    }

    /**
     * Navigate to login screen, you can pass objects or flags to the intent
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainDrawerActivity.this,LoginActivity.class);
        startActivity(intent);
    }

    /**
     * Check whether the user is logged in or not
     * if logged in, update the UI and change the UserModel static class to hold the user
     * for this session
     * @return
     */
    private FirebaseAuth.AuthStateListener listener(){
        return new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user !=null){
                    // User is signed in
                    Log.d(TAG,"onAuthStateChanged:signed_in: "+user.getUid());
                    fetchUserFromFirebaseDatabase(user.getUid());
                    UserModel.setUser(user);
                }else{
                    // User is signed out
                    Log.d(TAG,"onAuthStateChanged:signed_out");
                    navigateToLogin();
                }
            }
        };
    }

    private void fetchUserFromFirebaseDatabase(final String uid) {

        DatabaseReference userRef = mDatabase.getReference().child("Users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    UserModel.uid = uid;
                    UserModel.username = (String) dataSnapshot.child("username").getValue();
                    Number number = (Number) dataSnapshot.child("bank").getValue();
                    UserModel.bank = number.doubleValue();
                    updateUI();
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateUI() {
        // Get the header view within the Navigation View
        View headerView = mNavView.getHeaderView(0);

        // Bind the views to objects
        TextView email = (TextView) headerView.findViewById(R.id.email);
        TextView username = (TextView) headerView.findViewById(R.id.sellerName);

        // update UI
        email.setText(UserModel.email);
        username.setText(UserModel.username);
    }

    /**
     * Get the Database instance, reference the db to the Listings node, and query the items
     * I limit the query to first 50 items in order of their title
     */
    private void downloadListings() {
        Log.d(TAG,"downloadListings");
        mProgressBar.setVisibility(View.VISIBLE);
        mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference ref = mDatabase.getReference().child("Listing");
        Query query = ref.getRef().orderByChild("title");
        query.limitToFirst(50);
        query.addValueEventListener(listingValueListener());
    }

    /**
     * Listener for getting the listings
     * @return
     */
    private ValueEventListener listingValueListener(){
        return new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,"listingValueListener:onDataChanged: "+dataSnapshot);
                setProgressBar(View.GONE);
                // Create List to hold all the listings to pass to the recycler
                // Iterate through the dataSnapshots children
                // DataSnapshot looks liek a json array
                mAllListingsList = new ArrayList<>();
                Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
                if(snapshotIterator!=null) {
                    for (DataSnapshot aSnapshotIterator : snapshotIterator) {
                        Listing listing = aSnapshotIterator.getValue(Listing.class);
                        Log.d(TAG,":listingValueListener:onDataChange: wordListId: "+listing.getTitle());
                        if(listing.getIsActive()) {
                            Log.d(TAG,"getIsActive: true");
                            mAllListingsList.add(listing);
                        }else{
                            Log.d(TAG,"getIsActive: false");
                        }
                    }

                    mAdapter = new ListingRecyclerAdapter(MainDrawerActivity.this, mAllListingsList);
                    mRecycler.setAdapter(mAdapter);
                }else{
                    Log.d(TAG,":listingValueListener:onDataChange: empty snapshotIterator");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG,"onCancelled ",databaseError.toException());
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

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    private List<Listing> filter(List<Listing> list,String query){
        query = query.toLowerCase();

        final List<Listing> filteredList = new ArrayList<>();
        for(Listing listing: list){
            final String text = listing.getTitle().toLowerCase();
            if(text.contains(query)){
                filteredList.add(listing);
            }
        }
        return filteredList;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        if(mAdapter!=null) {
            mAdapter = new ListingRecyclerAdapter(this, filter(isBrowsingAll ? mAllListingsList : mCategoryList, query));
            mRecycler.setAdapter(mAdapter);
        }
        return true;
    }
}
