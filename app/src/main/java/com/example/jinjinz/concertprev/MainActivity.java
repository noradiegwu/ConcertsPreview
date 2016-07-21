package com.example.jinjinz.concertprev;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.jinjinz.concertprev.database.UserDataSource;
import com.example.jinjinz.concertprev.fragments.ConcertDetailsFragment;
import com.example.jinjinz.concertprev.fragments.SearchFragment;
import com.example.jinjinz.concertprev.fragments.PlayerBarFragment;
import com.example.jinjinz.concertprev.fragments.PlayerScreenFragment;
import com.example.jinjinz.concertprev.fragments.SongsFragment;
import com.example.jinjinz.concertprev.fragments.UserFragment;
import com.example.jinjinz.concertprev.models.Concert;
import com.example.jinjinz.concertprev.models.Song;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        SearchFragment.ConcertsFragmentListener, PlayerScreenFragment.PlayerScreenFragmentListener,
        PlayerBarFragment.PlayerBarFragmentListener, ConcertDetailsFragment.SongsFragmentListener,
        ConcertDetailsFragment.ConcertDetailsFragmentListener {

    // Media player variables
    private ArrayList<Song> mSongs;
    private Concert mMediaPlayerConcert;
    private Concert mConcert;
    private MediaPlayer mediaPlayer;
    private int mCurrentSongIndex;
    protected PlayerBarFragment barFragment;
    protected PlayerScreenFragment playerFragment;
    //TODO: make only one instance of player and playerBar run after the navigation works

    // Search Fragment variables
    protected SearchFragment mSearchFragment;
    private boolean readyToPopulate = false;
    private boolean apiConnected = false;

    // Concerts details variables
    private ArrayList<Parcelable> mParcelableSongs; //I need this now
    protected ConcertDetailsFragment mConcertDetailsFragment; // songs fragment

    // Location variables
    private String latlong;
    protected String queryText;
    protected String permissions; // array or nah
    private static final int LOCATION_PERMISSIONS = 10;
    protected GoogleApiClient mGoogleApiClient;
    protected Location lastLocation;
    private String[] locationPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET};

    // database variables
    public static UserDataSource userDataSource;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create an instance of GoogleApiClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        }

        // Concerts fragment should always show first
        if (savedInstanceState == null) {
            mSearchFragment = mSearchFragment.newInstance(); // add params if needed
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.mainFragment, mSearchFragment);
            ft.commit();
        }

        // open data source
        userDataSource = new UserDataSource(MainActivity.this);
        userDataSource.openDB();

    }

    //Player + MediaPlayer methods
    ////////////////////////////////////////////////////

    private void onNewConcert(Concert c, ArrayList<Song> s) {
        //only play if new concert or is different from current playing
        if (mMediaPlayerConcert == null) {
            mCurrentSongIndex = 0;
            mMediaPlayerConcert = c;
            mSongs = s;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            updateProgressBar();

            //on prepared listener --> what happens when it is ready to play
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (playerFragment != null && playerFragment.isVisible()) {
                        playerFragment.updateInterface(mSongs.get(mCurrentSongIndex));
                        playerFragment.updatePlay(true);
                    }
                    if (barFragment != null && barFragment.isVisible()) {
                        barFragment.updatePlay(true);
                        barFragment.updateInterface(mSongs.get(mCurrentSongIndex));
                    }
                    mediaPlayer.start();
                }
            });

            //switch between songs
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.reset();
                    mCurrentSongIndex++;
                    if(mSongs != null && mCurrentSongIndex == mSongs.size()) {
                        mCurrentSongIndex = 0;
                    }
                    try {
                        mediaPlayer.setDataSource(mSongs.get(mCurrentSongIndex).getPreviewUrl());
                        mediaPlayer.prepareAsync();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("music player", "unknown error");

                    }
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    Toast.makeText(MainActivity.this, "No music playing", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            try {
                mediaPlayer.setDataSource(mSongs.get(mCurrentSongIndex).getPreviewUrl());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("music player", "unknown error");

            }
        }
        else {
            //initialize
            mCurrentSongIndex = 0;
            mMediaPlayerConcert = c;
            mSongs = s;
            mediaPlayer.stop();
            mediaPlayer.reset();
            //start with first song
            try {
                mediaPlayer.setDataSource(mSongs.get(mCurrentSongIndex).getPreviewUrl());
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("music player", "unknown error");

            }
        }
    }

    @Override
    public String getConcertName() {
        return mMediaPlayerConcert.getEventName();
    }

    @Override
    public void onConcertClick() {
        //TODO: go to concert fragment on concert name click?
    }

    @Override
    public void playPauseSong() {
        PlayerScreenFragment fragment = (PlayerScreenFragment) getSupportFragmentManager().findFragmentById(R.id.mainFragment);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            fragment.updatePlay(false);
        }
        else {
            mediaPlayer.start();
            fragment.updatePlay(true);
        }
    }
    @Override
    public void backInStack() {
        super.onBackPressed();
    }
    @Override
    public void skipNext() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        try {
            mCurrentSongIndex++;
            if(mCurrentSongIndex == mSongs.size()) {
                mCurrentSongIndex = 0;
            }
            mediaPlayer.setDataSource(mSongs.get(mCurrentSongIndex).getPreviewUrl());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("music player", "unknown error: skipNext");

        }
    }

    @Override
    public void skipPrev() {
        mediaPlayer.stop();
        mediaPlayer.reset();
        if (mCurrentSongIndex >= 1) {
            mCurrentSongIndex = mCurrentSongIndex - 1;
        }
        else if (mCurrentSongIndex == 0) {
            mCurrentSongIndex = mSongs.size() - 1;
        }
        try {
            mediaPlayer.setDataSource(mSongs.get(mCurrentSongIndex).getPreviewUrl());
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("music player", "unknown error: skipPrev");
        }
    }
    @Override
    public void onClosePlayer() {
        //PlayerBarFragment playerBar = (PlayerBarFragment) getSupportFragmentManager().findFragmentByTag("bar");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (barFragment == null) {
            barFragment = PlayerBarFragment.newInstance();
            ft.replace(R.id.playerFragment, barFragment, "bar");
            ft.commit();
        }
        else {
            ft.show(barFragment);
            ft.commit();
            onOpenBar();
        }

    }

    @Override
    public void onOpenPlayer() {
        playerFragment.updateInterface(mSongs.get(mCurrentSongIndex));
        playerFragment.updatePlay(mediaPlayer.isPlaying());
    }

    public void updateProgressBar() {
        //attempt at progressbar
        new Thread(new Runnable() {
            public void run() {
                int currentPosition = 0;
                while (true) {
                    try {
                        Thread.sleep(100);
                        currentPosition = mediaPlayer.getCurrentPosition();
                    } catch (InterruptedException e) {
                        Log.d("progress bar", "error: Interrupted");
                        return;
                    } catch (Exception e) {
                        Log.d("progress bar", "unknown error concerning progress bar");
                        return;
                    }
                    //PlayerScreenFragment fragment = (PlayerScreenFragment) getSupportFragmentManager().findFragmentByTag("player");
                    if (playerFragment != null && playerFragment.isVisible()) {
                        playerFragment.setProgressBar(currentPosition);
                    }
                }
            }
        }).start();
    }
    ////////////////////////////////////////////////////

    // Search Fragment
    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////

    // Fragment methods

    AsyncHttpClient client;
    // Fetch
    public void fetchConcerts() {
        if (readyToPopulate && apiConnected) {
            // url: includes api key and music classification
            String eventsURL = "https://app.ticketmaster.com/discovery/v2/events.json?apikey=7elxdku9GGG5k8j0Xm8KWdANDgecHMV0&classificationName=Music";
            // the parameter(s)
            RequestParams params = new RequestParams();
            if (queryText != null) {
                params.put("keyword", queryText);
            }
            if (lastLocation != null) {
                latlong = lastLocation.getLatitude() + "," + lastLocation.getLongitude(); //(MessageFormat.format("{0},{1}", lastLocation.getLatitude(), lastLocation.getLongitude()));
                // getLastLocation()
            } else {
                latlong = null;
            }

            params.put("latlong", latlong); // must be N, E (in the us the last should def be -) that num + is W
            params.put("radius", "50");
            params.put("size", "100");

            // call client
            client = new AsyncHttpClient();
            client.get(eventsURL, params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject jsonObject) { // on success I will get back the large json obj: { _embedded: { events: [ {0, 1, 2, ..} ] } }
                    // DESERIALIZE JSON
                    // CREATE MODELS AND ADD TO ADAPTER
                    // LOAD MODEL INTO LIST VIEW
                    JSONArray eventsArray = null;
                    try {
                        eventsArray = jsonObject.getJSONObject("_embedded").getJSONArray("events");
                        mSearchFragment.addConcerts(Concert.concertsFromJsonArray(eventsArray)); //
                        SearchFragment.mSwipeRefreshLayout.setRefreshing(false);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("client calls", "error adding concerts: " + statusCode);
                        if(queryText != null) {
                            SearchFragment.searchAdapter.clear();
                            Toast.makeText(MainActivity.this, "There are no concerts for " + queryText + "in your area", Toast.LENGTH_LONG).show(); // maybe make a snack bar to go back to main page, filter, or search again
                        } else {
                            Toast.makeText(MainActivity.this, "Could not load page", Toast.LENGTH_SHORT).show();
                        }
                        SearchFragment.mSwipeRefreshLayout.setRefreshing(false);
                    }

                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    Log.d("client calls", "TicketMaster client GET error: " + statusCode);
                    Toast.makeText(MainActivity.this, "Could not display concerts. Please wait and try again later.", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    @Override
    public void populateConcerts(SearchFragment fragment, String query) {
        // set ready flag
        readyToPopulate = true;
        // set queryText for use in concerts GET method
        queryText = query;
        // fetch
        fetchConcerts();
    }
    //TODO figure out what's up
    @Override
    public void onConcertTap(Concert concert) {
        // open songs fragment --> needs more stuff from songsfrag
        mConcertDetailsFragment = mConcertDetailsFragment.newInstance(Parcels.wrap(concert)); // add params if needed
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFragment, mConcertDetailsFragment);
        ft.addToBackStack("concerts");
        ft.commit();
    }

    ///// Google api methods /////
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this, locationPermissions, LOCATION_PERMISSIONS);

        } else {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            apiConnected = true;
            fetchConcerts();

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        //TODO
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //TODO
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case LOCATION_PERMISSIONS: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("requestlocation", "Permissions Granted");

                } else {
                    Log.d("requestlocation", "Permissions Denied");
                }

            }
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    //Playerbar
    ////////////////////////////////////////////////////
    @Override
    public void openPlayer() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFragment, playerFragment, "player");
        if (barFragment != null) {
            ft.hide(getSupportFragmentManager().findFragmentByTag("bar"));
        }
        ft.addToBackStack("player");
        ft.commit();
    }

    @Override
    public void playPauseBarBtn() {
        PlayerBarFragment fragment = (PlayerBarFragment) getSupportFragmentManager().findFragmentById(R.id.playerFragment);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            fragment.updatePlay(false);
        }
        else {
            mediaPlayer.start();
            fragment.updatePlay(true);
        }
    }
    @Override
    public void onOpenBar() {
        barFragment.updateInterface(mSongs.get(mCurrentSongIndex));
        if (mediaPlayer.isPlaying()) {
            barFragment.updatePlay(true);
        }
        else {
            barFragment.updatePlay(false);
        }

    }
    ////////////////////////////////////////////////////



    // Concert + Songs Fragment
    ////////////////////////////////////////////////////

    public void setUpArtistSearch(final SongsFragment fragment, Concert concert, int artistIndex, int songsPerArtist){
        String url = "https://api.spotify.com/v1/search";

        client = new AsyncHttpClient();
        mParcelableSongs = new ArrayList<>();
        mConcert = concert;
        RequestParams params = new RequestParams();
        params.put("q", concert.getArtists().get(artistIndex));
        params.put("type", "artist");
        params.put("limit", 1);

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                String artistJSONResult;
                try {
                    artistJSONResult = response.getJSONObject("artists").getJSONArray("items").getJSONObject(0).getString("id");
                    searchArtistPlaylist(fragment, artistJSONResult);
                } catch (JSONException e){
                    e.printStackTrace();
                    Log.d("client calls", "could not retrieve artist id: " + statusCode);
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.d("client calls", "Spotify client GET error: " + statusCode);
            }
        });
    }

    public void searchArtistPlaylist(final SongsFragment fragment, String artistId){
        String ISOCountryCode = "US";
        String url = "https://api.spotify.com/v1/artists/" + artistId + "/top-tracks";
        RequestParams params = new RequestParams();
        params.put("country", ISOCountryCode);

        client.get(url, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                JSONArray songsJSONResult;
                try {
                    songsJSONResult = response.getJSONArray("tracks");
                    fragment.addSongs(Song.fromJSONArray(songsJSONResult, 7));

                } catch (JSONException e){
                    e.printStackTrace();
                    Log.d("client calls", "Spotify client tracks GET error: " + statusCode);
                }
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Toast.makeText(getApplicationContext(), "songs failed", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void launchSongView(Song song, ArrayList<Parcelable> tempSongs){
        if (playerFragment == null) {
            playerFragment = playerFragment.newInstance(song);
        }
        ArrayList<Song> pSongs2 = new ArrayList<>();
        for (int i = 0; i < tempSongs.size(); i++) {
            pSongs2.add(i, (Song) Parcels.unwrap(tempSongs.get(i)));
        }
        Collections.shuffle(pSongs2);
        pSongs2.add(0, song);
        onNewConcert(mConcert, pSongs2);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFragment, playerFragment, "player");
        if (barFragment != null) {
            ft.hide(getSupportFragmentManager().findFragmentByTag("bar"));
        }

        ft.addToBackStack("player");
        ft.commit();
    }

    @Override
    public void onLikeConcert(Concert concert) {
        userDataSource.insertLikedConcert(concert);
    }

    public void getLikes(MenuItem item) {
        Intent intent = new Intent(MainActivity.this, DBTestActivity.class);
        ArrayList<Concert> likedConcerts = userDataSource.getAllLikedConcerts();
        intent.putExtra("concerts", Parcels.wrap(likedConcerts));
        startActivity(intent);
    }


    ////////////////////////////////////////////////////

    public void getProfile(MenuItem item) {
        UserFragment userFragment = UserFragment.newInstance();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.mainFragment, userFragment, "user");
        ft.addToBackStack("user");
        ft.commit();
    }


}
