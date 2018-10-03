package com.darshan.android.fileexplorer;

import android.content.ContentResolver;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public final class GalleryLoader {
    private static final String TAG = "GalleryLoader";
    private static final GalleryLoader ourInstance = new GalleryLoader();

    private HashMap<String, ArrayList<Image>> mTimeFilesMap;
    private ArrayList<Image> mPreviousSelectedImages;
    private boolean finishedLoading = false;
    private String mPreviousSelectedItemFolderName = "";

    private boolean isTodaysFilesExist = false;
    private boolean isYesterdaysFilesExist = false;
    private boolean isLastWeeksFileExist = false;
    private boolean isOlderThanLastWeeksFileExist = false;

    private GalleryLoader() {
        mTimeFilesMap = new HashMap<>();
    }


    public static GalleryLoader getInstance() {
        return ourInstance;
    }


    public void startLoadingImages(final Context context, ContentResolver contentResolver) {
        final GalleryUtils galleryUtils = new GalleryUtils(context, contentResolver);
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Today
                ArrayList<Image> todayFiles = galleryUtils.getTodayFiles(GalleryConsts.IMAGE_TYPE);
                if(todayFiles == null) {
                    todayFiles = galleryUtils.getTodayFiles(GalleryConsts.VIDEO_TYPE);
                } else {
                    todayFiles.addAll(galleryUtils.getTodayFiles(GalleryConsts.VIDEO_TYPE));
                }
                if(todayFiles.size() > 0) {
                    isTodaysFilesExist = true;
                    mTimeFilesMap.put(context.getString(R.string.today), todayFiles);
                }

                //Yesterday
                ArrayList<Image> yesterdayFiles = galleryUtils.getYesterdayFiles(GalleryConsts.IMAGE_TYPE);
                if(yesterdayFiles == null) {
                    yesterdayFiles = galleryUtils.getYesterdayFiles(GalleryConsts.VIDEO_TYPE);
                } else {
                    yesterdayFiles.addAll(galleryUtils.getYesterdayFiles(GalleryConsts.VIDEO_TYPE));
                }
                if(yesterdayFiles.size() > 0) {
                    isYesterdaysFilesExist = true;
                    mTimeFilesMap.put(context.getString(R.string.yesterday), yesterdayFiles);
                }

                //last week
                ArrayList<Image> lastWeekFiles = galleryUtils.getLastWeekFiles(GalleryConsts.IMAGE_TYPE);
                if(lastWeekFiles == null) {
                    lastWeekFiles = galleryUtils.getLastWeekFiles(GalleryConsts.VIDEO_TYPE);
                } else {
                    lastWeekFiles.addAll(galleryUtils.getLastWeekFiles(GalleryConsts.VIDEO_TYPE));
                }
                if(lastWeekFiles.size() > 0) {
                    isLastWeeksFileExist = true;
                    mTimeFilesMap.put(context.getString(R.string.last_week), lastWeekFiles);
                }

                //older
                ArrayList<Image> olderThanWeekFiles = galleryUtils.getOlderThanWeekFiles(GalleryConsts.IMAGE_TYPE);
                if(olderThanWeekFiles == null) {
                    olderThanWeekFiles = galleryUtils.getOlderThanWeekFiles(GalleryConsts.VIDEO_TYPE);
                } else {
                    olderThanWeekFiles.addAll(galleryUtils.getOlderThanWeekFiles(GalleryConsts.VIDEO_TYPE));
                }
                if(olderThanWeekFiles.size() > 0) {
                    isOlderThanLastWeeksFileExist = true;
                    mTimeFilesMap.put(context.getString(R.string.older_than_week), olderThanWeekFiles);
                }

                finishedLoading = true;
            }
        }).start();
    }

    public boolean isFinishedLoading() {
        return finishedLoading;
    }

    //Call this method only after loading completes, other wise we will get irregular and incomplete map
    public HashMap<String, ArrayList<Image>> getAllTimeFilesMap() {
        return mTimeFilesMap;
    }

    public Image getFirstImageInTimeStamp(Context context) {
        if(isTodaysFilesExist) {
            return mTimeFilesMap.get(context.getString(R.string.today)).get(0);
        } else if(isYesterdaysFilesExist) {
            return mTimeFilesMap.get(context.getString(R.string.yesterday)).get(0);
        } else if(isLastWeeksFileExist) {
            return mTimeFilesMap.get(context.getString(R.string.last_week)).get(0);
        } else if(isOlderThanLastWeeksFileExist) {
            return mTimeFilesMap.get(context.getString(R.string.last_week)).get(0);
        } else {
            return null;
        }
    }

    public int getTotalItemCount() {
        int total = 0;
        for(String str : mTimeFilesMap.keySet()) {
           total += mTimeFilesMap.get(str).size();
        }
        return total;
    }

    public ArrayList<Image> getPreviousSelectedImages() {
        return mPreviousSelectedImages;
    }

    public void setPreviousSelectedImages(ArrayList<Image> mPreviousSelectedImages) {
        this.mPreviousSelectedImages = mPreviousSelectedImages;
    }

    public String getPreviousSelectedItemsFolderName() {
        return mPreviousSelectedItemFolderName;
    }

    public void setPreviousSelectedItemsFolderName(String mPreviousSelectedItemFolderName) {
        this.mPreviousSelectedItemFolderName = mPreviousSelectedItemFolderName;
    }

    public void clearPreviouslySelectedList(){
        mPreviousSelectedImages = null;
    }
}
