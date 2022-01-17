package io.github.hligaty.haibaracp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.HostHolder;
import io.github.hligaty.haibaracp.core.SftpTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@ActiveProfiles("multi")
@SpringBootTest
class MulitHostTests {
  @Resource
  private SftpTemplate sftpTemplate;
  @Resource
  private ClientProperties clientProperties;
  @Value("${download}")
  private String downloadDir;


  @Test
  void multiHost() throws SftpException {
    // view all host
    String hostInfo = String.join(",", HostHolder.hostKeys());
    System.out.println(hostInfo);

    // change host
    String firstHost = HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(firstHost);
    try {
      // success
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      // NullPointerException
      String home1 = sftpTemplate.execute(ChannelSftp::getHome);
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    HostHolder.changeHost(firstHost, false);
    try {
      // success
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      // success
      String home1 = sftpTemplate.execute(ChannelSftp::getHome);
    } finally {
      HostHolder.clearHostKey();
    }

    // batch execute
    // all execute
    for (String hostKey : HostHolder.hostKeys()) {
      HostHolder.changeHost(hostKey);
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(hostKey + " home: " + home);
    }
    // part execute
    for (String hostKey : HostHolder.hostKeys(host -> host.startsWith(firstHost))) {
      HostHolder.changeHost(hostKey);
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(hostKey + " home: " + home);
    }
  }

  @Test
  void upload() throws SftpException {
    Path path = Paths.get(System.getProperty("user.dir"), "file");
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")), false);
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
    } finally {
      HostHolder.clearHostKey();
    }
  }

  @Test
  void download() throws SftpException {
    Path path = Paths.get(downloadDir);
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")), false);
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
    } finally {
      HostHolder.clearHostKey();
    }
  }

  @Test
  void exists() throws SftpException {
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")), false);
    try {
      // Test path /home/username/doc/aptx4869.pdf
      System.out.println(sftpTemplate.exists("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
      // Test path /home/username/doc/aptx4869.doc
      System.out.println(sftpTemplate.exists("doc/aptx4869.doc"));
      // Test path /home/username/aptx4869.docx
      System.out.println(sftpTemplate.exists("aptx4869.docx"));
    } finally {
      HostHolder.clearHostKey();
    }
  }

  @Test
  void list() throws SftpException {
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")), false);
    try {
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
    } finally {
      HostHolder.clearHostKey();
    }
  }

  @Test
  void execute() throws SftpException {
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")));
    String home = sftpTemplate.execute(ChannelSftp::getHome);
    System.out.println(home);
  }

  @Test
  void executeWithoutResult() throws SftpException {
    HostHolder.changeHost(HostHolder.hostKeys().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")), false);
    try {
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("doc/aptx4869.doc"));
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("aptx4869.docx"));
    } finally {
      HostHolder.clearHostKey();
    }
  }
}
