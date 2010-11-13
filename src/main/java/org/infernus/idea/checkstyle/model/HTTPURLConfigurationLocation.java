package org.infernus.idea.checkstyle.model;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NonNls;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;

/**
 * A configuration file accessible via a HTTP URL.
 */
public class HTTPURLConfigurationLocation extends ConfigurationLocation {

    @NonNls
    private static final Logger LOG = Logger.getLogger(HTTPURLConfigurationLocation.class);

    /**
     * Create a new URL configuration.
     */
    HTTPURLConfigurationLocation() {
        super(ConfigurationType.HTTP_URL);
    }

    /**
     * {@inheritDoc}
     */
    protected InputStream resolveFile() throws IOException {
        Reader reader = null;
        Writer writer = null;
        try {
            final URLConnection urlConnection = new URL(getLocation()).openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(false);

            final File tempFile = File.createTempFile("checkStyle", ".xml");
            tempFile.deleteOnExit();
            writer = new BufferedWriter(new FileWriter(tempFile));

            urlConnection.connect();
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            int readChar;
            while ((readChar = reader.read()) != -1) {
                writer.write(readChar);
            }

            writer.flush();
            return new FileInputStream(tempFile);

        } catch (IOException e) {
            LOG.error("Couldn't read URL: " + getLocation(), e);
            throw e;

        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
