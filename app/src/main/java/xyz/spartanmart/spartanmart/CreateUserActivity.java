package xyz.spartanmart.spartanmart;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by stefan on 10/26/2016.
 */

public class CreateUserActivity extends AppCompatActivity {

    // Constants
    private static final String TAG = CreateUserActivity.class.getSimpleName();

    // Firebase Variables
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    // Views
    private EditText mConfirmET,
            mEmailET,
            mPasswordET,
            mUsernameET;
    private Button mSubmit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        // Bind Views to objects
        mConfirmET = (EditText) findViewById(R.id.password_confirm);
        mEmailET = (EditText) findViewById(R.id.email);
        mPasswordET = (EditText) findViewById(R.id.password);
        mUsernameET = (EditText) findViewById(R.id.username);
        mSubmit = (Button) findViewById(R.id.create_user);

        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user !=null){
                    // User is signed in
                    Log.d(TAG,"onAuthStateChanged:signed_in: "+user.getUid());

                }else{
                    // User is signed out
                    Log.d(TAG,"onAuthStateChanged:signed_out");
                }
            }
        };

        mSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String confirm = ""+mConfirmET.getText().toString().trim();
                String email = ""+mEmailET.getText().toString().trim();
                String password = ""+mPasswordET.getText().toString().trim();
                String username = ""+mUsernameET.getText().toString().trim();
                if(isValid(email,password,confirm)){
                    createUser(email,password,username);
                }
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener!=null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void createUser(final String email, final String password, final String username) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG,"createUser:onComplete "+task.isSuccessful());
                        if(!task.isSuccessful()){
                            Log.e(TAG,"createUser:onComplete fail",task.getException());
                        }else{
                            addUserToDatabase(email,password,username);
                            Intent main = new Intent(CreateUserActivity.this, MainDrawerActivity.class);
                            main.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(main);
                        }
                    }
                });
    }

    private void addUserToDatabase(String email, String password, String username) {
        Log.d(TAG,"addUserToDatabase()");
        if(mAuth.getCurrentUser()!=null) {
            String uid = mAuth.getCurrentUser().getUid();
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference().child("Users").child(uid);
            userRef.child("username").setValue(username);
            userRef.child("email").setValue(email);
        }
    }

    /** Checks Valid SJSU Email and if the passwords match*/
    private boolean isValid(String email, String password, String confirm) {
        boolean valid = true;

        // Check that it is a valid email & sjsu email
        String sjsuRegex = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@sjsu\\.edu";
        if (TextUtils.isEmpty(email)) {
            mEmailET.setError("Required");
            valid = false;
        }else if(!email.matches(sjsuRegex)){
            mEmailET.setError("SJSU Email Required");
            valid = false;
        }else{
            mEmailET.setError(null);
        }

        // check the passwords
        if(TextUtils.isEmpty(password)){
            mPasswordET.setError("Required");
            valid=false;
        }else{
            if(!confirm.equals(password)){
                mConfirmET.setError("Must Match Password");
                valid = false;
            }
            mPasswordET.setError(null);
        }

        return valid;

    }
}
