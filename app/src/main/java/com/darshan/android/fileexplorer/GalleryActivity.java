package com.darshan.android.fileexplorer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class GalleryActivity extends AppCompatActivity implements DirListAdapter.DirClickListener {
    private static final String TAG = GalleryActivity.class.getSimpleName();
    private static final int REQUEST_STORAGE_PERMISSION = 110;

    private ArrayList<Image> mGridImagesList;
    private ArrayList<Image> mDirecList;
    private ArrayList<LoadThumbAsyncTask> mAsyncTaskLists;
    private HashSet<String> mLastSubDirSet;
    private HashMap<String, ArrayList<Image>> mAllMediaFolderFileMap;
    private ThumbUtils mThumbUtils;
    private HashSet<String> mPreviouslySelectedImagesSet;
    private ArrayList<Image> mPreviouslySelectedImagesList;
    private String mCurrentDirName = "";

    //MIME type of wanted files
    private String mRequiredMediaType;
    private boolean isAllMediaFolderClicked = false;
    private boolean isPreviousSelectedListExists = false;

    //widgets
    private RecyclerView mImageGridRecyclerView;
    private RecyclerView mDirNameRecyclerView;
    private RecyclerView mAllMediaFilesGridRecyclerView;
    private RelativeLayout mFileSelectToolbar;
    private RelativeLayout mFoldersToolbar;
    private ProgressBar mLoadProgressBar;

    private FileListAdapter mImageGridAdapter;
    private DirListAdapter mDirListAdapter;
    private MediaFolderFileListAdapter mMediaFolderFileListAdapter;


    @Override
    public void onDirClick(String dirName) {
        Log.d(TAG, "onDirClick: " + dirName);
        mCurrentDirName = dirName;
        //hide SubDirectory list
        mDirNameRecyclerView.setVisibility(View.GONE);
//        isFolderList = false;
        if(dirName.equals(getString(R.string.all_media_files))) {
            isAllMediaFolderClicked = true;
            mImageGridRecyclerView.setVisibility(View.GONE);
            mAllMediaFilesGridRecyclerView.setVisibility(View.VISIBLE);

            getAllMediaFolderFiles();

        } else {
            isAllMediaFolderClicked = false;
            mImageGridRecyclerView.setVisibility(View.VISIBLE);
            mAllMediaFilesGridRecyclerView.setVisibility(View.GONE);

            if (mGridImagesList != null) {
                mGridImagesList.clear();
            }

            mImageGridAdapter.notifyDataSetChanged();
            //Stop AsyncTask from loading previously selected folder's files thumb images
            stopPreviousAsyncTasks();
            getMediaFiles(dirName);

        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        mImageGridRecyclerView = findViewById(R.id.gridImage_RV);
        mDirNameRecyclerView = findViewById(R.id.dirName_RV);
        mAllMediaFilesGridRecyclerView = findViewById(R.id.allMediaGrid_RV);
        mLoadProgressBar = findViewById(R.id.load_PB);
        mFoldersToolbar = findViewById(R.id.folders_toolbar);
        mFileSelectToolbar = findViewById(R.id.itemSelect_toolbar);

        showFolderSelectBar();

        mLoadProgressBar.setVisibility(View.VISIBLE);

        mPreviouslySelectedImagesSet = new HashSet<>();
        mLastSubDirSet = new HashSet<>();
        mThumbUtils = new ThumbUtils(this);
        mAsyncTaskLists = new ArrayList<>();
        mAllMediaFolderFileMap = new HashMap<>();


        //get intent extra
        Intent intent = getIntent();
        if (intent.hasExtra(GalleryConsts.INTENT_MEDIA_TYPE)) {
            mRequiredMediaType = intent.getStringExtra(GalleryConsts.INTENT_MEDIA_TYPE);
        }

        initRecyclerLists();
        checkPermissions();

        if(intent.hasExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS)) {
            //Get previously selected images
            mPreviouslySelectedImagesList = intent.getParcelableArrayListExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS);
            if(mPreviouslySelectedImagesList != null) {
                for(Image image : mPreviouslySelectedImagesList) {
                    mPreviouslySelectedImagesSet.add(image.getImageUri());
                }
                String itemSelectedFolder = intent.getStringExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS_FOLDER);

                if(!itemSelectedFolder.equals("") && mPreviouslySelectedImagesList.size() > 0) {
                    isPreviousSelectedListExists = true;
                    if(!itemSelectedFolder.equals(getString(R.string.all_media_files))) {
                        Log.d(TAG, "onCreate: intent not all media");
                        mImageGridAdapter.setPreviousSelectedList(mPreviouslySelectedImagesList);
                        mPreviouslySelectedImagesList.clear();
                    }
                    onDirClick(itemSelectedFolder);
                }
            }
        }



    }


    private void initRecyclerLists() {
        mGridImagesList = new ArrayList<>();
        mImageGridAdapter = new FileListAdapter(this, mGridImagesList);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
        mImageGridRecyclerView.setLayoutManager(gridLayoutManager);
        mImageGridRecyclerView.setHasFixedSize(true);
        mImageGridRecyclerView.setAdapter(mImageGridAdapter);

        mDirecList = new ArrayList<>();
        mDirListAdapter = new DirListAdapter(this, mDirecList, this, mRequiredMediaType);
        GridLayoutManager gridLayoutManager1 = new GridLayoutManager(this, 2);
        mDirNameRecyclerView.setLayoutManager(gridLayoutManager1);
        mDirNameRecyclerView.setAdapter(mDirListAdapter);

    }

    private void toggleMediaFolders() {
        Log.d(TAG, "toggleMediaFolders: ");
        switch (mRequiredMediaType) {
            case GalleryConsts.IMAGE_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.IMAGE_TYPE);
                break;
            case GalleryConsts.VIDEO_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.VIDEO_TYPE);
                break;
            case GalleryConsts.IMAGE_VIDEO_TYPE:
                getAllMediaFolderFromDb(GalleryConsts.IMAGE_TYPE);
                getAllMediaFolderFromDb(GalleryConsts.VIDEO_TYPE);
                break;
            default:break;
        }
    }

    private void getMediaFiles(String folderName) {
        Log.d(TAG, "getMediaFiles: ");
        //get intent extra
        Intent intent = getIntent();
        if (intent.hasExtra(GalleryConsts.INTENT_MEDIA_TYPE)) {
            mRequiredMediaType = intent.getStringExtra(GalleryConsts.INTENT_MEDIA_TYPE);
        }
//        Log.d(TAG, "getMediaFiles: " + mRequiredMediaType);
        switch (mRequiredMediaType) {
            case GalleryConsts.IMAGE_TYPE:
                getOnlyImageFiles(folderName);
                break;
            case GalleryConsts.VIDEO_TYPE:
                getOnlyVideoFiles(folderName);
                break;
            case GalleryConsts.IMAGE_VIDEO_TYPE:
                getBothImagesVideosFiles(folderName);
                break;
            default:break;
        }
    }

    private int getOnlyImageFiles(String folderName) {
        Cursor cursor = getFolderCursor(folderName, GalleryConsts.IMAGE_TYPE);
        int itemCount = cursor.getCount();
//        mNumFiles = itemCount;
        getAllFilesInFolder(cursor, GalleryConsts.IMAGE_TYPE);
        return itemCount;
    }

    private int getOnlyVideoFiles(String folderName) {
        Cursor cursor = getFolderCursor(folderName, GalleryConsts.VIDEO_TYPE);
        int itemCount = cursor.getCount();
//        mNumFiles = itemCount;
        getAllFilesInFolder(cursor, GalleryConsts.VIDEO_TYPE);
        return itemCount;
    }

    private void getBothImagesVideosFiles(String folderName) {
        getOnlyImageFiles(folderName);
        getOnlyVideoFiles(folderName);
    }


    private void stopPreviousAsyncTasks() {
        for (LoadThumbAsyncTask asyncTask : mAsyncTaskLists) {
            asyncTask.cancel(true);
        }
        mAsyncTaskLists.clear();
    }


    private void getAllMediaFolderFromDb(String mediaType) {
        Log.d(TAG, "getAllMediaFolderFromDb: ");
        Uri mediaUri = null;
        if (mediaType.equals(GalleryConsts.IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        //Get all the images(both in device/SdCard) and store them in cursor
        if (mediaUri != null) {
            Cursor cursor = getContentResolver().query(
                    mediaUri,
                    columns,
                    null,
                    null,
                    null);

            if(cursor != null) {
                getDirectoriesWithMedia(cursor, mediaType);
            }
        }
    }


    //Cursor will determine which media directories we are getting
    private void getDirectoriesWithMedia(Cursor cursor, String mediaType) {
//        mLastSubDirSet.clear();
//        Log.d(TAG, "getDirectoriesWithMedia: ");
//        isFolderList = true;

        //Total number of images/videos
        int count = cursor.getCount();

        for (int i = 0; i < count; i++) {
            cursor.moveToPosition(i);
            //TODO add further filters to include new media type files.
            if (mediaType.equals(GalleryConsts.IMAGE_TYPE) || mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
                //Getting image/video root path and id by querying MediaStore
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = cursor.getString(dataColumnIndex);

                int imageIdColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = cursor.getLong(imageIdColumnIndex);

                //add images parent root path, image id to map so that i can access them in Adapter
                String subDirName = new File(filePath.substring(0, filePath.lastIndexOf("/"))).getName();
                boolean addedFolder = mLastSubDirSet.add(subDirName);
                if (addedFolder) {
                    //Avoiding adding first image of the folder if that file is of 0 size
                    if (new File(filePath).length() == 0) {
                        mLastSubDirSet.remove(subDirName);
                    } else {
//                        Log.d(TAG, "getDirectoriesWithMedia: executing Async");
                        LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask(mediaType, true);
                        mAsyncTaskLists.add(asyncTask);
                        asyncTask.execute(filePath, String.valueOf(imageId));
                    }
                }
            }
        }
        cursor.close();
        mLoadProgressBar.setVisibility(View.GONE);
    }


    //Called when all media folder is clicked in Dir list
    private void getAllMediaFolderFiles() {
        Log.d(TAG, "getAllMediaFolderFiles: ");
       final GalleryLoader galleryLoader = GalleryLoader.getInstance();
       //If Gallery loader not finished loading wait
       while (!galleryLoader.isFinishedLoading()) {
           Log.d(TAG, "getAllMediaFolderFiles:  loading");
           mLoadProgressBar.setVisibility(View.VISIBLE);
       }

       mAllMediaFolderFileMap = galleryLoader.getAllTimeFilesMap();
       ArrayList<Image> allImageAndHeaderFiles = orderMapItemsAsList(mAllMediaFolderFileMap);

       final GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);
       mMediaFolderFileListAdapter = new MediaFolderFileListAdapter(this, allImageAndHeaderFiles);
       mAllMediaFilesGridRecyclerView.setAdapter(mMediaFolderFileListAdapter);
       mAllMediaFilesGridRecyclerView.setLayoutManager(gridLayoutManager);
       gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
           @Override
           public int getSpanSize(int position) {
               return mMediaFolderFileListAdapter.isHeader(position) ? gridLayoutManager.getSpanCount() : 1;
           }
       });

       //Saving previously selected items
        if(mPreviouslySelectedImagesList != null && mPreviouslySelectedImagesList.size() > 0) {
            mMediaFolderFileListAdapter.setPreviousSelectedList(mPreviouslySelectedImagesList);
            mPreviouslySelectedImagesList.clear();
        }
    }


    //Called when Folder is clicked
    public Cursor getFolderCursor(String folderName, String mediaType) {
//        Log.d(TAG, "getFolderCursor: " + folderName);
        final String[] columns = {MediaStore.Images.Media.DATA,
                MediaStore.Images.Media._ID};

        final String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        final String[] selectionArg = {folderName};

        //Name of several image and video data base columns are same.
        Uri mediaUri = null;
        if (mediaType.equals(GalleryConsts.IMAGE_TYPE)) {
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        //Get all the files under the folder store them in cursor
        Cursor cursor = null;
        if (mediaUri != null) {
            cursor = getContentResolver().query(
                    mediaUri,
                    columns,
                    selection,
                    selectionArg,
                    null);
        }
        return cursor;
    }


    private void getAllFilesInFolder(Cursor folderCursor, String mediaType) {
        if (folderCursor != null) {
            int size = folderCursor.getCount();
            for (int i = 0; i < size; i++) {
                folderCursor.moveToPosition(i);
                int dataColumnIndex = folderCursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String filePath = folderCursor.getString(dataColumnIndex);

                int imageIdColumnIndex = folderCursor.getColumnIndex(MediaStore.Images.Media._ID);
                long imageId = folderCursor.getLong(imageIdColumnIndex);

//                Log.d(TAG, "getAllFilesInFolder: " + filePath + imageId);
                LoadThumbAsyncTask asyncTask = new LoadThumbAsyncTask(mediaType, false);
                mAsyncTaskLists.add(asyncTask);
                asyncTask.execute(filePath, String.valueOf(imageId));
            }
            folderCursor.close();
        }
    }


    class LoadThumbAsyncTask extends AsyncTask<String, Void, Image> {
        String mediaType;
        boolean isFolderList;

        public LoadThumbAsyncTask(String mediaType, boolean isFolderList) {
            this.mediaType = mediaType;
            this.isFolderList = isFolderList;
        }

        @Override
        protected Image doInBackground(String... strings) {
            try {
                String filePath = strings[0];
                String imageId = strings[1];

                if (!isCancelled()) {
                    Image image = mThumbUtils.getMediaThumbnail(mediaType, getContentResolver(), Long.valueOf(imageId), filePath);
                    if(image != null) {
                        if (mediaType.equals(GalleryConsts.VIDEO_TYPE)) {
                            image.setVideo(true);
                        } else if (mediaType.equals(GalleryConsts.IMAGE_TYPE)) {
                            image.setVideo(false);
                        }
                    }
                    return image;
                } else {
                    Log.d(TAG, "doInBackground: cancelled");
                    return null;
                }

            } catch (Exception e) {
                Log.e(TAG, "doInBackground: ", e);
                return null;
            }

        }

        @Override
        protected void onPostExecute(Image image) {
            super.onPostExecute(image);
            if (image != null) {
                saveImage(image, isFolderList);
            }
        }
    }


    //Save files related info in lists
    private void saveImage(Image image, boolean isFolderList) {
//        Log.d(TAG, "saveImage: " + image);
        if (image != null) {
            if (!isFolderList) {
                if(mPreviouslySelectedImagesSet.contains(image.getImageUri())) {
                    image.setSelected(true);
                    mPreviouslySelectedImagesSet.remove(image.getImageUri());
                }
                int size = mGridImagesList.size();
                if (size > 0) {
                    mGridImagesList.add(size, image);
                } else {
                    mGridImagesList.add(image);
                }
                mImageGridAdapter.notifyItemInserted(size);

            } else {
                int size = mDirecList.size();
                if (size > 0) {
                    mDirecList.add(size, image);
                } else {
                    mDirecList.add(image);
                }
//                Log.d(TAG, "saveImage: " + image);
                mDirListAdapter.notifyItemInserted(size + 1);
            }
        }
    }


    public void showFileSelectBar(int itemCount) {
        mFileSelectToolbar.setVisibility(View.VISIBLE);
        mFoldersToolbar.setVisibility(View.GONE);

        TextView itemCountTV = findViewById(R.id.itemCount_TV);
        String countText = itemCount + " " + getString(R.string.items_selected);
        itemCountTV.setText(countText);

        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshGridImageList();
            }
        });

        TextView selectTV = findViewById(R.id.select_TV);

        selectTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Send the result back to calling activity
                HashSet<Image> selectedImages;
                if(!isAllMediaFolderClicked) {
                    selectedImages = mImageGridAdapter.getSelectedItems();
                } else {
                    selectedImages = mMediaFolderFileListAdapter.getSelectedItems();
                }
//                for (Image image : selectedImages) {
//                    Log.d(TAG, "onClick: " + image);
//                }
                sendResultBack(selectedImages);
            }
        });

  }

    public void showFolderSelectBar() {
        mFileSelectToolbar.setVisibility(View.GONE);
        mFoldersToolbar.setVisibility(View.VISIBLE);

        ImageView backArrow = findViewById(R.id.backArrow_yellow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }


    private void refreshGridImageList() {
        showFolderSelectBar();
        mImageGridAdapter.clearSelectedList();
        mImageGridAdapter.notifyDataSetChanged();

        if(mMediaFolderFileListAdapter != null) {
            mMediaFolderFileListAdapter.clearSelectedList();
            mMediaFolderFileListAdapter.notifyDataSetChanged();
        }

        if(mPreviouslySelectedImagesSet != null) {
            mPreviouslySelectedImagesSet.clear();
        }

        GalleryLoader.getInstance().clearPreviouslySelectedList();
    }

    //Send result back to calling activity
    private void sendResultBack(HashSet<Image> selectedImages) {
        ArrayList<Image> selectedImageList = new ArrayList<>(selectedImages);
        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra(GalleryConsts.INTENT_SELECT_GALLERY_ITEMS, selectedImageList);
        resultIntent.putExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS_FOLDER, mCurrentDirName);
        setResult(RESULT_OK, resultIntent);

        finish();
    }









    //-------------------------------------------------------------------------------------------//
    private void checkPermissions() {
        Log.d(TAG, "checkPermissions: ");
        // Check for the external storage permission
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)  {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            //Permission is granted proceed
            toggleMediaFolders();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case REQUEST_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // If you get permission, proceed
                    toggleMediaFolders();

                } else {
                    // If you do not get permission, show a Toast and exit from app.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mDirNameRecyclerView.getVisibility() != View.VISIBLE) {
            mDirNameRecyclerView.setVisibility(View.VISIBLE);
            if(isPreviousSelectedListExists) {
                mDirecList.clear();
                mLastSubDirSet.clear();
                toggleMediaFolders();
            }
            if(mImageGridRecyclerView.getVisibility() == View.VISIBLE) {
                mImageGridRecyclerView.setVisibility(View.GONE);
            } else if(mAllMediaFilesGridRecyclerView.getVisibility() == View.VISIBLE) {
                mAllMediaFilesGridRecyclerView.setVisibility(View.GONE);
            }
            refreshGridImageList();
        } else {
            super.onBackPressed();
        }
    }


    private ArrayList<Image> orderMapItemsAsList(HashMap<String, ArrayList<Image>> map) {
        Set<String> dirNames = map.keySet();
        ArrayList<Image> allMediaAndHeaderFiles = new ArrayList<>();

        //In order to get the items in order have to do this multiple calls
        //Add title item first, followed by image item list
        if(dirNames.contains(getString(R.string.today))) {
            String title = getString(R.string.today);
            allMediaAndHeaderFiles.add(new Image(title));
            allMediaAndHeaderFiles.addAll(getImageListWithMapKey(title, map));
        }

        if(dirNames.contains(getString(R.string.yesterday))) {
            String title = getString(R.string.yesterday);
            allMediaAndHeaderFiles.add(new Image(title));
            allMediaAndHeaderFiles.addAll(getImageListWithMapKey(title, map));
        }

        if(dirNames.contains(getString(R.string.last_week))) {
            String title = getString(R.string.last_week);
            allMediaAndHeaderFiles.add(new Image(title));
            allMediaAndHeaderFiles.addAll(getImageListWithMapKey(title, map));
        }

        if(dirNames.contains(getString(R.string.older_than_week))) {
            String title = getString(R.string.older_than_week);
            allMediaAndHeaderFiles.add(new Image(title));
            allMediaAndHeaderFiles.addAll(getImageListWithMapKey(title, map));
        }

        return allMediaAndHeaderFiles;





//        for(String title : dirNames) {
//            allMediaAndHeaderFiles.add(new Image(title));
//            //mark previously selected images
//            ArrayList<Image> imageList = map.get(title);
//            for(Image image : imageList) {
//                if (mPreviouslySelectedImagesSet.contains(image.getImageUri())) {
//                    image.setSelected(true);
//                    mPreviouslySelectedImagesSet.remove(image.getImageUri());
//                }
//            }
//            allMediaAndHeaderFiles.addAll(imageList);
//        }
//        return allMediaAndHeaderFiles;
    }


    private ArrayList<Image> getImageListWithMapKey(String title, HashMap<String, ArrayList<Image>> map) {
        //mark previously selected images
        ArrayList<Image> imageList = map.get(title);
        for(Image image : imageList) {
            if (mPreviouslySelectedImagesSet.contains(image.getImageUri())) {
                image.setSelected(true);
                mPreviouslySelectedImagesSet.remove(image.getImageUri());
            }
        }
        return imageList;
    }





//    private void displayMapItems(HashMap<String, ArrayList<Image>> map) {
//        Set<String> dirNames = map.keySet();
//        for(String str : dirNames) {
//            Log.d(TAG, "displayMapItems: " + str);
//            ArrayList<Image> files = map.get(str);
//            for(Image image : files) {
//                Log.d(TAG, "displayMapItems: " + image);
//            }
//
//        }
//    }

}

