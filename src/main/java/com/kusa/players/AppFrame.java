package com.kusa.players;

import java.util.Timer;
import java.util.TimerTask;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class AppFrame extends JFrame
{
  static GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];

  private SidePanel leftPanel;
  private VideoPanel middlePanel;
  private SidePanel rightPanel;

  public AppFrame()
  {
    leftPanel = new SidePanel("/home/kusa/UwU/jinzo/src/main/resources/ttuesday.jpg");
    rightPanel = new SidePanel("/home/kusa/UwU/jinzo/src/main/resources/qts.jpg");
    middlePanel = new VideoPanel();

    setUndecorated(true);
    addWindowListener(new WindowAdapter() {
      @Override 
      public void windowClosing(WindowEvent e)
      {
        middlePanel.release();
        System.exit(0); //app quits when main frame is closed.
      }
    });

    JPanel contentPane = new JPanel();

    //contentPane.setLayout(new GridLayout(1, 3, 0, 0));
    contentPane.setLayout(new BorderLayout());

    contentPane.add(leftPanel, BorderLayout.WEST);
    contentPane.add(middlePanel, BorderLayout.CENTER);
    contentPane.add(rightPanel, BorderLayout.EAST);

    setContentPane(contentPane);
    setVisible(true);
      
    device.setFullScreenWindow(this);

    middlePanel.playVideo("/home/kusa/UwU/jinzo/src/main/resources/special.mp4");

    /*
    Timer timer = new Timer();
    timer.schedule(new ImageSwitcher(leftPanel), 1000);
    */

    System.out.println("VIDEO FRAME CREATED, SHOWING CONTENT");
    System.out.println("Left Panel" + leftPanel.getSize());
    System.out.println("video " + middlePanel.getSize());
    System.out.println("Right Panel" + rightPanel.getSize());
  }
}
