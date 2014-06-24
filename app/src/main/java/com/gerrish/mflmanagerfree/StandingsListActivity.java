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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class StandingsListActivity extends ListActivity {

	
	public static final String TAG = StandingsListActivity.class.getSimpleName();
	protected JSONObject mStandingsData;
	protected JSONObject mLeagueNames;
	protected ProgressBar mProgressBar;
	private final String KEY_FRANCHISE = "id";
	private final String KEY_WINS = "h2hw";
	private final String KEY_NAME = "name";
	String currentLeagueId = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.standings_main_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		if (isNetworkAvailable()){
			mProgressBar.setVisibility(View.VISIBLE);
			
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  currentLeagueId = preferences.getString("Id","");
			  if(!currentLeagueId.equalsIgnoreCase(""))
			  {
			    currentLeagueId = preferences.getString("Id", "");
			  } 	
			
			GetStandingsDataTask getStandingsDataTask = new GetStandingsDataTask();
			getStandingsDataTask.execute();
			
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();			
		}			
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		try {			
			// We need an Editor object to make preference changes.
		      // All objects are from android.context.Context
			  String mTeamId = mStandingsData.getJSONObject("standings").getJSONArray("franchise").getJSONObject(position).getString("id");			  
			  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  SharedPreferences.Editor editor = preferences.edit();
			  
			  editor.putString("TeamID", mTeamId);			  
			  editor.commit();		 
					
			Intent intent = new Intent(this, SQLiteRosters.class); // goes to Rosters ListView			
			startActivity(intent);
			
		} catch (Exception e) {
			logException(e);
		}
		
	}

	private void logException(Exception e) {
		Log.e(TAG, "Exception caught: ", e);
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

	public void handleBlogResponse() {
		mProgressBar.setVisibility(View.INVISIBLE);
//		Log.w(TAG, mStandingsData.toString());
		
		if (mStandingsData == null || mLeagueNames == null) {			
			updateDisplayForError();

		}
		else {
			try {				
				JSONArray jsonStandings = mStandingsData.getJSONObject("standings").getJSONArray("franchise");						
				JSONArray jsonLeagues = mLeagueNames.getJSONObject("league").getJSONObject("franchises").getJSONArray("franchise");
				
				ArrayList<HashMap<String, String>> matchupList = 
						new ArrayList<HashMap<String, String>>();
						
				for (int i = 0; i < jsonStandings.length() ; i++) {
					
					JSONObject team = jsonStandings.getJSONObject(i);  //the ith franchise of standing list	 
					int teamLookup = Integer.parseInt(team.getString(KEY_FRANCHISE)) - 1;					
					int teamRecord = i + 1;
					String teamName = teamRecord + ". "+ jsonLeagues.getJSONObject(teamLookup).getString(KEY_NAME) +" ("+ team.getJSONObject(KEY_WINS).getString("$t") + "-" + team.getJSONObject("h2hl").getString("$t") + ")"; //team ID + win / loss					
					String winLoss =  "Points For: "+ team.getJSONObject("pf").getString("$t") + ", Points Against: " + team.getJSONObject("pa").getString("$t");  //team wins					
					HashMap<String, String> matchupHM = new HashMap<String, String>();					
					matchupHM.put(KEY_FRANCHISE, teamName);
					matchupHM.put(KEY_WINS, winLoss);
					
					matchupList.add(matchupHM);
				}				
				
				String[] keys = { KEY_FRANCHISE, KEY_WINS };
				int[] ids = { android.R.id.text1, android.R.id.text2 };
				SimpleAdapter adapter = new SimpleAdapter(this, matchupList, 
						android.R.layout.simple_list_item_2, 
						keys, ids); // moves HashMap data to ListView
				setListAdapter(adapter);				
			} catch (JSONException e) {
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
		
		TextView emptyTextView = (TextView) getListView().getEmptyView();
		emptyTextView.setText(getString(R.string.no_items));
	}

	private class GetStandingsDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;
			
			try {
				URL standingUrl = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=standings&L=" + currentLeagueId + "&W=&JSON=1");				
				HttpURLConnection connection = (HttpURLConnection) standingUrl.openConnection();
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
				
				return jsonResponse;			
		}
		
		@Override
		protected void onPostExecute(JSONObject result) {
			mStandingsData = result;
			//handleBlogResponse();
			
			GetLeagueDataTask getLeagueDataTask = new GetLeagueDataTask();			
			getLeagueDataTask.execute();			
		}
	}
	
	private class GetLeagueDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;
			
			try {
				URL Url = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=league&L=" + currentLeagueId + "&W=&JSON=1");				

				HttpURLConnection connection = (HttpURLConnection) Url.openConnection();
				
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
			mLeagueNames = result;
			handleBlogResponse();
		}
	}
}


