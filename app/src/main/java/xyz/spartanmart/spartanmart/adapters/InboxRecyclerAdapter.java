package xyz.spartanmart.spartanmart.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import xyz.spartanmart.spartanmart.ListingChatActivity;
import xyz.spartanmart.spartanmart.R;
import xyz.spartanmart.spartanmart.models.ChatRoom;

/**
 * Created by stefan on 12/11/2016.
 */

public class InboxRecyclerAdapter extends RecyclerView.Adapter<InboxRecyclerAdapter.InboxViewHolder> {

    private static final String TAG = InboxRecyclerAdapter.class.getSimpleName();

    // Member Variables
    private Context mContext;
    private List<ChatRoom> mList;

    // Constructor for RecyclerView Adapter
    public InboxRecyclerAdapter(Context context,List<ChatRoom> list) {
        Log.d(TAG,"constructor "+list.size());
        mContext = context;
        mList = list;
    }

    // Create the View for each item in the list
    @Override
    public InboxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(mContext).inflate(R.layout.inbox_item,parent,false);
        return new InboxViewHolder(view);
    }

    // Populate the View you created in onCreateViewHolder with items
    @Override
    public void onBindViewHolder(InboxViewHolder holder, int position) {
        Log.d(TAG,"onBindViewHolder: "+position);
        final ChatRoom chatRoom = mList.get(position);
        holder.seller.setText(chatRoom.getSellerName());
        holder.buyer.setText(chatRoom.getBuyerName());
        holder.listing.setText(chatRoom.getListingName());
        holder.offer.setText(chatRoom.getOffer());
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // change to ChatRoom, send chatroomID
                Intent intent = new Intent(mContext, ListingChatActivity.class);
                intent.putExtra("chatroomID",chatRoom.getId());
                mContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

     // This class relates the layout View in onCreateViewHolder to the objects in onBindViewHolder
    class InboxViewHolder extends RecyclerView.ViewHolder {

        TextView seller,buyer,listing,offer;
        RelativeLayout mainLayout;

        InboxViewHolder(View itemView) {
            super(itemView);
            seller = (TextView) itemView.findViewById(R.id.seller);
            buyer = (TextView) itemView.findViewById(R.id.buyer);
            listing = (TextView) itemView.findViewById(R.id.listing);
            offer = (TextView) itemView.findViewById(R.id.offer);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.main_layout);
        }
    }
}
