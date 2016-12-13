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
import xyz.spartanmart.spartanmart.models.Message;

public class ChatRecyclerAdapter extends RecyclerView.Adapter<ChatRecyclerAdapter.ChatViewHolder> {

    private static final String TAG = ChatRecyclerAdapter.class.getSimpleName();

    private List<Message> mMessageList;
    private Context mContext;
    private String mUsername;

    public ChatRecyclerAdapter(List<Message> mMessageList, Context mContext, String mUsername) {
        this.mMessageList = mMessageList;
        this.mContext = mContext;
        this.mUsername = mUsername;
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.chat_user,parent,false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        Log.d(TAG,"onBindViewHolder: "+position);
        Message message = mMessageList.get(position);

        holder.message.setText(message.getMessage());
        RelativeLayout.LayoutParams relativeParams = (RelativeLayout.LayoutParams)holder.relativeLayout.getLayoutParams();
        if(!message.getSender().equals(mUsername)){
            relativeParams.setMargins(5, 5, 50, 5);  // left, top, right, bottom
            holder.relativeLayout.setLayoutParams(relativeParams);
        }else{
            relativeParams.setMargins(5, 5, 50, 5);  // left, top, right, bottom
            holder.relativeLayout.setLayoutParams(relativeParams);
        }

    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public void addMessage(Message message){
        Log.d(TAG,"addMessage: "+message+"\n: list size: "+mMessageList.size());
        /*int position = mMessageList.size();
        mMessageList.add(position,message);
        notifyItemInserted(position);
        notifyItemRangeChanged(position,mMessageList.size());*/
        mMessageList.add(message);
        notifyDataSetChanged();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder{

        TextView message;
        RelativeLayout relativeLayout;

        public ChatViewHolder(View itemView) {
            super(itemView);
            message = (TextView) itemView.findViewById(R.id.message);
            relativeLayout = (RelativeLayout) itemView.findViewById(R.id.chat_container);
        }
    }
}
