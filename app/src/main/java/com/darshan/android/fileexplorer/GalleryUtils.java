package com.darshan.android.fileexplorer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

public class GalleryUtils {
    private static final String TAG = "GalleryUtils";
    private ContentResolver contentResolver;
    private ThumbUtils mThumbUtils;
    private Context mContext;
    private long currentTime;

    public GalleryUtils(Context context, ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
        mContext = context;
        mThumbUtils = new ThumbUtils(context);
        currentTime = System.currentTimeMillis();
    }


    public ArrayList<Image> getTodayFiles(String mediaType) {
        Log.d(TAG, "getTodayFiles: ");
        long today = currentTime - (1000 * 60 * 60 * 24);
        return getFilesInTimeRange(mediaType, today, currentTime);
    }

    public ArrayList<Image> getYesterdayFiles(String mediaType) {
        Log.d(TAG, "getYesterdayFiles: ");
        long today = currentTime - (1000 * 60 * 60 * 24);
        long yesterday = today - (1000 * 60 * 60 * 24);
        return getFilesInTimeRange(mediaType, yesterday, today);
    }

    public ArrayList<Image> getLastWeekFiles(String mediaType) {
        Log.d(TAG, "getLastWeekFiles: ");
        long yesterday = currentTime - (2 * 1000 * 60 * 60 * 24);
        long lastWeek = currentTime - (7 * 1000 * 60 * 60 * 24);
        return getFilesInTimeRange(mediaType, lastWeek, yesterday);
    }


    public ArrayList<Image> getOlderThanWeekFiles(String mediaType) {
        Log.d(TAG, "getOlderThanWeekFiles: ");
        long lastWeek = currentTime - (7 * 1000 * 60 * 60 * 24);
        return getFilesInTimeRange(mediaType, 0, lastWeek);
    }


    private ArrayList<Image> getFilesInTimeRange(String mediaType, long startTime, long endTime) {
        ArrayList<Image> filesInRange = null;
        Uri mediaUri = null;
        if (mediaType.equals(GalleryConsts.IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        String selection = MediaStore.Images.ImageColumns.DATE_TAKEN + " between ? and ?";
        String[] selectionArgs = {String.valueOf(startTime), String.valueOf(endTime)};
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};

        //Get all the images/videos taken within given time range
        if (mediaUri != null) {
            Cursor cursor = contentResolver.query(
                    mediaUri,
                    columns,
                    selection,
                    selectionArgs,
                    null);

            if(cursor != null) {
                filesInRange = getAllItemsInCursor(cursor, mediaType);
            }
            cursor.close();
        }
//        for(Image image : filesInRange) {
//            Log.d(TAG, "getFilesInTimeRange: " + image);
//        }
        return filesInRange;
    }


    private ArrayList<Image> getAllItemsInCursor(Cursor cursor, String mediaType) {
        ArrayList<Image> imageArrayList = new ArrayList<>();
        for(int i = 0 ; i < cursor.getCount() ; i++) {
            cursor.moveToPosition(i);
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            String filePath = cursor.getString(dataColumnIndex);

            int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            long imageId = cursor.getLong(imageIdColumnIndex);

            Image image = getThumb(mediaType, filePath, imageId);
            if(image != null) {
                if(mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
                    image.setVideo(true);
                }
                imageArrayList.add(image);
            }
        }
        cursor.close();
        return imageArrayList;
    }

    private Image getThumb(String mediaType, String filePath, long sourceId) {
        return mThumbUtils.getMediaThumbnail(mediaType, contentResolver, sourceId, filePath);
    }
}
