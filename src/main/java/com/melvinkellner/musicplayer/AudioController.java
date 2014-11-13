package com.melvinkellner.musicplayer;

import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;
import com.echonest.api.v4.Track;
import com.google.gson.Gson;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.IOException;

/**
 * Created by mk on 07/10/14.
 */
public class AudioController
{
  public final static AudioController instance = new AudioController();
  public Song currentSong = null;
  public Media currentMedia = null;
  public MediaPlayer currentPlayer = null;
  public boolean isCurrentlyPlaying = false;
  public double currentVolume = Controller.DEFAULT_VOLUME;
  public String currentSongJSON = "";


  public AudioController()
  {

  }

  public void playSong(Song song)
  {
    if (currentSong == null)
    {
      currentSong = DatabaseManager.instance.getNextSong();
      currentSongJSON = new Gson().toJson(AudioController.instance.currentSong);
    }
    try
    {
      System.gc();
      currentSong = song;
      currentSongJSON = new Gson().toJson(AudioController.instance.currentSong);
      currentMedia = new Media(new File(song.getPath()).toURI().toString());
      currentPlayer = new MediaPlayer(currentMedia);
      currentPlayer.setVolume(currentVolume);
      currentPlayer.setOnEndOfMedia(new Runnable()
      {
        @Override
        public void run()
        {
          next();
        }
      });
      currentPlayer.setOnError(new Runnable()
      {
        @Override
        public void run()
        {
          System.out.println("unkown error while playing file ");
          next();
        }
      });
      currentPlayer.play();

      if ((currentSong.getTitle().toLowerCase().equals("null") ||
          currentSong.getTitle().toLowerCase().equals("") ||
          currentSong.getTitle().toLowerCase().equals("unknown"))
          ||
          (currentSong.getArtist().toLowerCase().equals("null") ||
              currentSong.getArtist().toLowerCase().equals("") ||
              currentSong.getArtist().toLowerCase().equals("unknown")))
      {
        identifySong(currentSong, true);
      }
      isCurrentlyPlaying = true;
    }
    catch (Exception ex)
    {
      next();
      ErrorLogger.logError(ex);
    }
  }

  public void identifySong(Song song)
  {
    identifySong(song, false);
  }

  public void identifySong(final Song song, boolean async)
  {

    final Runnable runnable = new Runnable()
    {
      @Override
      public void run()
      {
        System.out.println("trying to identify song at " + song.getPath());

        EchoNestAPI echoNestAPI = new EchoNestAPI(Controller.ECHONEST_API_KEY);
        try
        {
          echoNestAPI.uploadTrack(new File(song.getPath()));
          Track track = echoNestAPI.getKnownTrack(new File(song.getPath()));
          if ((song.getTitle().toLowerCase().equals("null") ||
              song.getTitle().toLowerCase().equals("") ||
              song.getTitle().toLowerCase().equals("unknown")))
          {
            song.setTitle(track.getTitle());
          }
          if ((song.getArtist().toLowerCase().equals("null") ||
              song.getArtist().toLowerCase().equals("") ||
              song.getArtist().toLowerCase().equals("unknown")))
          {
            song.setArtist(track.getArtistName());
          }
          DatabaseManager.instance.updateSong(song);
          if (AudioController.instance.currentSong.getId() == song.getId())
          {
            AudioController.instance.currentSong = song;
            AudioController.instance.currentSongJSON = new Gson().toJson(AudioController.instance.currentSong);
            System.out.println("song supposidly succesfully identified " + song.getPath());
          }
        }
        catch (EchoNestException e)
        {
          e.printStackTrace();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    };

    if (async)
    {
      new Thread()
      {
        @Override
        public void run()
        {
          runnable.run();
          Thread.currentThread().interrupt();
        }
      }.start();
    }
    else
    {
      runnable.run();
    }
    System.out.println("test");
  }

  public void setVolume(double volume)
  {
    currentVolume = volume;
    if (currentPlayer != null)
    {
      currentPlayer.setVolume(volume);
    }
  }

  public void next()
  {
    if (currentPlayer != null)
    {
      currentPlayer.stop();
      isCurrentlyPlaying = false;
    }
    Song song = DatabaseManager.instance.getNextSong();
    if (song != null)
    {
      playSong(song);
    }
  }

  public void pause()
  {
    if (currentPlayer != null)
    {
      currentPlayer.pause();
      isCurrentlyPlaying = false;
    }
  }

  public void resume()
  {
    if (currentPlayer != null)
    {
      currentPlayer.play();
      isCurrentlyPlaying = true;
    }
  }

  public double getLength()
  {
    if (currentMedia != null)
    {
      return currentMedia.getDuration().toSeconds();
    }
    return 0d;
  }

  public double currentPlayTime()
  {
    if (currentPlayer != null)
    {
      return currentPlayer.getCurrentTime().toSeconds();
    }
    return 0d;
  }



}