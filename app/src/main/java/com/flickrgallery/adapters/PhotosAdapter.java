package com.flickrgallery.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.flickrgallery.R;
import com.flickrgallery.activities.SinglePhotoActivity;
import com.flickrgallery.imageLoader.ImageLoader;
import com.flickrgallery.model.PhotoModel;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.MyViewHolder> implements Filterable {

    private List<PhotoModel> photoModelList;
    private List<PhotoModel> orig;
    private Context context;
    private ImageLoader imgLoader;
    private String searchString = "";
    private Activity activity;

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView tvTitle;
        private ImageView ivPhotos;
        CardView cardView;

        MyViewHolder(View view) {
            super(view);
            tvTitle         = view.findViewById(R.id.tv_title);
            ivPhotos        = view.findViewById(R.id.iv_photos);
            cardView        = view.findViewById(R.id.card_view);

        }
    }

    public PhotosAdapter(Context context, List<PhotoModel> photoModelList) {
        this.photoModelList = photoModelList;
        this.context        = context;
        activity            = (Activity) context;
        orig                = photoModelList;
        imgLoader           = new ImageLoader(context.getApplicationContext());
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.photos_adapter, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final PhotoModel photoModel = photoModelList.get(position);

        final String id      = photoModel.getId();
        final String title   = photoModel.getTitle();
        final String owner   = photoModel.getOwner();
        final String secret  = photoModel.getSecret();
        final String server  = photoModel.getServer();
        final String farm    = photoModel.getFarm();

        final String url = "http://farm" + farm + ".static.flickr.com/" + server + "/" + id + "_" + secret + "_m.jpg";

        holder.tvTitle.setText(highlightText(searchString,title));

        imgLoader.DisplayImage(url, holder.ivPhotos);

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context,SinglePhotoActivity.class);
                int[] screenLocation = new int[2];
                holder.ivPhotos.getLocationOnScreen(screenLocation);
                intent.putExtra("left", screenLocation[0]).
                        putExtra("top", screenLocation[1]).
                        putExtra("width", holder.ivPhotos.getWidth()).
                        putExtra("height", holder.ivPhotos.getHeight());
                intent.putExtra("image_id",position);
                intent.putExtra("image_url",url);
                intent.putExtra("title",title);
                context.startActivity(intent);
                activity.overridePendingTransition(0,0);
            }
        });

    }

    public int item_id(int pos){
        PhotoModel rm= photoModelList.get(pos);
        int id= Integer.parseInt(rm.getId());
        return id;
    }

    @Override
    public int getItemCount() {
        return photoModelList.size();
    }

    private CharSequence highlightText(String search, String originalText) {
        if (search != null && !search.equalsIgnoreCase("")) {
            String normalizedText = Normalizer.normalize(originalText, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase();
            int start = normalizedText.indexOf(search);
            if (start < 0) {
                return originalText;
            } else {
                Spannable highlighted = new SpannableString(originalText);
                while (start >= 0) {
                    int spanStart = Math.min(start, originalText.length());
                    int spanEnd = Math.min(start + search.length(), originalText.length());
                    highlighted.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.colorAccent)), spanStart, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = normalizedText.indexOf(search, spanEnd);
                }
                return highlighted;
            }
        }
        return originalText;
    }

    @NonNull
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                searchString = (String) constraint;
                final FilterResults oReturn = new FilterResults();
                final List<PhotoModel> results = new ArrayList<>();

                if (!TextUtils.isEmpty(constraint)) {
                    if (orig != null && orig.size() > 0) {
                        for (final PhotoModel g : orig) {
                            if (g.getTitle().toLowerCase().contains(constraint.toString().toLowerCase())) {
                                results.add(g);
                            }
                        }
                    }
                    oReturn.values = results;
                }
                else {
                    oReturn.values = orig;
                }
                return oReturn;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                photoModelList = (ArrayList<PhotoModel>) results.values;
                notifyDataSetChanged();
            }
        };
    }
}
