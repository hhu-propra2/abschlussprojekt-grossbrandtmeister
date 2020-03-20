package mops.rheinjug2.controllers;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Collectors;
import mops.rheinjug2.Account;
import mops.rheinjug2.AccountCreator;
import mops.rheinjug2.entities.Event;
import mops.rheinjug2.fileupload.FileService;
import mops.rheinjug2.fileupload.Summary;
import mops.rheinjug2.repositories.EventRepository;
import mops.rheinjug2.repositories.StudentRepository;
import mops.rheinjug2.services.ModelService;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Secured({"ROLE_studentin"})
@RequestMapping("/rheinjug2/student")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class StudentController {

  private final transient Counter authenticatedAccess;
  private final transient ModelService modelService;


  transient FileService fileService;

  public StudentController(final MeterRegistry registry, final FileService fileService,
                           final ModelService modelService) {
    this.fileService = fileService;
    this.modelService = modelService;
    authenticatedAccess = registry.counter("access.authenticated");
  }

  /**
   * Event Übersicht für Studenten.
   */
  @GetMapping("/events")
  public String getEvents(final KeycloakAuthenticationToken token, final Model model) {
    model.addAttribute("account", AccountCreator.createAccountFromPrincipal(token));
    model.addAttribute("events", modelService.getAllEvents().stream().sorted(Comparator.comparing(Event::getDate).reversed()).collect(Collectors.toList()));
    authenticatedAccess.increment();
    return "student_events_overview";
  }

  /**
   * Übersicht der Events für die der aktuelle Student angemeldet war/ist.
   */
  @GetMapping("/visitedevents")
  public String getPersonal(final KeycloakAuthenticationToken token, final Model model) {
    final Account account = AccountCreator.createAccountFromPrincipal(token);
//  final long eventId = 1;
//  modelService.addStudentToEvent(account.getName(), account.getEmail(), eventId);
    model.addAttribute("account", account);
    model.addAttribute("exists", modelService.studentExists(account.getName()));
    model.addAttribute("hasEvents", modelService.studentHasEvents(account.getName()));
    model.addAttribute("studentEvents", modelService.getAllEventsPerStudent(account.getName()));
    authenticatedAccess.increment();
    return "personalView";
  }

  /**
   * Formular zum Beantragen von Credit-Points.
   */
  @GetMapping("/creditpoints")
  public String getCreditPoints(final KeycloakAuthenticationToken token, final Model model) {
    final Account account = AccountCreator.createAccountFromPrincipal(token);
    model.addAttribute("account", account);
    model.addAttribute("eventsExist", modelService.acceptedEventsExist(account.getName()));
    model.addAttribute("events", modelService.getAllEventsForCP(account.getName()));
    model.addAttribute("useForCP", modelService.useEventsIsPossible(account.getName()));
    model.addAttribute("exists", modelService.studentExists(account.getName()));
    authenticatedAccess.increment();
    return "credit_points_apply";
  }

  /**
   * Formular zur Einreichung der Zusammenfassung.
   * Das Summary-Objekt muss noch auf die Datenbank angepasst werden.
   */
  @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
  @GetMapping("/reportsubmit")
  public String reportsubmit(final KeycloakAuthenticationToken token, final Model model) {
    final LocalDate today = LocalDate.now();
    final String eventname = "das coolste Event";
    String content;
    try {
      content = fileService.getContentOfFileAsString("VorlageZusammenfassung.md");
      content = content.isEmpty()
          ? "Vorlage momentan nicht vorhanden. Schreib hier deinen Code hinein." : content;
    } catch (final Exception e) {
      content = "Vorlage momentan nicht vorhanden. Schreib hier deinen Code hinein.";
    }
    final String student = "Hannah Hengelbrock";
    final Summary summary = new Summary(eventname, student, content, today);
    model.addAttribute("summary", summary);
    model.addAttribute("account", AccountCreator.createAccountFromPrincipal(token));
    authenticatedAccess.increment();
    return "report_submit";
  }

  /**
   * Fügt einen Studenten einem Event hinzu.
   */
  @PostMapping("/events")
  public String addStudentToEvent(String name, String email, Long eventId) {
    modelService.addStudentToEvent(name, email, eventId);
    return "personalView";
  }

}
