package xyz.spartanmart.spartanmart.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import xyz.spartanmart.spartanmart.R;
import xyz.spartanmart.spartanmart.models.ChatRoom;

/**
 * Created by stefan on 12/11/2016.
 */

public class InboxRecyclerAdapter extends RecyclerView.Adapter<InboxRecyclerAdapter.InboxViewHolder> {

    private static final String TAG = InboxRecyclerAdapter.class.getSimpleName();

    private Context mContext;
    private List<ChatRoom> mList;

    public InboxRecyclerAdapter(Context context,List<ChatRoom> list) {
        Log.d(TAG,"constructor "+list.size());
        mContext = context;
        mList = list;
    }

    @Override
    public InboxViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG,"onCreateViewHolder");
        View view = LayoutInflater.from(mContext).inflate(R.layout.inbox_item,parent,false);
        return new InboxViewHolder(view);
    }

    @Override
    public void onBindViewHolder(InboxViewHolder holder, int position) {
        Log.d(TAG,"onBindViewHolder: "+position);
        ChatRoom chatRoom = mList.get(position);
        holder.seller.setText(chatRoom.getSellerName());
        holder.listing.setText(chatRoom.getListingName());
        holder.offer.setText(chatRoom.getOffer());
        holder.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // change to chatroom, send chatroomID

            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class InboxViewHolder extends RecyclerView.ViewHolder {

        TextView seller,listing,offer;
        RelativeLayout mainLayout;

        public InboxViewHolder(View itemView) {
            super(itemView);
            seller = (TextView) itemView.findViewById(R.id.seller);
            listing = (TextView) itemView.findViewById(R.id.listing);
            offer = (TextView) itemView.findViewById(R.id.offer);
            mainLayout = (RelativeLayout) itemView.findViewById(R.id.main_layout);
        }
    }
}
