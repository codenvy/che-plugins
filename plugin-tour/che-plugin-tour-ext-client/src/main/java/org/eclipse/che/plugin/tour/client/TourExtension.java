/*******************************************************************************
 * Copyright (c) 2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.tour.client;

import org.eclipse.che.api.project.gwt.client.ProjectServiceClient;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.event.ProjectActionEvent;
import org.eclipse.che.ide.api.event.ProjectActionHandler;
import org.eclipse.che.ide.api.extension.Extension;
import org.eclipse.che.ide.api.notification.Notification;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.StringUnmarshaller;
import org.eclipse.che.plugin.tour.client.lifecycle.GuidedTourLifeCycle;
import org.eclipse.che.plugin.tour.client.log.Log;
import org.eclipse.che.plugin.tour.client.tour.GuidedTour;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;

import java.util.List;
import java.util.Map;

import static org.eclipse.che.ide.api.notification.Notification.Type.ERROR;

/**
 * Defines the tour extension that will load a guided tour and display steps to the user
 *
 * @author Florent Benoit
 */
@Extension(title = "Guided Tour")
public class TourExtension implements GuidedTourLifeCycle {

    /**
     * Logger.
     */
    @Inject
    private Log log;

    /**
     * Application context.
     */
    @Inject
    private AppContext appContext;

    /**
     * Notification Manager used to send events in order to notify the user.
     */
    @Inject
    private NotificationManager notificationManager;

    /**
     * Project API client used to get file content.
     */
    @Inject
    private ProjectServiceClient projectServiceClient;

    /**
     * Loop has been initialized ?
     */
    private boolean scheduledLoop = false;

    /**
     * Repeating timer
     */
    private Timer repeatingTimer;

    /**
     * Guided tour.
     */
    private GuidedTour guidedTour;

    /**
     * Initialize the extension by adding
     * -  hook when project is opened
     * - hook with repeat timer
     *
     * @param eventBus
     *         the bus used to add the handler
     */
    @Inject
    public TourExtension(EventBus eventBus) {

        // Initialize the tour when project is opened
        eventBus.addHandler(ProjectActionEvent.TYPE, new ProjectActionHandler() {
            @Override
            public void onProjectCreated(ProjectActionEvent event) {
                initTour(event);
            }

            @Override
            public void onProjectDeleted(ProjectActionEvent event) {

            }
        });


    }

    /**
     * Initialize the tour by removing any previous tour
     * @param event the loading event
     */
    protected void initTour(ProjectActionEvent event) {

        // load the data
        loadData(event);
    }


    /**
     * Download and parse the given URL
     * @param url the URL containing JSON file to be analyzed
     */
    protected void remoteFetch(String url) {
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
        try {
            rb.setCallback(new ParseJsonFileCallback());
            rb.send();

        } catch (RequestException e) {
            notificationManager.showNotification(new Notification("Unable to get tour" + e.getMessage(), ERROR));
        }
    }

    /**
     * Load the data for the given project by picking up the attributes or checking for a given file in the project.
     * @param event the load event
     */
    protected void loadData(ProjectActionEvent event) {

        Map<String, List<String>> attributes = event.getProject().getAttributes();

        // attribute ?
        if (attributes != null && attributes.containsKey("codenvyGuidedTour")) {
            log.info("CodenvyTour has been found in the project attributes");
            remoteFetch(attributes.get("codenvyGuidedTour").get(0));
        } else {
            // check if there is a file named "CodenvyGuidedTour.json"
            final CurrentProject currentProject = appContext.getCurrentProject();

            projectServiceClient.getFileContent(currentProject.getRootProject().getPath() + "/CodenvyGuidedTour.json",
                                                new AsyncRequestCallback<String>(new StringUnmarshaller()) {
                                                    @Override
                                                    protected void onSuccess(String result) {
                                                        log.info("CodenvyTour has been found in the project files");
                                                        if (result != null) {
                                                            loadJsonData(result);
                                                        }
                                                    }

                                                    @Override
                                                    protected void onFailure(Throwable exception) {
                                                        // no file, so do nothing
                                                    }
                                                });
        }

    }

    /**
     * Init the schedule loop
     */
    protected void initLoop() {
        // Schedule the timer to run once in 1 second.
        repeatingTimer.scheduleRepeating(1000);

        scheduledLoop = true;

    }

    /**
     * Loads the given JSON data
     * @param json the JSON data
     */
    protected void loadJsonData(String json) {

        if (!scheduledLoop) {
            initLoop();
        }

        // reset the tour steps
        guidedTour.start(json);

    }


    /**
     * Called when there is no more steps to be displayed.
     */
    @Override
    public void end() {
        // Stop timer
        repeatingTimer.cancel();
    }

    /**
     * Timer used to run check tour operation
     */
    private static class CheckTourTimer extends Timer {

        /**
         * Guided tour.
         */
        private final GuidedTour guidedTour;

        /**
         * Build instance of timer around the given tour
         * @param guidedTour the guided tour to use
         */
        public CheckTourTimer(GuidedTour guidedTour) {
            this.guidedTour = guidedTour;
        }

        /**
         * Checks the tour ech time it is invoked
         */
        @Override
        public void run() {
            guidedTour.checkTour();
        }
    }


    /**
     * Callback used to parse the json data once the file has been retrieved
     */
    private class ParseJsonFileCallback implements RequestCallback {
        @Override
        public void onResponseReceived(Request request, Response response) {
            loadJsonData(response.getText());
        }

        @Override
        public void onError(Request request, Throwable exception) {
            notificationManager
                    .showNotification(new Notification("Error occurred when parsing the JSON file" + exception.getMessage(), ERROR));
        }
    }

    /**
     * When guided tour is injected, register callbacks and timer on it
     * @param guidedTour the instance of the tour
     */
    @Inject
    public void setGuidedTour(final GuidedTour guidedTour) {
        this.guidedTour = guidedTour;

        // register callback
        guidedTour.addCallback(this);

        // Check tour
        this.repeatingTimer = new CheckTourTimer(guidedTour);
    }


}
