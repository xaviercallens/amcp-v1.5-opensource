package io.amcp.connectors.boinc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Minimal client for the BOINC client GUI RPC protocol (XML over TCP, default port 31416).
 *
 * <p>Each message is wrapped in {@code <boinc_gui_rpc_request>} and terminated by an
 * ETX (0x03) byte; replies are wrapped in {@code <boinc_gui_rpc_reply>} with the same
 * terminator. Authentication is the standard nonce/MD5 challenge using the password
 * from the client's {@code gui_rpc_auth.cfg}.</p>
 *
 * <p>Instances are not thread-safe; callers must serialise access.</p>
 */
public class BoincRpcClient implements Closeable {

    public static final int DEFAULT_PORT = 31416;
    private static final int ETX = 0x03;
    private static final int MAX_REPLY_BYTES = 16 * 1024 * 1024;

    public static final Set<String> RUN_MODES = Set.of("always", "auto", "never");
    public static final Set<String> PROJECT_OPS =
            Set.of("suspend", "resume", "update", "nomorework", "allowmorework");

    private final String host;
    private final int port;
    private final String password;
    private final int timeoutMs;
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public BoincRpcClient(String host, int port, String password, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.password = password == null ? "" : password;
        this.timeoutMs = timeoutMs;
    }

    public synchronized void connect() throws IOException {
        close();
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeoutMs);
        socket.setSoTimeout(timeoutMs);
        in = socket.getInputStream();
        out = socket.getOutputStream();
        authenticate();
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void authenticate() throws IOException {
        Document nonceReply = parse(rawCall("<auth1/>"));
        String nonce = text(nonceReply.getDocumentElement(), "nonce");
        if (nonce == null) {
            throw new IOException("BOINC auth1 returned no nonce");
        }
        String hash = md5Hex(nonce + password);
        String reply = rawCall("<auth2><nonce_hash>" + hash + "</nonce_hash></auth2>");
        if (!reply.contains("<authorized")) {
            close();
            throw new IOException("BOINC authentication failed (check gui_rpc_auth.cfg password)");
        }
    }

    // ---------------------------------------------------------------- high-level calls

    /** Projects the client is attached to. */
    public List<Map<String, Object>> getProjectStatus() throws IOException {
        Document d = parse(call("<get_project_status/>"));
        return children(d.getDocumentElement(), "projects", "project",
                "project_name", "master_url", "user_name", "team_name", "user_total_credit",
                "user_expavg_credit", "host_total_credit", "resource_share", "nrpc_failures",
                "suspended_via_gui", "dont_request_more_work");
    }

    /** All workunit results known to the client (queued, running, finished). */
    public List<Map<String, Object>> getResults() throws IOException {
        Document d = parse(call("<get_results/>"));
        return children(d.getDocumentElement(), "results", "result",
                "name", "wu_name", "project_url", "state", "active_task_state",
                "fraction_done", "elapsed_time", "final_elapsed_time",
                "estimated_cpu_time_remaining", "report_deadline", "exit_status");
    }

    /** CPU/GPU/network run state as currently enforced by the client. */
    public Map<String, Object> getCcStatus() throws IOException {
        Document d = parse(call("<get_cc_status/>"));
        Element e = first(d.getDocumentElement(), "cc_status");
        return e == null ? Map.of() : leaves(e);
    }

    public void setRunMode(String mode, double durationSeconds) throws IOException {
        requireIn(RUN_MODES, mode, "run mode");
        expectSuccess(call("<set_run_mode><" + mode + "/><duration>" + durationSeconds
                + "</duration></set_run_mode>"));
    }

    public void projectOp(String projectUrl, String op) throws IOException {
        requireIn(PROJECT_OPS, op, "project operation");
        if (projectUrl == null || !projectUrl.matches("https?://[^<>\\s&]+")) {
            throw new IllegalArgumentException("Invalid project URL");
        }
        expectSuccess(call("<project_op><project_url>" + projectUrl + "</project_url><"
                + op + "/></project_op>"));
    }

    // ---------------------------------------------------------------- transport

    private synchronized String call(String body) throws IOException {
        if (!isConnected()) {
            connect();
        }
        return rawCall(body);
    }

    private String rawCall(String body) throws IOException {
        String msg = "<boinc_gui_rpc_request>\n" + body + "\n</boinc_gui_rpc_request>\n";
        out.write(msg.getBytes(StandardCharsets.UTF_8));
        out.write(ETX);
        out.flush();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != ETX) {
            buf.write(b);
            if (buf.size() > MAX_REPLY_BYTES) {
                throw new IOException("BOINC reply exceeds " + MAX_REPLY_BYTES + " bytes");
            }
        }
        if (b == -1) {
            close();
            throw new IOException("BOINC client closed the connection");
        }
        return buf.toString(StandardCharsets.UTF_8);
    }

    @Override
    public synchronized void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // best effort
            }
        }
        socket = null;
        in = null;
        out = null;
    }

    // ---------------------------------------------------------------- XML helpers

    private static void expectSuccess(String reply) throws IOException {
        if (!reply.contains("<success")) {
            throw new IOException("BOINC command failed: " + reply.trim());
        }
    }

    private static void requireIn(Set<String> allowed, String value, String what) {
        if (value == null || !allowed.contains(value)) {
            throw new IllegalArgumentException("Unsupported " + what + ": " + value);
        }
    }

    static Document parse(String xml) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IOException("Malformed BOINC reply: " + e.getMessage(), e);
        }
    }

    private static Element first(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : (Element) nl.item(0);
    }

    private static String text(Element parent, String tag) {
        Element e = first(parent, tag);
        return e == null ? null : e.getTextContent().trim();
    }

    private static Map<String, Object> leaves(Element e) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element c) {
                m.put(c.getTagName(), c.getTextContent().trim());
            }
        }
        return m;
    }

    private static List<Map<String, Object>> children(Element root, String container,
                                                     String item, String... fields) {
        List<Map<String, Object>> out = new ArrayList<>();
        Element c = first(root, container);
        if (c == null) {
            return out;
        }
        NodeList items = c.getElementsByTagName(item);
        for (int i = 0; i < items.getLength(); i++) {
            Element it = (Element) items.item(i);
            Map<String, Object> m = new LinkedHashMap<>();
            for (String f : fields) {
                String v = text(it, f);
                if (v != null) {
                    m.put(f, v);
                }
            }
            out.add(m);
        }
        return out;
    }

    private static String md5Hex(String s) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IOException("MD5 unavailable", e);
        }
    }
}
