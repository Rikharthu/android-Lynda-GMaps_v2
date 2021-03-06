package com.example.android.mymaps;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.List;

//TODO request permission in onCreate()
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback
, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean CUSTOM_INFO_WINDOW = true;

    private boolean mapReady = false;
    GoogleMap mMap;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private static final double
            SEATTLE_LAT = 47.60621,
            SEATTLE_LNG = -122.33207,
            SYDNET_LAT = -33.867487,
            SYDNEY_LNG = 151.20699,
            NEWYORK_LAT = 40.714353,
            NEWYORK_LNG = -74.005973;

    // better than old LocationManager
    private GoogleApiClient mLocationClient;
    private LocationListener mListener;
    private TextView mCoordinatesTextView;
    private Marker mMarker1,mMarker2;
    private Polyline mPolyLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (checkPlayServices()) {
            setContentView(R.layout.activity_map);
            mCoordinatesTextView= (TextView) findViewById(R.id.coordinates);

            // connect to google play services (for location)
            mLocationClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API) // specify we want Location Service
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            // now connect to services
            mLocationClient.connect();

            initMap();

        } else {
            setContentView(R.layout.activity_main);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.mapTypeNone:
                mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                break;
            case R.id.mapTypeHybrid:
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                break;
            case R.id.mapTypeNormal:
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
            case R.id.mapTypeSatellite:
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.mapTypeTerrain:
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean servicesOK() {
        // TODO find non-deprecated alternative
        int isAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            // everything is ok
            return true;
        } else if (GooglePlayServicesUtil.isUserRecoverableError(isAvailable)) {
            // user can take action to recover
            Dialog dialog =
                    GooglePlayServicesUtil.getErrorDialog(isAvailable, this, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            // user can't do anything about the error
            Toast.makeText(this, "Could not connect to mapping service", Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }

    /** Non-deprecated check */
    private boolean checkPlayServices() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                // user can take action to fix situation
                googleAPI.getErrorDialog(this, result,
                        ERROR_DIALOG_REQUEST).show();
            }

            return false;
        }
        // everything is OK
        return true;
    }

    private void initMap() {
        if (mMap == null) {
            SupportMapFragment mapFragment =
                    (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            // get map associated with that fragment
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // map is ready to be used
        mMap = googleMap;
        Toast.makeText(this, "Ready to map", Toast.LENGTH_SHORT).show();
        mapReady = true;
//        gotoLocation(SEATTLE_LAT, SEATTLE_LNG, 5);
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        setMyLocation();

        // setup custom info window from xml layout
        if(CUSTOM_INFO_WINDOW){
            // custom info window
            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View view = getLayoutInflater().inflate(R.layout.info_window,null);

                    TextView tvLocality = (TextView) view.findViewById(R.id.tvLocality);
                    TextView tvLatitude = (TextView) view.findViewById(R.id.tvLat);
                    TextView tvLongtitude = (TextView) view.findViewById(R.id.tvLng);
                    TextView tvSnippet = (TextView) view.findViewById(R.id.tvSnippet);

                    // get info from the marker object
                    LatLng latlng = marker.getPosition();
                    tvLatitude.setText("latitude: "+latlng.latitude+"");
                    tvLongtitude.setText("longtitude: "+latlng.longitude+"");
                    tvLocality.setText(marker.getTitle());
                    tvSnippet.setText(marker.getSnippet());

                    return view;
                }
            });


            // setup marker listener

            mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    Geocoder gc = new Geocoder(MainActivity.this);
                    List<Address> list=null;

                    try {
                        list=gc.getFromLocation(latLng.latitude,latLng.longitude,1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    Address add = list.get(0);
                    MainActivity.this.addMarker(add,latLng.latitude,latLng.longitude);

                    Vibrator v = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    v.vibrate(500);
                }
            });

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String msg = marker.getTitle()+" ("+marker.getPosition().latitude+", "+marker.getPosition().longitude+")";
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return false;
                }
            });

            // Marker drag listener
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    LatLng latlng=marker.getPosition();
                    Geocoder gc = new Geocoder(MainActivity.this);
                    List<Address> list=null;

                    try {
                        list=gc.getFromLocation(latlng.latitude,latlng.longitude,1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    Address add = list.get(0);
                    marker.setTitle(add.getLocality());
                    marker.setSnippet(add.getCountryName());
                    // re-display info window (updates coordinates too)
                    marker.showInfoWindow();
                }
            });
        }

    }

    private void gotoLocation(double lat, double lng, float zoom) {
        LatLng latlng = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(latlng, zoom);
//        mMap.moveCamera(update); // without animation
        mMap.animateCamera(update);

        if(mMarker1 !=null){
            // delete previous marker
            mMarker1.remove();
        }
        // save marker instance
        mMarker1 = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title("You are here!")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker))
//                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(.5f));
        // all marker attributes can also be changed durign runtime
//        mMarker1.setAlpha(.5f);


    }

    private void setMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an expanation to the user
                Toast.makeText(this, "I NEED IT!", Toast.LENGTH_SHORT).show();
                String[] permissions = new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, permissions, 4008);
            } else {
                String[] permissions = new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION};
                ActivityCompat.requestPermissions(this, permissions, 4008);
            }
            return;
        }

        // Enable "my location" button in the upper-right corner
        mMap.setMyLocationEnabled(true);
    }

    private void hideSoftKeyboard(View v){
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(),0);
    }

    public void geoLocate(View view) throws IOException {
        hideSoftKeyboard(view);

        TextView tv = (TextView) findViewById(R.id.editText1);
        String searchString = tv.getText().toString();
        Toast.makeText(this,"Searching for: "+searchString,Toast.LENGTH_SHORT).show();


        Geocoder gc = new Geocoder(this);
        List <Address> list=gc.getFromLocationName(searchString,1);
        if(list.size()>0){
            Address add=list.get(0);
            String locality = add.getLocality(); // name of that location from geocoding service
            Toast.makeText(this,"Found: "+locality,Toast.LENGTH_SHORT).show();
            double lat = add.getLatitude();
            double lng = add.getLongitude();
//            gotoLocation(lat,lng,15);
            CameraUpdate update = CameraUpdateFactory.newLatLng(new LatLng(lat,lng));
            mMap.animateCamera(update);
            addMarker(add,lat,lng);

        }
    }

    private void addMarker(Address add, double lat, double lng){
        MarkerOptions options = new MarkerOptions()
                .title(add.getLocality())
                .position(new LatLng(lat,lng))
                .draggable(true) // if is draggable
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker));
        // if you change icon - info window wont be shown until you code it yourself
        String country = add.getCountryName();
        if(country.length()>0){
            options.snippet(country); // text below the title
        }
        if(mMarker1==null){
            Toast.makeText(this, "Marker 1 set", Toast.LENGTH_SHORT).show();
            mMarker1=mMap.addMarker(options);
        }else if(mMarker2==null){
            Toast.makeText(this, "Marker 2 set", Toast.LENGTH_SHORT).show();
            mMarker2=mMap.addMarker(options);
            drawLine();
        }else{
            Toast.makeText(this, "Reset. Marker 1 set", Toast.LENGTH_SHORT).show();
            // reset
            mMarker1.remove();
            mMarker1=null;
            mMarker2.remove();
            mMarker2=null;
            if(mPolyLine!=null){
                mPolyLine.remove();
                mPolyLine=null;
            }
            mMarker1=mMap.addMarker(options);
        }
    }

    private void drawLine() {
        PolylineOptions lineOptions = new PolylineOptions()
                .add(mMarker1.getPosition(),mMarker2.getPosition());
        mPolyLine = mMap.addPolyline(lineOptions);
    }

    private void addCustomWindowMarker(Address add, double lat, double lng){
        MarkerOptions options = new MarkerOptions()
                .title(add.getLocality())
                .position(new LatLng(lat,lng))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker));

        String country = add.getCountryName();
        if(country.length()>0){
            options.snippet(country); // text below the title
        }
        mMap.addMarker(options);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 4008: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    setMyLocation();
                } else {
                    Toast.makeText(this, "Permissions is required to access your location!", Toast.LENGTH_SHORT).show();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public void showCurrentLocation(MenuItem item) {
        // use google play services for location
        Location currentLocation = LocationServices.FusedLocationApi
                .getLastLocation(mLocationClient);
        if(currentLocation == null){
            Toast.makeText(this, "Couldn't connect", Toast.LENGTH_SHORT).show();
        }else{
            gotoLocation(currentLocation.getLatitude(),currentLocation.getLongitude(),15);
            Toast.makeText(this,"Your location:\n lat: "+currentLocation.getLatitude()+"\n"
            +"lng: "+currentLocation.getLongitude(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // successfully connected to the location service
        Toast.makeText(this, "Connected to Location Service", Toast.LENGTH_SHORT).show();
        final Geocoder gc = new Geocoder(this);
        // register location listener
        // FIXME do it in another thread
        mListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double lat = location.getLatitude();
                double lng=location.getLongitude();
                mCoordinatesTextView.setText(String.format("%f, %f",lat,lng));
                /*
                try {
                    // FIXME refactor
                    mCoordinatesTextView.append("\n"+gc.getFromLocation(lat,lng,1).get(0).getAddressLine(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            }
        };
        LocationRequest request = LocationRequest.create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(5000); // refresh duration in ms
        request.setFastestInterval(3000);
        // FIXME disconnect in onPause, reconnect in onResume()
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient,request,mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disconnect from location updates
        LocationServices.FusedLocationApi.removeLocationUpdates(mLocationClient,mListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mListener!=null) {
            LocationRequest request = LocationRequest.create();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            request.setInterval(5000); // refresh duration in ms
            request.setFastestInterval(1000);
            LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, request, mListener);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
