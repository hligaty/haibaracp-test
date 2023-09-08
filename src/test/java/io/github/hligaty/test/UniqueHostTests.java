package io.github.hligaty.test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.SftpException;
import io.github.hligaty.config.SftpExecShellSession;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.config.PoolProperties;
import io.github.hligaty.haibaracp.core.SessionCallback;
import io.github.hligaty.haibaracp.core.SessionCallbackWithoutResult;
import io.github.hligaty.haibaracp.core.SessionException;
import io.github.hligaty.haibaracp.core.SftpSessionFactory;
import io.github.hligaty.haibaracp.core.SftpTemplate;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ActiveProfiles("unique")
@SpringBootTest
class UniqueHostTests {

    @Bean
    public SftpSessionFactory sftpSessionFactory(ClientProperties clientProperties, PoolProperties poolProperties) {
        return new SftpSessionFactory(clientProperties, poolProperties) {
            @Override
            public SftpExecShellSession getSftpSession(ClientProperties clientProperties) {
                return new SftpExecShellSession(clientProperties);
            }
        };
    }


    @Resource
    private SftpTemplate sftpTemplate;
    @Resource
    private ClientProperties clientProperties;
    @Value("${download}")
    private String downloadDir;

    @Test
    public void contextLoads() throws SftpException, IOException {
        upload();
        download();
        exists();
        list();
        execute();
        executeWithoutResult();
        executeSession();
        executeSessionWithoutResult();
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

    void download() throws SftpException {
        Path path = Paths.get(downloadDir);
        try {
            // download /home/username/doc/aptx4869.pdf
            sftpTemplate.download("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf", path.resolve("aptx4869.pdf").toString());
            // download /home/username/doc/aptx4869.doc
            sftpTemplate.download("doc/aptx4869.doc", path.resolve("aptx4869.doc").toString());
            // download /home/username/aptx4869.pdf
            sftpTemplate.download("aptx4869.docx", path.resolve("aptx4869.docx").toString());
        } catch (SessionException e) {
            if (e.getCause() instanceof SftpException sftpException) {
                if (sftpException.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    System.out.println("remote file not exists");
                } else if (sftpException.id == ChannelSftp.SSH_FX_FAILURE && sftpException.getCause() instanceof FileNotFoundException) {
                    System.out.println("local path not exists");
                }
                throw sftpException;
            }
        }
    }

    void exists() {
        // Test path /home/username/doc/aptx4869.pdf
        System.out.println(sftpTemplate.exists("/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
        // Test path /home/username/doc/aptx4869.doc
        System.out.println(sftpTemplate.exists("doc/aptx4869.doc"));
        // Test path /home/username/aptx4869.docx
        System.out.println(sftpTemplate.exists("aptx4869.docx"));
    }

    void list() {
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

    void execute() throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(Paths.get("/root/aptx4869.doc"))) {
            sftpTemplate.executeWithoutResult(channelSftp -> {
                try {
                    channelSftp.get("aptx4869.doc", outputStream);
                } catch (SftpException e) {
                    throw new SessionException("Failed to get file", e);
                }
            });
        }
    }

    void executeWithoutResult() {
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, "/home/" + clientProperties.getUsername() + "/doc/aptx4869.pdf"));
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, "doc/aptx4869.doc"));
        sftpTemplate.executeWithoutResult(channelSftp -> rm(channelSftp, "aptx4869.docx"));
    }

    static void rm(ChannelSftp channelSftp, String path) {
        try {
            channelSftp.rm(path);
        } catch (SftpException e) {
            throw new SessionException("Failed to rm path '" + path + "'", e);
        }
    }

    void executeSession() {
        Boolean result = sftpTemplate.executeSession((SessionCallback<SftpExecShellSession, Boolean>) sftpSession -> {
            ChannelExec channelExec = sftpSession.channelExec();
            System.out.println("get ChannelExec :" + channelExec);
            return true;
        });
    }

    void executeSessionWithoutResult() {
        sftpTemplate.executeSessionWithoutResult((SessionCallbackWithoutResult<SftpExecShellSession>) sftpSession -> {
            ChannelShell channelShell = sftpSession.channelShell();
            System.out.println("get ChannelShell :" + channelShell);
        });
    }
}