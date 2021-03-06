package com.example.jinjinz.concertprev.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.jinjinz.concertprev.R;
import com.example.jinjinz.concertprev.models.Concert;
import com.squareup.picasso.Picasso;

import java.text.MessageFormat;
import java.util.ArrayList;

public class SearchRecyclerAdapter extends RecyclerView.Adapter<SearchRecyclerAdapter.ViewHolder> implements View.OnClickListener {
    @Override
    public void onClick(View view) {
        ImageView ivBackgroundImage = (ImageView) view.findViewById(R.id.ivBackgroundImage);
        Concert concert = (Concert) ivBackgroundImage.getTag();
        mSearchRecyclerAdapterListener.onConcertTap(concert);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView tvEventName;
        public TextView tvEventLocation;
        public RelativeLayout rlConcert;
        public ImageView ivBackgroundImage;


        public ViewHolder(View itemView) {
            super(itemView);
            tvEventName = (TextView) itemView.findViewById(R.id.tvEventName);
            tvEventLocation = (TextView) itemView.findViewById(R.id.tvEventLocation);
            rlConcert = (RelativeLayout) itemView.findViewById(R.id.rlConcert);
            ivBackgroundImage = (ImageView) itemView.findViewById(R.id.ivBackgroundImage);
        }

    }



    public interface SearchRecyclerAdapterListener {
        void onConcertTap(Concert concert);
    }

    SearchRecyclerAdapterListener mSearchRecyclerAdapterListener;

    private ArrayList<Concert> mConcerts;
    private Context mContext;

    public SearchRecyclerAdapter(Context context, ArrayList<Concert> concerts, SearchRecyclerAdapterListener searchRecyclerAdapterListener) {
        mConcerts = concerts;
        mContext = context;
        mSearchRecyclerAdapterListener = searchRecyclerAdapterListener;
    }

    // access to the context object in the recyclerview
    private Context getContext() {
        return mContext;
    }

    // Usually involves inflating a layout from XML and returning the holder
    /** Inflates XML layout and returns the ViewHolder */
    @Override
    public SearchRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View concertView = layoutInflater.inflate(R.layout.item_concert, parent, false);

        //Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(concertView);
        concertView.setOnClickListener(this);
        return viewHolder;
    }

    /** Populates data into views utilizing the ViewHolder */
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        //Get concert based on position
        Concert concert = mConcerts.get(position);

        // Set item views based on your views and data model
        TextView eventName = viewHolder.tvEventName;
        eventName.setText(concert.getEventName());
        TextView eventLoc = viewHolder.tvEventLocation;
        if (concert.getStateCode() != null) {
            eventLoc.setText(MessageFormat.format("{0}, {1}", concert.getCity(), concert.getStateCode()));
        } else {
            eventLoc.setText(MessageFormat.format("{0}, {1}", concert.getCity(), concert.getCountryCode()));

        }
        ImageView backgroundImg = viewHolder.ivBackgroundImage;
        Picasso.with(getContext()).load(concert.getBackdropImage()).placeholder(R.drawable.concert_placeholder).into(backgroundImg);
        viewHolder.ivBackgroundImage.setTag(concert);
    }

    @Override
    public int getItemCount() {
        return mConcerts.size();
    }

    /**
     * Clears the concerts array and notifies the adapter
     * */
    public void clear() {
        mConcerts.clear();
        notifyDataSetChanged();
    }
}
