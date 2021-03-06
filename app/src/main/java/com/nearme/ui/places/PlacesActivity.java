package com.nearme.ui.places;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.nearme.R;
import com.nearme.location.LocationManager;
import com.nearme.location.LocationManagerImpl;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.DaggerAppCompatActivity;

public class PlacesActivity extends DaggerAppCompatActivity implements LocationManager.Callback {


    @BindView(R.id.tab_layout)
    TabLayout mTabLayout;


    @BindView(R.id.view_pager)
    ViewPager mViewPager;


    @BindView(R.id.search_box)
    EditText mSearchBox;


    // PlacesActivity presenter
    @Inject
    PlacesPresenter mPlacesPresenter;


    // location manager
    @Inject
    LocationManager mLocationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // remove theme's activity background
        getWindow().setBackgroundDrawable(null);

        ButterKnife.bind(this);

        // setup TabLayout
        mTabLayout.addTab(mTabLayout.newTab().setIcon(getResources().getDrawable(R.drawable.ic_list)));
        mTabLayout.addTab(mTabLayout.newTab().setIcon(getResources().getDrawable(R.drawable.ic_map)));
        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // setup ViewPager
        mViewPager.setAdapter(new PlacesPageAdapter(getSupportFragmentManager(), mTabLayout.getTabCount()));
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        // add tab selection listener
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // pass tab number
                mViewPager.setCurrentItem(tab.getPosition());
                // register current view to presenter for data show
                registerCurrentView();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        // setup search box listener
        mSearchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (((event != null) && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || actionId == EditorInfo.IME_ACTION_DONE) {
                    // search for inserted place
                    mPlacesPresenter.searchPlace(mSearchBox.getText().toString());
                    // hide keyboard
                    View view = getCurrentFocus();
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                    // remove focus from search box
                    mSearchBox.clearFocus();
                    return true;
                }
                return false;
            }
        });
    }


    /**
     * Check and request location permissions from the user
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (ActivityCompat.checkSelfPermission(PlacesActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(PlacesActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                createLocationPermissionsRequest();
                return;
            }
        }
        // connect location manager
        mLocationManager.connect(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // disconnect location manager and release ref
        if (mLocationManager != null) mLocationManager.disconnect();
        mLocationManager = null;
    }


    /**
     * Get current user's location when location manager
     * is connected
     */
    @Override
    public void onLocationManagerConnected() {
        Location location = mLocationManager.getLocation();
        // save current location in presenter
        mPlacesPresenter.setLocation(location.getLatitude(), location.getLongitude());
        // start downloading nearby bars
        mPlacesPresenter.getNearbyBars(location.getLatitude(), location.getLongitude());
        // register current view to presenter for data show
        registerCurrentView();
    }


    /**
     * Request resolution on location api conflict
     *
     * @param status of Google Location API
     */
    @Override
    public void onLocationResolution(Status status) {
        try {
            status.startResolutionForResult(this, LocationManagerImpl.REQUEST_CODE_CHECK_LOCATION_SETTINGS);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }


    /**
     * Handle permission request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // connect location manager
            mLocationManager.connect(this);
        } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            // request permission
            createLocationPermissionsRequest();
        }
    }


    /**
     * Create location permission request
     */
    @SuppressLint("NewApi")
    private void createLocationPermissionsRequest() {
        requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        }, 10);
    }


    /**
     * Register selected view in presenter
     */
    public void registerCurrentView() {
        // get current fragment
        int index = mViewPager.getCurrentItem();
        PlacesContract.View currentView = (PlacesContract.View) getSupportFragmentManager().getFragments().get(index);
        // set current view in presenter
        mPlacesPresenter.setView(currentView);
        // pass presenter reference to current view
        currentView.setPresenter(mPlacesPresenter);
        // notify download completed to proceed with display
        currentView.onDownloadCompleted();
    }

}
