package xyz.doikki.videoplayer.exo;

import android.app.Application;

public class App extends Application {
  private static App instance;
  public static App get() {
        return instance;
    }
}  
