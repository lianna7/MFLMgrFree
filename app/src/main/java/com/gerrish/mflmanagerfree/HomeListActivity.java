package com.gerrish.mflmanagerfree;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class HomeListActivity extends ListActivity {	
	
	public static final String TAG = HomeListActivity.class.getSimpleName();
	String currentLeagueName = "";
	String currentLeagueId = "";
	
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	protected JSONObject mAllPlayersData;
	
//	@SuppressWarnings("null")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_list);		

		ListView listView = (ListView) findViewById(android.R.id.list);
		        
        // INIT OUR SQLITE HELPER
        sqh = new SQLiteHelper(this);

        // RETRIEVE A READABLE AND WRITEABLE DATABASE
        sqdb = sqh.getWritableDatabase();  

        String querySql = "SELECT * FROM " + SQLiteHelper.PlayerTable.TABLE_NAME;
		Cursor cursor = sqdb.rawQuery(querySql, null);		
		
		if (cursor == null || cursor.getCount() == 0) {
			Log.i(TAG, "Creating DB");			
//			// GET PLAYERS DATABASE
			GetAllPlayersDataTask getAllPlayersDataTask = new GetAllPlayersDataTask();
			getAllPlayersDataTask.execute();
//			Toast.makeText(this, "Commencing one-time player database download in the background.", Toast.LENGTH_SHORT).show();
		}
				
		// We need an Editor object to make preference changes.
	      // All objects are from android.context.Context		
		 SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		  String name = preferences.getString("Name","");
		  if(!name.equalsIgnoreCase(""))
		  {
		    currentLeagueName = name;
		    currentLeagueId = preferences.getString("Id", "");
		  } 
			
	    		
	    String[] values = new String[] { "Add League", "View Matchups", "View League Standings", 
	    		"Submit Your Lineup [on MFL.com]", "Quit App", "Current League: " + currentLeagueName };

	        final ArrayList<String> listArray = new ArrayList<String>();
	        for (int i = 0; i < values.length; ++i) {
	          listArray.add(values[i]);
	        }
	        
			final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
	            android.R.layout.simple_list_item_1, listArray);
	        listView.setAdapter(adapter);
	        
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		try {
				if (position == 0) {
					Intent intent = new Intent(this, LogInListActivity.class); // go to Search League screen			
					startActivity(intent);		
				}
				else if (position == 1) {
					Intent intent = new Intent(this, MainListActivity.class); // go to matchup screen			
					startActivity(intent);							
				}
				else if (position == 2) {
					Intent intent = new Intent(this, StandingsListActivity.class); // go to Standings screen			
					startActivity(intent);							
				}				
				else if (position == 4) {
					this.moveTaskToBack(true);	// Minimize App
					SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
					SharedPreferences.Editor editor = preferences.edit();
					  
					editor.putString("Week", "");   //clear Week selection				  
					editor.commit();
					  					
				}
				else {
				try {
					String lineupUrl = "http://football.myfantasyleague.com/" + Globals.YEAR + "/options?L=" + currentLeagueId + "&O=02";
					
					Intent intent = new Intent(this, WebViewActivity.class); // open a browser window
					intent.setData(Uri.parse(lineupUrl));
					startActivity(intent);	
					} catch (Exception e) {
						logException(e);
					}
				}
				
			} catch (Exception e) {
					logException(e);
				}		
	}
	
	private void logException(Exception e) {
		Log.e(TAG, "Exception caught: ", e);
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
				
				JSONArray jsonAllPlayers = jsonAllPlayersResponse.getJSONObject("players").getJSONArray("player");
				
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
				return jsonAllPlayersResponse;				
		}	

		@Override
		protected void onPostExecute(JSONObject result) {
			mAllPlayersData = result;
//			handleMflResponse();
		}
	}	

//	public void handleMflResponse() {
//		
//			try {
//					JSONArray jsonAllPlayers = mAllPlayersData.getJSONObject("players").getJSONArray("player");
//					
//					// INSERT USING CONTENTVALUE CLASS
//			        ContentValues cv3 = new ContentValues();
//			        
//					for (int i = 0; i < jsonAllPlayers.length(); i++) {
//						String playerName = jsonAllPlayers.getJSONObject(i).getString("name");
//						String mflId = jsonAllPlayers.getJSONObject(i).getString("id");
//						String positionType = jsonAllPlayers.getJSONObject(i).getString("position");
//						String teamName = jsonAllPlayers.getJSONObject(i).getString("team");
//				        
//						cv3.put(SQLiteHelper.PlayerTable.NAME, playerName);
//						cv3.put(SQLiteHelper.PlayerTable.MFLID, mflId);
//				        cv3.put(SQLiteHelper.PlayerTable.POSITION, positionType);
//				        cv3.put(SQLiteHelper.PlayerTable.TEAM, teamName);
//				        
//				        // CALL INSERT METHOD
//				        sqdb.insert(SQLiteHelper.PlayerTable.TABLE_NAME, SQLiteHelper.PlayerTable.NAME, cv3);
//					}						
//					Toast.makeText(this, "One-time player data successfully added.", Toast.LENGTH_SHORT).show();				
//				}						
//				
//			catch (JSONException e) {
//				logException(e);
//			}			
//		
//	}	

	
}



