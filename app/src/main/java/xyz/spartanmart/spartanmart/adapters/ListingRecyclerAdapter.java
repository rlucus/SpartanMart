package xyz.spartanmart.spartanmart.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import xyz.spartanmart.spartanmart.R;
import xyz.spartanmart.spartanmart.models.Listing;

/**
 * Created by stefan on 10/27/2016.
 */

public class ListingRecyclerAdapter extends RecyclerView.Adapter<ListingRecyclerAdapter.ListingViewHolder> {

    public class ListingViewHolder extends RecyclerView.ViewHolder{

        // This ViewHolder is the layout for one Recyclable item
        // On Click Listeners is preference

        private TextView price;
        private TextView title;
        private TextView user;
        private ImageView image;
        private RelativeLayout mainLayout;

        public ListingViewHolder(View itemView) {
            super(itemView);
            price = (TextView) itemView.findViewById(R.id.price);
            title = (TextView) itemView.findViewById(R.id.title);
            user = (TextView) itemView.findViewById(R.id.username);
            image = (ImageView) itemView.findViewById(R.id.image);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.relativeLayout);
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
        holder.mainLayout.setOnClickListener(listener(listing));
    }

    private View.OnClickListener listener(final Listing listing){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Navigate to the activity
                //Intent intent = new Intent(mContext,ViewListingActivity.class);
                Log.d(TAG,"listing: "+listing.getTitle());
            }
        };
    }

    private void setList(List<Listing> list){
        mListings = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mListings.size();
    }
}
