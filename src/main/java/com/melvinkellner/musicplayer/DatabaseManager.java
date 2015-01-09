package com.melvinkellner.musicplayer;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.farng.mp3.MP3File;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;

/**
 * Created by mk on 07/10/14.
 */
public class DatabaseManager
{
  public final static DatabaseManager instance = new DatabaseManager();
  public static ArrayList<Song> currentRequests = new ArrayList<Song>();
  public static Song[] visiblePlayList = new Song[Controller.MAX_VISIBLE_PLAYLIST];

  public static String requestJSON = "";
  public static String playlistJSON = "";

  public Connection conn = null;


  public DatabaseManager()
  {

  }

  public void init()
  {
    try
    {
      System.out.println("init db");
      Class.forName("org.h2.Driver");
      conn = DriverManager.getConnection("jdbc:h2:" + System.getProperty("user.dir") + "/" + Controller.DB_DIR + "/music", "sa", "");
      if (conn == null)
      {
        System.out.println("conn is null");
      }
      else
      {
        System.out.println("conn is not null");
      }
      createTables();

      setCurrentRequests();
      setVisiblePlayList();
    }
    catch (ClassNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  public String setActive(int id, boolean active)
  {
    Song song = getSongFromId(id);
    song.setActive(active);
    updateSong(song);
    return "deleted";
  }

  public void updateSong(Song song)
  {
    String query = "UPDATE SONGS set path=" + "?" + ", artist=" + "?" +
        ", album=" + "?" + ", title=" + "?" + ", active=" + "?" + ", hash=" + "?" + " WHERE p_id='" + song.getId() + "'";
    try
    {
      PreparedStatement stmt = conn.prepareStatement(query);
      stmt.setString(1, song.getPath());
      stmt.setString(2, song.getArtist());
      stmt.setString(3, song.getAlbum());
      stmt.setString(4, song.getTitle());
      stmt.setBoolean(5, song.getActive());
      stmt.setString(6, song.getHash());
      stmt.executeUpdate();
    } catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  public ArrayList<Integer> getAllSongIds() throws SQLException
  {
    final ArrayList<Integer> idArray = new ArrayList<Integer>();
    ResultSet result = conn.createStatement().executeQuery("select p_id from SONGS WHERE active");
    while(result.next())
    {
      idArray.add(result.getInt("p_id"));
    }
    return idArray;
  }

  public void shufflePlayList()
  {
    try
    {
      final ArrayList<Integer> idArray = getAllSongIds();
      final int[] idPrimitiveArray = new int[idArray.size()];
      for (int i = 0;i<idArray.size();i++)
      {
        idPrimitiveArray[i] = idArray.get(i);
        idArray.set(i, null);
      }
      Controller.shuffleArray(idPrimitiveArray);
      conn.createStatement().executeUpdate("TRUNCATE TABLE PLAYLIST");
      for (int i = 0;i<idPrimitiveArray.length;i++)
      {
        String sql = "INSERT INTO PLAYLIST VALUES ("+null+", "+idPrimitiveArray[i]+")";
        try
        {
          conn.createStatement().execute(sql);
        }
        catch (SQLException e)
        {
          e.printStackTrace();
        }
      }
      conn.createStatement().executeQuery("select * from PLAYLIST");
      setVisiblePlayList();
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  private void createTables()
  {
    String songsDB = "CREATE TABLE SONGS " +
        "(p_id int(11) not NULL AUTO_INCREMENT PRIMARY KEY, " +
        " path VARCHAR(255), " +
        " artist TEXT, " +
        " album TEXT, " +
        " title TEXT, " +
        " active BOOL, " +
        " hash VARCHAR(100))";

    String playlistDB = "CREATE TABLE PLAYLIST " +
        "(p_id int(11) not NULL AUTO_INCREMENT PRIMARY KEY, " +
        " song_id int(11))";

    String requestDB = "CREATE TABLE REQUESTS " +
        "(p_id int(11) not NULL AUTO_INCREMENT PRIMARY KEY, " +
        " song_id int(11))";

    try
    {conn.createStatement().execute(songsDB);}
    catch (SQLException e)
    {}
    try
    {conn.createStatement().execute(playlistDB);}
    catch (SQLException e)
    {}
    try
    {conn.createStatement().execute(requestDB);}
    catch (SQLException e)
    {}
  }

  public boolean addSong(String filePath)
  {
    String hash;
    MP3File mp3;
    try
    {
      hash = Controller.getMD5Checksum(filePath);
      if (getSongByHash(hash) != null)
      {
        new File(filePath).delete();
      }
      else
      {
        File file = new File(filePath);
        filePath = Controller.savedMusicDir + "/" + hash + ".mp3";
        FileUtils.copyFile(file, new File(filePath));
        file.delete();
        mp3 = new MP3File(filePath);

        Song song = new Song();
        String artist = song.getArtist();
        String album = song.getArtist();
        String title = song.getTitle();

        try {artist = mp3.getID3v1Tag().getArtist();}
        catch (NullPointerException ex)
        { try
          {artist = mp3.getID3v2Tag().getLeadArtist();}
          catch (NullPointerException e)
          {}
        }
        try {album = mp3.getID3v1Tag().getAlbum();}
        catch (NullPointerException ex)
        {
          try
          {album = mp3.getID3v2Tag().getAlbumTitle();}
          catch (NullPointerException e)
          {}
        }
        try {title = mp3.getID3v1Tag().getTitle();}
        catch (NullPointerException ex)
        {
          try
          {title = mp3.getID3v2Tag().getSongTitle();}
          catch (NullPointerException e)
          {}
        }

        song.setPath(filePath);
        song.setArtist(artist);
        song.setAlbum(album);
        song.setTitle(title);
        song.setActive(true);
        song.setHash(hash);
        writeSong(song);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return false;
    }
    return false;
  }

  public Song getSongByHash(String hash)
  {
    try
    {
      ResultSet result = conn.createStatement().executeQuery("select * from SONGS");
      while(result.next())
      {
        if (result.getString("hash").equals(hash))
        {
          return getSongFromResult(result);
        }
      }
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public Song getNextSong()
  {
    try
    {
      ResultSet result = conn.createStatement().executeQuery("select * from REQUESTS LIMIT 1");
      if (result.next())
      {
        int id = result.getInt(2);
        Song song = getSongFromId(id);
        conn.createStatement().execute("DELETE FROM REQUESTS WHERE song_id=" + id + " LIMIT 1");
        setCurrentRequests();
        setVisiblePlayList();
        return song;
      }
      else
      {
        result = conn.createStatement().executeQuery("select * from PLAYLIST LIMIT 1");
        if (result.next())
        {
          int id = result.getInt(2);
          Song song = getSongFromId(id);
          conn.createStatement().execute("DELETE FROM PLAYLIST WHERE song_id=" + id + " LIMIT 1");
          setCurrentRequests();
          setVisiblePlayList();
          return song;
        }
      }
      setCurrentRequests();
      setVisiblePlayList();
    }
    catch (SQLException e)
    {
      e.printStackTrace();
  }
    return null;
  }

  public Song getSongFromId(int id)
  {
    try
    {
      ResultSet result = conn.createStatement().executeQuery("select * from SONGS WHERE p_id=" + Integer.toString(id));
      if (result.next())
      {
        return getSongFromResult(result);
      }
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public Song[] getAllSongs()
  {
    final ArrayList<Song> songs = new ArrayList<Song>();
    ResultSet result = null;
    try
    {
      result = conn.createStatement().executeQuery("select * from SONGS ORDER BY artist, album, title");
      while(result.next())
      {
        Song song = getSongFromResult(result);
      }
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public Song[] searchSongForString(String string)
  {
    System.out.println("requested " + string);
    final ArrayList<Song> songs = new ArrayList<Song>();
    ResultSet result = null;
    try
    {
      result = conn.createStatement().executeQuery("SELECT * from SONGS ORDER BY artist, album, title");
      while(result.next())
      {
        Song song = getSongFromResult(result);
        if (song != null)
        {
          if (song.getTitle() != null && song.getTitle().toLowerCase().contains(string.toLowerCase()))
          {
            songs.add(song);
          }
          else if (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(string.toLowerCase()))
          {
            songs.add(song);
          }
          else if (song.getArtist() != null && song.getArtist().toLowerCase().contains(string.toLowerCase()))
          {
            songs.add(song);
          }
        }
      }
      final Song[] songList = new Song[songs.size()];
      for (int i = 0;i<songs.size();i++)
      {
        songList[i] = songs.get(i);
        songs.set(i, null);
      }
      return songList;
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  public void addRequest(int songID)
  {
    String sql = "INSERT INTO REQUESTS VALUES ("+null+", "+songID+")";
    try
    {
      conn.createStatement().execute(sql);
      setCurrentRequests();
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  public void setCurrentRequests() throws SQLException
  {
    ArrayList<Song> songs = new ArrayList<Song>();
    ResultSet result = conn.createStatement().executeQuery("select * from REQUESTS");
    while (result.next())
    {
      int id = result.getInt("song_id");
      Song song = getSongFromId(id);
      if (song != null)
      {
        songs.add(song);
      }
    }
    currentRequests = songs;
    requestJSON = new Gson().toJson(DatabaseManager.instance.currentRequests);
  }

  public void setVisiblePlayList() throws SQLException
  {
    ResultSet result = conn.createStatement().executeQuery("select * from PLAYLIST LIMIT " + Integer.toString(Controller.MAX_VISIBLE_PLAYLIST));
    while (result.next())
    {
      int id = result.getInt("song_id");
      Song song = getSongFromId(id);
      if (song != null)
      {
        visiblePlayList[result.getRow() - 1] = song;
      }
    }
    playlistJSON = new Gson().toJson(DatabaseManager.instance.visiblePlayList);
  }

  public void writeSong(Song song)
  {
    try
    {
      PreparedStatement ps = conn.prepareStatement("INSERT INTO SONGS (p_id, path, artist, album, title, active, hash) values (?, ?, ?, ?, ?, ?, ?)");
      ps.setString(1, null);
      ps.setString(2, song.getPath());
      ps.setString(3, song.getArtist());
      ps.setString(4, song.getAlbum());
      ps.setString(5, song.getTitle());
      ps.setBoolean(6, song.getActive());
      ps.setString(7, song.getHash());
      ps.executeUpdate();
      ps.close();
    }
    catch (SQLException ex)
    {
      ex.printStackTrace();
    }
  }

  public Song getSongFromResult(ResultSet result)
  {
    try
    {
      Song song = new Song();
      song.setId(result.getInt("p_id"));
      song.setPath(result.getString("path"));
      song.setArtist(result.getString("artist"));
      song.setAlbum(result.getString("album"));
      song.setTitle(result.getString("title"));
      song.setActive(result.getBoolean("active"));
      song.setHash(result.getString("hash"));
      return song;
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
  }
}
