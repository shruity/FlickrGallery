package com.flickrgallery.interfaces;


public interface GetPhotosTask {
    void onTaskCompleted(String result);

    void onTaskStart();
}
