package com.gerrish.mflmanagerfree;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class LogInListActivity extends ListActivity {
	
	public static final String TAG = LogInListActivity.class.getSimpleName();
	protected JSONObject mLeagueSearchs;	
	protected ProgressBar mProgressBar;
	private final String KEY_FRANCHISE = "id";	
	private final String KEY_NAME = "name";
	public String value = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.leagues_main_list);
		
		mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		
		if (isNetworkAvailable()){
			mProgressBar.setVisibility(View.VISIBLE);
			
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Find Your League");
			alert.setMessage("Type in at least 3 letters of your league name.");

			// Set an EditText view to get user input 
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				 
					value = input.getText().toString();
					
					GetStandingsDataTask getStandingsDataTask = new GetStandingsDataTask();
					getStandingsDataTask.execute();
				  }
			});

			alert.show();			
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();			
		}			
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		JSONArray jsonLeagues;

		try {
			jsonLeagues = mLeagueSearchs.getJSONObject("leagues").getJSONArray("league");
			JSONObject chosenLeague = jsonLeagues.getJSONObject(position);  //the ith league of search results

			  // We need an Editor object to make preference changes.
		      // All objects are from android.context.Context
			  String mName = chosenLeague.getString(KEY_NAME);
			  String mId = chosenLeague.getString(KEY_FRANCHISE);
			  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  SharedPreferences.Editor editor = preferences.edit();
			  
			  editor.putString("Name", mName);
			  editor.putString("Id", mId);
			  editor.commit();
		 
			Intent intent = new Intent(this, HomeListActivity.class); // go to home screen			
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
		
		if (mLeagueSearchs == null) {			
			updateDisplayForError();

		}
		else {
			try {				
				boolean arrayCheck = false;
				try {
					arrayCheck = mLeagueSearchs.getJSONObject("leagues").getJSONArray("league").getJSONObject(1).has("id");	
				}
				catch (JSONException e) {
					logException(e);
				}
				Log.w(TAG, "array: " + arrayCheck);
				if (arrayCheck != false) {					
					JSONArray jsonStandings = mLeagueSearchs.getJSONObject("leagues").getJSONArray("league");					
					
					
					ArrayList<HashMap<String, String>> matchupList = 
							new ArrayList<HashMap<String, String>>();
							
					for (int i = 0; i < jsonStandings.length() ; i++) {
						
						JSONObject league = jsonStandings.getJSONObject(i);  //the ith league of search results										
						int searchResult = i + 1;
						String teamName = searchResult + ". " + league.getString(KEY_NAME); //team name					
						String secondLine = "League ID: "+ league.getString(KEY_FRANCHISE);					
						
						HashMap<String, String> matchupHM = new HashMap<String, String>();					
						matchupHM.put(KEY_NAME, teamName);
						matchupHM.put(KEY_FRANCHISE, secondLine);
						
						matchupList.add(matchupHM);
					}				
					
					String[] keys = { KEY_NAME, KEY_FRANCHISE };
					int[] ids = { android.R.id.text1, android.R.id.text2 };
					SimpleAdapter adapter = new SimpleAdapter(this, matchupList, 
							android.R.layout.simple_list_item_2, 
							keys, ids); // moves HashMap data to ListView
					setListAdapter(adapter);					
					
				}
				
				else {										
					JSONObject jsonStandings = mLeagueSearchs.getJSONObject("leagues").getJSONObject("league");					
												
					  // We need an Editor object to make preference changes.
				      // All objects are from android.context.Context
					  String mName = jsonStandings.getString(KEY_NAME); //team name
					  String mId = jsonStandings.getString(KEY_FRANCHISE); //team id		
					  SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
					  SharedPreferences.Editor editor = preferences.edit();
					  
					  editor.putString("Name", mName);
					  editor.putString("Id", mId);
					  editor.commit();
				 
					Intent intent = new Intent(this, HomeListActivity.class); // go to home screen			
					startActivity(intent);							
				}
				
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
				
				URL searchUrl = new 
						URL("http://football.myfantasyleague.com/" + Globals.YEAR + "/export?TYPE=leagueSearch&L=&W=&JSON=1&SEARCH=" + URLEncoder.encode(value, "UTF-8")); //Uri.encode(value)				
				
				HttpURLConnection connection = (HttpURLConnection) searchUrl.openConnection();
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
			mLeagueSearchs = result;
			handleBlogResponse();			
			
		}
	}	
}


