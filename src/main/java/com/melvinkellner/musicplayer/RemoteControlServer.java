package com.melvinkellner.musicplayer;

/**
 * Created by mk on 07/10/14.
 */

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;


public class RemoteControlServer implements Container
{
  public void handle(final Request request, final Response response)
  {
    if (request.getPath().getPath().toLowerCase().contains("favicon.ico"))
    {
      return;
    }
    try
    {
      Thread thread = new Thread()
      {
        @Override
        public void run()
        {
          try
          {
            PrintStream body = response.getPrintStream();
            long time = System.currentTimeMillis();
            response.setValue("Server", "MUSIC-SERVER/" + Controller.VERSION_NUMBER);
            response.setDate("Date", time);
            response.setValue("Access-Control-Allow-Origin", "*");
            response.setValue("Content-Type", "text/html");
            response.setDate("Last-Modified", time);

            Query query = request.getQuery();
            query.put(Controller.ipadress, request.getClientAddress().getAddress().getHostAddress());
            commandFinder:for (int i = 0; i < Controller.instance.COMMANDS.length; i++)
            {
              if (query.get(Controller.instance.COMMANDS[i]) != null)
              {
                if (i != 18)
                {
                  body.print(Controller.instance.sendCommand(Controller.instance.COMMANDS[i], query.get(Controller.instance.COMMANDS[i]), query));
                }
                else
                {
                  body.print(Controller.instance.saveUploadedFile(request));
                }
               break commandFinder;
              }
            }
            body.close();
            super.run();
            Thread.currentThread().interrupt();
            return;
          }
          catch (IOException ex)
          {
            ex.printStackTrace();
            Thread.currentThread().interrupt();
            return;
          }
        }
      };
      thread.start();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  public static void main() throws Exception
  {
    Container container = new RemoteControlServer();
    Server server = new ContainerServer(container);
    Connection connection = new SocketConnection(server);
    SocketAddress address = new InetSocketAddress(Controller.SERVER_PORT);
    connection.connect(address);
  }
}