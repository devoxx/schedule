package org.vaadin.devoxx2k10.ui.calendar;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.vaadin.devoxx2k10.DevoxxScheduleApplication;
import org.vaadin.devoxx2k10.data.RestApiFacade;
import org.vaadin.devoxx2k10.data.domain.DevoxxPresentation;
import org.vaadin.devoxx2k10.data.domain.MyScheduleUser;

import com.vaadin.addon.calendar.event.BasicEventProvider;
import com.vaadin.addon.calendar.event.CalendarEvent;

public class DevoxxEventProvider extends BasicEventProvider {

    private static final long serialVersionUID = -6066313242075569496L;

    private transient final Logger logger = Logger.getLogger(getClass());
    private boolean eventsLoaded;
    private DevoxxPresentation selectedEvent;

    private static final long SHORT_EVENT_THRESHOLD_MS = 1000 * 60 * 30;

    @Override
    public List<CalendarEvent> getEvents(final Date startDate, final Date endDate) {
        loadEventsFromBackendIfNeeded();

        final List<CalendarEvent> result = super.getEvents(startDate, endDate);

        // Update the selected style name.
        for (final CalendarEvent event : eventList) {
            if (event instanceof DevoxxCalendarEvent) {
                final DevoxxCalendarEvent devoxxCalEvent = ((DevoxxCalendarEvent) event);
                if (devoxxCalEvent.getDevoxxEvent().equals(selectedEvent)) {
                    devoxxCalEvent.addStyleName("selected");
                } else {
                    devoxxCalEvent.removeStyleName("selected");
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Returning " + result.size() + " events for " + startDate + " - " + endDate);
        }

        return result;
    }

    public CalendarEvent getEvent(final int id) {
        loadEventsFromBackendIfNeeded();

        for (final CalendarEvent event : eventList) {
            if (event instanceof DevoxxCalendarEvent && ((DevoxxCalendarEvent) event).getDevoxxEvent().getId() == id) {
                return event;
            }
        }

        return null;
    }

    public void refreshAttendingStyles() {
        final MyScheduleUser user = (MyScheduleUser) DevoxxScheduleApplication.getCurrentInstance().getUser();

        for (final CalendarEvent event : eventList) {
            if (event instanceof DevoxxCalendarEvent) {
                final DevoxxCalendarEvent devoxxEvent = (DevoxxCalendarEvent) event;
                if (user != null && user.hasFavourited(devoxxEvent.getDevoxxEvent())) {
                    devoxxEvent.addStyleName("attending");
                } else {
                    devoxxEvent.removeStyleName("attending");
                }
            }
        }
    }

    private void loadEventsFromBackendIfNeeded() {
        if (eventsLoaded) {
            // already loaded -> do nothing
            return;
        }

        final RestApiFacade facade = DevoxxScheduleApplication.getCurrentInstance().getBackendFacade();
        final List<DevoxxPresentation> schedule = facade.getFullSchedule();

        // wrap data from the model into CalendarEvents for UI
        for (final DevoxxPresentation event : schedule) {
            final DevoxxCalendarEvent calEvent = new DevoxxCalendarEvent();
            calEvent.setStyleName(event.getKind().name().toLowerCase());
            calEvent.addStyleName("at-" + event.getRoom().toLowerCase().replaceAll(" ", "").replaceAll("/", ""));
            if (isShortEvent(event)) {
                calEvent.addStyleName("short-event");
            }
            calEvent.setDevoxxEvent(event);
            calEvent.addListener(this);
            super.addEvent(calEvent);
        }
        eventsLoaded = true;
        refreshAttendingStyles();

        if (logger.isDebugEnabled()) {
            logger.debug("Fetched schedule from backend (total " + schedule.size() + " events).");
        }
    }

    private static boolean isShortEvent(final DevoxxPresentation event) {
        return event.getToTime().getTime() - event.getFromTime().getTime() < SHORT_EVENT_THRESHOLD_MS;
    }

    public void setSelectedPresentation(final DevoxxPresentation event) {
        selectedEvent = event;
    }
}
