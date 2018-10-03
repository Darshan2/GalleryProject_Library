# GalleryProject_Library

 When Lokal app start call 
 ```java
 GalleryLoader.getInstance().startLoadingImages(getApplicationContext(), getContentResolver()); 
 ```
it will load the 'All media' folder files in a background thread.It's a must otherwise when user clicked on ALL MEDIA folder it may not show any files.</br>


 <br>
 Pass type of media file you want the library to load, 'previously/pre selected imges list', 'previously selected files folder name' as intent extras. If previously selectes image list doesnot exist pass NULL, if previously selected files folder name does not exist pass empty string.
 
 ```java
 Intent intent = new Intent(this, GalleryActivity.class);
 intent.putExtra(GalleryConsts.INTENT_MEDIA_TYPE, mediaType);
 intent.putExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS, mGalleryLoader.getPreviousSelectedImages());
 intent.putExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS_FOLDER, mGalleryLoader.getPreviousSelectedItemsFolderName());
 startActivityForResult(intent, GALLERY_ACTIVITY_REQUEST_CODE);
 ```
 mediaType may be GalleryConst.IMAGE_TYPE, GalleryConst.VIDEO_TYPE, GalleryConst.IMAGE_VIDEO_TYPE(pass this by default)<br>
 
 <br>
 In your activitys onActivityResult, you will receive the list of all the selected files list, along with their parent folder name,
 store them in GalleryLoader
 
 ```java
 @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == GALLERY_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<Image> selectedImages = intent.getParcelableArrayListExtra(GalleryConsts.INTENT_SELECT_GALLERY_ITEMS);
            String selectedItemsDirName = intent.getStringExtra(GalleryConsts.INTENT_PREVIOUSLY_SELECT_ITEMS_FOLDER);
  
            GalleryLoader.getInstance().setPreviousSelectedImages(selectedImages);
            GalleryLoader.getInstance().setPreviousSelectedItemsFolderName(selectedItemsDirName);
            
            //Do your thing with the selected files list
        }
    }
   ```
 
 
