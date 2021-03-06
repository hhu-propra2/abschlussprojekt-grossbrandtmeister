package mops.rheinjug2.entities;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Log4j2
@Table("student")
public class Student {
  @Id
  private Long id;

  private String login;
  private String name;
  private String email;

  private Set<EventRef> events = new HashSet<>();

  public Student(final String login, final String email) {
    this.login = login;
    this.email = email;
  }

  /**
   * Event hinzufügen.
   */
  public void addEvent(final Event event) {
    events.add(new EventRef(event.getId(), event.getDeadline()));
  }

  /**
   * Gibt alle Id's der Veranstaltungen eines Students zurück.
   */
  public Set<Long> getEventsIds() {
    return events.stream().map(EventRef::getEvent).collect(Collectors.toSet());
  }

  /**
   * Entfernt eine Veranstaltung.
   */
  public void deleteEvent(final Event event) {
    final EventRef ref = findEventRef(event);
    events.remove(ref);
  }

  /**
   * Eine Zusammenfassung hinzufügen.
   */
  public boolean addSummary(final Event event) {
    if (event.isOpenForSubmission()) {
      final EventRef ref = findEventRef(event);
      ref.setSubmittedSummary(true);
      ref.setTimeOfSubmission(LocalDateTime.now(ZoneId.of("Europe/Berlin")));
      log.info("Summary submitted.");
      return true;
    }
    log.info("For this event there is a submission.");
    return false;
  }

  public void useEventsForCP(final List<Event> events) {
    events.stream().map(this::findEventRef)
        .forEach(eventRef -> eventRef.setUsedForCertificate(true));
  }

  /**
   * Gibt alle IDs der Veranstaltungen mit Zusammenfassungen, die akzeptiert, aber
   * nicht für einen Schein verwendet wurden.
   */
  public Set<Long> getEventsIdsWithSummaryAcceptedNotUsed() {
    return events.stream().filter(EventRef::isSubmittedAndAcceptedButNotUsed)
        .map(EventRef::getEvent).collect(Collectors.toSet());
  }

  public Set<Long> getEventsIdsWithSummaryNotAccepted() {
    return events.stream().filter(EventRef::isSubmittedAndNotAccepted)
        .map(EventRef::getEvent).collect(Collectors.toSet());
  }

  public Set<Long> getEventsIdsWithSummaryAccepted() {
    return events.stream().filter(EventRef::isSubmittedAndAccepted)
        .map(EventRef::getEvent).collect(Collectors.toSet());
  }

  public Set<Long> getEventsIdsWithNoSummary() {
    return events.stream().filter(x -> !x.isSubmittedSummary())
        .map(EventRef::getEvent).collect(Collectors.toSet());
  }

  public EventRef findEventRef(final Event event) {
    return events.stream().filter(x -> x.getEvent()
        .equals(event.getId())).findAny().orElse(null);
  }

  public void setAccepted(final boolean value, final Event event) {
    final EventRef ref = findEventRef(event);
    ref.setAccepted(value);
  }

  public boolean isRegiteredForEvent(final Long eventId) {
    return events.stream().anyMatch(eventRef -> eventRef.getEvent().equals(eventId));
  }

  public boolean isEventSubmitted(final Long eventId) {
    return events.stream().anyMatch(
        eventRef -> eventRef.getEvent().equals(eventId) && eventRef.isSubmittedSummary());
  }

  /**
   * Die mithode wird von orga gebraucht um eine verspätet Abgabe zu machen.
   *
   * @param event .
   */
  public void setSubmittedAndAccepted(final Event event) {
    final EventRef ref = findEventRef(event);
    ref.setSubmittedSummary(true);
    ref.setAccepted(true);
  }

  /**
   * Ermöglicht einen Veröffentlichung der Zusammenfassung, wenn der Student zustimmt hat.
   * Default ist auf nein gesetzt.
   */
  public void addAcceptPublishingOfSummary(final Event event) {
    final EventRef ref = findEventRef(event);
    ref.setPublishSummary(true);
  }
}
