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
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainListActivity extends ListActivity {	
	
	public static final String TAG = MainListActivity.class.getSimpleName();
	protected JSONObject mMatchupData;
	protected JSONObject mLeagueNames;
	protected ProgressBar mProgressBar;
	private final String KEY_FRANCHISE = "id";
	private final String KEY_SCORE = "score";
	private final String KEY_NAME = "name";
	String currentLeagueId = "";
	String mWeek = "";
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner1);
		// Create an ArrayAdapter using the string array and a default spinner layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.weeks, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		// Set to First Option
		spinner.setSelection(0);
		
		Button refresh = (Button) findViewById(R.id.button2);		
		
		// INIT OUR SQLITE HELPER
        sqh = new SQLiteHelper(this);

        // RETRIEVE A READABLE AND WRITEABLE DATABASE
        sqdb = sqh.getWritableDatabase();        
		
		if (isNetworkAvailable()){
			mProgressBar.setVisibility(View.VISIBLE);
			 
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  currentLeagueId = preferences.getString("Id","");
			  if(!currentLeagueId.equalsIgnoreCase(""))
			  {
			    currentLeagueId = preferences.getString("Id", "");
			  } 	
//			GetMatchupDataTask getMatchupDataTask = new GetMatchupDataTask();
//			getMatchupDataTask.execute();			

				  
		 // Listening for alternate Week Selection
		  spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v,
					int id, long l) {
				if (id > 0)
				{
				Toast.makeText(parent.getContext(), 
						"Updating Matchup Week To: " + parent.getItemAtPosition(id).toString(), Toast.LENGTH_SHORT).show();
				}
				mWeek = parent.getItemAtPosition(id).toString();

				GetMatchupDataTask getMatchupDataTask = new GetMatchupDataTask();
				getMatchupDataTask.execute();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub				
			}
			  
		  });		  
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();			
		}		  
	    //Listening to button event
	    refresh.setOnClickListener(new View.OnClickListener() {

        public void onClick(View arg0) {
			GetMatchupDataTask getMatchupDataTask = new GetMatchupDataTask();
			getMatchupDataTask.execute();
        	}
	    });
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
//		JSONArray jsonMatchupIds;
//		try {
//			jsonMatchupIds = mMatchupData.getJSONObject("liveScoring").getJSONArray("matchup").getJSONObject(position).getJSONArray("franchise").getJSONObject(0)
//					.getJSONObject("players").getJSONArray("player");  // Array of the first team of the matchup clicked
//			
//			//Week check
//			String currentWeek ="";
//			if (mWeek.length() == 0) {
//			currentWeek = mMatchupData.getJSONObject("liveScoring").getString("week");			
//			} 
//			else currentWeek = mWeek;			
//						
//			JSONObject firstId = jsonMatchupIds.getJSONObject(position).getJSONArray("franchise").getJSONObject(0);  //the ith franchise of matchup list
//			JSONObject secondId = jsonMatchupIds.getJSONObject(position).getJSONArray("franchise").getJSONObject(1);  //the ith franchise of matchup lis
//			String clickedMatchups = firstId.getString(KEY_FRANCHISE) + "_" + secondId.getString(KEY_FRANCHISE);
//			String matchupUrl = "http://football.myfantasyleague.com/"+ Globals.YEAR + "/live_scoring?L=" + currentLeagueId + "&W=" + currentWeek +
//					"&FRANCHISES=" + clickedMatchups;
//			
//			Intent intent = new Intent(this, WebViewActivity.class); // open a browser window
//			intent.setData(Uri.parse(matchupUrl));
		
		  // We need an Editor object to make preference changes.
	      // All objects are from android.context.Context
		  int mMatchupClicked = position;			  
		  try {
		  JSONArray jsonMatchups = mMatchupData.getJSONObject("liveScoring").getJSONArray("matchup");						
		  JSONArray jsonLeagues = mLeagueNames.getJSONObject("league").getJSONObject("franchises").getJSONArray("franchise");
		  JSONObject matchup = jsonMatchups.getJSONObject(position).getJSONArray("franchise").getJSONObject(0);  //the ith franchise of matchup list
		  JSONObject matchup2 = jsonMatchups.getJSONObject(position).getJSONArray("franchise").getJSONObject(1);  //the ith franchise of matchup list
				 
			int teamLookup = Integer.parseInt(matchup.getString(KEY_FRANCHISE)) - 1;					
			int teamLookup2 = Integer.parseInt(matchup2.getString(KEY_FRANCHISE)) - 1;
			String teamName = jsonLeagues.getJSONObject(teamLookup).getString(KEY_NAME);
			String teamName2 = jsonLeagues.getJSONObject(teamLookup2).getString(KEY_NAME);

		  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		  SharedPreferences.Editor editor = preferences.edit();
		  
		  editor.putInt("MatchupClicked", mMatchupClicked);			  
		  editor.putString("MatchupTeam1", teamName);
		  editor.putString("MatchupTeam2", teamName2);
		  editor.putString("Week", mWeek);
		  editor.commit();
		  
			Intent intent = new Intent(this, LiveScoringActivity.class); // open a browser window
			startActivity(intent);		
			} catch (JSONException e) {
				logException(e);
			}			
//		} catch (Exception e) {
//			logException(e);
//		}		
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

		
		if (mMatchupData == null || mLeagueNames == null) {			
			updateDisplayForError();
		}
		else {
			try {				
				JSONArray jsonMatchups = mMatchupData.getJSONObject("liveScoring").getJSONArray("matchup");						
				JSONArray jsonLeagues = mLeagueNames.getJSONObject("league").getJSONObject("franchises").getJSONArray("franchise");
				
		
				ArrayList<HashMap<String, String>> matchupList = 
						new ArrayList<HashMap<String, String>>();
						
				for (int i = 0; i < jsonMatchups.length() ; i++) {
					
					JSONObject matchup = jsonMatchups.getJSONObject(i).getJSONArray("franchise").getJSONObject(0);  //the ith franchise of matchup list
					JSONObject matchup2 = jsonMatchups.getJSONObject(i).getJSONArray("franchise").getJSONObject(1);  //the ith franchise of matchup list
						 
					int teamLookup = Integer.parseInt(matchup.getString(KEY_FRANCHISE)) - 1;					
					int teamLookup2 = Integer.parseInt(matchup2.getString(KEY_FRANCHISE)) - 1;
					String teamName = jsonLeagues.getJSONObject(teamLookup).getString(KEY_NAME) + " vs. " 
							+ jsonLeagues.getJSONObject(teamLookup2).getString(KEY_NAME);  //team ID					
					String score = "  > Scores:     " + matchup.getString(KEY_SCORE) + "    " +  matchup2.getString(KEY_SCORE);  //team score
//					score = Html.fromHtml(score).toString();
					HashMap<String, String> matchupHM = new HashMap<String, String>();					
					matchupHM.put(KEY_FRANCHISE, teamName);
					matchupHM.put(KEY_SCORE, score);
					
					matchupList.add(matchupHM);
				}				
				
				String[] keys = { KEY_FRANCHISE, KEY_SCORE };
				int[] ids = { android.R.id.text1, android.R.id.text2 };
				SimpleAdapter adapter = new SimpleAdapter(this, matchupList, 
						android.R.layout.simple_list_item_2, keys, ids); // moves HashMap data to ListView
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

	public class GetMatchupDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;
			
			try {
				
				URL matchupUrl = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=liveScoring&L=" + currentLeagueId +"&W=" + mWeek + "&JSON=1");				
				
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
			mMatchupData = result;
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
						URL("http://football.myfantasyleague.com/" + Globals.YEAR + "/export?TYPE=league&L=" + currentLeagueId + "&W=&JSON=1");				
				
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


