package io.github.hligaty.haibaracp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.SftpTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@ActiveProfiles("unique")
@SpringBootTest
class UniqueHostTests {
  @Autowired
  private SftpTemplate sftpTemplate;
  @Autowired
  private ClientProperties clientProperties;
  @Value("${download}")
  private String downloadDir;

  @Test
  void contextLoads() throws SftpException {
    upload();
    download();
    exists();
    list();
    execute();
    executeWithoutResult();
  }

  void upload() throws SftpException {
    Path path = Paths.get(System.getProperty("user.dir"), "file");
    try {
      // upload to /home/username/doc/aptx4869.pdf
      sftpTemplate.upload(path.resolve("aptx4869.pdf").toString(), "/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf");
      // upload to /home/username/doc/aptx4869.doc
      sftpTemplate.upload(path.resolve("aptx4869.doc").toString(), "doc/aptx4869.doc");
      // upload to /home/username/aptx4869.docx
      sftpTemplate.upload(path.resolve("aptx4869.docx").toString(), "aptx4869.docx");
    } catch (SftpException e) {
      if (e.id == ChannelSftp.SSH_FX_FAILURE && e.getCause() instanceof FileNotFoundException) {
        System.out.println("local file not exists");
      }
      throw e;
    }
  }

  void download() throws SftpException {
    Path path = Paths.get(downloadDir);
    try {
      // download /home/username/doc/aptx4869.pdf
      sftpTemplate.download("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf", path.resolve("aptx4869.pdf").toString());
      // download /home/username/doc/aptx4869.doc
      sftpTemplate.download("doc/aptx4869.doc", path.resolve("aptx4869.doc").toString());
      // download /home/username/aptx4869.pdf
      sftpTemplate.download("aptx4869.docx", path.resolve("aptx4869.docx").toString());
    } catch (SftpException e) {
      if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
        System.out.println("remote file not exists");
      } else if (e.id == ChannelSftp.SSH_FX_FAILURE && e.getCause() instanceof FileNotFoundException) {
        System.out.println("local path not exists");
      }
      throw e;
    }
  }

  void exists() throws SftpException {
    // Test path /home/username/doc/aptx4869.pdf
    System.out.println(sftpTemplate.exists("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
    // Test path /home/username/doc/aptx4869.doc
    System.out.println(sftpTemplate.exists("doc/aptx4869.doc"));
    // Test path /home/username/aptx4869.docx
    System.out.println(sftpTemplate.exists("aptx4869.docx"));
  }
  
  void list() throws SftpException {
    // view /home/username/doc/aptx4869.pdf
    sftpTemplate.list("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf");
    // view /home/username/doc/aptx4869.doc
    sftpTemplate.list("doc/aptx4869.doc");
    // view /home/username/aptx4869.docx
    sftpTemplate.list("aptx4869.docx");
    // view /home/username/doc
    sftpTemplate.list("/home/" + clientProperties.getUsername() + "/doc");
    // view /home/username/doc
    sftpTemplate.list("doc");
  }

  void execute() throws SftpException {
    String home = sftpTemplate.execute(ChannelSftp::getHome);
    System.out.println(home);
  }

  void executeWithoutResult() throws SftpException {
    sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
    sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("doc/aptx4869.doc"));
    sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("aptx4869.docx"));
  }
}
