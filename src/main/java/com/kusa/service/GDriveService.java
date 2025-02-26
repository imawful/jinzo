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
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import com.kusa.Config;

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
   
  private Drive drive;
  private String jinzoId; // id of folder we need. 
  private static final String googleAppName = "Jinzo";
  private static final String MAIN_FOLDER_NAME = "JINZO";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  private File jinzoFolder;
  private File videoFolder;
  private File photoFolder;

  //we only use read only. app only needs to be able to download the videos.
  private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE_READONLY);

  /* we specify the paths used for storing the apps data + login tokens.
   * 'tokens/' locally stored login tokens (drive user's tokens)
   * '/credentials' this file contains the app's credentials, comes from google dev console.
   */
  private static final String TOKEN_STORAGE_PATH = Config.getProperty("tokenStoragePath");
  private static final String CREDENTIALS_FILE_PATH = Config.getProperty("googleCredentialsPath");



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
   */
  public GDriveService()
  {
    try
    {
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      drive = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(googleAppName).build();
      initFolders();
    }
    catch(Exception e)
    {
      System.out.println("Failed to launch gdrive service." + e);
    }
  }

  //videos folder and photos folder needed to in the drive so we can perform downloads.
  //TODO handle failure properly.
  private void initFolders()
  {
    try{
      //setPageSize(pageSize).   nextPageToken
      FileList result = drive.files().list().setQ("mimeType = 'application/vnd.google-apps.folder'").setFields("files(id, name)").execute();
      List<File> files = result.getFiles();
      for(File file : files)
      {
        if(file.getName().equals(MAIN_FOLDER_NAME))
        {
          jinzoFolder = file;
          videoFolder = null;
          photoFolder = null;
        }
      }
      FileList result2 = drive.files().list().setQ("'" + jinzoFolder.getId() + "' in parents").setFields("files(id, name, parents)").execute();
      for(File file : result2.getFiles())
      {
        if(file.getName().equals("videos") && videoFolder == null)
        {
          videoFolder = file; 
        }
      }

      FileList result3 = drive.files().list().setQ("'" + jinzoFolder.getId() + "' in parents").setFields("files(id, name, parents)").execute();
      for(File file : result3.getFiles())
      {
        if(file.getName().equals("photos") && photoFolder == null)
        {
          photoFolder = file;
        }
      }
    }
    catch(Exception e)
    {
      System.out.println("GDS FAILURE: FAILED TO FIND NEEDED FOLDERS IN DRIVEE!!!");
      e.printStackTrace();
      return;
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
    return credential;
  }

  /**
   * Gets a list of files from the drive.
   *
   * we are handling the exception internally right now
   * idk if thats the best approach but if any failure occurs
   * it will return an empty list.
   *
   * @param pageSize number of pages to retrieve.
   * @return List of google drive files.
   */
  public List<File> getFileList(int pageSize)
  {
    try
    {
      //setPageSize(pageSize).   nextPageToken
      FileList result = drive.files().list().setFields("files(id, name, parents, description)").execute();
      //Google drive File
      List<File> files = result.getFiles();
      return files;
    }
    catch(Exception e)
    {
      System.out.println("GDriveService: Failed to get files." + e); 
      return Collections.emptyList();
    }
  }

  /**
   * Gets the videos for app as a list of files.
   *
   * this will return google drive files of
   * files in the drive under MAIN_FOLDER_NAME/videos/ in the drive.
   *
   * if it fails it will return an empty list.
   *
   * DONT confuse this File with java.io.File!
   *
   * @return list of videos for app as google drive files or empty list.
   */
  public List<File> getVideoFiles()
  {
    try
    {

      FileList result = drive.files().list().setQ("'" + videoFolder.getId() + "' in parents").setFields("files(id, name)").execute();
      return result.getFiles() == null ? Collections.emptyList() : result.getFiles();
    }
    catch(Exception e)
    {
      System.out.println("GDriveService: Failed to get main folder name with the current id." + e); 
      System.out.println("FILE ID: " + jinzoId);
      return Collections.emptyList();
    }
  }

  public List<File> getPhotoFiles()
  {
    try
    {
      FileList result = drive.files().list().setQ("'" + photoFolder.getId() + "' in parents").setFields("files(id, name)").execute();
      return result.getFiles() == null ? Collections.emptyList() : result.getFiles();
    }
    catch(Exception e)
    {
      System.out.println("GDS FAILURE: Failed to get photo folder files!!");
      e.printStackTrace();
      return Collections.emptyList();
    }
  }

  /**
   * Downlaods a file to the applications media folder given a google drive file id.
   *
   * the applications media folder is specified in application.properties downloadPath
   * property.
   *
   * if you provide a folder paramter you must ensure that the directory exists!
   * you must also be sure to include a / at the end.
   *
   * for example gds.downloadFile(someId, "videos/");
   *
   * if the folder paramter is not provided it will download directly into 
   * the media folder.
   *
   *
   * @param driveFileId id of the file in the drive.
   * @param folder sub directory you want to place file in. 
   */
  public void downloadFile(String driveFileId) { downloadFile(driveFileId, ""); }
  public void downloadFile(String driveFileId, String folder)
  {
    try
    {
      File file = drive.files().get(driveFileId).execute();
      if(new java.io.File(Config.getProperty("downloadPath") + folder + file.getName()).exists())
      {
        System.out.println("Skipping this download..." + file.getName());
        return;
      }
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      drive.files().get(driveFileId).executeMediaAndDownloadTo(os);
      FileOutputStream fos = new FileOutputStream(Config.getProperty("downloadPath") + folder + file.getName());
      os.writeTo(fos);
    }
    catch(Exception e)
    {
      System.out.println("download failed! " + e);
      return;
    }
  }
}
