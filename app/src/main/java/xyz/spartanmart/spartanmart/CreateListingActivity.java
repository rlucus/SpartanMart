package xyz.spartanmart.spartanmart;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class CreateListingActivity extends AppCompatActivity implements View.OnClickListener,AdapterView.OnItemSelectedListener {

    private static final String TAG = CreateListingActivity.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PERMISSION =201;

    private FirebaseDatabase mDatabase;
    private DatabaseReference mListingRef;

    private Spinner mSpinner;
    private EditText mPrice,mTitle,mDescription;
    private ImageView mImageView;
    private Button mSubmit;

    private Uri mImageUri;
    private String mCurrentPhotoPath;
    private String mCategory="";

    private Listing mListing;
    private boolean isEditing=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        // Variables for uploading to Firebase
        mImageUri = null;
        mDatabase = FirebaseDatabase.getInstance();

        // Bind Views to objects
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mPrice = (EditText) findViewById(R.id.price);
        mTitle = (EditText) findViewById(R.id.title);
        mDescription = (EditText) findViewById(R.id.description);
        mImageView = (ImageView) findViewById(R.id.image);
        mSubmit = (Button) findViewById(R.id.submit);

        // Initialize dropdown list of categories
        initSpinner();

        // Check to see if we are editing an existing Listing or creating a new one
        getOrigin();

        // Set OnClickListeners
        mSpinner.setOnItemSelectedListener(this);
        mSubmit.setOnClickListener(this);
        mImageView.setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Glide.with(this)
                    .load(mImageUri)
                    .into(mImageView);
        }
    }

    private void initSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.submit:
                // Grab Strings from  TextViews, trim the ends of whitespace, and create listing
                String title = mTitle.getText().toString().trim();
                String price = mPrice.getText().toString().trim();
                if (isValid(title, price)&& !mCategory.equals("")) {
                    createListing(title,price,mCategory);
                }
                break;
            case R.id.image:
                // Take Photo
                if(checkMicrophonePermissions()) {
                    try {
                        ContentValues values = new ContentValues(1);
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                        mImageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        takePicture();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    /**
     * Create new listing and prepare to upload to firebase
     * @param title     : title set by user
     * @param price     : price set by user
     * @param category  : category set by user
     */
    private void createListing(String title, String price, String category) {
        Log.d(TAG,"createListing: "+title+", "+price+", "+category);
        mDatabase = FirebaseDatabase.getInstance();

        // Check if user is editing an existing listing or is creating  new one
        if(!isEditing) {
            mListingRef = mDatabase.getReference().child("Listing").push();

        }else{
            mListingRef = mDatabase.getReference().child("Listing").child(mListing.getId());
        }

        String id = isEditing ? mListing.getId() : mListingRef.getKey();
        String desc = mDescription.getText().toString().trim();

        // Create new listing & prepare to upload it
        Listing listing = isEditing ? mListing : new Listing();
        listing.setCategory(category);
        listing.setCreator(UserModel.username);
        listing.setCreatorId(UserModel.uid);
        listing.setDescription(desc);
        listing.setId(id);
        listing.setIsActive(true);
        listing.setPrice(price);
        listing.setTitle(title);

        // Check if user set a photo or not
        if(mImageUri!=null) {
            encodeBitmapAndSaveToFirebase(listing);
        }else{
            Log.d(TAG,"no image");
            mListingRef.setValue(listing);
            finish();
        }
    }

    /**
     * Encode the image to a string and upload to firebase, 'finish()' to close activity
     * @param listing
     */
    public void encodeBitmapAndSaveToFirebase(final Listing listing) {
        mImageView.setDrawingCacheEnabled(true);
        mImageView.buildDrawingCache();
        Bitmap bitmap = mImageView.getDrawingCache();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String imageEncoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
            listing.setPhotoUrl(imageEncoded);
            mListingRef.setValue(listing);
            finish();
        }catch(NullPointerException e){
            e.printStackTrace();
            Toast.makeText(this, "Error submitting listing", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Spinner that controls the Category picked
     * @param adapterView
     * @param view
     * @param position
     * @param l
     */
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        mCategory= String.valueOf(adapterView.getItemAtPosition(position));
        Log.d(TAG,"onItemSelected: "+mCategory);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.d(TAG,"onNothingSelected");
    }

    // Check that the input is valid, requires a title and a price
    private boolean isValid(String title, String price) {
        boolean valid = true;

        // Check title
        if (TextUtils.isEmpty(title)) {
            mTitle.setError("Required");
            valid = false;
        }else{
            mTitle.setError(null);
        }

        // Check price
        if(TextUtils.isEmpty(price)){
            mPrice.setError("Required");
            valid=false;
        }else{
            mPrice.setError(null);
        }
        return valid;
    }

    /**
      * Start Activity to take a picture. When photo is take, it calls onActivityResult()
      */
    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if(checkMicrophonePermissions()) {
            //mImageUri = Uri.fromFile(getOutputMediaFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
            intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY,1);

            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * On phones with api >24, the phone requires the user give the app permission to use the
     * camera and external storage
     * @return
     *
     * Nexus Strict Mode problem HERE
     *
     */
    private boolean checkMicrophonePermissions() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
            Log.d(TAG,":checkMicrophonePermissions: false");
            return false;
        }else{
            Log.d(TAG,":checkMicrophonePermissions: true");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG,"onRequestPermissionResult: granted");
            }
        }
    }

    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraDemo");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".png");
    }

    /**
     * Get information passed from a previous activity
     * 'MainDrawerActivity' or 'ListingChatActivity'
     */
    public void getOrigin() {
        Intent intent = getIntent();
        if(intent.hasExtra("listingID")){
            isEditing=true;
            String id = intent.getStringExtra("listingID");
            DatabaseReference listingRef = mDatabase.getReference().child("Listing").child(id);
            listingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG,"getOrigin:onDataChange: "+dataSnapshot.exists());
                    mListing = dataSnapshot.getValue(Listing.class);

                    updateUI();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(TAG,"getOrigin:onCancelled: ",databaseError.toException());

                }
            });
        }
    }

    private void updateUI() {
        Log.d(TAG,"updateUI");
        if(mListing!=null){
            mTitle.setText(mListing.getTitle());
            mPrice.setText(mListing.getPrice());
            mDescription.setText(mListing.getDescription());

            // Load Photo
            if(mListing.getPhotoUrl()!=null){
                try{
                    Bitmap imageBitmap = decodeFromFirebaseBase64(mListing.getPhotoUrl());
                    mImageView.setImageBitmap(imageBitmap);
                }catch (IOException e){
                    Log.e(TAG,"Error with bitmap",e);
                }
            }

            // Get Category
            String[] categories = getResources().getStringArray(R.array.categories);
            List<String> list = Arrays.asList(categories);
            for(int i=0;i<list.size();i++){
                if(list.get(i).equals(mListing.getCategory())){
                    mSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    /**
     * Decode string from firebase into a useable Bitmap
     * @param image     : encoded image
     * @return
     * @throws IOException
     */
    private static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }
}
