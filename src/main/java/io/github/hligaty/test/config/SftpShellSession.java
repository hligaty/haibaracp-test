package io.github.hligaty.test.config;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.SftpSession;
import org.springframework.lang.NonNull;

/**
 * As an example only
 */
public class SftpShellSession extends SftpSession {

    private ChannelShell channelShell;

    public SftpShellSession(ClientProperties clientProperties) {
        super(clientProperties);
    }

    public ChannelShell channelShell() {
        return channelShell;
    }

    @Override
    protected boolean test() {
        return super.test() && channelShell.isConnected();
    }

    @Override
    @NonNull
    protected Session createJschSession(ClientProperties clientProperties) throws Exception {
        Session jschSession = super.createJschSession(clientProperties);
        channelShell = (ChannelShell) jschSession.openChannel("shell");
        channelShell.connect();
        return jschSession;
    }

}
