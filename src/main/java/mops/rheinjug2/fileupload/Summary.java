package mops.rheinjug2.fileupload;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Summary {

  private final String eventname;
  private final String studentname;
  private final String content;
  private final LocalDateTime date;
  private final Long eventId;

}