/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.microprofile.config.source;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Config Source that reads properties from files from a directory
 * where filename is the property name and file contents is the property value.
 * @since 4.1.181
 * @author steve
 */
public class SecretsDirConfigSource extends PayaraConfigSource implements ConfigSource {

    private Path secretsDir;
    private HashMap<String, String> properties;
    private HashMap<String, FileTime> storedModifiedTimes;

    public SecretsDirConfigSource() {
        findFile();
        loadProperties();
    }
    
    SecretsDirConfigSource(Path directory) {
        super(true);
        secretsDir = directory;
        loadProperties();

    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public int getOrdinal() {
        return configService.getMPConfig().getSecretDirOrdinality();
    }

    @Override
    public String getValue(String property) {
        String result = properties.get(property);
        if (result != null) {
            try {
                // check the last modified time
                FileTime ft = storedModifiedTimes.get(property);
                Path path = Paths.get(secretsDir.toString(), property);
                if (Files.exists(path) && Files.getLastModifiedTime(path).compareTo(ft) > 0) {
                    // file  has been modified since last check
                    result = readFile(property);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    properties.put(property, result);
                }
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // check whether there is a file there now as there wasn't before
            Path path = Paths.get(secretsDir.toString(), property);
            if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
                try {
                    result = readFile(property);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    properties.put(property, result);
                } catch (IOException ex) {
                    Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "Secrets Directory";
    }

    private void findFile() {
        secretsDir = Paths.get(configService.getMPConfig().getSecretDir());

        if (!Files.exists(secretsDir) || !Files.isDirectory(secretsDir) || !Files.isReadable(secretsDir)) {
            // let's try it relative to server environment root
            String instancePath = System.getProperty("com.sun.aas.instanceRoot");
            Path test = Paths.get(instancePath, secretsDir.toString());
            if (Files.exists(test) && Files.isDirectory(test) && Files.isReadable(test)) {
                secretsDir = test;
            }
        }
    }

    private String readFile(String name) {
        String result = null;
        if (Files.exists(secretsDir) && Files.isDirectory(secretsDir) && Files.isReadable(secretsDir)) {
            try {
                Path file = Paths.get(secretsDir.toString(), name);
                if (Files.exists(file) && Files.isReadable(file)) {
                    StringBuilder collector = new StringBuilder();
                    for (String line : Files.readAllLines(file)) {
                        collector.append(line);
                    }
                    result = collector.toString();
                }
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    private void loadProperties() {
        properties = new HashMap<>();
        storedModifiedTimes = new HashMap<>();
        if (Files.exists(secretsDir) && Files.isDirectory(secretsDir) && Files.isReadable(secretsDir)) {
            File files[] = secretsDir.toFile().listFiles();
            for (File file : files) {
                try {
                    if (file.isFile() && file.canRead()) {
                        properties.put(file.getName(), readFile(file.getName()));
                        storedModifiedTimes.put(file.getName(), Files.getLastModifiedTime(file.toPath()));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, "Unable to read file in the directory", ex);
                }
            }
        }
    }

}
