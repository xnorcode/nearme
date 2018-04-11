package com.nearme.data.source.remote;

import java.io.IOException;

import okhttp3.Call;

/**
 * Created by xnorcode on 06/04/2018.
 */

public interface GooglePlacesApiHelper {


    /**
     * @param lat the latitude
     * @param lng the longitude
     * @return OkHttp network call response
     * @throws IOException for network call
     */
    Call getNearbyBars(double lat, double lng) throws IOException;


    /**
     * @param name the name of place to be searched
     * @return OkHttp network call response
     * @throws IOException for network call
     */
    Call searchPlace(String name) throws IOException;

}
