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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class LiveScoringActivity extends ListActivity {

	public static final String TAG = LiveScoringActivity.class.getSimpleName();
	protected JSONObject mLiveScoreData;
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	String currentLeagueId = "";
	String currentTeamId = "";
	ProgressBar mProgressBar;
	String mWeek = "";
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scoring_list);        
        
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar1);
		        
		Button refresh = (Button) findViewById(R.id.button2);
		 
        if (isNetworkAvailable()){
			
			mProgressBar.setVisibility(View.VISIBLE);
			
	        // Check Shared Preferences for League ID
	        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			  currentLeagueId = preferences.getString("Id","");
			  mWeek = preferences.getString("Week","");  // WEEK CHECK
			  if(!currentLeagueId.equalsIgnoreCase(""))
			  {
			    currentLeagueId = preferences.getString("Id", "");
			    currentTeamId = preferences.getString("TeamID", "");
			  }			 
				
	        // INIT OUR SQLITE HELPER
	        sqh = new SQLiteHelper(this);

	        // RETRIEVE A READABLE AND WRITEABLE DATABASE
	        sqdb = sqh.getWritableDatabase();
	        	        
	        // GET DATA FROM API IN JSON FORMAT
			GetLiveScoreDataTask getLiveScoreDataTask = new GetLiveScoreDataTask();
			getLiveScoreDataTask.execute();
		}
		else {
			Toast.makeText(this, "Network is unavailable", Toast.LENGTH_LONG).show();			
		}
	    //Listening to button event
	    refresh.setOnClickListener(new View.OnClickListener() {

        public void onClick(View arg0) {
        	finish();
        	startActivity(getIntent());
        	}
	    });
	}

	private class GetLiveScoreDataTask extends AsyncTask<Object, Void, JSONObject> {

		@Override
		protected JSONObject doInBackground(Object... arg0) {			
			int responseCode = -1;			
			JSONObject jsonResponse = null;			
			
			try {

				URL scoreUrl = new 
						URL("http://football.myfantasyleague.com/2013/export?TYPE=liveScoring&L=" + currentLeagueId + "&W=" + mWeek +"&JSON=1");				
				 
				HttpURLConnection connection = (HttpURLConnection) scoreUrl.openConnection();
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
			mLiveScoreData = result;
			
			// PARSE JSON DATA INO STRINGS
			handleMflResponse();			
		}
	}
	
//	@SuppressWarnings("deprecation")
	public void handleMflResponse() {
		
		mProgressBar.setVisibility(View.INVISIBLE);
		
		if (mLiveScoreData == null ) {			
			updateDisplayForError();
		}
		else {
			try {
				// Check Shared Preferences for which Matchup was clicked
		        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				  int matchupClicked = preferences.getInt("MatchupClicked", 0);

				JSONArray jsonArrayScores = mLiveScoreData.getJSONObject("liveScoring").getJSONArray("matchup").getJSONObject(matchupClicked).getJSONArray("franchise")
						.getJSONObject(0).getJSONObject("players").getJSONArray("player");				
				JSONArray jsonArrayScores2 = mLiveScoreData.getJSONObject("liveScoring").getJSONArray("matchup").getJSONObject(matchupClicked).getJSONArray("franchise")
						.getJSONObject(1).getJSONObject("players").getJSONArray("player");
								
				// INSERT USING CONTENTVALUE CLASS
		        ContentValues cv = new ContentValues();
		        ContentValues cv2 = new ContentValues();
		        
				for (int i = 0; i < jsonArrayScores.length(); i++) {
					
					String mflId = jsonArrayScores.getJSONObject(i).getString("id");
					String mScore = jsonArrayScores.getJSONObject(i).getString("score");				
					String mflId2 = jsonArrayScores2.getJSONObject(i).getString("id");
					String mScore2 = jsonArrayScores2.getJSONObject(i).getString("score");
					
					cv.put(SQLiteHelper.WeeklyScoreTable.PLAYERSCOREID, mflId);
			        cv.put(SQLiteHelper.WeeklyScoreTable.SCORE, mScore);
			        cv2.put(SQLiteHelper.WeeklyScoreTable2.PLAYERSCOREID2, mflId2);
			        cv2.put(SQLiteHelper.WeeklyScoreTable2.SCORE2, mScore2);
			        
			        // CALL INSERT METHOD
			        sqdb.insert(SQLiteHelper.WeeklyScoreTable.TABLE_NAME, SQLiteHelper.WeeklyScoreTable.PLAYERSCOREID, cv);
			        sqdb.insert(SQLiteHelper.WeeklyScoreTable2.TABLE_NAME, SQLiteHelper.WeeklyScoreTable2.PLAYERSCOREID2, cv2);
				}
		        
				String querySql = "SELECT " + 
			        		" * " +
			        		" FROM " + SQLiteHelper.WeeklyScoreTable.TABLE_NAME + ", " + SQLiteHelper.PlayerTable.TABLE_NAME +
			        		"     WHERE " + SQLiteHelper.WeeklyScoreTable.PLAYERSCOREID + " = " + SQLiteHelper.PlayerTable.MFLID +
			        		"     ORDER BY CASE " + SQLiteHelper.PlayerTable.POSITION +
			        		"     WHEN 'QB' THEN 1 WHEN 'RB' THEN 2 WHEN 'WR' THEN 3 WHEN 'TE' THEN 4 WHEN 'PK' THEN 5 WHEN 'DEF' THEN 6 ELSE 7 END";
				    // THE DESIRED COLUMNS TO BE BOUND        
			        String[] cols = new String[] { SQLiteHelper.PlayerTable.NAME, SQLiteHelper.WeeklyScoreTable.SCORE	};
			        			      
			        Cursor c = sqdb.rawQuery(querySql, null);
			        startManagingCursor(c);
			        
			        
//			        ArrayList<HashMap<String, String>> playerList = new ArrayList<HashMap<String, String>>();
//			       
//			        while (c.moveToNext()) {
//			            
//			            int colid = c.getColumnIndex(cols[0]);  // NAME
//			            int colid2 = c.getColumnIndex(cols[1]);  // POSITION			                       
//			            
//			            String rosterText = c.getString(colid);
//			            String positionText = c.getString(colid2);            
////			            System.out.println(teamName + " || PLAYER ID " + playerName + " || Position " + positionName);
//			        
//			            HashMap<String, String> playerHM = new HashMap<String, String>();    					
//						playerHM.put("Position", positionText);
//						playerHM.put("Details", rosterText);
//						
//						playerList.add(playerHM);
//			            
//			        }
//
//			        String[] keys = { "Position", "Details" };
//					int[] ids = { android.R.id.text1, android.R.id.text2};
//					SimpleAdapter adapter = new SimpleAdapter(this, playerList, 
//							android.R.layout.simple_list_item_2, keys, ids); // moves HashMap data to ListView
//					setListAdapter(adapter);
					
					// LETS TRY A SIMPLE CURSOR ADAPTER
					int[] ids = { android.R.id.text1};
					
//					SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
//							android.R.layout.simple_list_item_2, c, cols, ids);
//					
					CustomMatchupsAdapter cAdapter = new CustomMatchupsAdapter(this, 
							android.R.layout.simple_list_item_1, c, cols, ids);

					String querySql2 = "SELECT " + 
			        		" * " +
			        		" FROM " + SQLiteHelper.WeeklyScoreTable2.TABLE_NAME + ", " + SQLiteHelper.PlayerTable.TABLE_NAME +
			        		"     WHERE " + SQLiteHelper.WeeklyScoreTable2.PLAYERSCOREID2 + " = " + SQLiteHelper.PlayerTable.MFLID +
			        		"     ORDER BY CASE " + SQLiteHelper.PlayerTable.POSITION +
			        		"     WHEN 'QB' THEN 1 WHEN 'RB' THEN 2 WHEN 'WR' THEN 3 WHEN 'TE' THEN 4 WHEN 'PK' THEN 5 WHEN 'DEF' THEN 6 ELSE 7 END";
			     
				    // THE DESIRED COLUMNS TO BE BOUND        
			        String[] cols2 = new String[] { SQLiteHelper.PlayerTable.NAME, SQLiteHelper.WeeklyScoreTable2.SCORE2	};
			        			      
			        Cursor c2 = sqdb.rawQuery(querySql2, null);
			        startManagingCursor(c2);

			        CustomMatchupsAdapter2 cAdapter2 = new CustomMatchupsAdapter2(this, 
							android.R.layout.simple_list_item_1, c2, cols2, ids);
			        
					String totalScore = mLiveScoreData.getJSONObject("liveScoring").getJSONArray("matchup").getJSONObject(matchupClicked).getJSONArray("franchise")
							.getJSONObject(0).getString("score");
					String totalScore2 = mLiveScoreData.getJSONObject("liveScoring").getJSONArray("matchup").getJSONObject(matchupClicked).getJSONArray("franchise")
							.getJSONObject(1).getString("score");
					
					// Check Shared Preferences for which Team Name was clicked
			        SharedPreferences teamPreferences = PreferenceManager.getDefaultSharedPreferences(this);
					String TeamNameClicked = teamPreferences.getString("MatchupTeam1", "");
					String TeamNameClicked2 = teamPreferences.getString("MatchupTeam2", "");		
					
					TextView teamHeader = (TextView) findViewById(R.id.TeamHeader);										
					teamHeader.setText(TeamNameClicked + " [" + totalScore + "] vs. " + TeamNameClicked2 + " ["+ totalScore2 + "]");
					
					ListView listView = getListView();
					listView.setAdapter(cAdapter);
					
					ListView listView2 = (ListView) findViewById(R.id.list2);
					listView2.setAdapter(cAdapter2);
										
					Log.i(TAG, "All done and displayed!");		
//					c.close();
				      
			  	  	// CLOSE DATABASE CONNECTIONS
					sqdb.close();		
					sqh.close();
				
			}						
				
			catch (JSONException e) {
				logException(e);
			}			
		
		}  //belongs to the else
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
		
		   @SuppressWarnings("deprecation")
		@Override
		   public void startManagingCursor(Cursor c) {
		    // TODO Auto-generated method stub
		    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {

		    super.startManagingCursor(c); 

		    }
		    }
		   
		private void logException(Exception e) {
			Log.e(TAG, "Exception caught: ", e);
		}
		
		private void updateDisplayForError() {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(getString(R.string.title));
			builder.setMessage(getString(R.string.error_message));
			builder.setPositiveButton(android.R.string.ok, null);
			AlertDialog dialog = builder.create();
			dialog.show();			
		}	
		
		public class CustomMatchupsAdapter extends SimpleCursorAdapter {

		    private int layout;

		    @SuppressWarnings("deprecation")
			public CustomMatchupsAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
		        super(context, layout, c, from, to);
		        this.layout = layout;
		    }

		    @Override
		    public View newView(Context context, Cursor cursor, ViewGroup parent) {
		        final LayoutInflater inflater = LayoutInflater.from(context);
		        View v = inflater.inflate(layout, parent, false);
		        return v;
		    }

		    @Override
		    public void bindView(View v, Context context, Cursor c) {
		        int nameCol = c.getColumnIndex(SQLiteHelper.PlayerTable.NAME);
		        int numCol = c.getColumnIndex(SQLiteHelper.WeeklyScoreTable.SCORE);		        
		        int positionCol = c.getColumnIndex(SQLiteHelper.PlayerTable.POSITION);

		        String name = c.getString(positionCol) + " " + c.getString(nameCol) + " [" + c.getString(numCol) + "]";

		        // FIND THE VIEW AND SET THE NAME
		        TextView name_text = (TextView) v.findViewById(android.R.id.text1);
		        name_text.setText(name);

		    }
		}
	
		public class CustomMatchupsAdapter2 extends SimpleCursorAdapter {

		    private int layout;

		    @SuppressWarnings("deprecation")
			public CustomMatchupsAdapter2(Context context, int layout, Cursor c, String[] from, int[] to) {
		        super(context, layout, c, from, to);
		        this.layout = layout;
		    }

		    @Override
		    public View newView(Context context, Cursor cursor, ViewGroup parent) {
		        final LayoutInflater inflater = LayoutInflater.from(context);
		        View v = inflater.inflate(layout, parent, false);
		        return v;
		    }

		    @Override
		    public void bindView(View v, Context context, Cursor c) {
		        int nameCol = c.getColumnIndex(SQLiteHelper.PlayerTable.NAME);
		        int numCol = c.getColumnIndex(SQLiteHelper.WeeklyScoreTable2.SCORE2);		        
		        int positionCol = c.getColumnIndex(SQLiteHelper.PlayerTable.POSITION);

		        String name = c.getString(positionCol) + " " + c.getString(nameCol) + "  [" + c.getString(numCol) + "]";

		        // FIND THE VIEW AND SET THE NAME
		        TextView name_text = (TextView) v.findViewById(android.R.id.text1);
		        name_text.setText(name);

		    }
		}

}
