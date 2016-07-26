package com.example.jinjinz.concertprev.databases;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

/**
 * Created by jinjinz on 7/25/16.
 */
public class SongProvider extends ContentProvider {
    // Use an int for each URI we will run, this represents the different queries
    private static final int PLAYLIST = 100;
    private static final int PLAYLIST_ID = 101;
    private static final int CURRENT_SONG = 200;
    private static final int CURRENT_SONG_ID = 201;
    private static final int CURRENT_CONCERT = 300;
    private static final int CURRENT_CONCERT_ID = 301;

    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private MediaPlayerDatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new MediaPlayerDatabaseHelper(getContext());
        return true;
    }

    /**
     * Builds a UriMatcher that is used to determine witch database request is being made.
     */
    public static UriMatcher buildUriMatcher(){
        String content = MediaContract.CONTENT_AUTHORITY;

        // All paths to the UriMatcher have a corresponding code to return
        // when a match is found (the ints above).
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(content, MediaContract.PATH_CONCERT, CURRENT_CONCERT);
        matcher.addURI(content, MediaContract.PATH_CONCERT + "/#", CURRENT_CONCERT_ID);
        matcher.addURI(content, MediaContract.PATH_NOW, CURRENT_SONG);
        matcher.addURI(content, MediaContract.PATH_NOW + "/#", CURRENT_SONG_ID);
        matcher.addURI(content, MediaContract.PATH_PLAYLIST, PLAYLIST);
        matcher.addURI(content, MediaContract.PATH_PLAYLIST + "/#", PLAYLIST_ID);
        return matcher;
    }

    @Override
    public String getType(Uri uri) {
        switch(sUriMatcher.match(uri)){
            case CURRENT_CONCERT:
                return CurrentConcertTable.CONTENT_TYPE;
            case CURRENT_CONCERT_ID:
                return CurrentConcertTable.CONTENT_ITEM_TYPE;
            case CURRENT_SONG:
                return CurrentSongTable.CONTENT_TYPE;
            case CURRENT_SONG_ID:
                return CurrentSongTable.CONTENT_ITEM_TYPE;
            case PLAYLIST:
                return PlaylistTable.CONTENT_ITEM_TYPE;
            case PLAYLIST_ID:
                return PlaylistTable.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor retCursor;
        switch(sUriMatcher.match(uri)){
            case CURRENT_CONCERT:
                retCursor = db.query(
                        CurrentConcertTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case CURRENT_CONCERT_ID:
                long _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        CurrentConcertTable.TABLE_NAME,
                        projection,
                        CurrentConcertTable.COLUMN_ENTRY_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            case CURRENT_SONG:
                retCursor = db.query(
                        CurrentSongTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case CURRENT_SONG_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        CurrentSongTable.TABLE_NAME,
                        projection,
                        CurrentSongTable.COLUMN_ENTRY_ID+ " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            case PLAYLIST:
                retCursor = db.query(
                        PlaylistTable.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case PLAYLIST_ID:
                _id = ContentUris.parseId(uri);
                retCursor = db.query(
                        PlaylistTable.TABLE_NAME,
                        projection,
                        PlaylistTable.COLUMN_ENTRY_ID + " = ?",
                        new String[]{String.valueOf(_id)},
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Set the notification URI for the cursor to the one passed into the function. This
        // causes the cursor to register a content observer to watch for changes that happen to
        // this URI and any of it's descendants. By descendants, we mean any URI that begins
        // with this path.
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long _id;
        Uri returnUri;

        switch(sUriMatcher.match(uri)){
            case CURRENT_CONCERT:
                _id = db.insert(CurrentConcertTable.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri =  CurrentConcertTable.buildCurrConcertUri(_id);
                } else{
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            case CURRENT_SONG:
                _id = db.insert(CurrentSongTable.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri = CurrentSongTable.buildCurrentUri(_id);
                } else{
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            case PLAYLIST:
                _id = db.insert(PlaylistTable.TABLE_NAME, null, values);
                if(_id > 0){
                    returnUri = PlaylistTable.buildPlaylistUri(_id);
                } else{
                    throw new UnsupportedOperationException("Unable to insert rows into: " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Use this on the URI passed into the function to notify any observers that the uri has
        // changed.
        getContext().getContentResolver().notifyChange(uri, null);
        return returnUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rows; // Number of rows effected

        switch(sUriMatcher.match(uri)){
            case CURRENT_CONCERT:
                rows = db.delete(CurrentConcertTable.TABLE_NAME, selection, selectionArgs);
                break;
            case CURRENT_SONG:
                rows = db.delete(CurrentSongTable.TABLE_NAME, selection, selectionArgs);
                break;
            case PLAYLIST:
                rows = db.delete(PlaylistTable.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because null could delete all rows:
        if(selection == null || rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int rows;

        switch(sUriMatcher.match(uri)){
            case CURRENT_CONCERT:
                rows = db.update(CurrentConcertTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CURRENT_SONG:
                rows = db.update(CurrentSongTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            case PLAYLIST:
                rows = db.update(PlaylistTable.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(rows != 0){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rows;
    }
}
