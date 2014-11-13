package com.melvinkellner.musicplayer;

import com.google.gson.Gson;

/**
 * Created by mk on 07/10/14.
 */
public class Song
{
  private int id = 0;
  private String path = "";
  private String artist = "unknown";
  private String album = "unknown";
  private String title = "unknown";
  private boolean active = true;
  private String hash = "";

  public Song()
  {

  }

  public int getId()
  {
    return this.id;
  }

  public void setId(int id)
  {
    this.id = id;
  }

  public String getPath()
  {
    return this.path;
  }

  public void setPath(String path)
  {
    this.path = path;
  }

  public String getArtist()
  {
    return this.artist;
  }

  public void setArtist(String artist)
  {
    this.artist = artist;
  }

  public String getAlbum()
  {
    return this.album;
  }

  public void setAlbum(String album)
  {
    this.album = album;
  }

  public String getTitle()
  {
    return this.title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public boolean getActive()
  {
    return this.active;
  }

  public void setActive(boolean active)
  {
    this.active = active;
  }

  public String getHash()
  {
    return this.hash;
  }

  public void setHash(String hash)
  {
    this.hash = hash;
  }

  public String getJson()
  {
    String json = new Gson().toJson(this);
    return json;
  }
}
