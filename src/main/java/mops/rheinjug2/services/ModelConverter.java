package mops.rheinjug2.services;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;
import lombok.extern.log4j.Log4j2;
import mops.rheinjug2.meetupcom.Event;
import org.springframework.stereotype.Service;

@Service
@Log4j2
public class ModelConverter {

  public static final int DEFAULT_DEADLINE_DAYS_AFTER_EVENT = 7;

  static mops.rheinjug2.entities.Event parseMeetupEvent(
      final Event meetupEvent, final mops.rheinjug2.entities.Event eventEntity) {
    log.trace("Parsing " + meetupEvent);

    final LocalDateTime eventTime = LocalDateTime.ofInstant(
        meetupEvent.getTime(),
        ZoneId.ofOffset("UTC",
            ZoneOffset.ofHoursMinutes(
                meetupEvent.getUtcOffset().toHoursPart(),
                meetupEvent.getUtcOffset().toMinutesPart()))
    );

    // Falls die eventEntity bereits vorhanden ist nutzen wir sie, sonst erzeugen wir eine neue
    final mops.rheinjug2.entities.Event event = eventEntity != null
        ? eventEntity : new mops.rheinjug2.entities.Event();

    // Setze Felder unserer Entity auf die Werte aus der API
    event.setMeetupId(meetupEvent.getId());
    event.setTitle(meetupEvent.getName());
    event.setDescription(meetupEvent.getDescription());
    event.setPrice(meetupEvent.getFee() == null ? 0.0 : meetupEvent.getFee().getAmount());
    event.setDate(eventTime);
    event.setDuration(meetupEvent.getDuration());
    event.setDeadline(eventTime
        .plus(meetupEvent.getDuration())
        .plusDays(DEFAULT_DEADLINE_DAYS_AFTER_EVENT)
    );
    event.setAddress(meetupEvent.getVenue().getAddress1()
        + ", " + meetupEvent.getVenue().getCity());
    event.setVenue(meetupEvent.getVenue().getName());
    event.setUrl(meetupEvent.getLink());
    event.setStatus(meetupEvent.getStatus().name());
    event.setType(meetupEvent.getName().toLowerCase(Locale.GERMAN).contains("entwickelbar")
        ? "EntwickelBar" : "Abendveranstaltung");

    return event;
  }

}
