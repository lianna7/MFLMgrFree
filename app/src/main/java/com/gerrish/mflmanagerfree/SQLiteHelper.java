package com.gerrish.mflmanagerfree;

import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "mfl_data.db";
	public static final String TAG =SQLiteGetPlayers.class.getSimpleName();
	protected JSONObject mPlayerData;
	SQLiteHelper sqh;
	SQLiteDatabase sqdb;
	
    // TOGGLE THIS NUMBER FOR UPDATING TABLES AND DATABASE
    private static final int DATABASE_VERSION = 12;        
    
    SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        
    	// KILL PREVIOUS TABLE IF UPGRADED
        db.execSQL("DROP TABLE IF EXISTS " + PlayerTable.TABLE_NAME);
    	db.execSQL("DROP TABLE IF EXISTS " + RosterTable.TABLE_NAME);   
        db.execSQL("DROP TABLE IF EXISTS " + YTDScoresTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable2.TABLE_NAME);
        
    	db.execSQL("CREATE TABLE " + PlayerTable.TABLE_NAME + " (" + PlayerTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + PlayerTable.NAME
                + " VARCHAR(255)," + PlayerTable.MFLID + " VARCHAR(255),"+ PlayerTable.POSITION + " VARCHAR(255)," + PlayerTable.TEAM + " VARCHAR(255));" );

        db.execSQL("CREATE TABLE " + RosterTable.TABLE_NAME + " (" + RosterTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        RosterTable.PLAYERID + " VARCHAR(255)," + RosterTable.FRANCHISE + " VARCHAR(255));" );
        
        db.execSQL("CREATE TABLE " + YTDScoresTable.TABLE_NAME + " (" + YTDScoresTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		YTDScoresTable.PLAYERSCOREID + " VARCHAR(255)," + YTDScoresTable.SCORE+ " VARCHAR(255));" );
        
        db.execSQL("CREATE TABLE " + WeeklyScoreTable.TABLE_NAME + " (" + WeeklyScoreTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		WeeklyScoreTable.PLAYERSCOREID + " VARCHAR(255)," + WeeklyScoreTable.SCORE+ " VARCHAR(255));" );
        
        db.execSQL("CREATE TABLE " + WeeklyScoreTable2.TABLE_NAME + " (" + WeeklyScoreTable2.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		WeeklyScoreTable2.PLAYERSCOREID2 + " VARCHAR(255)," + WeeklyScoreTable2.SCORE2+ " VARCHAR(255));" );

    }

        @Override
    public void onOpen(SQLiteDatabase db) {

        // KILL PREVIOUS TABLE IF UPGRADED
//    	db.execSQL("DROP TABLE IF EXISTS " + PlayerTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RosterTable.TABLE_NAME);   
        db.execSQL("DROP TABLE IF EXISTS " + YTDScoresTable.TABLE_NAME);        
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable2.TABLE_NAME);
        
//    	db.execSQL("CREATE TABLE " + PlayerTable.TABLE_NAME + " (" + PlayerTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + PlayerTable.NAME
//                + " VARCHAR(255)," + PlayerTable.MFLID + " VARCHAR(255),"+ PlayerTable.POSITION + " VARCHAR(255)," + PlayerTable.TEAM + " VARCHAR(255));" );
    	
        db.execSQL("CREATE TABLE " + RosterTable.TABLE_NAME + " (" + RosterTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        RosterTable.PLAYERID + " VARCHAR(255)," + RosterTable.FRANCHISE + " VARCHAR(255));" );
        
        db.execSQL("CREATE TABLE " + YTDScoresTable.TABLE_NAME + " (" + YTDScoresTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		YTDScoresTable.PLAYERSCOREID + " VARCHAR(255)," + YTDScoresTable.SCORE+ " VARCHAR(255));" );      
        
        db.execSQL("CREATE TABLE " + WeeklyScoreTable.TABLE_NAME + " (" + WeeklyScoreTable.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		WeeklyScoreTable.PLAYERSCOREID + " VARCHAR(255)," + WeeklyScoreTable.SCORE+ " VARCHAR(255));" );
        
        db.execSQL("CREATE TABLE " + WeeklyScoreTable2.TABLE_NAME + " (" + WeeklyScoreTable2.UID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
        		WeeklyScoreTable2.PLAYERSCOREID2 + " VARCHAR(255)," + WeeklyScoreTable2.SCORE2+ " VARCHAR(255));" );
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("LOG_TAG", "Upgrading database from version " + oldVersion + " to " + newVersion
                + ", which will destroy all old data");

        // KILL PREVIOUS TABLE IF UPGRADED        
        db.execSQL("DROP TABLE IF EXISTS " + PlayerTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RosterTable.TABLE_NAME);        
        db.execSQL("DROP TABLE IF EXISTS " + YTDScoresTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WeeklyScoreTable2.TABLE_NAME);
        
        // CREATE NEW INSTANCE OF TABLE
        onCreate(db);
    }
    
    // MFL PLAYER MASTER DATABASE
    public class PlayerTable {
	
        public static final String UID = "_id";
        public static final String NAME = "name";    
        public static final String MFLID = "mflId";  // My Fantasy League ID
        public static final String POSITION = "position";    
        public static final String TEAM = "team";
        
        // TABLE NAME
    	public static final String TABLE_NAME = "players";
    }
    
    // LEAGUE ROSTER INFO
    public class RosterTable {
	    
    	public static final String UID = "_id";  	// UNIQUE ID         
    	public static final String PLAYERID = "playerId";  // ROSTER PLAYER ID FROM MFL
    	public static final String FRANCHISE = "franchise";  // FRANCHISE ID
    	    	
    	public static final String TABLE_NAME = "rosters";  // TABLE NAME
    }
    
    // LEAGUE YTD SCORES INFO
    public class YTDScoresTable {	
    	
    	public static final String UID = "_id";         // UNIQUE ID
    	public static final String PLAYERSCOREID = "playerScoreId";  // ROSTER PLAYER ID FROM MFL   	
    	public static final String SCORE = "score";  // PLAYER YTD SCORE
        	
    	public static final String TABLE_NAME = "scores"; // TABLE NAME
    }
    
    // LEAGUE WEEKLY SCORES INFO
    public class WeeklyScoreTable {	
    	
    	public static final String UID = "_id";         // UNIQUE ID
    	public static final String PLAYERSCOREID = "playerScoreId";  // ROSTER PLAYER ID FROM MFL   	
    	public static final String SCORE = "weeklyScore";  // PLAYER WEEKLY SCORE
        	
    	public static final String TABLE_NAME = "weekly_scores"; // TABLE NAME
    }
    
    // LEAGUE WEEKLY SCORES INFO
    public class WeeklyScoreTable2 {	
    	
    	public static final String UID = "_id";         // UNIQUE ID
    	public static final String PLAYERSCOREID2 = "playerScoreId";  // ROSTER PLAYER ID FROM MFL   	
    	public static final String SCORE2 = "weeklyScore";  // PLAYER WEEKLY SCORE
        	
    	public static final String TABLE_NAME = "weekly_scores2"; // TABLE NAME
    }
}

