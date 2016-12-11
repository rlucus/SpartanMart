package xyz.spartanmart.spartanmart;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import xyz.spartanmart.spartanmart.models.UserModel;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = LoginActivity.class.getSimpleName();
    private static final int RC_SIGN_IN = 201;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private GoogleApiClient mGoogleApiClient;

    private EditText mEmailET,mPasswordET;
    private TextView mStatusTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Log.d(TAG,"onCreate");

        // Bind Views to Objects
        mEmailET = (EditText) findViewById(R.id.email);
        mPasswordET = (EditText) findViewById(R.id.password);
        mStatusTV = (TextView) findViewById(R.id.status);

        // Used for authorizing user in firebase
        mAuth = FirebaseAuth.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user !=null){
                    // User is signed in
                    Log.d(TAG,"onAuthStateChanged:signed_in: "+user.getUid());
                    UserModel.setUser(user);
                    finish();
                }else{
                    // User is signed out
                    Log.d(TAG,"onAuthStateChanged:signed_out");
                }
            }
        };

        // Google Sign In Auth
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to GoogleSignIn.API and the options above.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Set Listeners to Objects
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.create_user).setOnClickListener(this);
        findViewById(R.id.google_sign_in).setOnClickListener(this);
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sign_in_button:
                String email = mEmailET.getText().toString().trim();
                String password = mPasswordET.getText().toString().trim();
                if(isValid(email,password)){
                    signIn(email,password);
                }
                break;
            case R.id.create_user:
                // Create a user via email/password method
                Intent intent = new Intent(this,CreateUserActivity.class);
                startActivity(intent);
                break;
            case R.id.google_sign_in:
                Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                startActivityForResult(signInIntent, RC_SIGN_IN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }else{

                            // User was successfully added, use Uid to make a node in the 'Users' tab in firebase DB
                            String uid = mAuth.getCurrentUser().getUid();
                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            DatabaseReference userRef = database.getReference().child("Users").child(uid);
                            userRef.child("username").setValue(task.getResult().getUser().getDisplayName());
                            userRef.child("email").setValue(task.getResult().getUser().getEmail());
                            finish();
                        }
                        // ...
                    }
                });
    }

    private boolean isValid(String email, String password) {
        boolean valid = true;

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

        if(TextUtils.isEmpty(password)){
            mPasswordET.setError("Required");
            valid=false;
        }else{
            mPasswordET.setError(null);
        }

        return valid;

    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG,"signIn:onComplete: "+task.isSuccessful());
                        if(!task.isSuccessful()){
                            Log.e(TAG,"signIn:onComplete unsuccessful. ",task.getException());
                            mStatusTV.setText(task.getException().getLocalizedMessage());
                        }else{
                            finish();
                        }
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG,"onConnectionFailed: "+connectionResult);
    }
}
