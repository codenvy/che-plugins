/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.docker.machine;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.machine.server.InstanceStateEvent;
import org.eclipse.che.plugin.docker.client.DockerConnector;
import org.eclipse.che.plugin.docker.client.MessageProcessor;
import org.eclipse.che.plugin.docker.client.json.Event;
import org.eclipse.che.plugin.docker.client.json.Filters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Track docker containers events to detect containers stop or failure.
 *
 * @author Alexander Garagatyi
 */
@Singleton
public class DockerInstanceStopDetector {
    private static final Logger LOG = LoggerFactory.getLogger(DockerInstanceStopDetector.class);

    private final EventService        eventService;
    private final DockerConnector     dockerConnector;
    private final ExecutorService     executorService;
    private final Map<String, String> instances;

    private long lastProcessedEventDate = 0;

    @Inject
    public DockerInstanceStopDetector(EventService eventService, DockerConnector dockerConnector) {
        this.eventService = eventService;
        this.dockerConnector = dockerConnector;
        this.instances = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("DockerInstanceStopDetector-%d")
                                          .setDaemon(true)
                                          .build());
    }

    /**
     * Start container stop detection.
     *
     * @param containerId
     *         id of a container to start detection for
     * @param machineId
     *         id of a machine which container implements
     */
    public void startDetection(String containerId, String machineId) {
        instances.put(containerId, machineId);
    }

    /**
     * Stop container stop detection.
     *
     * @param containerId
     *         id of a container to start detection for
     */
    public void stopDetection(String containerId) {
        instances.remove(containerId);
    }

    @PostConstruct
    private void detectContainersEvents() {
        executorService.execute(() -> {
            while (true) {
                try {
                    dockerConnector.getEvents(lastProcessedEventDate,
                                              0,
                                              new Filters().withFilter("event", "die", "oom"),
                                              new EventsProcessor());
                } catch (IOException e) {
                    // usually connection timeout
                    LOG.debug(e.getLocalizedMessage(), e);
                }
            }
        });
    }

    private class EventsProcessor implements MessageProcessor<Event> {
        @Override
        public void process(Event message) {
            InstanceStateEvent.Type instanceStateChangeType;
            switch (message.getStatus()) {
                case "oom":
                    instanceStateChangeType = InstanceStateEvent.Type.OOM;
                    break;
                case "die":
                    instanceStateChangeType = InstanceStateEvent.Type.DIE;
                    break;
                default:
                    LOG.error("Unknown state. " + message.toString());
                    return;
            }
            final String instanceId = instances.get(message.getId());
            if (instanceId != null) {
                eventService.publish(new InstanceStateEvent(instanceId, instanceStateChangeType));
                lastProcessedEventDate = message.getTime();
            }
        }
    }
}
