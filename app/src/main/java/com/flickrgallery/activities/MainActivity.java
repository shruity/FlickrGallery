package com.flickrgallery.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.flickrgallery.R;
import com.flickrgallery.adapters.PhotosAdapter;
import com.flickrgallery.interfaces.GetPhotosTask;
import com.flickrgallery.model.PhotoModel;
import com.flickrgallery.request.GetPhotos;
import com.flickrgallery.util.CheckNetwork;
import com.flickrgallery.util.FlickerUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements GetPhotosTask {
    private static final String TAG = "MainActivity";
    public String url;
    private ArrayList<PhotoModel> photoModelArrayList;
    private RecyclerView rvPhotos;
    private static final int TIME_DELAY = 2000;
    private static long back_pressed;
    private PhotosAdapter photosAdapter;
    private TextView tv_heading;
    private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    private LinearLayoutManager layoutManager;
    private ProgressDialog progressDialog;

    private static final int PAGE_START = 1;
    // limiting to 5 for this tutorial, since total pages in actual API is very large. Feel free to modify.
    private int currentPage = PAGE_START;

    int limit = 6;
    boolean loadingMore = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        rvPhotos            = findViewById(R.id.rv_photos);
        tv_heading          = findViewById(R.id.tv_heading);
        Toolbar toolbar     = findViewById(R.id.toolbar);

        photoModelArrayList = new ArrayList<>();

        setSupportActionBar(toolbar);

        checkWriteExternalStoragePermission();

        if (CheckNetwork.isInternetAvailable(MainActivity.this)){
            fetchData(currentPage);
        }

        rvPhotos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleCount = layoutManager.findLastVisibleItemPosition();

                if(!(loadingMore)){
                    if (lastVisibleCount+1 >= totalItemCount){
                        currentPage = (totalItemCount/5)+1;
                        loadingMore=true;
                        if (currentPage <= limit) {
                            if (CheckNetwork.isInternetAvailable(MainActivity.this))
                                fetchData(currentPage);
                        }
                    }
                }
            }
        });

    }

    public void fetchData(int page){

        url = FlickerUtil.LIST_URL + FlickerUtil.METHOD_GET_PHOTOS + "&" + FlickerUtil.API_KEY
                + "&" + FlickerUtil.PER_PAGE + "&" + FlickerUtil.PAGE + page
                + "&format=json&nojsoncallback=1";

        GetPhotos getPhotos = new GetPhotos(this);
        getPhotos.execute(url);
    }
    @Override
    public void onTaskStart(){
        progressDialog = new ProgressDialog(MainActivity.this, ProgressDialog.THEME_HOLO_DARK);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    @Override
    public void onTaskCompleted(String result) {
        Log.e(TAG,"result main "+result);
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject photoList = jsonObject.getJSONObject("photos");
            int page = photoList.getInt("page");
            Log.e(TAG,"page "+page);
            JSONArray photoArray = photoList.getJSONArray("photo");

            for (int i=0; i<photoArray.length(); i++){
                JSONObject photoObject = photoArray.getJSONObject(i);
                String id     = photoObject.getString("id");
                String owner  = photoObject.getString("owner");
                String secret = photoObject.getString("secret");
                String server = photoObject.getString("server");
                String farm   = photoObject.getString("farm");
                String title  = photoObject.getString("title");

                PhotoModel photoModel = new PhotoModel();
                photoModel.setId(id);
                photoModel.setOwner(owner);
                photoModel.setSecret(secret);
                photoModel.setServer(server);
                photoModel.setFarm(farm);
                photoModel.setTitle(title);

                photoModelArrayList.add(photoModel);
            }
            loadingMore = false;
            if (rvPhotos.getAdapter() == null) {
                photosAdapter = new PhotosAdapter(MainActivity.this, photoModelArrayList);
                layoutManager = new GridLayoutManager(getApplicationContext(), 2);
                rvPhotos.setLayoutManager(layoutManager);
                photosAdapter.notifyDataSetChanged();
                rvPhotos.setAdapter(photosAdapter);
            } else {
                rvPhotos.getAdapter().notifyDataSetChanged();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_heading.setVisibility(View.GONE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                tv_heading.setVisibility(View.VISIBLE);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    photosAdapter.getFilter().filter("");
                    photosAdapter.notifyDataSetChanged();
                } else {
                    photosAdapter.getFilter().filter(newText);
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            layoutManager = new GridLayoutManager(getApplicationContext(), 4);
            rvPhotos.setLayoutManager(layoutManager);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            //Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            layoutManager = new GridLayoutManager(getApplicationContext(), 2);
            rvPhotos.setLayoutManager(layoutManager);
        }
    }

    public void checkWriteExternalStoragePermission() {

        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    @Override
    public void onBackPressed(){
        if (back_pressed + TIME_DELAY > System.currentTimeMillis()) {
            super.onBackPressed();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            System.exit(0);
        } else {
            View view1 = findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(view1,"Press once again to exit!", Snackbar.LENGTH_SHORT);
            snackbar.show();
            View view=snackbar.getView();
            view.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        back_pressed = System.currentTimeMillis();
    }
}
