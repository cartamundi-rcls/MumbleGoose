package com.melvinkellner.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.h2.engine.Database;

import java.util.Set;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();

        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent windowEvent) {
                DatabaseManager.instance.closeDB();
                Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                Thread[] threadArray = threadSet.toArray(new Thread[threadSet.size()]);
                for (Thread thread : threadArray)
                {
                    if (!thread.getName().equals("Main"))
                    {
                        try
                        {
                            thread.interrupt();
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                }
                Platform.exit();
            }
        });
    }


    public static void main(String[] args)
    {
      launch(args);
    }
}
