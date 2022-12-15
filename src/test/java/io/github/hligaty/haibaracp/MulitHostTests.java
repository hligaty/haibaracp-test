package io.github.hligaty.haibaracp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.HostHolder;
import io.github.hligaty.haibaracp.core.SftpTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@ActiveProfiles("multi")
@SpringBootTest
class MulitHostTests {
  @Autowired
  private SftpTemplate sftpTemplate;
  @Autowired
  private ClientProperties clientProperties;
  @Value("${download}")
  private String downloadDir;

  @Test
  void contextLoads() throws SftpException {
    multiHost();
    upload();
    download();
    exists();
    list();
    execute();
    executeWithoutResult();
  }

  void multiHost() throws SftpException {
    // view all host
    String hostInfo = String.join(",", HostHolder.hostNames());
    System.out.println(hostInfo);

    // change host
    String firstHost = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(firstHost);
    try {
      // success
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(home);
      // NullPointerException
      sftpTemplate.execute(ChannelSftp::getHome);
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    HostHolder.changeHost(firstHost, false);
    try {
      // success
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(home);
      // success
      String home1 = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(home1);
    } finally {
      HostHolder.clearHost();
    }

    // batch execute
    // all execute
    for (String hostKey : HostHolder.hostNames()) {
      HostHolder.changeHost(hostKey);
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(hostKey + " home: " + home);
    }
    // part execute
    for (String hostKey : HostHolder.hostNames(host -> host.startsWith(firstHost))) {
      HostHolder.changeHost(hostKey);
      String home = sftpTemplate.execute(ChannelSftp::getHome);
      System.out.println(hostKey + " home: " + home);
    }
  }

  void upload() throws SftpException {
    Path path = Paths.get(System.getProperty("user.dir"), "file");
    String hostName = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(hostName, false);
    try {
      // upload to /home/username/doc/aptx4869.pdf
      sftpTemplate.upload(path.resolve("aptx4869.pdf").toString(), "/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc/aptx4869.pdf");
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
      HostHolder.clearHost();
    }
  }

  void download() throws SftpException {
    Path path = Paths.get(downloadDir);
    String hostName = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(hostName, false);
    try {
      // download /home/username/doc/aptx4869.pdf
      sftpTemplate.download("/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc/aptx4869.pdf", path.resolve("aptx4869.pdf").toString());
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
      HostHolder.clearHost();
    }
  }

  void exists() throws SftpException {
    String hostName = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(hostName, false);
    try {
      // Test path /home/username/doc/aptx4869.pdf
      System.out.println(sftpTemplate.exists("/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc/aptx4869.pdf"));
      // Test path /home/username/doc/aptx4869.doc
      System.out.println(sftpTemplate.exists("doc/aptx4869.doc"));
      // Test path /home/username/aptx4869.docx
      System.out.println(sftpTemplate.exists("aptx4869.docx"));
    } finally {
      HostHolder.clearHost();
    }
  }

  void list() throws SftpException {
    String hostName = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(hostName, false);
    try {
      // view /home/username/doc/aptx4869.pdf
      sftpTemplate.list("/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc/aptx4869.pdf");
      // view /home/username/doc/aptx4869.doc
      sftpTemplate.list("doc/aptx4869.doc");
      // view /home/username/aptx4869.docx
      sftpTemplate.list("aptx4869.docx");
      // view /home/username/doc
      sftpTemplate.list("/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc");
      // view /home/username/doc
      sftpTemplate.list("doc");
    } finally {
      HostHolder.clearHost();
    }
  }

  void execute() throws SftpException {
    HostHolder.changeHost(HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null")));
    String home = sftpTemplate.execute(ChannelSftp::getHome);
    System.out.println(home);
  }

  void executeWithoutResult() throws SftpException {
    String hostName = HostHolder.hostNames().stream().findFirst().orElseThrow(() -> new IllegalArgumentException("sftp.hosts must not be null"));
    HostHolder.changeHost(hostName, false);
    try {
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("/home/" + clientProperties.getHosts().get(hostName).getUsername() + "/doc/aptx4869.pdf"));
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("doc/aptx4869.doc"));
      sftpTemplate.executeWithoutResult(channelSftp -> channelSftp.rm("aptx4869.docx"));
    } finally {
      HostHolder.clearHost();
    }
  }
}
