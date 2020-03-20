package mops.rheinjug2.repositories;

import java.util.List;
import mops.rheinjug2.entities.Event;
import mops.rheinjug2.entities.EventRef;
import mops.rheinjug2.model.UnacceptedSummaryId;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends CrudRepository<Event, Long> {

  @Query(value = "SELECT COUNT(*) FROM student_event WHERE student_event.event = :id")
  int countStudentsPerEventById(@Param("id") Long id);

  @Query(value = "SELECT student FROM student_event WHERE student_event.event = :id")
  List<Long> findAllStudentsIdsPerEventById(@Param("id") Long id);

  @Modifying
  @Query(value = "UPDATE student_event SET accepted = :accepted WHERE"
      + " :eventid = student_event.event AND student_event.student = :studentid")
  int setAccepted(@Param("accepted") boolean accepted,
                  @Param("eventid") Long eventid, @Param("studentid") Long studentid);

  @Query(value = "SELECT * FROM event WHERE meetup_id = :id")
  Event findEventByMeetupId(@Param("id") String id);

  @Query(value = "SELECT * FROM event WHERE status = :status")
  List<Event> findEventsByStatus(@Param("status") String status);

  @Query(value = "SELECT COUNT(*) FROM student_event "
      + "WHERE student_event.event = :id AND student_event.submitted_summary = TRUE")
  int countSubmittedSummaryPerEventById(@Param("id") Long id);

  @Query(value = "SELECT student,event FROM student_event WHERE"
      + " submitted_summary = TRUE AND accepted = FALSE")
  List<UnacceptedSummaryId> getSubmittedAndUnacceptedSummaries();


  @Query(value = "SELECT * FROM event WHERE event.id = :id ")
  Event getEventById(@Param("id") Long eventId);

  @Query(value = "SELECT * FROM student_event WHERE"
      + " student_event.student = :studentid AND student_event.event= :eventid")
  EventRef getEventRefByStudentIdAndEventId(@Param("studentid") Long studentId,
                                            @Param("eventid") Long eventId);

  @Modifying
  @Query(value = "UPDATE student_event SET student_event.accepted = TRUE "
      + "WHERE student_event.student = :studentid AND student_event.event = :eventid")
  void updateSummaryToAccepted(@Param("studentid") Long studentid, @Param("eventid") Long eventid);
}
