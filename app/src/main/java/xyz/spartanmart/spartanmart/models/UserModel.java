package xyz.spartanmart.spartanmart.models;

import com.google.firebase.auth.FirebaseUser;

public class UserModel {

    public static String email="";
    public static String username="";
    public static String uid="";
    public static double bank;

    public static Listing currentListing;

    public static void setUser(FirebaseUser user) {
        email = user.getEmail();
        username = user.getDisplayName();
        uid = user.getUid();
    }
}
