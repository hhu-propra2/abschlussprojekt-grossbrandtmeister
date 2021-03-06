package mops.rheinjug2.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mops.rheinjug2.entities.Event;
import mops.rheinjug2.entities.EventRef;
import mops.rheinjug2.entities.Student;
import mops.rheinjug2.repositories.EventRepository;
import mops.rheinjug2.repositories.StudentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.OidcKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@DataJdbcTest
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ModelServiceTest {

  @Autowired
  private transient EventRepository eventRepository;
  @Autowired
  private transient StudentRepository studentRepository;

  private transient ModelService modelService;

  @Mock
  private static KeycloakAuthenticationToken keycloakAuthenticationToken;

  @Mock
  private static OidcKeycloakAccount keycloakAccount;

  @Mock
  private static KeycloakPrincipal<KeycloakSecurityContext> keycloakPrincipal;

  @Mock
  private static KeycloakSecurityContext keycloakSecurityContext;

  @Mock
  private static AccessToken accessToken;

  private static final Set<String> roles = new HashSet<>();

  @BeforeEach
  public void init() {
    modelService = new ModelService(studentRepository, eventRepository);
  }

  @Test
  public void testGetAllEvents() {
    final Event event1 = createAndSaveEvent("Event 1.0");
    event1.setDate(LocalDateTime.now());
    final Event event2 = createAndSaveEvent("Event 2.0");
    event2.setDate(LocalDateTime.now());
    eventRepository.saveAll(List.of(event1, event2));
    final List<Event> allEvents = modelService.getAllEvents();
    assertThat(allEvents).containsExactlyInAnyOrder(event2, event1);
  }

  @Test
  public void testAddStudentToEventIfStudentNotExistsInDatabase() {
    final Event event = createAndSaveEvent("Event 1.3");
    final Student savedStudent =
        modelService.addStudentToEvent("testLogin3", "test3@hhu.de", event.getId());
    assertThat(savedStudent.getEventsIds()).containsExactly(event.getId());
  }

  @Test
  public void testAddStudentToEventIfStudentExistsInDatabase() {
    createAndSaveStudent("testLogin5", "test5@hhu.de");
    final Event event = createAndSaveEvent("Event 1.4");
    final Student savedStudent =
        modelService.addStudentToEvent("testLogin5", "test4@hhu.de", event.getId());
    assertThat(savedStudent.getEventsIds()).containsExactly(event.getId());
  }

  @Test
  public void testGetAdmissionOfSummaryPublicationIsTrue() {
    final Event event1 = createAndSaveEvent("Event 5");
    event1.setType("Entwickelbar");
    eventRepository.saveAll(List.of(event1));
    final Student student = createAndSaveStudent("testLogin", "test5@hhu.de");
    addEventsToStudent(List.of(event1), student);
    student.addSummary(event1);
    studentRepository.save(student);

    modelService.addPublishingIsPossible(student.getLogin(), event1.getId());

    final Optional<Student> studentTest = studentRepository.findById(student.getId());
    final EventRef summary = studentTest.get().findEventRef(event1);

    assertTrue(summary.isPublishSummary());
  }

  @Test
  public void testGetAdmissionOfSummaryPublicationIsDEfaultFalse() {
    final Event event1 = createAndSaveEvent("Event 5");
    event1.setType("Entwickelbar");
    eventRepository.saveAll(List.of(event1));
    final Student student = createAndSaveStudent("testLogin", "test5@hhu.de");
    addEventsToStudent(List.of(event1), student);
    student.addSummary(event1);
    studentRepository.save(student);

    final Optional<Student> studentTest = studentRepository.findById(student.getId());
    final EventRef summary = studentTest.get().findEventRef(event1);

    assertEquals(false, summary.isPublishSummary());
  }


  @Test
  public void testGetAllEventsForCP() {
    final Event event1 = createAndSaveEvent("Event 5");
    final Event event2 = createAndSaveEvent("Event 6");
    event1.setDate(LocalDateTime.now());
    event2.setDate(LocalDateTime.now());
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    student.addEvent(event1);
    student.addEvent(event2);
    student.addSummary(event1);
    student.addSummary(event2);
    student.useEventsForCP(List.of(event1));
    studentRepository.save(student);
    modelService.acceptSummary(event2.getId(), student.getLogin());

    assertThat(modelService.getAllEventsForCP("testLogin5")).containsExactly(event2);

  }

  @Test
  public void testSubmitSummary() {
    final Event event1 = createAndSaveEvent("Event 5");
    final Event event2 = createAndSaveEvent("Event 6");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    student.addEvent(event1);
    student.addEvent(event2);
    studentRepository.save(student);
    modelService.submitSummary("testLogin5", event1.getId());
    final Student savedStudent = modelService.submitSummary("testLogin5", event2.getId());
    assertThat(savedStudent.getEventsIdsWithSummaryNotAccepted())
        .containsExactly(event1.getId(), event2.getId());
  }

  @Test
  public void testGetAllEventsPerStudent() {
    final Event eventUpcoming = createAndSaveEvent("Veranstaltung Java 1");
    eventUpcoming.setDate(LocalDateTime.now().plusDays(1));
    eventUpcoming.setStatus("Upcoming");

    final Event eventOpen = createAndSaveEvent("Veranstaltung Java 2");
    eventOpen.setDate(LocalDateTime.now());
    eventOpen.setStatus("Past");

    final Event eventPassed = createAndSaveEvent("Veranstaltung Java 3");
    eventPassed.setDate(LocalDateTime.of(2020, 1, 2, 12, 20));
    eventPassed.setDeadline(eventPassed.getDate().plusDays(7));
    eventPassed.setStatus("Past");

    final Event eventWithSubmissionNotAccepted = createAndSaveEvent("Veranstaltung Java 4");
    eventWithSubmissionNotAccepted.setDate(LocalDateTime.now());
    eventWithSubmissionNotAccepted.setStatus("Past");

    final Event eventWithSubmissionAccepted = createAndSaveEvent("Veranstaltung Java 5");
    eventWithSubmissionAccepted.setDate(LocalDateTime.now());
    eventWithSubmissionAccepted.setStatus("Past");

    final List<Event> events = List.of(eventOpen, eventPassed, eventUpcoming,
        eventWithSubmissionAccepted, eventWithSubmissionNotAccepted);
    eventRepository.saveAll(events);

    final Student student = createAndSaveStudent("ll100", "ll@hhu.de");
    addEventsToStudent(events, student);
    student.addSummary(eventWithSubmissionAccepted);
    student.addSummary(eventWithSubmissionNotAccepted);
    studentRepository.save(student);
    modelService.acceptSummary(eventWithSubmissionAccepted.getId(), "ll100");

    final Map<Event, SubmissionStatus> allEvents =
        modelService.getAllEventsPerStudent(student.getLogin());

    assertThat(allEvents).containsOnly(entry(eventUpcoming, SubmissionStatus.UPCOMING),
        entry(eventOpen, SubmissionStatus.OPEN_FOR_SUBMISSION),
        entry(eventPassed, SubmissionStatus.NO_SUBMISSION),
        entry(eventWithSubmissionAccepted, SubmissionStatus.SUBMITTED_ACCEPTED),
        entry(eventWithSubmissionNotAccepted,
            SubmissionStatus.SUBMITTED_NOT_ACCEPTED));

  }

  @Test
  public void testGetAllEventIdsPerStudent() {

    final Event eventUpcoming = createAndSaveEvent("Veranstaltung Java");
    eventUpcoming.setDate(LocalDateTime.now().plusDays(1));
    eventUpcoming.setStatus("Upcoming");

    final Event eventOpen = createAndSaveEvent("Veranstaltung Java2");
    eventOpen.setDate(LocalDateTime.now());
    eventOpen.setStatus("Past");

    final List<Event> events = List.of(eventOpen, eventUpcoming);
    eventRepository.saveAll(events);

    final Student student = createAndSaveStudent("test120", "test120@hhu.de");
    addEventsToStudent(events, student);
    studentRepository.save(student);

    assertThat(modelService.getAllEventIdsPerStudent(student.getLogin()))
        .containsExactlyInAnyOrder(events.get(0).getId(), events.get(1).getId());

  }

  @Test
  public void testGetCertificateEntwickelbar() {
    final Event event1 = createAndSaveEvent("Event 5");
    final Event event2 = createAndSaveEvent("Event 6");
    event1.setType("Entwickelbar");
    event2.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2), student);
    student.addSummary(event1);
    student.addSummary(event2);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());

    assertThat(modelService.useEventsForCertificate(student.getLogin())).isTrue();

  }

  @Test
  public void testGetCertificateThreeNormalEvents() {
    final Event event1 = createAndSaveEvent("Event 5");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 6");
    event2.setType("Normal");
    final Event event3 = createAndSaveEvent("Event 7");
    event3.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2, event3));

    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2, event3), student);
    student.addSummary(event1);
    student.addSummary(event2);
    student.addSummary(event3);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());
    modelService.acceptSummary(event3.getId(), student.getLogin());

    assertThat(modelService.useEventsForCertificate(student.getLogin())).isTrue();
  }

  @Test
  public void testGetCertificateNotEnoughEvents() {
    final Event event1 = createAndSaveEvent("Event 5");
    final Event event2 = createAndSaveEvent("Event 6");
    event1.setType("Normal");
    event2.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2), student);
    student.addSummary(event1);
    student.addSummary(event2);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());

    assertThat(modelService.useEventsForCertificate(student.getLogin())).isFalse();
  }

  @Test
  public void testCheckEventsForCertificateEntwickelbar() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Entwickelbar");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2), student);
    student.addSummary(event1);
    student.addSummary(event2);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());

    assertThat(modelService.checkEventsForCertificate(student.getLogin()))
        .isTrue();
  }

  @Test
  public void testCheckEventsForCertificateThreeEveningEvents() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("Normal");
    final Event event3 = createAndSaveEvent("Event 3");
    event3.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2, event3));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2, event3), student);
    student.addSummary(event1);
    student.addSummary(event2);
    student.addSummary(event3);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());
    modelService.acceptSummary(event3.getId(), student.getLogin());

    assertThat(modelService.checkEventsForCertificate(student.getLogin()))
        .isTrue();
  }

  @Test
  public void testCheckEventsForCertificateNotEnoughEvents() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2), student);
    student.addSummary(event1);
    student.addSummary(event2);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());

    assertThat(modelService.checkEventsForCertificate(student.getLogin()))
        .isFalse();
  }

  @Test
  public void testGetEventsForCertificateEntwickelBarAvailable() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("EntwickelBar");
    final Event event3 = createAndSaveEvent("Event 3");
    event3.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2, event3));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2, event3), student);
    student.addSummary(event1);
    student.addSummary(event2);
    student.addSummary(event3);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());
    modelService.acceptSummary(event3.getId(), student.getLogin());

    final List<Event> usableEvents = modelService.getEventsForCertificate(student.getLogin());

    assertThat(usableEvents)
        .containsExactly(event2);
  }

  @Test
  public void testGetEventsForCertificateMoreThanThreeEveningEventsAvailable() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("Normal");
    final Event event3 = createAndSaveEvent("Event 3");
    event3.setType("Normal");
    final Event event4 = createAndSaveEvent("Event 4");
    event4.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2, event3, event4));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2, event3, event4), student);
    student.addSummary(event1);
    student.addSummary(event2);
    student.addSummary(event3);
    student.addSummary(event4);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());
    modelService.acceptSummary(event3.getId(), student.getLogin());
    modelService.acceptSummary(event4.getId(), student.getLogin());


    final List<Event> usableEvents = modelService.getEventsForCertificate(student.getLogin());

    assertThat(usableEvents)
        .containsAnyElementsOf(List.of(event1, event2, event3, event4))
        .hasSize(3);
  }

  @Test
  public void testGetEventsForCertificateNotEnoughEvents() {
    final Event event1 = createAndSaveEvent("Event 1");
    event1.setType("Normal");
    final Event event2 = createAndSaveEvent("Event 2");
    event2.setType("Normal");
    eventRepository.saveAll(List.of(event1, event2));
    final Student student = createAndSaveStudent("testLogin5", "test5@hhu.de");
    addEventsToStudent(List.of(event1, event2), student);
    student.addSummary(event1);
    student.addSummary(event2);
    studentRepository.save(student);
    modelService.acceptSummary(event1.getId(), student.getLogin());
    modelService.acceptSummary(event2.getId(), student.getLogin());

    final List<Event> usableEvents = modelService.getEventsForCertificate(student.getLogin());

    assertThat(usableEvents)
        .isEmpty();
  }

  @AfterEach
  public void cleanUpEach() {
    eventRepository.deleteAll();
    studentRepository.deleteAll();
  }

  private Student createAndSaveStudent(final String login, final String email) {
    final Student student = new Student(login, email);
    studentRepository.save(student);
    return student;
  }

  private Event createAndSaveEvent(final String title) {
    final Event event = new Event();
    event.setTitle(title);
    event.setDate(LocalDateTime.now());
    event.setDuration(Duration.ZERO);
    event.setDeadline(LocalDateTime.now().plusDays(7));
    eventRepository.save(event);
    return event;
  }

  private void addEventsToStudent(final List<Event> events, final Student student) {
    events.forEach(student::addEvent);
    studentRepository.save(student);
  }

}
