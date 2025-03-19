package com.kusa.player;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Image;
import java.awt.BorderLayout;

import java.io.File;
import javax.imageio.ImageIO;
import org.imgscalr.Scalr;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

//might be able to remove these.
import javax.swing.BorderFactory;
import java.awt.GridLayout;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.util.Set;
import com.kusa.service.LocalService;

/**
 * Class for managing a side panel of an engagment frame.
 *
 * right now this class only works by displaying an image from
 * a path specified in the constructor. in the future i want to extend
 * functionality to allow for creating panels with custom text as well.
 */
public class SidePanel extends JPanel
{
  private BufferedImage originalImage;
  private Image scaledImage;
  private ImageIcon icon;
  private JLabel imageLabel;

  /**
   * Constructs a side panel with the default dimensions.
   * default dimensions attempt to take up 1/3 of the 1080p display.
   * be careful if your display is not 1080p the images might not show up as expected.
   * @param path path of image content to be displayed.
   */ 
  public SidePanel(String path) { this(path, 640, 1080); }
  /**
   * Constucts a side panel with custom dimensions.
   * we can specify our own width and height but it's up to the engament frame to 
   * determine how it will be displayed in the end.
   * (this might not be true. the size should probably be set HERE)
   * @param path path of image content to be displayed.
   * @param width int representing width of side panel.
   * @param height int representing height of side panel.
   */
  public SidePanel(String path, int width, int height)
  {
    //layout (border layout might not be best)
    setLayout(new BorderLayout());

    /*
     * LOADING IMAGE.
     *
     * if we fail to read the image this panel will just be black.
     * im trying to come up with better ways to scale the image so that the side panels image still
     * fits correctly on different resolutions. previously i had used get scaled instance, but it
     * didn't seem to work as nicely when displaying on TVs with high res. I think it could've been
     * a bug that made the images not look as nice, i was passing in the screens width for both width and height
     * so although the bug is gone our new solutions utilizes Scalr library to scale based on the width which
     * should be 1/3 of the display. Im currently testing out better methods to scale the image.
     *
     * previous scaling code: scaledImage = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
     */
    try{
      originalImage = ImageIO.read(new File(path));
      scaledImage = Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.FIT_TO_WIDTH, width);
    }catch(Exception e)
    {
      System.out.println("IMAGE " + path + " FAILED TO LOAD");
      scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics g = scaledImage.getGraphics();
      g.setColor(Color.BLACK);
      g.fillRect(0, 0, width, height);
      g.dispose();
    }

    icon = new ImageIcon(scaledImage);
    imageLabel = new JLabel(icon);
    imageLabel.setOpaque(true);
    imageLabel.setBackground(Color.BLACK);

    add(imageLabel, BorderLayout.CENTER); 
  }

  /**
   * Sets the content displayed on the side panel.
   *
   * if it fails to load the new image it will
   * just stick to the image it currently has.
   *
   * we have a Task for using this function i just havent
   * found the best place to use it.
   * 
   * @param path path of new image to be displayed.
   */
  public void setImage(String path)
  {
    BufferedImage newImage;
    Image newScaledImage;
    int width = imageLabel.getIcon().getIconWidth();
    int height = imageLabel.getIcon().getIconHeight();
    try{
      newImage = ImageIO.read(new File(path));
      newScaledImage = Scalr.resize(originalImage, Scalr.Method.BALANCED, Scalr.Mode.FIT_TO_WIDTH, width);
      ImageIcon newIcon = new ImageIcon(newScaledImage);
      imageLabel.setIcon(newIcon);
    }catch(Exception e)
    {
      System.out.println("FAILED TO SET NEW IMAGE: " + path);
    }
  }

  /**
   * Returns the mrls for side panels of the day.
   *
   * will default to just left or right if no daily images.
   * @param leftPanel - true if you want get mrls of left panel.
   */
  public static Set<String> photoMRLS(boolean leftPanel)
  {
    String dir = leftPanel ? "photos/left/" : "photos/right/";
    switch(LocalDateTime.now().getDayOfWeek())
    {
      case MONDAY:
        Set<String> mondayMRLS = LocalService.getLocalMRLS(dir+"monday/", true);
        if(!mondayMRLS.isEmpty())
          return mondayMRLS;
        break;
      case TUESDAY:
        Set<String> tuesdayMRLS = LocalService.getLocalMRLS(dir+"tuesday/", true);
        if(!tuesdayMRLS.isEmpty())
          return tuesdayMRLS;
        break;
      case WEDNESDAY:
        Set<String> wednesdayMRLS = LocalService.getLocalMRLS(dir+"wednesday/", true);
        if(!wednesdayMRLS.isEmpty())
          return wednesdayMRLS;
        break;
      case THURSDAY:
        Set<String> thursdayMRLS = LocalService.getLocalMRLS(dir+"thursday/", true);
        if(!thursdayMRLS.isEmpty())
          return thursdayMRLS;
        break;
      case FRIDAY:
        Set<String> fridayMRLS = LocalService.getLocalMRLS(dir+"friday/", true);
        if(!fridayMRLS.isEmpty())
          return fridayMRLS;
        break;
      case SATURDAY:
        Set<String> saturdayMRLS = LocalService.getLocalMRLS(dir+"saturday/", true);
        if(!saturdayMRLS.isEmpty())
          return saturdayMRLS;
        break;
      case SUNDAY:
        Set<String> sundayMRLS = LocalService.getLocalMRLS(dir+"sunday/", true);
        if(!sundayMRLS.isEmpty())
          return sundayMRLS;
        break;
    }
    //default, MIGHT be empty.
    return LocalService.getLocalMRLS(dir, false);
  } 
}
