package com.melvinkellner.musicplayer;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.h2.engine.Database;

import java.io.File;
import java.io.IOException;
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
    private Song[] songs = new Song[Controller.MAX_CACHED_ITEMS];

    public void addSong(Song song, int index)
    {
        if (isStreaming)
        {
            if (index > 0 && index < songs.length)
            {
                songs[index] = song;
            }
        }
    }


    public void setStreamMap()
    {
        ArrayList<Integer> unusedId = new ArrayList<Integer>();
        Iterator<Map.Entry<Integer, String>> it = streamMap.entrySet().iterator();
        while (it.hasNext())
        {
            int id = it.next().getKey();
            boolean isIn = false;
            inner:for (int i = 0;i<songs.length;i++)
            {
                if (songs[i] != null && id == songs[i].getId())
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
        for (int i = 0;i<songs.length;i++)
        {
            if (songs[i] != null && !streamMap.containsKey(songs[i].getId()))
            {
                cacheSong(songs[i]);
            }
        }
        songs = new Song[Controller.MAX_CACHED_ITEMS];
    }


    public void cacheSong(final Song song)
    {
        if (!isStreaming)
        {return;}
        if (song == null)
        {return;}
        if (streamMap.size() < Controller.MAX_CACHED_ITEMS)
        {
            if (!streamMap.containsKey(song.getHash()))
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
                            streamMap.remove(song.getHash());
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
        cacheSong(AudioController.instance.currentSong);
        for (Song song : DatabaseManager.currentRequests)
        {
            cacheSong(song);
        }
        for (Song song : DatabaseManager.visiblePlayList)
        {
            cacheSong(song);
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
