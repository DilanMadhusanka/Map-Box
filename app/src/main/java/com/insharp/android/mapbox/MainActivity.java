package com.insharp.android.mapbox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.layers.Property;
import com.mapbox.mapboxsdk.style.layers.PropertyFactory;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.mapboxsdk.utils.BitmapUtils;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements Callback<DirectionsResponse>, PermissionsListener {

    private MapView mapView;
    public MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    LocationComponent locationComponent;
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private CarmenFeature home;
    private CarmenFeature work;
    private String geojsonSourceLayerId = "geojsonSourceLayerId";
    private String symbolIconId = "symbolIconId";
    private static final int REQUEST_CODE = 5678;
    String address;
    Point origin = Point.fromLngLat(90.399452, 23.777176);
    Point destination = Point.fromLngLat(90.399452, 23.777176);
    private static final String ROUTE_LAYER_ID = "route-layer-id";
    private static final String ROUTE_SOURCE_ID = "route-source-id";
    private static final String ICON_LAYER_ID = "icon-layer-id";
    private static final String ICON_SOURCE_ID = "icon-source-id";
    private static final String RED_PIN_ICON_ID = "red-pin-icon-id";
    private MapboxDirections client;
    int c = 0;
    MapboxNavigation navigation;
    double distance;
    String st;
    String startLocation = "";
    String endLocation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Mapbox.getInstance(this, "pk.eyJ1IjoiZGlsem1hcGJveCIsImEiOiJja2w5YWp1ajMwbnZ6Mndydno0b2F3cm14In0.Ec9QDo9rPKV8pph8Ix3WEQ");
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));

        setContentView(R.layout.activity_main);

        navigation = new MapboxNavigation(this, getString(R.string.mapbox_access_token));

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull MapboxMap mapboxMap) {
                MainActivity.this.mapboxMap = mapboxMap;

                mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {

                        // Map is set up and the style has loaded. Now you can add data or make other map adjustments
//                        Layer waterLayer = style.getLayer("water");
//
//                        if (waterLayer != null) {
//                            waterLayer.setProperties(PropertyFactory.fillColor(Color.parseColor("#004f6b")));
//                        }

                        enableLocationComponent(style);
                        initSearchFab();

                        addUserLocation();

                        Drawable drawable = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_baseline_location_on_24, null);
                        Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);

                        // Add symbol layer icon to map for future use
                        style.addImage(symbolIconId, bitmap);

                        // Create an empty GeoJson source using the empty feature collection
                        setUpSource(style);

                        // Set up a new symbol layer for displaying the searched location's feature coordinates
                        setupLayer(style);

                        initSource(style);

                        initLayers(style);

                        mapboxMap.addOnMapClickListener(new MapboxMap.OnMapClickListener() {
                            @Override
                            public boolean onMapClick(@NonNull LatLng point) {
                                LatLng source;

                                // c is used to count no. of clicks on the map
                                if (c == 0) {
                                    origin = Point.fromLngLat(point.getLongitude(), point.getLatitude());
                                    source = point;
                                    MarkerOptions markerOptions = new MarkerOptions();
                                    markerOptions.position(point);
                                    markerOptions.title("Source");
                                    mapboxMap.addMarker(markerOptions);
                                    reverseGeocodeFunc(point, c); // to get Location details, place name from latitude and longitude
                                }
                                if (c == 1) {   // if c==1 then destination
                                    destination = Point.fromLngLat(point.getLongitude(), point.getLatitude());
                                    getRoute(mapboxMap, origin, destination);
                                    MarkerOptions markerOptions2 = new MarkerOptions();
                                    markerOptions2.position(point);
                                    markerOptions2.title("destination");
                                    mapboxMap.addMarker(markerOptions2);
                                    getRoute(mapboxMap, origin, destination); // then we show the route using polylines
                                }
                                if (c > 1) {
                                    c = 0;
                                    recreate(); // more than 2 click will restart the activity
                                }

                                c++;
                                return true;
                            }
                        });
                    }
                });

            }
        });
    }

    private void reverseGeocodeFunc(LatLng point, int c) {  // for getting place name
        MapboxGeocoding reverseGeocode = MapboxGeocoding.builder()
                .accessToken(getString(R.string.mapbox_access_token))
                .query(Point.fromLngLat(point.getLongitude(), point.getLatitude()))
                .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                .build();
        reverseGeocode.enqueueCall(new Callback<GeocodingResponse>() {
            @Override
            public void onResponse(Call<GeocodingResponse> call, Response<GeocodingResponse> response) {
                List<CarmenFeature> results = response.body().features();
                if (results.size() > 0) {
                    CarmenFeature feature;
                    // Log the first results point
                    Point firstResultPoint = results.get(0).center();
                    feature = results.get(0);
                    if (c==0) { // if c=0 we show source location
                        startLocation += feature.placeName();
                        TextView tv = findViewById(R.id.s);
                        tv.setText(startLocation);
                    }
                    if (c==1) { // show destination
                        endLocation += feature.placeName();
                        TextView tv2 = findViewById(R.id.d);
                        tv2.setText(endLocation);
                    }
                    Toast.makeText(MainActivity.this, "" + feature.placeName(), Toast.LENGTH_LONG).show();
                } else {
                    // No result for your request were found
                    Toast.makeText(MainActivity.this, "Not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeocodingResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void initLayers(@NonNull Style loadedMapStyle) {    // setting up design layer
        LineLayer routeLayer = new LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID);

        // Add the LineLayer to the map. This layer will display the direction route
        routeLayer.setProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(5f),
                PropertyFactory.lineColor(Color.parseColor("#009688"))
        );
        loadedMapStyle.addLayer(routeLayer);

        // Add the red marker icon image to the map
        loadedMapStyle.addImage(RED_PIN_ICON_ID, BitmapUtils.getBitmapFromDrawable(
                getResources().getDrawable(R.drawable.ic_baseline_location_on_24)
        ));

        // Add red marker icon SymbolLayer to the map
        loadedMapStyle.addLayer(new SymbolLayer(ICON_LAYER_ID, ICON_SOURCE_ID).withProperties(
                PropertyFactory.iconImage(RED_PIN_ICON_ID),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconOffset(new Float[]{0f, -9f})
        ));
    }

    private void initSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(ROUTE_SOURCE_ID));

        GeoJsonSource iconGeoJsonSource = new GeoJsonSource(ICON_SOURCE_ID, FeatureCollection.fromFeatures(new Feature[]{
                Feature.fromGeometry(Point.fromLngLat(origin.longitude(), origin.latitude())),
                Feature.fromGeometry(Point.fromLngLat(destination.longitude(), destination.latitude()))
        }));
        loadedMapStyle.addSource(iconGeoJsonSource);
    }

    //  function to show route between two points it was call from here
    private void getRoute(final MapboxMap mapboxMap, Point origin, final Point destination) {
        client = MapboxDirections.builder()
                .origin(origin)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.mapbox_access_token))
                .build();
        client.enqueueCall(this);
    }

    // navigation function use your own token everytime
    private void navigationRoute() {
        NavigationRoute.builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if (response.body() == null) {
                            Toast.makeText(MainActivity.this, "No routes found make sure to set right user and access token", Toast.LENGTH_SHORT).show();
                            return;
                        } else if (response.body().routes().size() <1 ) {
                            Toast.makeText(MainActivity.this, "No routes found", Toast.LENGTH_LONG).show();
                            return;
                        }
                        DirectionsRoute route = response.body().routes().get(0);    // getting route
                        boolean simulateRoute = true;

                        // Create a NavigationLauncherOptions object to package everything together
                        // launching navigation for the route
                        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                .directionsRoute(route)
                                .shouldSimulateRoute(simulateRoute)
                                .build();

                        // Call this method with context from within an Activity
                        NavigationLauncher.startNavigation(MainActivity.this, options);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

                    }
                });
    }

    //  this function is called from here
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        //  You can get the generic HTTP info about the response
        if (response.body() == null) {
            Toast.makeText(MainActivity.this, "No routes found make sure to set right user and access token", Toast.LENGTH_LONG).show();
            return;
        } else if (response.body().routes().size() < 1) {
            Toast.makeText(MainActivity.this, "No routes found", Toast.LENGTH_LONG).show();
        }

        // Get the direction route
        final DirectionsRoute currentRoute = response.body().routes().get(0);   // getting route we use two route
        // 1st one was for navigation, this one is when we click destination we get a route this is using mapbox direction
        distance = currentRoute.distance() / 1000; // Route distance in KM
        st = String.format("%.2f K.M", distance);
        TextView dv = findViewById(R.id.distanceView);
        dv.setText(st);

        if (mapboxMap != null) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {

                    // Retrieve and update the source designated for showing the directions route
                    GeoJsonSource source = style.getSourceAs(ROUTE_SOURCE_ID);

                    // Create a LineString with the direction s route's geometry and reset the GeoJSON source for the route LineLayer source
                    if (source != null) {
                        source.setGeoJson(LineString.fromPolyline(currentRoute.geometry(), Constants.PRECISION_6));
                    }
                }
            });
        }
    }

    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {

    }

    public void confirmed(View view) {
        navigationRoute();  // if clicked then navigation starts
    }

    //  placeAutoComplete initialize
    private void initSearchFab() {
        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.mapbox_access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#EEEEEE"))
                                .limit(10)
                                .addInjectedFeature(home)   // with two location 1st one
                                .addInjectedFeature(work)   // 2nd
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MainActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    // those locations were initialized here
    private void addUserLocation() {
        home = CarmenFeature.builder().text("Mapbox SF Office")
                .geometry(Point.fromLngLat(-122.3964485, 37.7912561))
                .placeName("50 Beale St, San Francisco, CA")
                .id("mapbox-sf")
                .properties(new JsonObject())
                .build();

        work = CarmenFeature.builder().text("Mapbox DC Office")
                .geometry(Point.fromLngLat(-77.0338348, 38.899750))
                .placeName("740 15th Street NW, Washington DC")
                .id("mapbox-dc")
                .properties(new JsonObject())
                .build();
    }

    private void setUpSource(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addSource(new GeoJsonSource(geojsonSourceLayerId));
    }

    private void setupLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addLayer(new SymbolLayer("SYMBOL_LAYER_ID", geojsonSourceLayerId).withProperties(
//                iconImage(symbolIconId);
//                iconOffset(new Float[] {0f, -8f});
        ));
    }

    // place autocomplete search activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {

            // Retrieve selected location's CarmenFeature
            CarmenFeature selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

            // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
            // Then retrieve and update the source designated for showing a selected location's symbol layer icon
            if (mapboxMap != null) {
                Style style = mapboxMap.getStyle();
                if (style != null) {
                    GeoJsonSource source = style.getSourceAs(geojsonSourceLayerId);
                    if (source != null) {
                        source.setGeoJson(FeatureCollection.fromFeatures(
                                new Feature[] {
                                        Feature.fromJson(selectedCarmenFeature.toJson())
                                }
                        ));
                    }

                    //  Move map camera to the selected location
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                            .target(new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(), ((Point) selectedCarmenFeature.geometry()).longitude()))
                            .zoom(14)
                            .build()
                    ), 4000);
                }
            }
        }
    }

    // function to show users initial location
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {

        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(MainActivity.this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Active with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(MainActivity.this, loadedMapStyle).build()
            );

            // Enable to make component visible
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationComponent.setLocationComponentEnabled(true);

            //  Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);   // if user moves the icon will move

            //  Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                }
            });
        } else {
            finish();
        }
    }

    //  add the mapView lifecycle to the activity's lifecycle methods - not essential function
    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigation.onDestroy(); //  you must add this
        mapView.onDestroy();
    }


}