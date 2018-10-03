package com.darshan.android.fileexplorer;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

public class GridImageAdapter extends BaseAdapter {
    private static final String TAG = "GridImageAdapter";

    private Context mContext;
    private ArrayList<Image> mImageList;

    public GridImageAdapter(Context mContext, ArrayList<Image> imageList) {
        this.mContext = mContext;
        this.mImageList = imageList;
    }

    @Override
    public int getCount() {
        int size = mImageList.size();
        Log.d(TAG, "getCount: " + size);
        return size;
    }

    @Override
    public Object getItem(int position) {
        return mImageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setAdjustViewBounds(true);
//            imageView.setLayoutParams(new ViewGroup.LayoutParams(85, 85));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
        } else {
            imageView = (ImageView) convertView;
        }

        String thumbUri = mImageList.get(position).getThumbUri();
        if (thumbUri != null) {
            RequestOptions placeHolderOption = new RequestOptions().placeholder(R.drawable.blank_video_screen);
            Glide.with(mContext)
                    .load(thumbUri)
                    .apply(placeHolderOption)
                    .into(imageView);
        }
        return imageView;
    }

}

