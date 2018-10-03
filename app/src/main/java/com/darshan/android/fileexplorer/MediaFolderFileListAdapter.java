package com.darshan.android.fileexplorer;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.HashSet;

public class MediaFolderFileListAdapter extends RecyclerView.Adapter<MediaFolderFileListAdapter.MediaFolderFileListVH> {
    private static final String TAG = "MediaFolderFileListAdap";

    private Context mContext;
    private ArrayList<Image> mFilesInMediaFolderList;
    private HashSet<Image> mSelectedImageSet;

    public final int VIEW_TYPE_HEADER = 1;
    public final int VIEW_TYPE_ITEM = 2;

    public MediaFolderFileListAdapter(Context mContext, ArrayList<Image> filesInMediaFolder) {
        this.mContext = mContext;
        this.mFilesInMediaFolderList = filesInMediaFolder;
        mSelectedImageSet = new HashSet<>();
    }

    public boolean isHeader(int position) {
        if(mFilesInMediaFolderList.get(position).getTitle().equals("")) {
            return false;
        } else {
            //If image has title in it it is considered as header type.
            return true;
        }
    }


    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? VIEW_TYPE_HEADER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public MediaFolderFileListVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view;
        if(viewType == VIEW_TYPE_HEADER) {
            view = inflater.inflate(R.layout.item_gallery_all_media_folder_title, null);
        } else {
            view = inflater.inflate(R.layout.item_gallery_all_media_folder_image, null);
        }

        return new MediaFolderFileListVH(view);
    }


    @Override
    public void onBindViewHolder(@NonNull final MediaFolderFileListVH holder, int position) {
        final Image image = mFilesInMediaFolderList.get(position);
        int viewType = getItemViewType(position);
        if(viewType == VIEW_TYPE_HEADER) {
            holder.tvTitle.setText(image.getTitle());
        } else {
            RequestOptions placeHolderOption = new RequestOptions().placeholder(R.drawable.blank_video_screen);
            Glide.with(mContext)
                    .load(image.getThumbUri())
                    .apply(placeHolderOption)
                    .into(holder.ivFileImage);

            if(!image.isSelected()) {
                hideSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
            } else {
                boolean exists = false;
                if(mSelectedImageSet != null) {
                    for(Image selectedImage : mSelectedImageSet) {
                        if(image.getImageUri().equals(selectedImage.getImageUri())) {
                            //i.e selected item already exist in set
                            exists = true;
                            break;
                        }
                    }
                }

                if(!exists) {
                    mSelectedImageSet.add(image);
                }
                showSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
            }

            if(image.isVideo()) {
                holder.ivVideoIcon.setVisibility(View.VISIBLE);
            } else {
                holder.ivVideoIcon.setVisibility(View.GONE);
            }


            holder.ivFileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                selectEnabled = true;
                    boolean exists = false;
                    if(mSelectedImageSet != null) {
                        for (Image img : mSelectedImageSet) {
                            //Check existing selected set for the image's imageUri(do not use mSelectedImageSet.contains(image))
                            if (image.getImageUri().equals(img.getImageUri())) {
                                exists = true;
                                image.setSelected(false);
                                mSelectedImageSet.remove(img);
                                hideSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
                                break;
                            }
                        }
                    }

                    if(!exists) {
                        image.setSelected(true);
                        mSelectedImageSet.add(image);
                        showSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
                    }
                }

            });

//            holder.ivFileImage.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if(mSelectedImageSet.contains(image)) {
//                        image.setSelected(false);
//                        holder.ivFileImage.clearColorFilter();
//                        mSelectedImageSet.remove(image);
//                        hideSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
//                    } else {
//                        image.setSelected(true);
//                        mSelectedImageSet.add(image);
//                        showSelectedMark(holder.ivSelectedLogo, holder.ivFileImage);
//                    }
//                }
//
//            });
        }
    }


    @Override
    public int getItemCount() {
        int size = mFilesInMediaFolderList.size();
//        Log.d(TAG, "getItemCount: " + size);
        return size;
    }

    class MediaFolderFileListVH extends RecyclerView.ViewHolder {

        TextView tvTitle;
        ImageView ivFileImage;
        ImageView ivSelectedLogo, ivVideoIcon;

        public MediaFolderFileListVH(View itemView) {
            super(itemView);

            ivFileImage = itemView.findViewById(R.id.allMediaGrid_IV);
            tvTitle = itemView.findViewById(R.id.title_TV);
            ivSelectedLogo = itemView.findViewById(R.id.selectedIcon);
            ivVideoIcon = itemView.findViewById(R.id.videoIcon);

        }
    }

    private void showSelectedMark(ImageView selectLogo, ImageView filterImageView) {
        ((GalleryActivity) mContext).showFileSelectBar(mSelectedImageSet.size());
        selectLogo.setVisibility(View.VISIBLE);
        filterImageView.setColorFilter(Color.argb(100, 0, 0, 0));
    }

    private void hideSelectedMark(ImageView selectLogo, ImageView filterImageView) {
        if(mSelectedImageSet.size() == 0) {
            ((GalleryActivity)mContext).showFolderSelectBar();
        } else {
            ((GalleryActivity) mContext).showFileSelectBar(mSelectedImageSet.size());
        }
        selectLogo.setVisibility(View.GONE);
        filterImageView.clearColorFilter();
    }

    public HashSet<Image> getSelectedItems() {
        return mSelectedImageSet;
    }

    public void clearSelectedList() {
        for(Image image : mFilesInMediaFolderList) {
            image.setSelected(false);
        }
        mSelectedImageSet.clear();
    }

    public void setPreviousSelectedList(ArrayList<Image> previousSelectedList) {
        mSelectedImageSet.addAll(previousSelectedList);
        ((GalleryActivity) mContext).showFileSelectBar(mSelectedImageSet.size());

    }
}




