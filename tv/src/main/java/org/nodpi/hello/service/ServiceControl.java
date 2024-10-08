package org.nodpi.hello.service;

public interface ServiceControl {
    void startService();
    void stopService();
    boolean vpnProtect(int socket);
    // Other methods as needed
}
