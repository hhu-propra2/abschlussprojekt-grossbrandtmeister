package mops.rheinjug2.fileupload;

import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidArgumentException;
import io.minio.errors.InvalidBucketNameException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.NoResponseException;
import io.minio.errors.RegionConflictException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.xmlpull.v1.XmlPullParserException;

@Controller
@RequestMapping("/rheinjug2")
public class FileUploadController {

  FileService fileService;

  FileCheckService fileCheckService;

  @Autowired
  public FileUploadController(final FileService fileService) {
    this.fileService = fileService;
  }

  @RequestMapping("/file")
  public String showPage(final Model model) {
    return "fileUpload";
  }


  /**
   * Gibt das File an den FileService weiter um das File zu speichern.
   */
  @PostMapping(path = "/file")
  public String uploadFile(@RequestParam(value = "file") final MultipartFile file,
                           final Model model) {
    if (fileCheckService.checkIfIsAdoc(file)) {
      try {
        final String filename = "Student_Veranstaltung_Version.adoc";
        fileService.uploadFile(file, filename);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }
    return "fileUpload";
  }

  /**
   * füge das File auf einer eigenen Website hinzu. Evtl in späteren versionen zu ändern.
   *
   * @param model
   * @return String
   */
  @RequestMapping("/download")
  public String downloadFile(final Model model) throws IOException, XmlPullParserException,
      NoSuchAlgorithmException, InvalidKeyException, InvalidArgumentException,
      InvalidResponseException, ErrorResponseException, NoResponseException,
      InvalidBucketNameException, InsufficientDataException, InternalException {
    final String filename = "documentation";
    final File file = fileService.getFile(filename);
    model.addAttribute("file", file);
    return "download";
  }

  /**
   * Nimmt Inpustream mit Inhalt des zusuchenden adocs und gibt es als Response an einen
   * eigenen Download-Link zurück.
   *
   * @param object
   * @param response
   */
  @RequestMapping("/download/file/{filename}")
  @ResponseBody
  public void downloadFile(@PathVariable("filename") final String object, final HttpServletResponse response)
      throws IOException, XmlPullParserException, NoSuchAlgorithmException,
      InvalidKeyException, InvalidArgumentException, InvalidResponseException,
      ErrorResponseException, NoResponseException, InvalidBucketNameException,
      InsufficientDataException, InternalException, RegionConflictException {
    final InputStream inputStream = fileService.getFileInputStream(object);

    response.addHeader("Content-disposition", "attachment;filename=" + object + ".md");
    response.setContentType(URLConnection.guessContentTypeFromName(object));

    IOUtils.copy(inputStream, response.getOutputStream());
    response.flushBuffer();

    inputStream.close();
  }

}

