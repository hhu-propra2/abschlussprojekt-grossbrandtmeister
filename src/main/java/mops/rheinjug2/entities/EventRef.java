package mops.rheinjug2.entities;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Table;

@Table("student_event")
@Data
public class EventRef {

  private Long event;

  private boolean submittedSummary;
  private LocalDateTime timeOfSubmission;
  private LocalDateTime deadline;
  private boolean accepted;
  private boolean usedForCertificate;
  private boolean publishSummary;

  EventRef(final Long event, final LocalDateTime deadline) {
    this.event = event;
    this.deadline = deadline;
  }

  /**
   * Gibt an, ob für eine Veranstaltung eine Zusammenfassung abgegeben
   * werden kann.
   */
  public boolean isOpenForSubmission() {
    return LocalDateTime.now().isBefore(deadline);
  }

  boolean isSubmittedAndAcceptedButNotUsed() {
    return submittedSummary && accepted && (!usedForCertificate);
  }

  public boolean isSubmittedAndNotAccepted() {
    return submittedSummary && !accepted;
  }

  public boolean isDelayed() {
    return !submittedSummary && deadline.isBefore(LocalDateTime.now());
  }

  boolean isSubmittedAndAccepted() {
    return submittedSummary && accepted;
  }
}
