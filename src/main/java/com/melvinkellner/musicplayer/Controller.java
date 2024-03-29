package com.melvinkellner.musicplayer;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Track;
import com.google.gson.Gson;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagException;
import org.h2.engine.Database;
import org.simpleframework.http.Part;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class Controller
{
  public static Controller instance = null;

  public static String VERSION_NUMBER = "0.0.1";
  public static String newMusicDir = "newMusic";
  public static String savedMusicDir = "Music";
  public static String DB_DIR = "DB";
  public static String SOUNDBYTE_DIR = "soundbytes";
  public static String WWW_DIR = "www";
  public static boolean ALLOW_STREAM = true;
  public static boolean ALLOW_VETO = false;
  public static int VETO_TIME_MS = 10000;
  public static int MAX_VISIBLE_PLAYLIST = 15;
  public static double DEFAULT_VOLUME = 0.5D;
  public static String ECHONEST_API_KEY = "Your key here";
  public static int SERVER_PORT = 8080;
  public static int MAX_CACHED_ITEMS = 5;
  public static int MAX_NEXT_ATTEMPTS = 2;

  public static String NEXT_SOUND = "";
  public static String VETO_SOUND = "";


  public static String ipadress = "IP_ADDRES";
  public static final String[] COMMANDS = new String[]{"play","pause","next","request","getplaylist","getrequests",
          "findsongbystring", "scanForNew", "shuffleplaylist",
          "setvolume", "getvolume", "getcurrentsong", "deletesong", "editsong", "identifysong",
          "findmissingtags", "getspecials", "playspecial", "uploadfile",
          "getduration", "stream", "getsongbase64byid", "playerstatus", "veto", "getnextAndVetoSounds"};

  public static byte[] createChecksum(String filename) throws Exception {
    InputStream fis =  new FileInputStream(filename);

    byte[] buffer = new byte[1024];
    MessageDigest complete = MessageDigest.getInstance("MD5");
    int numRead;

    do {
      numRead = fis.read(buffer);
      if (numRead > 0) {
        complete.update(buffer, 0, numRead);
      }
    } while (numRead != -1);

    fis.close();
    return complete.digest();
  }

  public static String getMD5Checksum(String filename) throws Exception {
    byte[] b = createChecksum(filename);
    String result = "";

    for (int i=0; i < b.length; i++) {
      result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
    }
    return result;
  }


  public Controller()
  {

    File configFile = new File("server.cfg");
    if (configFile.exists())
    {
      try
      {
        List<String> lines = FileUtils.readLines(configFile);
        for (int i = 0; i< lines.size();i++)
        {
          if (lines.get(i).startsWith("VERSION_NUMBER="))
          {Controller.VERSION_NUMBER = lines.get(i).replace("VERSION_NUMBER=","");}
          else if (lines.get(i).startsWith("newMusicDir="))
          {Controller.newMusicDir = lines.get(i).replace("newMusicDir=","");}
          else if (lines.get(i).startsWith("savedMusicDir="))
          {Controller.savedMusicDir = lines.get(i).replace("savedMusicDir=","");}
          else if (lines.get(i).startsWith("DB_DIR="))
          {Controller.DB_DIR = lines.get(i).replace("DB_DIR=","");}
          else if (lines.get(i).startsWith("WWW_DIR="))
          {Controller.WWW_DIR = lines.get(i).replace("newMusicDir=","");}
          else if (lines.get(i).startsWith("MAX_VISIBLE_PLAYLIST="))
          {Controller.MAX_VISIBLE_PLAYLIST = Integer.parseInt(lines.get(i).replace("MAX_VISIBLE_PLAYLIST=",""));}
          else if (lines.get(i).startsWith("DEFAULT_VOLUME="))
          {Controller.DEFAULT_VOLUME = Double.parseDouble(lines.get(i).replace("DEFAULT_VOLUME=",""));}
          else if (lines.get(i).startsWith("ECHONEST_API_KEY="))
          {Controller.ECHONEST_API_KEY = lines.get(i).replace("ECHONEST_API_KEY=","");}
          else if (lines.get(i).startsWith("SERVER_PORT="))
          {Controller.SERVER_PORT = Integer.parseInt(lines.get(i).replace("SERVER_PORT=",""));}
          else if (lines.get(i).startsWith("SOUNDBYTE_DIR="))
          {Controller.SOUNDBYTE_DIR = lines.get(i).replace("SOUNDBYTE_DIR=","");}
          else if (lines.get(i).startsWith("ALLOW_STREAM="))
          {Controller.ALLOW_STREAM = Boolean.parseBoolean(lines.get(i).replace("ALLOW_STREAM=",""));}
          else if (lines.get(i).startsWith("ALLOW_VETO="))
          {Controller.ALLOW_VETO = Boolean.parseBoolean(lines.get(i).replace("ALLOW_VETO=",""));}
          else if (lines.get(i).startsWith("VETO_TIME_MS="))
          {Controller.VETO_TIME_MS = Integer.parseInt(lines.get(i).replace("VETO_TIME_MS=", ""));}
          else if (lines.get(i).startsWith("MAX_CACHED_ITEMS="))
          {Controller.MAX_CACHED_ITEMS = Integer.parseInt(lines.get(i).replace("MAX_CACHED_ITEMS=", ""));}
          else if (lines.get(i).startsWith("MAX_NEXT_ATTEMPTS="))
          {Controller.MAX_NEXT_ATTEMPTS = Integer.parseInt(lines.get(i).replace("MAX_NEXT_ATTEMPTS=", ""));}
        }

        Controller.NEXT_SOUND = Base64.encodeBase64String(FileUtils.readFileToByteArray(
            new File(Controller.SOUNDBYTE_DIR + "/" + "next.mp3")));
        Controller.VETO_SOUND = Base64.encodeBase64String(FileUtils.readFileToByteArray(
            new File(Controller.SOUNDBYTE_DIR + "/" + "veto.mp3")));
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    DatabaseManager.instance.init();
    instance = this;
    new File(newMusicDir).mkdirs();
    new File(savedMusicDir).mkdirs();
    new File(DB_DIR).mkdirs();

    RemoteControlServer rc = new RemoteControlServer();
    try
    {
      rc.main();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }



  private void scanMusic(String dir, boolean shouldMoveFile)
  {
    File[] fileList = new File(dir).listFiles();
    System.out.print(fileList.length);
    for (File file:fileList)
    {
      if (!file.isDirectory())
      {
        if (file.getName().toLowerCase().endsWith("mp3"))
        {
          DatabaseManager.instance.addSong(file.getAbsolutePath(), shouldMoveFile);
        }
      }
      else
      {
        scanMusic(file.getAbsolutePath(), shouldMoveFile);
      }
    }
  }

  static void shuffleArray(int[] ar)
  {
    Random rnd = new Random();
    for (int i = ar.length - 1; i > 0; i--)
    {
      int index = rnd.nextInt(i + 1);
      // Simple swap
      int a = ar[index];
      ar[index] = ar[i];
      ar[i] = a;
    }
  }

  private String editSong(Query query)
  {
    String[] vars = new String[]{"id" ,"artist", "album", "title"};
    if (query.get(vars[0]) != null)
    {
      Song song = DatabaseManager.instance.getSongFromId(Integer.parseInt(query.get(vars[0])));
      if (song == null)
      {
        return "failed";
      }
      if (query.get(vars[1]) != null)
      {
        song.setArtist(query.get(vars[1]));
      }
      if (query.get(vars[2]) != null)
      {
        song.setAlbum(query.get(vars[2]));
      }
      if (query.get(vars[3]) != null)
      {
        song.setTitle(query.get(vars[3]));
      }
      try
      {
        MP3File songfile = new MP3File(song.getPath());
        songfile.getID3v1Tag().setArtist(song.getArtist());
        songfile.getID3v1Tag().setAlbum(song.getAlbum());
        songfile.getID3v1Tag().setTitle(song.getTitle());
        songfile.save();
      } catch (IOException e)
      {
        e.printStackTrace();
      } catch (TagException e)
      {
        e.printStackTrace();
      }
      DatabaseManager.instance.updateSong(song);
      AudioController.instance.currentSong = song;
      AudioController.instance.currentSongJSON = new Gson().toJson(AudioController.instance.currentSong);
    }
    else
    {
      return "fail";
    }
    return "done";
  }

  private void findAllMissingTags()
  {
    try
    {
      ArrayList<Integer> idArray = DatabaseManager.instance.getAllSongIds();
      while (idArray.size() > 0)
      {
        Song song = DatabaseManager.instance.getSongFromId(idArray.get(0));
        findMissingTags(song);
        idArray.remove(0);
      }
    } catch (SQLException e)
    {
      e.printStackTrace();
    }
  }

  private void findMissingTags(Song song)
  {
    if ((song.getTitle().toLowerCase().equals("null") ||
        song.getTitle().toLowerCase().equals("") ||
        song.getTitle().toLowerCase().equals("unknown"))
        ||
        (song.getArtist().toLowerCase().equals("null") ||
            song.getArtist().toLowerCase().equals("") ||
            song.getArtist().toLowerCase().equals("unknown")))
    {
      try
      {
        MP3File mp3 = new MP3File(song.getPath());
        String artist = song.getArtist();
        String album = song.getArtist();
        String title = song.getTitle();

        if (song.getArtist().toLowerCase().equals("null") ||
            song.getArtist().toLowerCase().equals("") ||
            song.getArtist().toLowerCase().equals("unknown"))
        {
          try
          {artist = mp3.getID3v2Tag().getLeadArtist();}
          catch (NullPointerException e){}
        }

        if (song.getAlbum().toLowerCase().equals("null") ||
            song.getAlbum().toLowerCase().equals("") ||
            song.getAlbum().toLowerCase().equals("unknown"))
        {
          try
          {album = mp3.getID3v2Tag().getAlbumTitle();}
          catch (NullPointerException e){}
        }

        if (song.getTitle().toLowerCase().equals("null") ||
            song.getTitle().toLowerCase().equals("") ||
            song.getTitle().toLowerCase().equals("unknown"))
        {
          try
          {title = mp3.getID3v2Tag().getSongTitle();}
          catch (NullPointerException e) {}
        }
        song.setArtist(artist);
        song.setTitle(title);
        song.setAlbum(album);
        DatabaseManager.instance.updateSong(song);
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      catch (TagException e)
      {
        e.printStackTrace();
      }
    }
  }

  private String getCurrentPlaylist()
  {
    if (DatabaseManager.instance.visiblePlayList[0] == null)
    {
      DatabaseManager.instance.shufflePlayList();
      AudioController.instance.next();
    }
    return DatabaseManager.playlistJSON;
  }

  private String getCurrentRequests()
  {
    return DatabaseManager.requestJSON;
  }

  private void shufflePlayList()
  {
    DatabaseManager.instance.shufflePlayList();
  }

  private String findSongsByString(String value)
  {
    Song[] songs = DatabaseManager.instance.searchSongForString(value);
    return new Gson().toJson(songs);
  }

  private String soundBytesJson()
  {
    ArrayList<File> files = new ArrayList<File>(FileUtils.listFiles(new File(SOUNDBYTE_DIR), null, true));
    String[] names = new String[files.size()];
    for (int i = 0;i<files.size();i++)
    {
      names[i] = files.get(i).getName();
    }
    return new Gson().toJson(names);
  }

  public String saveUploadedFile(Request request) throws IOException
  {
    List<Part> list = request.getParts();
    final boolean isSoundByte = getBoolFromParts("isSoundByte", list);
    final String artist = getStringFromParts("artist", list);
    final String album = getStringFromParts("album", list);
    final String title = getStringFromParts("title", list);
    for(Part part : list)
    {
      String name = part.getName();
      if(part.isFile())
      {
        name = part.getFileName();
        InputStream initialStream = part.getInputStream();
        byte[] buffer = new byte[initialStream.available()];
        initialStream.read(buffer);
        File targetFile = new File(name);
        OutputStream outStream = new FileOutputStream(targetFile);
        outStream.write(buffer);
        outStream.close();

        if (!isSoundByte)
        {
          Song song = new Song();
          song.setPath(targetFile.getAbsolutePath());
          findMissingTags(song);
          if (!artist.equals(""))
          {
            song.setArtist(artist);
          }
          if (!album.equals(""))
          {
            song.setAlbum(album);
          }
          if (!title.equals(""))
          {
            song.setTitle(title);
          }
          DatabaseManager.instance.addSong(song);
          return "added song " + song.getTitle();
        }
        else
        {
          if (!new File(Controller.SOUNDBYTE_DIR + "/" + targetFile.getName()).exists())
          {
            FileUtils.moveFile(targetFile, new File(Controller.SOUNDBYTE_DIR + "/" + targetFile.getName()));
          }
          else
          {
            targetFile.delete();
          }
          return "added soundbyte " + targetFile.getName();
        }
      }
    }
    return "Failed to add song";
  }

  public String getStringFromParts(String name, List<Part> list)
  {
    for(Part part : list)
    {
      if (part.getName().equals(name))
      {
        try
        {
          return part.getContent();
        }
        catch (IOException ex)
        {
          ex.printStackTrace();
        }
      }
    }
    return "";
  }

  public boolean getBoolFromParts(String name, List<Part> list)
  {
    for(Part part : list)
    {
      if (part.getName().equals(name))
      {
        return true;
      }
    }
    return false;
  }


  public String sendCommand(String command, String value, Query query)
  {
    if (command.equals(COMMANDS[22]))
    {
      Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
      HashMap<String, String> resultMap = new HashMap<String, String>();
      resultMap.put("currentsong", AudioController.instance.currentSongJSON);
      resultMap.put("playlist", getCurrentPlaylist());
      resultMap.put("requests", getCurrentRequests());
      resultMap.put("volume", Double.toString(AudioController.instance.currentVolume));
      HashMap<String, Double> map = new HashMap<String, Double>();
      map.put("current", AudioController.instance.currentPlayTime());
      map.put("total", AudioController.instance.getLength());

      try
      {
        resultMap.put("duration", new Gson().toJson(map));
      }
      catch (IllegalArgumentException ex)
      {ex.printStackTrace();}
      if (ALLOW_VETO)
      {
        resultMap.put("isnexted", Boolean.valueOf(AudioController.instance.nextQueued).toString());
      }
      return new Gson().toJson(resultMap);
    }
    else if (command.equals(COMMANDS[0]))
    {
      AudioController.instance.resume();
    }
    else if (command.equals(COMMANDS[1]))
    {
      AudioController.instance.pause();
    }
    else if (command.equals(COMMANDS[2]))
    {
      if (!ALLOW_VETO)
      {
        AudioController.instance.next();
      }
      else
      {
        AudioController.instance.queNext();
      }
    }
    else if (command.equals(COMMANDS[3]))
    {
      DatabaseManager.instance.addRequest(Integer.parseInt(value));
    }
    else if (command.equals(COMMANDS[4]))
    {
      return getCurrentPlaylist();
    }
    else if (command.equals(COMMANDS[5]))
    {
      return getCurrentRequests();
    }
    else if (command.equals(COMMANDS[6]))
    {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      return findSongsByString(value);
    }
    else if (command.equals(COMMANDS[7]))
    {
      Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
      scanMusic(newMusicDir, true);
      scanMusic(savedMusicDir, false);
    }
    else if (command.equals(COMMANDS[8]))
    {
      shufflePlayList();
    }
    else if (command.equals(COMMANDS[9]))
    {
      AudioController.instance.setVolume(Double.parseDouble(value));
    }
    else if (command.equals(COMMANDS[10]))
    {
      HashMap<String, Double> map = new HashMap<String, Double>();
      map.put("volume", AudioController.instance.currentVolume);
      return new Gson().toJson(map);
    }
    else if (command.equals(COMMANDS[11]))
    {
      return AudioController.instance.currentSongJSON;
    }
    else if (command.equals(COMMANDS[12]))
    {
      return DatabaseManager.instance.setActive(Integer.parseInt(value), false);
    }
    else if (command.equals(COMMANDS[13]))
    {
      return editSong(query);
    }
    else if (command.equals(COMMANDS[14]))
    {
      Song song = DatabaseManager.instance.getSongFromId(Integer.parseInt(value));
      AudioController.instance.identifySong(song, true);
      return "identifySong";
    }
    else if (command.equals(COMMANDS[15]))
    {
      findAllMissingTags();
    }
    else if (command.equals(COMMANDS[16]))
    {
      return soundBytesJson();
    }
    else if (command.equals(COMMANDS[17]))
    {
      File file = new File(Controller.SOUNDBYTE_DIR + "/" + value);
      if (file.exists())
      {
        Song song = new Song();
        song.setPath(file.getAbsolutePath());
        AudioController.instance.playSpecial(song);
      }
    }
    //else if (command.equals(COMMANDS[18]))
    //{
      //handled in the remote control server
    //}
    else if (command.equals(COMMANDS[19]))
    {
        HashMap<String, Double> map = new HashMap<String, Double>();
        map.put("current", AudioController.instance.currentPlayTime());
        map.put("total", AudioController.instance.getLength());
        return new Gson().toJson(map);
    }
    else if (command.equals(COMMANDS[20]))
    {
      if (ALLOW_STREAM)
      {
        if (value.equals("start"))
        {
          StreamManager.instance.addStreamer(query.get(Controller.ipadress));
          HashMap<String, Integer> map = new HashMap<String, Integer>();
          map.put("cacheSize", Controller.MAX_CACHED_ITEMS);
          map.put("isStreaming", 1);
          return new Gson().toJson(map);
        } else if (value.equals("stop"))
        {
          StreamManager.instance.removeStreamer(query.get(Controller.ipadress));
        } else if (value.equals("status"))
        {
          return StreamManager.instance.getAudioStatus(query.get(Controller.ipadress));
        }
      }
      else
      {
        return "streaming not allowed by server";
      }
      return command;
    }
    else if (command.equals(COMMANDS[21]))
    {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        return StreamManager.instance.getSongBase64ById(Integer.parseInt(value));
    }
    /*else if (command.equals(COMMANDS[22]))
    {
      handeled in beginning of method
    }*/
    else if (command.equals(COMMANDS[23]))
    {
      AudioController.instance.veto();
    }
    else if (command.equals(COMMANDS[24]))
    {
      HashMap<String, String> map = new HashMap<String, String>();
      map.put("nextSound", Controller.NEXT_SOUND);
      map.put("vetoSound", Controller.VETO_SOUND);
      return new Gson().toJson(map);
    }
    return command;
  }


}
