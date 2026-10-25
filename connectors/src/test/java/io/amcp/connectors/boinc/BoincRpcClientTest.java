package io.amcp.connectors.boinc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the client against an in-process fake of the BOINC GUI RPC server. */
class BoincRpcClientTest {

    private static final String NONCE = "1234.5678";
    private static final String PASSWORD = "s3cret";

    private ServerSocket server;
    private Thread serverThread;
    private volatile String lastRequest = "";

    @BeforeEach
    void startServer() throws Exception {
        server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress());
        serverThread = new Thread(() -> {
            try (Socket s = server.accept()) {
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                while (true) {
                    String req = readMessage(in);
                    if (req == null) return;
                    lastRequest = req;
                    out.write(reply(req).getBytes(StandardCharsets.UTF_8));
                    out.write(3);
                    out.flush();
                }
            } catch (Exception ignored) {
                // socket closed by test
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    @AfterEach
    void stop() throws Exception {
        server.close();
    }

    private static String readMessage(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int c;
        while ((c = in.read()) != -1 && c != 3) b.write(c);
        return c == -1 ? null : b.toString(StandardCharsets.UTF_8);
    }

    private static String md5(String s) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte x : MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8)))
            sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String reply(String req) throws Exception {
        String body;
        if (req.contains("<auth1/>")) {
            body = "<nonce>" + NONCE + "</nonce>";
        } else if (req.contains("<auth2>")) {
            body = req.contains(md5(NONCE + PASSWORD)) ? "<authorized/>" : "<unauthorized/>";
        } else if (req.contains("<get_project_status/>")) {
            body = "<projects><project><project_name>Einstein@Home</project_name>"
                    + "<master_url>https://einstein.phys.uwm.edu/</master_url>"
                    + "<user_total_credit>1234.5</user_total_credit>"
                    + "<suspended_via_gui/></project></projects>";
        } else if (req.contains("<get_results/>")) {
            body = "<results><result><name>wu_1_0</name><project_url>https://einstein.phys.uwm.edu/</project_url>"
                    + "<active_task_state>1</active_task_state><fraction_done>0.42</fraction_done></result></results>";
        } else if (req.contains("<get_cc_status/>")) {
            body = "<cc_status><task_mode>2</task_mode><gpu_mode>3</gpu_mode></cc_status>";
        } else {
            body = "<success/>";
        }
        return "<boinc_gui_rpc_reply>\n" + body + "\n</boinc_gui_rpc_reply>\n";
    }

    private BoincRpcClient client(String password) {
        return new BoincRpcClient("127.0.0.1", server.getLocalPort(), password, 3000);
    }

    @Test
    void authenticatesAndParsesProjectsTasksAndStatus() throws Exception {
        try (BoincRpcClient c = client(PASSWORD)) {
            c.connect();
            List<Map<String, Object>> projects = c.getProjectStatus();
            assertEquals(1, projects.size());
            assertEquals("Einstein@Home", projects.get(0).get("project_name"));
            assertTrue(projects.get(0).containsKey("suspended_via_gui"));

            List<Map<String, Object>> tasks = c.getResults();
            assertEquals("wu_1_0", tasks.get(0).get("name"));
            assertEquals("0.42", tasks.get(0).get("fraction_done"));

            assertEquals("2", c.getCcStatus().get("task_mode"));
        }
    }

    @Test
    void wrongPasswordIsRejected() {
        BoincRpcClient c = client("wrong");
        assertThrows(java.io.IOException.class, c::connect);
    }

    @Test
    void sendsRunModeAndProjectOps() throws Exception {
        try (BoincRpcClient c = client(PASSWORD)) {
            c.connect();
            c.setRunMode("never", 3600);
            assertTrue(lastRequest.contains("<set_run_mode><never/>"));
            c.projectOp("https://einstein.phys.uwm.edu/", "suspend");
            assertTrue(lastRequest.contains("<project_op>") && lastRequest.contains("<suspend/>"));
        }
    }

    @Test
    void rejectsInjectionAndUnknownOperations() throws Exception {
        try (BoincRpcClient c = client(PASSWORD)) {
            c.connect();
            assertThrows(IllegalArgumentException.class, () -> c.setRunMode("always/><evil", 0));
            assertThrows(IllegalArgumentException.class,
                    () -> c.projectOp("https://x.org/</project_url><detach/>", "suspend"));
            assertThrows(IllegalArgumentException.class,
                    () -> c.projectOp("https://x.org/", "detach"));
        }
    }

    @Test
    void rejectsDoctypeInReplies() {
        assertThrows(java.io.IOException.class,
                () -> BoincRpcClient.parse("<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>"));
    }
}
