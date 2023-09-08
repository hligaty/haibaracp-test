package io.github.hligaty.config;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.core.SftpSession;

/**
 * As an example only
 */
public class SftpExecShellSession extends SftpSession {

    private ChannelExec channelExec;
    
    private ChannelShell channelShell;

    public SftpExecShellSession(ClientProperties clientProperties) {
        super(clientProperties);
    }

    public ChannelExec channelExec() {
        return channelExec;
    }

    public ChannelShell channelShell() {
        return channelShell;
    }

    @Override
    protected boolean test() {
        return super.test() && channelExec.isConnected() && channelShell.isConnected();
    }

    @Override
    protected Session createJschSession(ClientProperties clientProperties) throws Exception {
        Session jschSession = super.createJschSession(clientProperties);
        channelExec = (ChannelExec) jschSession().openChannel("exec");
        channelExec.connect();
        channelShell = (ChannelShell) jschSession().openChannel("shell");
        channelShell.connect();
        return jschSession;
    }
    
    
}
