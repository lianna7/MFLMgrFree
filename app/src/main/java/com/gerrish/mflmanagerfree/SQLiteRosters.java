package com.gerrish.mflmanagerfree;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class SQLiteRosters extends ListActivity {
	
	public static final String TAG =SQLiteRosters.class.getSimpleName();
	protected JSONObject mPlayerData;
	protected JSONObject mAllPlayersData;
	protected JSONObject mScoreData;
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	String currentLeagueId = "";
	String currentTeamId = "";
	ProgressDialog progressDialog;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rosters_list);         
		
		if (isNetworkAvailable()){
				
	        // Check Shared Preferences for League ID
	        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  currentLeagueId = preferences.getString("Id","");
			  if(!currentLeagueId.equalsIgnoreCase(""))
			  {
			    currentLeagueId = preferences.getString("Id", "");
			    currentTeamId = preferences.getString("TeamID", "");
			  }
			  
	        // INIT OUR SQLITE HELPER
	        sqh = new SQLiteHelper(this);

	        // RETRIEVE A READABLE AND WRITEABLE DATABASE
	        sqdb = sqh.getWritableDatabase();        
	        
	        progressDialog = new ProgressDialog(SQLiteRosters.this);
	        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        progressDialog.setMax(100);	        
	        progressDialog.setTitle("Loading Roster Data");
	        progressDialog.setMessage("Please Wait...");
	        progressDialog.setCancelable(false);
	        progressDialog.show();
	        
	        // GET DATA FROM API IN JSON FORMAT
			GetRosterDataTask getPlayerDataTask = new GetRosterDataTask();
			getPlayerDataTask.execute();
	 		
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();			
		}			

    }
	
	private class GetRosterDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;			
			
			try {							 	
				  
				URL rosterUrl = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=rosters&L=" + currentLeagueId + "&FRANCHISE=" + currentTeamId + "&JSON=1");				
				
				HttpURLConnection connection = (HttpURLConnection) rosterUrl.openConnection();
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
//			if(progressDialog!=null)
//			    progressDialog.dismiss();
			progressDialog.setProgress(40);
			// PARSE JSON DATA INO STRINGS
			GetScoreDataTask getScoreDataTask = new GetScoreDataTask();
			getScoreDataTask.execute();			
		}
	}
		
		private class GetScoreDataTask extends AsyncTask<Object, Void, JSONObject> {
			
					
			@Override
			protected JSONObject doInBackground(Object... arg0) {			
				int responseCode2 = -1;			
				JSONObject jsonScoreResponse = null;				
				
				
				try {	
					URL scoresUrl = new 
							URL("http://football.myfantasyleague.com/2013/export?TYPE=playerScores&L=" + currentLeagueId + "&W=YTD&JSON=1");
					
					HttpURLConnection connection = (HttpURLConnection) scoresUrl.openConnection();
					connection.connect();
					responseCode2 = connection.getResponseCode();
					
					if (responseCode2 == HttpURLConnection.HTTP_OK ) { 
						InputStream inputStream = connection.getInputStream();
						String scoreResponseData = IOUtils.toString(inputStream, "ISO-8859-1");					

						jsonScoreResponse = new JSONObject(scoreResponseData);
						Log.i(TAG, "Code: " + responseCode2);
						progressDialog.setProgress(65);
					}
					else {
						Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode2);
						}
					
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
					return jsonScoreResponse;				
			}			

		@Override
		protected void onPostExecute(JSONObject result) {
			mScoreData = result;
			progressDialog.setProgress(65);		
			String querySql = "SELECT * FROM " + SQLiteHelper.PlayerTable.TABLE_NAME;	        		
	//		SQLiteHelper.PlayerTable.NAME + 
	//		" FROM " + SQLiteHelper.PlayerTable.TABLE_NAME; 
//			Log.i(TAG, "Made it past the query creation");
			Cursor cursor = sqdb.rawQuery(querySql, null);
//			Log.i(TAG, "Made it past the cursor creation");
			
			if (cursor != null && cursor.getCount()>0) {
//				Log.i(TAG, "Made to if statement - 1" + cursor.getCount());			
				// PARSE JSON DATA INO STRINGS
				progressDialog.setProgress(70);
				handleMflResponse();			
			}
			else {
//				Log.i(TAG, "Made to if statement - 2");
				ToastShow();
				GetAllPlayersDataTask getAllPlayersDataTask = new GetAllPlayersDataTask();
				getAllPlayersDataTask.execute();
				}
			}
		}
		
		public void ToastShow() {
			Toast.makeText(this, "Commencing one-time player download, estimated completion time: 45 seconds. Thanks for your patience!", Toast.LENGTH_LONG).show();
		}
		
		
		public class GetAllPlayersDataTask extends AsyncTask<Object, Void, JSONObject> {
			
			@Override
			protected JSONObject doInBackground(Object... arg0) {			
				int responseCode = -1;			
				JSONObject jsonAllPlayersResponse = null;
							
				try {				

					URL allPlayersUrl= new 
							URL("http://football.myfantasyleague.com/2013/export?TYPE=players&L=&W=&JSON=1");				
					
					HttpURLConnection connection = (HttpURLConnection) allPlayersUrl.openConnection();
					connection.connect();
					responseCode = connection.getResponseCode();
					
					if (responseCode == HttpURLConnection.HTTP_OK ) { 
						InputStream inputStream = connection.getInputStream();
						String responseData = IOUtils.toString(inputStream, "ISO-8859-1");					

						jsonAllPlayersResponse = new JSONObject(responseData);
						Log.i(TAG, "Code: " + responseCode);						
					}
					else {
						Log.i(TAG, "Unsuccessful HTTP Response Code: " + responseCode);
						}					
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
//					Log.i(TAG, jsonResponse.toString());
					return jsonAllPlayersResponse;				
			}	

			@Override
			protected void onPostExecute(JSONObject result) {
				mAllPlayersData = result;
				progressDialog.setProgress(70);
				// PARSE JSON DATA INO STRINGS					
				handleMflResponse();
			}
		}		
	
	private void logException(Exception e) {
		Log.e(TAG, "Exception caught: ", e);
	}
	
	public void handleMflResponse() {
				
		if (mPlayerData == null || mScoreData == null) {			
			updateDisplayForError();
		}
		else {
			try {
				progressDialog.setProgress(100);
				if(progressDialog!=null)
				progressDialog.dismiss();
				
				JSONArray jsonPlayers = mPlayerData.getJSONObject("rosters").getJSONObject("franchise").getJSONArray("player");
				JSONArray jsonScores = mScoreData.getJSONObject("playerScores").getJSONArray("playerScore");
				
				if (mAllPlayersData == null){
				}
					else {
						JSONArray jsonAllPlayers = mAllPlayersData.getJSONObject("players").getJSONArray("player");
						
						// INSERT USING CONTENTVALUE CLASS
				        ContentValues cv3 = new ContentValues();
				        
						for (int i = 0; i < jsonAllPlayers.length(); i++) {
							String playerName = jsonAllPlayers.getJSONObject(i).getString("name");
							String mflId = jsonAllPlayers.getJSONObject(i).getString("id");
							String positionType = jsonAllPlayers.getJSONObject(i).getString("position");
							String teamName = jsonAllPlayers.getJSONObject(i).getString("team");
					        
							cv3.put(SQLiteHelper.PlayerTable.NAME, playerName);
							cv3.put(SQLiteHelper.PlayerTable.MFLID, mflId);
					        cv3.put(SQLiteHelper.PlayerTable.POSITION, positionType);
					        cv3.put(SQLiteHelper.PlayerTable.TEAM, teamName);
					        
					        // CALL INSERT METHOD
					        sqdb.insert(SQLiteHelper.PlayerTable.TABLE_NAME, SQLiteHelper.PlayerTable.NAME, cv3);
						}						
					}				
								
				// INSERT USING CONTENTVALUE CLASS
		        ContentValues cv = new ContentValues();
		        
				for (int i = 0; i < jsonPlayers.length(); i++) {
					
					String mflId = jsonPlayers.getJSONObject(i).getString("id");
					String franchiseId = mPlayerData.getJSONObject("rosters").getJSONObject("franchise").getString("id");
					
					cv.put(SQLiteHelper.RosterTable.PLAYERID, mflId);
			        cv.put(SQLiteHelper.RosterTable.FRANCHISE, franchiseId);			        
			        
			        // CALL INSERT METHOD
			        sqdb.insert(SQLiteHelper.RosterTable.TABLE_NAME, SQLiteHelper.RosterTable.PLAYERID, cv);		        
				}

				ContentValues cv2 = new ContentValues();
				
				for (int i = 0; i < jsonScores.length(); i++) {
					
					String mflId = jsonScores.getJSONObject(i).getString("id");
					String mflScore = jsonScores.getJSONObject(i).getString("score");
										
					cv2.put(SQLiteHelper.YTDScoresTable.PLAYERSCOREID, mflId);
			        cv2.put(SQLiteHelper.YTDScoresTable.SCORE, mflScore);
			        			        
			        // CALL INSERT METHOD
			        sqdb.insert(SQLiteHelper.YTDScoresTable.TABLE_NAME, SQLiteHelper.YTDScoresTable.PLAYERSCOREID, cv2);       

				}				
		        queryRoster();					
			}						
				
			catch (JSONException e) {
				logException(e);
			}			
		
		}  //belongs to the else
	}	
		
	
	private void queryRoster() {
        
        String querySql = "SELECT " + 
//        		SQLiteHelper.RosterTable.UID + ", " +
//        		SQLiteHelper.PlayerTable.NAME + ", " +
//        		SQLiteHelper.PlayerTable.POSITION + ", " +
//        		SQLiteHelper.PlayerTable.TEAM +", " +
//        		SQLiteHelper.YTDScoresTable.SCORE +
				" * " +
        		" FROM " + SQLiteHelper.RosterTable.TABLE_NAME + ", " + SQLiteHelper.PlayerTable.TABLE_NAME + ", " + SQLiteHelper.YTDScoresTable.TABLE_NAME + 
        		"     WHERE " + SQLiteHelper.RosterTable.PLAYERID + " = " + SQLiteHelper.PlayerTable.MFLID +
        		"     AND " + SQLiteHelper.PlayerTable.MFLID + " = " + SQLiteHelper.YTDScoresTable.PLAYERSCOREID + 
	    		"     ORDER BY CASE " + SQLiteHelper.PlayerTable.POSITION +
        		"     WHEN 'QB' THEN 1 WHEN 'RB' THEN 2 WHEN 'WR' THEN 3 WHEN 'TE' THEN 4 WHEN 'PK' THEN 5 WHEN 'DEF' THEN 6 ELSE 7 END";
     // THE DESIRED COLUMNS TO BE BOUND        
        String[] cols = new String[] { SQLiteHelper.PlayerTable.NAME, SQLiteHelper.PlayerTable.POSITION,
        		SQLiteHelper.PlayerTable.TEAM, SQLiteHelper.YTDScoresTable.SCORE};
        
     // MAKE QUERY TO CONTACT CONTENTPROVIDER
//        String query = sqb.buildQuery(null, null, null, null, null, null, null);                
//        System.out.println(querySql);
        
        Cursor c = sqdb.rawQuery(querySql, null);                 
        
        
        c.moveToFirst();         
        
        ArrayList<HashMap<String, String>> playerList = new ArrayList<HashMap<String, String>>();
       
        while (c.moveToNext()) {
            
            int colid = c.getColumnIndex(cols[0]);  // NAME
            int colid2 = c.getColumnIndex(cols[1]);  // POSITION
            int colid3 = c.getColumnIndex(cols[2]);  // TEAM
            int colid4 = c.getColumnIndex(cols[3]);  // YTD SCORE            
            
            String rosterText = c.getString(colid) + " (" + c.getString(colid3) + ")  - " +c.getString(colid4) +"pts";
            String positionText = c.getString(colid2);            
//            System.out.println(teamName + " || PLAYER ID " + playerName + " || Position " + positionName);
        
            HashMap<String, String> playerHM = new HashMap<String, String>();    					
			playerHM.put("Position", positionText);
			playerHM.put("Details", rosterText);
			
			playerList.add(playerHM);
            
        }

        String[] keys = { "Position", "Details" };
		int[] ids = { R.id.position_entry, R.id.name_entry};
		SimpleAdapter adapter = new SimpleAdapter(this, playerList, 
				R.layout.rosters_list_entry, keys, ids); // moves HashMap data to ListView

        		
		setListAdapter(adapter);
		
		Log.i(TAG, "All done and displayed!");		
		c.close();
	      
  	  	// CLOSE DATABASE CONNECTIONS
		sqdb.close();		
		sqh.close();
	}

	private void updateDisplayForError() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title));
		builder.setMessage(getString(R.string.error_message));
		builder.setPositiveButton(android.R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();			
	}	
	
	private boolean isNetworkAvailable() {
		ConnectivityManager manager = (ConnectivityManager) 
				getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		
		boolean isAvailable = false;
		if (networkInfo != null && networkInfo.isConnected()) {
			isAvailable = true;
		}
		
		return isAvailable;
	}
	
}

