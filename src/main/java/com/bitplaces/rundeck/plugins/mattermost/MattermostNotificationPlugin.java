/*
 * Copyright 2014 Andrew Karpow
 * based on Mattermost Plugin from Hayden Bakkum
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.bitplaces.rundeck.plugins.mattermost;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Sends Rundeck job notification messages to a Mattermost room.
 *  Forked from https://github.com/zodman/rundeck-mattermost-incoming-webhook-plugin    (@author Zodeman)
 *                  > https://github.com/higanworks/rundeck-slack-incoming-webhook-plugin (@author Hayden Bakkum)
 * @author Matth Gyver
 */

@Plugin(service= "Notification", name="MattermostNotification")
@PluginDescription(title="Mattermost Incoming WebHook", description="Sends Rundeck Notifications to Mattermost")
public class MattermostNotificationPlugin implements NotificationPlugin {

    private static final String MATTERMOST_MESSAGE_COLOR_GREEN = "good";
    private static final String MATTERMOST_MESSAGE_COLOR_YELLOW = "warning";
    private static final String MATTERMOST_MESSAGE_COLOR_RED = "danger";

    private static final String MATTERMOST_MESSAGE_FROM_NAME = "Rundeck";
//    private static final String SLACK_EXT_MESSAGE_TEMPLATE_PATH = "/var/lib/rundeck/libext/templates";

    private static final String MATTERMOST_MESSAGE_TEMPLATE = "mattermost-incoming-message.ftl";
    // private static final String MATTERMOST_MESSAGE_TEMPLATE = "mattermost-message.ftl";


    private static final String TRIGGER_START = "start";
    private static final String TRIGGER_SUCCESS = "success";
    private static final String TRIGGER_FAILURE = "failure";

    private static final Map<String, MattermostNotificationData> TRIGGER_NOTIFICATION_DATA = new HashMap<String, MattermostNotificationData>();

    private static final Configuration FREEMARKER_CFG = new Configuration();

    @PluginProperty(title = "WebHook URL", description = "Mattermost Incoming WebHook URL", required = true)
    private String webhook_url;

    /**
     * Sends a message to a Mattermost room when a job notification event is raised by Rundeck.
     *
     * @param trigger name of job notification event causing notification
     * @param executionData job execution data
     * @param config plugin configuration
     * @throws MattermostNotificationPluginException when any error occurs sending the Mattermost message
     * @return true, if the Mattermost API response indicates a message was successfully delivered to a chat room
     */
    public boolean postNotification(String trigger, Map executionData, Map config) {

        String ACTUAL_MATTERMOST_TEMPLATE;

//        if(null != external_template && !external_template.isEmpty()) {
//            try {
//                FileTemplateLoader externalTemplate = new FileTemplateLoader(new File(SLACK_EXT_MESSAGE_TEMPLATE_PATH));
//                System.err.printf("Found external template directory. Using it.\n");
//                TemplateLoader[] loaders = new TemplateLoader[]{externalTemplate};
//                MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
//                FREEMARKER_CFG.setTemplateLoader(mtl);
//                ACTUAL_MATTERMOST_TEMPLATE = external_template;
//            } catch (Exception e) {
//                System.err.printf("No such directory: %s\n", SLACK_EXT_MESSAGE_TEMPLATE_PATH);
//                return false;
//            }
//        }else{
            ClassTemplateLoader builtInTemplate = new ClassTemplateLoader(MattermostNotificationPlugin.class, "/templates");
            TemplateLoader[] loaders = new TemplateLoader[]{builtInTemplate};
            MultiTemplateLoader mtl = new MultiTemplateLoader(loaders);
            FREEMARKER_CFG.setTemplateLoader(mtl);
            ACTUAL_MATTERMOST_TEMPLATE = MATTERMOST_MESSAGE_TEMPLATE;
//        }

        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_START,   new MattermostNotificationData(ACTUAL_MATTERMOST_TEMPLATE, MATTERMOST_MESSAGE_COLOR_YELLOW));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_SUCCESS, new MattermostNotificationData(ACTUAL_MATTERMOST_TEMPLATE, MATTERMOST_MESSAGE_COLOR_GREEN));
        TRIGGER_NOTIFICATION_DATA.put(TRIGGER_FAILURE, new MattermostNotificationData(ACTUAL_MATTERMOST_TEMPLATE, MATTERMOST_MESSAGE_COLOR_RED));

        try {
            FREEMARKER_CFG.setSetting(Configuration.CACHE_STORAGE_KEY, "strong:20, soft:250");
        }catch(Exception e){
            System.err.printf("Got and exception from Freemarker: %s", e.getMessage());
        }

        if (!TRIGGER_NOTIFICATION_DATA.containsKey(trigger)) {
            throw new IllegalArgumentException("Unknown trigger type: [" + trigger + "].");
        }

        String message = generateMessage(trigger, executionData, config);
        String mattermostResponse = invokeMattermostAPIMethod(webhook_url, message);
        String ms = "payload=" + URLEncoder.encode(message);

        if ("ok".equals(mattermostResponse)) {
            return true;
        } else {
            // Unfortunately there seems to be no way to obtain a reference to the plugin logger within notification plugins,
            // but throwing an exception will result in its message being logged.
            throw new MattermostNotificationPluginException("Unknown status returned from Mattermost API: [" + mattermostResponse + "]." + "\n" + ms);
        }
    }

    // private String generateMessage(String trigger, Map executionData, Map config, String channel) {
    private String generateMessage(String trigger, Map executionData, Map config) {
        String templateName = TRIGGER_NOTIFICATION_DATA.get(trigger).template;
        String color = TRIGGER_NOTIFICATION_DATA.get(trigger).color;

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("trigger", trigger);
        model.put("color", color);
        model.put("executionData", executionData);
        model.put("config", config);
//         model.put("channel", channel);
//        if(username != null && !username.isEmpty()) {
//            model.put("username", username);
//        }
//        if(icon_url != null && !icon_url.isEmpty()) {
//            model.put("icon_url", icon_url);
//        }
        StringWriter sw = new StringWriter();
        try {
            Template template = FREEMARKER_CFG.getTemplate(templateName);
            template.process(model,sw);

        } catch (IOException ioEx) {
            throw new MattermostNotificationPluginException("Error loading Mattermost notification message template: [" + ioEx.getMessage() + "].", ioEx);
        } catch (TemplateException templateEx) {
            throw new MattermostNotificationPluginException("Error merging Mattermost notification message template: [" + templateEx.getMessage() + "].", templateEx);
        }

        return sw.toString();
//        String mm = "{\"text\": \"This is posted from rundeck\"}";
//        return urlEncode(mm);
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new MattermostNotificationPluginException("URL encoding error: [" + unsupportedEncodingException.getMessage() + "].", unsupportedEncodingException);
        }
    }

    // private String invokeMattermostAPIMethod(String teamDomain, String token, String message) {
    private String invokeMattermostAPIMethod(String webhook_url, String message) {
        // URL requestUrl = toURL(SLACK_API_URL_SCHEMA + teamDomain + SLACK_API_BASE + SLACK_API_WEHOOK_PATH + token);
        URL requestUrl = toURL(webhook_url);

        HttpURLConnection connection = null;
        InputStream responseStream = null;
        String body = "payload=" + URLEncoder.encode(message);
        try {
            connection = openConnection(requestUrl);
            putRequestStream(connection, body);
            responseStream = getResponseStream(connection);
            return getMattermostResponse(responseStream);

        } finally {
            closeQuietly(responseStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private URL toURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException malformedURLEx) {
            throw new MattermostNotificationPluginException("Mattermost API URL is malformed: [" + malformedURLEx.getMessage() + "].", malformedURLEx);
        }
    }

    private HttpURLConnection openConnection(URL requestUrl) {
        try {
            return (HttpURLConnection) requestUrl.openConnection();
        } catch (IOException ioEx) {
            throw new MattermostNotificationPluginException("Error opening connection to Mattermost URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void putRequestStream(HttpURLConnection connection, String message) {
        try {
            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("charset", "utf-8");

            connection.setDoInput(true);
            connection.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(message);
            wr.flush();
            wr.close();
        } catch (IOException ioEx) {
            throw new MattermostNotificationPluginException("Error putting data to Mattermost URL: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private InputStream getResponseStream(HttpURLConnection connection) {
        InputStream input = null;
        try {
            input = connection.getInputStream();
        } catch (IOException ioEx) {
            input = connection.getErrorStream();
        }
        return input;
    }

    private int getResponseCode(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException ioEx) {
            throw new MattermostNotificationPluginException("Failed to obtain HTTP response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private String getMattermostResponse(InputStream responseStream) {
        try {
            return new Scanner(responseStream,"UTF-8").useDelimiter("\\A").next();
        } catch (Exception ioEx) {
            throw new MattermostNotificationPluginException("Error reading Mattermost API JSON response: [" + ioEx.getMessage() + "].", ioEx);
        }
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException ioEx) {
                // ignore
            }
        }
    }

    private static class MattermostNotificationData {
        private String template;
        private String color;
        public MattermostNotificationData(String template, String color) {
            this.color = color;
            this.template = template;
        }
    }

}
