package com.gerrish.mflmanagerfree;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class SQLiteGetPlayers extends Activity {
	
	public static final String TAG =SQLiteGetPlayers.class.getSimpleName();
	protected JSONObject mPlayerData;
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_list);

        // INIT OUR SQLITE HELPER
        sqh = new SQLiteHelper(this);

        // RETRIEVE A READABLE AND WRITEABLE DATABASE
        sqdb = sqh.getWritableDatabase();

        // GET DATA FROM API IN JSON FORMAT
		GetPlayerDataTask getPlayerDataTask = new GetPlayerDataTask();
		getPlayerDataTask.execute();
        

    }
	
	public class GetPlayerDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;
			
			try {
				
				URL matchupUrl = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=players&L=&W=&JSON=1");				
				
				HttpURLConnection connection = (HttpURLConnection) matchupUrl.openConnection();
				connection.connect();
				responseCode = connection.getResponseCode();
				
				if (responseCode == HttpURLConnection.HTTP_OK ) { 
					InputStream inputStream = connection.getInputStream();
					String responseData = IOUtils.toString(inputStream, "ISO-8859-1");					

					jsonResponse = new JSONObject(responseData);								
					
				}
				else {
					Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode);
				}
				Log.i(TAG, "Code: " + responseCode);
				}
			catch (MalformedURLException e) {
				logException(e);
			}
			catch (IOException e) {
				logException(e);
			}
			catch (Exception e) {
				logException(e);
			}
//				Log.i(TAG, jsonResponse.toString());
				return jsonResponse;				
		}
		
		@Override
		protected void onPostExecute(JSONObject result) {
			mPlayerData = result;
			
			//// PARSE JSON DATA INO STRINGS
			handleBlogResponse();			
		}
	}
	
	private void logException(Exception e) {
		Log.e(TAG, "Exception caught: ", e);
	}
	
	public void handleBlogResponse() {
				
		if (mPlayerData == null ) {			
			updateDisplayForError();
		}
		else {
			try {
				
				JSONArray jsonPlayers = mPlayerData.getJSONObject("players").getJSONArray("player");	
				
				// INSERT USING CONTENTVALUE CLASS
		        ContentValues cv = new ContentValues();
		        
				for (int i = 0; i < jsonPlayers.length(); i++) {
					String playerName = jsonPlayers.getJSONObject(i).getString("name");
					String mflId = jsonPlayers.getJSONObject(i).getString("id");
					String positionType = jsonPlayers.getJSONObject(i).getString("position");
					String teamName = jsonPlayers.getJSONObject(i).getString("team");
			        
					cv.put(SQLiteHelper.PlayerTable.NAME, playerName);
					cv.put(SQLiteHelper.PlayerTable.MFLID, mflId);
			        cv.put(SQLiteHelper.PlayerTable.POSITION, positionType);
			        cv.put(SQLiteHelper.PlayerTable.TEAM, teamName);
			        
			        // CALL INSERT METHOD
			        sqdb.insert(SQLiteHelper.PlayerTable.TABLE_NAME, SQLiteHelper.PlayerTable.NAME, cv);

				}

		        // QUERY USING WRAPPER METHOD
		        Cursor c = sqdb.query(SQLiteHelper.PlayerTable.TABLE_NAME, new String[] { SQLiteHelper.PlayerTable.UID, SQLiteHelper.PlayerTable.NAME, 
		        		SQLiteHelper.PlayerTable.MFLID, SQLiteHelper.PlayerTable.POSITION, SQLiteHelper.PlayerTable.TEAM }, null, null, null, null, null);

		        while (c.moveToNext()) {
		            // GET COLUMN INDICES AS WELL AS VALUES OF THOSE COLUMNS
		            int id = c.getInt(c.getColumnIndex(SQLiteHelper.PlayerTable.UID));
		            String name = c.getString(c.getColumnIndex(SQLiteHelper.PlayerTable.NAME));
		            Log.i("LOG_TAG", "ROW " + id + " HAS NAME " + name);
		        }

		        c.close();

		        // CLOSE DATABASE CONNECTIONS
		        sqdb.close();
		        sqh.close();		
			
			}				
				
				
			catch (JSONException e) {
				logException(e);
			}			
		
		}  //belongs to the else
	}

	private void updateDisplayForError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title));
		builder.setMessage(getString(R.string.error_message));
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();			
	}	
	
}

