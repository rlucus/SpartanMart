package xyz.spartanmart.spartanmart;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class CreateListingActivity extends AppCompatActivity implements View.OnClickListener,AdapterView.OnItemSelectedListener {

    private static final String TAG = CreateListingActivity.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_PERMISSION =201;

    private FirebaseStorage mStorage;
    private StorageReference mStorageRef;
    private FirebaseDatabase mDatabase;
    private DatabaseReference mListingRef;

    private Spinner mSpinner;
    private EditText mPrice,mTitle,mDescription;
    private ImageView mImageView;
    private Button mSubmit;

    private Uri mImageUri;
    private String mCurrentPhotoPath;
    private String mCategory="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);

        mImageUri=null;
        mStorage = FirebaseStorage.getInstance();
        mDatabase = FirebaseDatabase.getInstance();

        // Bind Views to objects
        mSpinner = (Spinner) findViewById(R.id.spinner);
        mPrice = (EditText) findViewById(R.id.price);
        mTitle = (EditText) findViewById(R.id.title);
        mDescription = (EditText) findViewById(R.id.description);
        mImageView = (ImageView) findViewById(R.id.image);
        mSubmit = (Button) findViewById(R.id.submit);

        // Initialize Spinner
        initSpinner();

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
                String title = mTitle.getText().toString().trim();
                String price = mPrice.getText().toString().trim();
                if (isValid(title, price)&& !mCategory.equals("")) {
                    createListing(title,price,mCategory);
                }
                break;
            case R.id.image:
                takePicture();
                break;
        }
    }

    private void createListing(String title, String price, String category) {
        Log.d(TAG,"createListing: "+title+", "+price+", "+category);
        mDatabase = FirebaseDatabase.getInstance();
        mListingRef = mDatabase.getReference().child("Listing").push();

        String id = mListingRef.getKey();
        String desc = mDescription.getText().toString().trim();

        Listing listing = new Listing(price,title,category,id);
        listing.setCreator(UserModel.username);
        listing.setCreatorId(UserModel.uid);
        listing.setDescription(desc);


        if(mImageUri!=null) {
            storeImage(id,listing);
        }else{
            Log.d(TAG,"no image");
            mListingRef.setValue(listing);
        }
    }

    private void storeImage(String id, final Listing listing) {
        Log.d(TAG,"storeImage");
        mStorageRef = mStorage.getReference().child("images").child(id);

        mImageView.setDrawingCacheEnabled(true);
        mImageView.buildDrawingCache();
        Bitmap bitmap = mImageView.getDrawingCache();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = mStorageRef.putBytes(data);
        Log.d(TAG,"storeImage: uploadTask initialized");
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG,"failed to upload image ",e);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                listing.setPhotoUrl(downloadUrl);
                mListingRef.setValue(listing);
                Log.d(TAG,"onSuccess: "+taskSnapshot.getMetadata());
            }
        });



    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        mCategory= String.valueOf(adapterView.getItemAtPosition(position));
        Log.d(TAG,"onItemSelected: "+mCategory);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        Log.d(TAG,"onNothingSelected");
    }

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

    private void takePicture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(checkMicrophonePermissions()) {
            mImageUri = Uri.fromFile(getOutputMediaFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);

            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private boolean checkMicrophonePermissions() {
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION);
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
                "IMG_"+ timeStamp + ".jpg");
    }

}
