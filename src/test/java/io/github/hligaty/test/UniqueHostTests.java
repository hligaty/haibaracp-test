package io.github.hligaty.test;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import io.github.hligaty.haibaracp.core.ChannelSftpWrapper;
import io.github.hligaty.test.config.SftpShellSession;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.SessionException;
import io.github.hligaty.haibaracp.core.SftpTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UniqueHostTests {

    @Autowired
    private SftpTemplate<SftpShellSession> sftpTemplate;
    @Autowired
    private ClientProperties clientProperties;
    @Value("${download}")
    private String downloadDir;

    @Test
    public void contextLoads() throws IOException {
        upload();
        download();
        exists();
        list();
        downloadByExecute();
        rmUploadFileByExecuteWithoutResult();
        executeSession();
        uploadByExecuteSessionWithoutResult();
    }

    void upload() {
        Path path = Paths.get(System.getProperty("user.dir"), "file");
        try {
            // upload to /home/username/doc/aptx4869.pdf
            sftpTemplate.upload(path.resolve("aptx4869.pdf").toString(), "/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf");
            // upload to /home/username/doc/aptx4869.doc
            sftpTemplate.upload(path.resolve("aptx4869.doc").toString(), "doc/aptx4869.doc");
            // upload to /home/username/aptx4869.docx
            sftpTemplate.upload(path.resolve("aptx4869.docx").toString(), "aptx4869.docx");
        } catch (SessionException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                System.out.println("local file not exists." + e.getMessage());
            }
            throw e;
        }
    }

    void download() throws IOException {
        Path path = Paths.get(downloadDir);
        Files.createDirectories(path);
        try {
            // download /home/username/doc/aptx4869.pdf
            Path downloadPath = path.resolve("aptx4869.pdf").toAbsolutePath();
            Files.deleteIfExists(downloadPath);
            sftpTemplate.download("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf", downloadPath.toString());
            assertTrue(Files.exists(downloadPath));
            // download /home/username/doc/aptx4869.doc
            downloadPath = path.resolve("aptx4869.doc").toAbsolutePath();
            Files.deleteIfExists(downloadPath);
            sftpTemplate.download("doc/aptx4869.doc", downloadPath.toString());
            assertTrue(Files.exists(downloadPath));
            // download /home/username/aptx4869.pdf
            downloadPath = path.resolve("aptx4869.docx").toAbsolutePath();
            Files.deleteIfExists(downloadPath);
            sftpTemplate.download("aptx4869.docx", downloadPath.toString());
            assertTrue(Files.exists(downloadPath));
        } catch (SessionException e) {
            if (e.getCause() instanceof SftpException) {
                SftpException sftpException = (SftpException) e.getCause();
                if (sftpException.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    System.out.println("remote file not exists");
                } else if (sftpException.id == ChannelSftp.SSH_FX_FAILURE && sftpException.getCause() instanceof FileNotFoundException) {
                    System.out.println("local path not exists");
                }
                throw e;
            }
        }
    }

    void exists() {
        // Test path /home/username/doc/aptx4869.pdf
        assertTrue(sftpTemplate.exists("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
        // Test path /home/username/doc/aptx4869.doc
        assertTrue(sftpTemplate.exists("doc/aptx4869.doc"));
        // Test path /home/username/aptx4869.docx
        assertTrue(sftpTemplate.exists("aptx4869.docx"));
    }

    void list() {
        ChannelSftp.LsEntry[] lsEntries;
        // view /home/username/doc/aptx4869.pdf
        lsEntries = sftpTemplate.list("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf");
        assertTrue(lsEntries.length == 1 && "aptx4869.pdf".equals(lsEntries[0].getFilename()));
        // view /home/username/doc/aptx4869.doc
        lsEntries = sftpTemplate.list("doc/aptx4869.doc");
        assertTrue(lsEntries.length == 1 && "aptx4869.doc".equals(lsEntries[0].getFilename()));
        // view /home/username/aptx4869.docx
        lsEntries = sftpTemplate.list("aptx4869.docx");
        assertTrue(lsEntries.length == 1 && "aptx4869.docx".equals(lsEntries[0].getFilename()));
        // view /home/username/doc
        assertEquals(
                Arrays.toString(sftpTemplate.list("/home/" + clientProperties.getUsername() + "/doc")),
                Arrays.toString(sftpTemplate.list("doc"))
        );
    }

    void downloadByExecute() throws IOException {
        Path downloadPath = Paths.get(downloadDir).resolve("aptx4869-new.doc");
        Files.deleteIfExists(downloadPath);
        try (OutputStream outputStream = Files.newOutputStream(downloadPath)) {
            sftpTemplate.executeWithoutResult(channelSftp -> {
                try {
                    channelSftp.get("doc/aptx4869.doc", outputStream);
                } catch (SftpException e) {
                    throw new SessionException("Failed to get file", e);
                }
            });
        }
        assertTrue(Files.exists(downloadPath));
    }

    void rmUploadFileByExecuteWithoutResult() {
        String path = "/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf";
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, path));
        assertFalse(sftpTemplate.exists(path));
        String path1 = "doc/aptx4869.doc";
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, path1));
        assertFalse(sftpTemplate.exists(path1));
        String path2 = "aptx4869.docx";
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, path2));
        assertFalse(sftpTemplate.exists(path2));
    }

    static void rm(ChannelSftp channelSftp, String path) {
        try {
            channelSftp.rm(path);
        } catch (SftpException e) {
            throw new SessionException("Failed to rm path '" + path + "'", e);
        }
    }

    void executeSession() {
        Boolean result = sftpTemplate.executeSession(sftpSession -> sftpSession.channelShell().isConnected());
        assertEquals(Boolean.TRUE, result);
    }

    void uploadByExecuteSessionWithoutResult() throws IOException {
        String uploadPath = "doc/new/4869/aptx4869.pdf";
        try (InputStream inputStream = Files.newInputStream(Paths.get(downloadDir).resolve("aptx4869-new.doc").toAbsolutePath())) {
            sftpTemplate.executeSessionWithoutResult(sftpSession -> {
                ChannelSftp channelSftp = sftpSession.channelSftp();
                ChannelSftpWrapper channelSftpWrapper = new ChannelSftpWrapper(channelSftp);
                channelSftpWrapper.upload(inputStream, uploadPath);
            });
        }
        assertTrue(sftpTemplate.exists(uploadPath));
    }

}
