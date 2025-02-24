package com.kusa.players;

import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.events.MediaPlayerEvent;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import com.kusa.service.LocalService;
import java.util.List;
import java.util.ArrayList;
import java.util.Timer;
import com.kusa.PlaylistUpdaterTask;


/**
 * Class representing the video content in an engagment frame.
 *
 * this class extends vlcjs embedded media player component for
 * access to vlc bindings.
 */
public class VideoPanel extends EmbeddedMediaPlayerComponent
{
  private Playlist playlist;
  
  /**
   * Constructs a video panel for use in an engagment frame.
   *
   * we should proably add the playlist as a parameter to this 
   * class eventually so we don't have to manage the playlist 
   * within a video panel.
   */
  public VideoPanel()
  {
    playlist = new Playlist();
    setOpaque(true);

    //we setup a event adapter to automatically play the next video in a playlist
    //once a video ends.
    mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
      @Override
      public void finished(MediaPlayer mp)
      {
        mediaPlayer().submit(() -> mediaPlayer().media().play(playlist.next()));
      }
    });

    //we also start a task here for managing our playlist
    //TODO this is probably not the best place to do this!!!
    Timer t = new Timer();
    t.schedule(new PlaylistUpdaterTask(playlist), 1000, 60000);
  }

  /**
   * Plays the current media.
   *
   * called by the main app to start the video panel. 
   *
   * because the video panel takes care of starting new videos 
   * after one ends this method should only be called once 
   * after the engagment frame has been created.
   */
  public void play()
  {
    mediaPlayer().media().play(playlist.next());
  }

}
