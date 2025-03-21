package com.kusa.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import com.kusa.Config;
import com.kusa.util.PathedFile;
import org.apache.commons.io.FileUtils;

/**
 * Class that will connect to the google drive.
 *
 * it finds a folder with the name of 
 * MAIN_FOLDER_NAME in your google drive.
 * sub directories used: 
 *  - "videos/"
 *  - "pictures/"
 *
 * try to keep only one instance of this.
 */
public class GDriveService
{
  private static final String googleAppName = "Jinzo";
  private static final String MAIN_FOLDER_NAME = "JINZO";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

  //we only use read only. app only needs to be able to download the videos.
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);

  /* we specify the paths used for storing the apps data + login tokens.
   * 'tokens/' locally stored login tokens (drive user's tokens)
   * '/credentials' this file contains the app's credentials, comes from google dev console.
   */
  private static final String TOKEN_STORAGE_PATH = Config.getProperty("tokenStoragePath");
  private static final String CREDENTIALS_FILE_PATH = Config.getProperty("googleCredentialsPath");

  private Drive drive;
  private File jinzoFolder;
  private boolean valid;

  /**
   * Constructor for creating the google drive service.
   *
   * we should only be creating one instance of this service
   * if its possbile i'll try my best to make this a "static" class. 
   *
   * if the service fails it won't retry or anything just none of
   * the functionality will work.
   *
   * TODO handle failure properly.
   *  - currently using valid flag.
   */
  public GDriveService()
  {
    this.valid = false;
    init();
  }

  private void init()
  {
    boolean valid = false;
    try
    {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(googleAppName).build();
      valid = findAppFolder();
      if(!valid)
        System.out.println("Failed to launch gdrive service. COULDNT FIND APP FOLDER");
    }
    catch(Exception e)
    {
      System.out.println("Failed to launch gdrive service." + e);
      valid = false;
    }
    finally
    {
      this.valid = valid;
    }
  }

  /**
   * Returns true if you can safely use this service.
   *
   * will attempt to re initialize if it's not valid.
   */
  public boolean isValid() { 
    if(!this.valid)
    {
      System.out.println("GDS SERVICE IS INVLALID ATTEMPTING TO VALIDATE");
      init();
    }

    if(this.valid)
      System.out.println("GDS SERVICE IS VALID!");

    return this.valid;
  }

  /**
   * Returns true if jinzo folder is found in drive.
   *
   * sets jinzo folder to the google drive's corresponding folder file.
   *
   * @return true if jinzo folder is found and will not be null.
   *         false if jinzo folder will be null after this method.
   */
  private boolean findAppFolder()
  {
    try{
      FileList result = drive.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").setFields("files(id, name)").execute();
      List<File> files = result.getFiles();
      for(File file : files)
      {
        if(file.getName().equals(MAIN_FOLDER_NAME))
        {
          jinzoFolder = file;
          return true;
        }
      }
      return false;
    }
    catch(Exception e)
    {
      System.out.println("GDS FAILURE: FAILED TO FIND NEEDED FOLDERS IN DRIVEE!!!");
      e.printStackTrace();
      return false;
    }
  }


  private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException, FileNotFoundException
  {
    java.io.File creds = new java.io.File(CREDENTIALS_FILE_PATH);
    if(!creds.exists())
      throw new FileNotFoundException("Resouce not found: " + CREDENTIALS_FILE_PATH); 

    FileInputStream in = new FileInputStream(creds);
    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                                                                      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKEN_STORAGE_PATH)))
                                                                      .setAccessType("offline")
                                                                      .build();

    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    System.out.printf("YOUR DRIVE CREDENTIALS EXPIRES IN %d SECONDS.\n", credential.getExpiresInSeconds());
    return credential;
  }


  /**
   * Gets a list of pathed files in application named folder from the drive.
   *
   * we are handling the exception internally right now
   *
   * idk if thats the best approach but if any failure occurs
   * it will return an empty list.
   *
   * @return List of google drive files in application folder (JINZO).
   */
  public List<PathedFile> getFileList() { return getFileList("", true); }
  public List<PathedFile> getFileList(String nestedFolders, boolean recursive)
  {
    try
    {
      PathedFile start = new PathedFile(jinzoFolder, "");

      boolean skipSearch = true;
      if(!nestedFolders.equals(""))
      {
        System.out.println("GETTING FILES FROM GOOGLE DRIVE SEARCHING FOR START DIR: " + nestedFolders);
        skipSearch = false;
      }

      if(!nestedFolders.equals(""))
      {
        String pageToken = null;
        PathedFile pf = new PathedFile(jinzoFolder, "");
        File folder = pf.file();
        String path = pf.path();
        String id = folder.getId();
        do {
          String query = String.format("'%s' in parents and trashed = false",id);
          FileList result = drive.files().list().setQ(query).setFields("nextPageToken, files(id, name, parents, description, mimeType)")
            .setPageToken(pageToken).execute();
          if(result.getNextPageToken() != null)
          {
            System.out.println("NEXT PAGE TOKEN: " + result.getNextPageToken());
          }
          if(result.getIncompleteSearch() != null)
            if(result.getIncompleteSearch())
              System.out.println("GDS: INCOMPLETE SEARCH OCCURED IN GET FILE LIST");
          pageToken = result.getNextPageToken();
          List<File> folderFiles = result.getFiles();
          System.out.println("GDS: FOUND " + folderFiles.size() + " FILES IN :/" + path);
          for(File file : folderFiles)
          {
            if(file.getMimeType().equals(FOLDER_MIME_TYPE)
                && nestedFolders.equals(path + file.getName() + "/"))
            {
              start = new PathedFile(file, path + file.getName() + "/");
              skipSearch = true;
              break;
            }
          }
        } while(pageToken != null && !skipSearch);
      }
      
      //Google drive File
      Queue<PathedFile> folders = new LinkedList<>(List.of(start));
      List<PathedFile> files = new ArrayList<>();
      while(!folders.isEmpty())
      {
        PathedFile pf = folders.poll();
        File folder = pf.file();
        String path = pf.path();
        String id = folder.getId();

        String query = String.format("'%s' in parents and trashed = false",id);
        String pageToken = null;
        do{
          FileList result = drive.files().list().setQ(query).setFields("nextPageToken, files(id, name, parents, description, mimeType)")
            .setPageToken(pageToken).execute();
          if(result.getNextPageToken() != null)
          {
            System.out.println("NEXT PAGE TOKEN: " + result.getNextPageToken());
          }
          if(result.getIncompleteSearch() != null)
            if(result.getIncompleteSearch())
              System.out.println("GDS: INCOMPLETE SEARCH OCCURED IN GET FILE LIST");
          pageToken = result.getNextPageToken();
          List<File> folderFiles = result.getFiles();
          for(File file : folderFiles)
          {
            if(file.getMimeType().equals(FOLDER_MIME_TYPE) && recursive)
              folders.add(new PathedFile(file, path + file.getName() + "/"));

            if(!file.getMimeType().equals(FOLDER_MIME_TYPE))
              files.add(new PathedFile(file, path));
          }
        } while(pageToken != null);
      }
      return files;
    }
    catch(Exception e)
    {
      System.out.println("GDriveService: Failed to get files. " + e + e.getMessage()); 
      e.printStackTrace();
      System.out.println("GDriveService: Setting valid to FALSE"); 
      this.valid = false;
      return Collections.emptyList();
    }
  }

  /**
   * Downloads a PathedFile locally.
   *
   *
   * if file already exists we skip it.
   *
   * download file with their path as destination.
   *
   * @param pf PathedFile we want to download. (pathed files are gdrive files)
   * @return true if file was downloaded.
   *
   */
  public boolean downloadPathedFile(PathedFile pf)
  {
    final String id = pf.file().getId();
    final String path = Config.getProperty("downloadPath") + pf.path();
    final String name = pf.file().getName();  
    try
    {
      if(new java.io.File(path + name).exists())
        return false;
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      drive.files().get(id).executeMediaAndDownloadTo(os);
      FileOutputStream fos = new FileOutputStream(path + name);
      os.writeTo(fos);
      System.out.printf("SUCCESSFULLY DOWNLOADED NEW FILE\n file:%s\n driveID:%s\n path:%s\n", name, id, path);
      return true;
    }
    catch(Exception e)
    {
      System.out.printf("download failed!\n name:%s\n id:%s", name, id);
      System.out.println(e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  public boolean downloadMedia()
  {
    try{
      boolean changed = false;
      List<PathedFile> files = getFileList();
      if(files.isEmpty())
      {
        System.out.println("GDriveService: GOT NO FILES FROM CALLING GET FILES WILL SKIP DOWNLOADS");
        return false;
      }
      System.out.println("gds: starting downloads... ");
      for(PathedFile pf : files)
      {
        LocalService.checkDir(Config.getProperty("downloadPath") + pf.path());
        String mt = pf.file().getMimeType();
        if(mt.contains("video") || mt.contains("image"))
          if(downloadPathedFile(pf)) //DOWNLOAD DRIVE FILE.
            changed = true;
      }
      return changed;
    } catch(Exception e)
    {
      System.out.println("FAILED TO PULL DRIVE FILES.");
      e.printStackTrace();
      return false;
    }
  }
}
