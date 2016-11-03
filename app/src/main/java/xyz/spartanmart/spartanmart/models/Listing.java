package xyz.spartanmart.spartanmart.models;

import android.net.Uri;

/**
 * Created by stefan on 10/27/2016.
 */

public class Listing {

    private static final String TAG = Listing.class.getSimpleName();

    private String id;
    private String price;
    private String title;
    private String category;
    private String creator;
    private String creatorId;
    private String description;
    private Uri photoUrl;

    public Listing(){

    }

    public Listing(String price, String title, String category, String id){
        this.price = price;
        this.title = title;
        this.category = category;
        this.id = id;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Uri getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(Uri photoUrl) {
        this.photoUrl = photoUrl;
    }
}
