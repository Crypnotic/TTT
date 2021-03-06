/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2016, Max Roncace <me@caseif.net>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.caseif.ttt.util;

import com.google.common.collect.ImmutableList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Utility class for use with Mojang's UUID API.
 *
 * <p>This class has been modified from the original for use with MGLib.</p>
 *
 * @author evilmidget38
 */
public final class UUIDFetcher implements Callable<Map<String, UUID>> {

    static HashMap<String, UUID> uuids = new HashMap<>();

    private static final double PROFILES_PER_REQUEST = 100;
    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private final JSONParser jsonParser = new JSONParser();
    private final List<String> names;
    private final boolean rateLimiting;

    public UUIDFetcher(List<String> names, boolean rateLimiting) {
        this.names = ImmutableList.copyOf(names);
        this.rateLimiting = rateLimiting;
    }

    public UUIDFetcher(List<String> names) {
        this(names, true);
    }

    public Map<String, UUID> call() throws UUIDException {
        Map<String, UUID> uuidMap = new HashMap<>();
        int requests = (int)Math.ceil(names.size() / PROFILES_PER_REQUEST);
        try {
            for (int i = 0; i < requests; i++) {
                HttpURLConnection connection = createConnection();
                String body = JSONArray.toJSONString(names.subList(i * 100, Math.min((i + 1) * 100, names.size())));
                writeBody(connection, body);
                JSONArray array = (JSONArray) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
                for (Object profile : array) {
                    JSONObject jsonProfile = (JSONObject) profile;
                    String id = (String) jsonProfile.get("id");
                    String name = (String) jsonProfile.get("name");
                    UUID uuid = getUUID(id);
                    uuidMap.put(name, uuid);
                }
                if (rateLimiting && i != requests - 1) {
                    Thread.sleep(100L);
                }
            }
        } catch (InterruptedException | IOException | ParseException ex) {
            throw new UUIDException(ex);
        }
        uuids.putAll(uuidMap);
        return uuidMap;
    }

    private static void writeBody(HttpURLConnection connection, String body) throws UUIDException {
        try {
            OutputStream stream = connection.getOutputStream();
            stream.write(body.getBytes());
            stream.flush();
            stream.close();
        } catch (IOException ex) {
            throw new UUIDException(ex);
        }
    }

    private static HttpURLConnection createConnection() throws UUIDException {
        try {
            URL url = new URL(PROFILE_URL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection;
        } catch (IOException ex) {
            throw new UUIDException(ex);
        }
    }

    private static UUID getUUID(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-"
                + id.substring(16, 20) + "-" + id.substring(20, 32));
    }

    public static byte[] toBytes(UUID uuid) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
        byteBuffer.putLong(uuid.getMostSignificantBits());
        byteBuffer.putLong(uuid.getLeastSignificantBits());
        return byteBuffer.array();
    }

    public static UUID fromBytes(byte[] array) {
        if (array.length != 16) {
            throw new IllegalArgumentException("Illegal byte array length: " + array.length);
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(array);
        long mostSignificant = byteBuffer.getLong();
        long leastSignificant = byteBuffer.getLong();
        return new UUID(mostSignificant, leastSignificant);
    }

    public static UUID getUUIDOf(String name) throws UUIDException {
        if (uuids.containsKey(name)) {
            return uuids.get(name);
        }
        UUID uuid = new UUIDFetcher(Collections.singletonList(name)).call().get(name);
        addUUID(name, uuid);
        return uuid;
    }

    public static void addUUID(String name, UUID uuid) {
        uuids.put(name, uuid);
    }

    public static void removeUUID(String name) {
        uuids.remove(name);
    }

    public static void uninitialize() {
        uuids.clear();
        uuids = null;
    }

    public static class UUIDException extends Exception {
        private static final long serialVersionUID = -1428969262256907686L;

        public UUIDException(Exception cause) {
            super("Failed to retrieve UUID(s)", cause);
        }
    }

}
