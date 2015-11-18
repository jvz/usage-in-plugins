package org.jenkinsci.deprecatedusage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

public class UpdateCenter {
    private static final String UPDATE_CENTER_URL = "http://updates.jenkins-ci.org/update-center.json";

    private final JenkinsFile core;
    private final List<JenkinsFile> plugins = new ArrayList<>();

    public UpdateCenter() throws IOException, ParserConfigurationException, SAXException {
        super();
        final String string = getUpdateCenterJson();

        final JSONObject jsonRoot = new JSONObject(string);
        final JSONObject jsonCore = jsonRoot.getJSONObject("core");
        core = parse(jsonCore);

        final JSONObject jsonPlugins = jsonRoot.getJSONObject("plugins");
        for (final Object pluginId : jsonPlugins.keySet()) {
            final JSONObject jsonPlugin = jsonPlugins.getJSONObject(pluginId.toString());
            final JenkinsFile plugin = parse(jsonPlugin);
            plugins.add(plugin);
        }
    }

    private String getUpdateCenterJson() throws IOException, MalformedURLException {
        final byte[] updateCenterData = new HttpGet(new URL(UPDATE_CENTER_URL)).read();
        final String string = new String(updateCenterData, StandardCharsets.UTF_8).replace(
                "updateCenter.post(", "");
        return string;
    }

    private JenkinsFile parse(JSONObject jsonObject) throws MalformedURLException, JSONException {
        return new JenkinsFile(jsonObject.getString("name"), jsonObject.getString("version"),
                jsonObject.getString("url"));
    }

    public void download() throws Exception {
        // download in parallel
        core.startDownloadIfNotExists();
        for (final JenkinsFile plugin : plugins) {
            plugin.startDownloadIfNotExists();
        }
        // wait end of downloads
        core.waitDownload();
        for (final JenkinsFile plugin : new ArrayList<>(plugins)) {
            try {
                plugin.waitDownload();
            } catch (final FileNotFoundException e) {
                Log.log(e.toString());
                plugins.remove(plugin);
            }
        }
    }

    public JenkinsFile getCore() {
        return core;
    }

    public List<JenkinsFile> getPlugins() {
        return plugins;
    }
}