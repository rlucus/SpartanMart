package xyz.spartanmart.spartanmart.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.List;

import xyz.spartanmart.spartanmart.ListingDetailsActivity;
import xyz.spartanmart.spartanmart.R;
import xyz.spartanmart.spartanmart.models.Listing;
import xyz.spartanmart.spartanmart.models.UserModel;

public class ListingRecyclerAdapter extends RecyclerView.Adapter<ListingRecyclerAdapter.ListingViewHolder> {



    public class ListingViewHolder extends RecyclerView.ViewHolder{

        // This ViewHolder is the layout for one Recyclable item
        // On Click Listeners is preference

        private TextView price;
        private TextView title;
        private TextView user;
        private TextView chats;
        private TextView status;
        private ImageView image;
        private RelativeLayout mainLayout;
        private RelativeLayout userLayout;

        public ListingViewHolder(View itemView) {
            super(itemView);
            price = (TextView) itemView.findViewById(R.id.price);
            title = (TextView) itemView.findViewById(R.id.title);
            user = (TextView) itemView.findViewById(R.id.sellerName);
            status = (TextView) itemView.findViewById(R.id.status);
            chats = (TextView) itemView.findViewById(R.id.chats);
            image = (ImageView) itemView.findViewById(R.id.image);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
            userLayout = (RelativeLayout) itemView.findViewById(R.id.status_container);
        }
    }

    private static final String TAG = ListingRecyclerAdapter.class.getSimpleName();

    private Context mContext;
    private List<Listing> mListings;

    public ListingRecyclerAdapter(Context context){
        mContext = context;
    }

    public ListingRecyclerAdapter(Context context, List<Listing> list){
        mContext = context;
        mListings = list;
    }

    @Override
    public ListingRecyclerAdapter.ListingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_listing,parent,false);
        return new ListingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListingRecyclerAdapter.ListingViewHolder holder, int position) {
        Listing listing = mListings.get(position);
        holder.title.setText(listing.getTitle());
        holder.price.setText(listing.getPrice());
        holder.user.setText(listing.getCreator());
        if(listing.getPhotoUrl()!=null){
            try{
                Bitmap imageBitmap = decodeFromFirebaseBase64(listing.getPhotoUrl());
                holder.image.setImageBitmap(imageBitmap);
            }catch (IOException e){
                Log.e(TAG,"Error with bitmap",e);
            }
        }else{
            holder.image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_visibility_off_black_48dp));
        }

        if(listing.getCreatorId().equals(UserModel.uid)){
            holder.userLayout.setVisibility(View.VISIBLE);
            String status = listing.getIsActive() ? "Active" : "Closed";
            holder.status.setText(status);
            String chats = listing.getChatRoomIDs()!=null ? String.valueOf(listing.getChatRoomIDs().size()) : "0";
            holder.chats.setText(chats);
        }else{
            holder.userLayout.setVisibility(View.GONE);
        }

        holder.mainLayout.setOnClickListener(listener(listing));
    }

    private View.OnClickListener listener(final Listing listing){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    // Navigate to the activity
                    String userId = UserModel.uid;
                    String createrId = listing.getCreatorId();
                    Log.d(TAG,"userId: "+userId+", creatorId: "+createrId);
                    boolean isOwner = createrId.equals(userId);
                    Intent intent = new Intent(mContext, ListingDetailsActivity.class);
                    intent.putExtra(ListingDetailsActivity.LISTING, listing.getId());
                    intent.putExtra(ListingDetailsActivity.IS_OWNER, isOwner);
                    mContext.startActivity(intent);
                    Log.d(TAG, "listing: " + listing.getTitle());
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            }
        };
    }

    private void setList(List<Listing> list){
        mListings = list;
        notifyDataSetChanged();
    }

    private static Bitmap decodeFromFirebaseBase64(String image) throws IOException {
        byte[] decodedByteArray = android.util.Base64.decode(image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.length);
    }


    @Override
    public int getItemCount() {
        return mListings.size();
    }
}
