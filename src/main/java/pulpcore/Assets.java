/*
    Copyright (c) 2008, Interactive Pulp, LLC
    All rights reserved.
    
    Redistribution and use in source and binary forms, with or without 
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright 
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright 
          notice, this list of conditions and the following disclaimer in the 
          documentation and/or other materials provided with the distribution.
        * Neither the name of Interactive Pulp, LLC nor the names of its 
          contributors may be used to endorse or promote products derived from 
          this software without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
    POSSIBILITY OF SUCH DAMAGE.
*/

package pulpcore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import pulpcore.util.ByteArray;

/**
    The Assets class provided a central location to retrieve game assets
    (images, fonts, sounds, etc.) from the jar or from zip files.
*/
public class Assets {
    
    // Lock for multiple app contexts running simultaneously.
    private static final Object LOCK = new Object();
                     
    private static final Map CATALOGS = new HashMap();
    private static final Map ASSETS = new HashMap();
    
    // Prevent instantiation
    private Assets() { }
    
    /**
        Adds the contents of an asset catalog (zip file) into memory.
        <p>
        If this catalog contains an asset that has the same name as an existing asset, 
        the old asset is replaced with the new one. If this zip file contains errors,
        no existing assets are affected and this method returns false.
        
        @param zipFileData a zip file
        @return true on success; false otherwise.
    */
    public static boolean addCatalog(String catalogName, byte[] zipFileData) {
        if (zipFileData == null || zipFileData.length == 0) {
            return false;
        }
        
        return addCatalog(catalogName, new ByteArrayInputStream(zipFileData));
    }
    
    /**
        Adds the contents of an asset catalog (zip file) into memory.
        <p>
        If this catalog contains an asset that has the same name as an existing asset, 
        the old asset is replaced with the new one. If this zip file contains errors,
        no existing assets are affected and this method returns false.
        
        @param is an input stream that points to the contents of a zip file.
        @return true on success; false otherwise.
    */
    public static boolean addCatalog(String catalogName, InputStream is) {
        // NOTE: this method used by PulpCorePlayer via reflection 
        
        if (is == null) {
            return false;
        }
        
        List assetNames = new ArrayList();
        List assetData = new ArrayList();
        
        // Read zip file
        try {
            ZipInputStream in = new ZipInputStream(is);
            
            while (true) {
                ZipEntry entry = in.getNextEntry();
                if (entry == null) {
                    break;
                }
                
                int size = (int)entry.getSize();
                String entryName = entry.getName();
                byte[] entryData;
                if (size != -1) {
                    entryData = new byte[size];
                    int bytesToRead = size;
                    while (bytesToRead > 0) {
                        int bytesRead = in.read(entryData, size - bytesToRead, bytesToRead);
                        if (bytesRead == -1) {
                            if (Build.DEBUG) {
                                CoreSystem.print("Couldn't add asset (EOF reached): " + entryName);
                            }
                            in.close();
                            return false;
                        }
                        else {
                            bytesToRead -= bytesRead;
                        }
                    }
                }                                 
                else {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    while (true) {
                        int bytesRead = in.read(buffer);
                        if (bytesRead == -1) {
                            entryData = out.toByteArray();
                            break;
                        }
                        out.write(buffer, 0, bytesRead);
                    }
                }
                assetNames.add(entryName);
                assetData.add(entryData);
            }
            in.close();
        }
        catch (IOException ex) {
            if (Build.DEBUG) CoreSystem.print("Couldn't add asset catalog: " + catalogName, ex);
            return false;
        }
        
        if (assetNames.size() == 0) {
            if (Build.DEBUG) CoreSystem.print("Warning: no assets found: " + catalogName);
        }
        
        synchronized (LOCK) {
            // Remove old catalog
            removeCatalog(catalogName);
            
            // Add assets
            for (int i = 0; i < assetNames.size(); i++) {
                String name = (String)assetNames.get(i);
                byte[] data = (byte[])assetData.get(i);
                
                // Remove any asset of the same name from a different catalog
                removeAsset(name);
                
                // Add the new entry
                ASSETS.put(name, data);
            }
            
            // Add catalog name and its list of assets
            CATALOGS.put(catalogName, assetNames);
        }
        return true;        
    }
    
    /**
        Gets an iterator of catalog names (zip files) stored in memory.
    */
    public static Iterator getCatalogs() {
        // NOTE: this method used by PulpCorePlayer via reflection
        synchronized (LOCK) {
            return CATALOGS.keySet().iterator();
        }
    }

    /**
        Checks if the specified catalog name (zip file) is stored in memory.
    */
    public static boolean containsCatalog(String catalogName) {
        synchronized (LOCK) {
            return (CATALOGS.get(catalogName) != null);
        }
    }
    
    /**
        Removes all assets downloaded from the specified catalog (zip file).
    */
    public static void removeCatalog(String catalogName) {
        
        synchronized (LOCK) {
            List assetList = (List)CATALOGS.get(catalogName);
            
            if (assetList != null) {
                for (int i = 0; i < assetList.size(); i++) {
                    ASSETS.remove(assetList.get(i));
                }
            }
            
            CATALOGS.remove(catalogName);
        }
    }
    
    /**
        Gets an iterator of the names of all assets from all zip files stored in memory. 
        Does not include assets in the jar file.
    */
    public static Iterator getAssetNames() {
        synchronized (LOCK) {
            return ASSETS.keySet().iterator();
        }
    }
    
    /**
        Returns true if the specified asset in any zip file exists. Does not check the jar.
    */
    public static boolean containsAsset(String assetName) {
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }
        
        synchronized (LOCK) {
            return (ASSETS.get(assetName) != null);
        }
    }
    
    /**
        Removes a specific asset from memory.
    */
    public static void removeAsset(String assetName) {
        synchronized (LOCK) {
            ASSETS.remove(assetName);
        
            // Remove it from the catalog list
            Iterator i = CATALOGS.values().iterator();
            while (i.hasNext()) {
                List list = (List)i.next();
                for (int j = 0; j < list.size(); j++) {
                    if (assetName.equals(list.get(j))) {
                        list.remove(j);
                        j--;
                    }
                }
            }
        }
    }    
    
    /**
        Gets an asset as a {@link pulpcore.util.ByteArray}.
        <p>
        This method first checks the downloaded asset catalogs, then the jar file. For some 
        platforms (Applets), the originating server is also checked if the
        asset is not in the zip file(s) or the jar file.
        Returns null if the asset was not found.
        <p>
        The returned ByteArray's position is set to zero.
    */
    public static ByteArray get(String assetName) {
        
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }
        
        // Check loaded zip file(s)
        synchronized (LOCK) {
            byte[] assetData = (byte[])ASSETS.get(assetName);
            if (assetData != null) {
                return new ByteArray(assetData);
            }
        }
        
        // Check the jar file, then the server
        Class parentLoader = CoreSystem.getPlatform().getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);
        
        if (in == null) {
            if (Build.DEBUG) CoreSystem.print("Asset not found: " + assetName);
            return null;
        }
        else {
            ByteArray byteArray = new ByteArray();
            try {
                byteArray.write(in);
                in.close();
                byteArray.reset();
                return byteArray;
            }
            catch (IOException ex) {
                if (Build.DEBUG) CoreSystem.print("Error reading asset: " + assetName);
                return null;
            }
        }
    }
    
    /**
        Gets an asset as an {@link java.io.InputStream}.
        <p>
        This method first checks the downloaded asset catalogs, then the jar file. For some 
        platforms (Applets), the originating server is also checked if the
        asset is not in the zip file(s) or the jar file.
        Returns null if the asset was not found.
    */
    public static InputStream getAsStream(String assetName) {
        
        if (assetName.startsWith("/")) {
            assetName = assetName.substring(1);
        }
        
        // Check loaded zip file(s)
        synchronized (LOCK) {
            byte[] assetData = (byte[])ASSETS.get(assetName);
            if (assetData != null) {
                return new ByteArrayInputStream(assetData);
            }
        }
        
        // Check the jar file, then the server
        Class parentLoader = CoreSystem.getPlatform().getClass();
        InputStream in = parentLoader.getResourceAsStream("/" + assetName);
        
        if (in == null) {
            if (Build.DEBUG) CoreSystem.print("Asset not found: " + assetName);
        }
        
        return in;
    }
}