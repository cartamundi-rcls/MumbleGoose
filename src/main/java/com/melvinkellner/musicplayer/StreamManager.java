package com.melvinkellner.musicplayer;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.h2.engine.Database;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by melvin on 3/18/15.
 */
public class StreamManager
{
    public static StreamManager instance = new StreamManager();
    public boolean isStreaming = false;
    private HashMap<Integer, String> streamMap = new HashMap<Integer, String>();
    private HashMap<String, Long> activeStreamers = new HashMap<String, Long>();
    private final static int RECHECK_TIME = 30000;
    private ArrayList<Song> songs = new ArrayList<Song>();

    public void revalidateCache()
    {
        cacheSongs();
    }

    private void addSong(Song song)
    {
        if (songs.size() < Controller.MAX_CACHED_ITEMS)
        {
            songs.add(song);
        }
    }

    private void setStreamMap()
    {
        ArrayList<Integer> unusedId = new ArrayList<Integer>();
        Iterator<Map.Entry<Integer, String>> it = streamMap.entrySet().iterator();
        while (it.hasNext())
        {
            int id = it.next().getKey();
            boolean isIn = false;
            inner:for (int i = 0;i<songs.size();i++)
            {
                if (songs.get(i) != null && id == songs.get(i).getId())
                {
                    isIn = true;
                    break inner;
                }
            }
            if (!isIn)
            {
                unusedId.add(id);
            }
        }
        for (Integer key : unusedId)
        {
            streamMap.remove(key);
        }
        System.gc();
        for (int i = 0;i<songs.size();i++)
        {
            if (songs.get(i) != null && !streamMap.containsKey(songs.get(i).getId()))
            {
                cacheSong(songs.get(i));
            }
        }
        songs = new ArrayList<Song>();
    }


    public void cacheSong(final Song song)
    {
        if (!isStreaming)
        {return;}
        if (song == null)
        {return;}
        if (streamMap.size() < Controller.MAX_CACHED_ITEMS)
        {
            if (!streamMap.containsKey(song.getId()))
            {
                streamMap.put(song.getId(), null);
                new Thread()
                {
                    @Override
                    public void run()
                    {
                        super.run();
                        try
                        {
                            streamMap.put(song.getId(), Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(song.getPath()))));
                        }
                        catch (IOException e)
                        {
                            streamMap.remove(song.getId());
                            e.printStackTrace();
                        }
                        this.interrupt();
                    }
                }.start();
            }
        }
    }

    public String getAudioStatus(String address)
    {
        activeStreamers.put(address, System.currentTimeMillis());
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("isPlaying", Boolean.toString(AudioController.instance.isCurrentlyPlaying));
        map.put("currentPlayTime", Double.toString(AudioController.instance.currentPlayTime()));
        return new Gson().toJson(map);
    }

    public void removeCachedSong(Song song)
    {
        if (song != null)
        {
            if (streamMap.containsKey(song.getId()))
            {
                streamMap.remove(song.getId());
            }
        }
    }

    public void addStreamer(String address)
    {
        activeStreamers.put(address, System.currentTimeMillis());
        if (!isStreaming)
        {
            isStreaming = true;
            cacheSongs();
        }
    }

    public void removeStreamer(String address)
    {
        activeStreamers.remove(address);
        if (activeStreamers.size() == 0)
        {
            isStreaming = false;
            activeStreamers.clear();
        }
    }

    private void cacheSongs()
    {
        if (isStreaming)
        {
            addSong(AudioController.instance.currentSong);
            for (Song song : DatabaseManager.currentRequests) {
                addSong(song);
            }
            for (Song song : DatabaseManager.visiblePlayList) {
                addSong(song);
            }
            setStreamMap();
        }
    }

    public String getSongBase64ById(int id)
    {
        if (streamMap.containsKey(id))
        {
            return streamMap.get(id);
        }
        return "";
    }
}
