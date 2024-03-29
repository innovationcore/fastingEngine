package fasting.Database;

import com.google.gson.Gson;
import fasting.Launcher;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DBEngine {
    private final int MAX_RETRIES = 5;
    private Gson gson;
    private DataSource ds;
    public DBEngine() {

        try {
            gson = new Gson();
            //Driver needs to be identified in order to load the namespace in the JVM
            String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            Class.forName(dbDriver).getDeclaredConstructor().newInstance();

            String dbConnectionString = "jdbc:sqlserver://" + Launcher.config.getStringParam("db_host") +":"+ 1433 + ";databaseName=" + Launcher.config.getStringParam("db_name") + ";encrypt=false";
            ds = setupDataSource(dbConnectionString, Launcher.config.getStringParam("db_user"), Launcher.config.getStringParam("db_password"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static DataSource setupDataSource(String connectURI, String login, String password) {
        //
        // First, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        //
        ConnectionFactory connectionFactory;
        if(login == null && password == null) {
            connectionFactory = new DriverManagerConnectionFactory(connectURI, null);
        } else {
            connectionFactory = new DriverManagerConnectionFactory(connectURI,
                    login, password);
        }


        //
        // Next we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        //
        PoolableConnectionFactory poolableConnectionFactory =
                new PoolableConnectionFactory(connectionFactory, null);



        //
        // Now we'll need a ObjectPool that serves as the
        // actual pool of connections.
        //
        // We'll use a GenericObjectPool instance, although
        // any ObjectPool implementation will suffice.
        //
        ObjectPool<PoolableConnection> connectionPool =
                new GenericObjectPool<>(poolableConnectionFactory);

        // Set the factory's pool property to the owning pool
        poolableConnectionFactory.setPool(connectionPool);



        //
        // Finally, we create the PoolingDriver itself,
        // passing in the object pool we created.

        return new PoolingDataSource<>(connectionPool);
    }


    public Map<String, String> getParticipantIdFromPhoneNumber(String PhoneNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> participantId = new HashMap<>();
        try {
            String queryString = "SELECT participant_uuid, study FROM participants WHERE JSON_VALUE(participant_json, '$.number') = ? AND study != 'ADMIN'";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, PhoneNumber);
            rs = stmt.executeQuery();

            while (rs.next()) {
                participantId.put(rs.getString("study"), rs.getString("participant_uuid"));
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return participantId;
    }

    public List<Map<String,String>> getParticipantMapByGroup(String groupName, String study) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Map<String,String>> participantMaps = null;
        try {
            participantMaps = new ArrayList<>();

            String queryString = "SELECT participant_uuid, participant_json FROM participants WHERE JSON_VALUE(participant_json, '$.group') = ? AND study = ?";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, groupName);
            stmt.setString(2, study);
            rs = stmt.executeQuery();

            while (rs.next()) {
                //Map<String,String> participantMap = new HashMap<>();
                Map<String,String> participantMap = gson.fromJson(rs.getString("participant_json"), Map.class);
                participantMap.put("participant_uuid",rs.getString("participant_uuid"));
                participantMaps.add(participantMap);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }

        return participantMaps;
    }


    public int executeUpdate(String stmtString) {
        Connection conn = null;
        Statement stmt = null;
        int result = -1;
        try {
            conn = ds.getConnection();
            stmt = conn.createStatement();
            result = stmt.executeUpdate(stmtString);

        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return  result;
    }

    public int getDaysWithoutEndCal(String participant_id){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int result = 0;
        try {
            String query = "SELECT JSON_VALUE(participant_json, '$.days_without_endcal') FROM participants WHERE participant_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participant_id);
            rs = stmt.executeQuery();

            if (rs.next()) {
                result = rs.getInt(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return result;
    }

    public void resetDaysWithoutEndCal(String participant_id){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            String query = "UPDATE participants set participant_json=JSON_MODIFY(participant_json, '$.days_without_endcal', 0) WHERE participant_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participant_id);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void updateDaysWithoutEndCal(String participant_id){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            int currentDaysWithoutEndCal = getDaysWithoutEndCal(participant_id);

            String query = "UPDATE participants set participant_json=JSON_MODIFY(participant_json, '$.days_without_endcal', "+(currentDaysWithoutEndCal+1)+") WHERE participant_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participant_id);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public String getEnrollmentUUID(String uuid){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String result = null;
        try {
            String query = "SELECT enrollment_uuid FROM enrollments WHERE participant_uuid = ? AND status = 1";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();

            while (rs.next()) {
                result = rs.getString(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            try { rs.close();   } catch (Exception e) { /* Null Ignored */ }
        }
        return result;
    }

    public String getEnrollmentName(String enrollUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String result = "";

        try {
            String query = "SELECT name FROM protocol_types WHERE protocol_type_uuid IN (SELECT protocol_type_uuid FROM enrollments WHERE enrollment_uuid = ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, enrollUUID);
            rs = stmt.executeQuery();
            if (rs.next()) {
                result = rs.getString(1);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            try { rs.close();   } catch (Exception e) { /* Null Ignored */ }
        }
        return result;
    }

    // remove all but latest 50 rows from save state
    void pruneSaveStateEntries(String enrollment_uuid) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            String query = "DELETE FROM save_state WHERE TS NOT IN (SELECT TOP 50 TS FROM save_state WHERE enrollment_uuid = ? ORDER BY TS DESC) AND enrollment_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, enrollment_uuid);
            stmt.setString(2, enrollment_uuid);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public boolean uploadSaveState(String stateJSON, String participant_uuid){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            String enrollment_uuid = getEnrollmentUUID(participant_uuid);
            if(enrollment_uuid == null) 
                return false;
            pruneSaveStateEntries(enrollment_uuid);
            String query = "INSERT INTO save_state (enrollment_uuid, TS, state_json) VALUES (?, GETUTCDATE(), ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, enrollment_uuid);
            stmt.setString(2, stateJSON);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return true;
    }

    public String getParticipantCurrentState(String partUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String result = "";
        try {
            String query = "SELECT TOP 1 JSON_VALUE(log_json,'$.state') AS state FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.protocol') != 'WeeklyMessage' AND JSON_VALUE(log_json, '$.protocol') != 'DailyMessage' AND JSON_VALUE(log_json, '$.protocol') IN (SELECT name FROM protocol_types WHERE protocol_type_uuid IN (SELECT protocol_type_uuid FROM enrollments WHERE participant_uuid = ? AND status = 1)) ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, partUUID);
            stmt.setString(2, partUUID);
            rs = stmt.executeQuery();

            if (rs.next()) {
                result = rs.getString(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            try { rs.close();   } catch (Exception e) { /* Null Ignored */ }
        }
        return result;
    }

    public String getSaveState(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String json = "";
        try {
            String query = "SELECT TOP 1 state_json FROM save_state WHERE enrollment_uuid = ? AND enrollment_uuid IN (SELECT enrollment_uuid FROM enrollments WHERE enrollment_uuid = ? AND status=1) ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            String enrollment_uuid = getEnrollmentUUID(participantUUID);
            stmt.setString(1, enrollment_uuid);
            stmt.setString(2, enrollment_uuid);
            rs = stmt.executeQuery();

            if (rs.next()) {
                json = rs.getString(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            try { rs.close();   } catch (Exception e) { /* Null Ignored */ }
        }
        
        return json;
    }

    public void saveStartCalTime(String participantUUID, long unixTS) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.start_cal_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, Long.toString(unixTS));
                stmt.setString(2, participantUUID);
                stmt.executeUpdate();
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("saveStartCalTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
    }

    public void saveSleepTime(String participantUUID, long unixTS) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.sleep_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'sleep' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, Long.toString(unixTS));
                stmt.setString(2, participantUUID);
                stmt.executeUpdate();
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("saveSleepTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
    }

    public long getStartCalTime(String participantUUID) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString;
        long unixTS = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                // changing this from TOP 1 bc a new log without time gets logged which will always make this return null
                String query = "SELECT JSON_VALUE(log_json, '$.start_cal_time') FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    unixString = rs.getString(1);
                    if (!rs.wasNull()) {
                        unixTS = Long.parseLong(unixString);
                        break;
                    }
                }
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("getStartCalTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { rs.close(); } catch (Exception e) { /* Null Ignored */ }
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
        return unixTS;
    }

    public long getSleepTime(String participantUUID) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString;
        long unixTS = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                // changing this from TOP 1 bc a new log without time gets logged which will always make this return null
                String query = "SELECT JSON_VALUE(log_json, '$.sleep_time') FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'sleep' ORDER BY TS DESC";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    unixString = rs.getString(1);
                    if(!rs.wasNull()){
                        unixTS = Long.parseLong(unixString);
                        break;
                    }
                }
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("getSleepTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
        return unixTS;
    }

    public void saveEndCalTime(String participantUUID, long unixTS) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.end_cal_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, Long.toString(unixTS));
                stmt.setString(2, participantUUID);
                stmt.executeUpdate();
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("saveEndCalTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
    }

    public void saveWakeTime(String participantUUID, long unixTS) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.wake_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'wake' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, Long.toString(unixTS));
                stmt.setString(2, participantUUID);
                stmt.executeUpdate();
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("saveWakeTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
    }

    public long getEndCalTime(String participantUUID) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString;
        long unixTS = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "SELECT JSON_VALUE(log_json, '$.end_cal_time') FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    unixString = rs.getString(1);
                    if(!rs.wasNull()){
                        unixTS = Long.parseLong(unixString);
                        break;
                    }
                }
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("getEndCalTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
        return unixTS;
    }

    public long getWakeTime(String participantUUID) {
        int retryCount = 0;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString;
        long unixTS = 0;

        while (retryCount < MAX_RETRIES) {
            try {
                String query = "SELECT JSON_VALUE(log_json, '$.wake_time') FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'wake' ORDER BY TS DESC";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                rs = stmt.executeQuery();

                while (rs.next()) {
                    unixString = rs.getString(1);
                    if(!rs.wasNull()){
                        unixTS = Long.parseLong(unixString);
                        break;
                    }
                }
                break;
            } catch (SQLException e) {
                if (e.getErrorCode() == 1205) { // SQL Server deadlock error code
                    // Handle deadlock
                    System.out.println("getWakeTime(): Deadlock occurred. Retrying.");
                    retryCount++;
                } else {
                    e.printStackTrace();
                    break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            } finally {
                try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
                try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
                try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
            }
        }
        return unixTS;
    }

    public void saveStartCalTimeCreateTemp(String participantUUID, long unixTS){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String json = "{\"state\": \"startcal\", \"start_cal_time\":" + unixTS + ", \"temp\": \"true\"}";
            String query = "INSERT INTO state_log VALUES (?, GETUTCDATE(), ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void removeTempStartCal(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            String query = "DELETE FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.temp') = 'true' AND JSON_VALUE(log_json, '$.state') = 'startcal'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void saveSleepTimeCreateTemp(String participantUUID, long unixTS){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String json = "{\"state\": \"sleep\", \"sleep_time\":" + unixTS + ", \"temp\": \"true\"}";
            String query = "INSERT INTO state_log VALUES (?, GETUTCDATE(), ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void removeTempSleep(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            String query = "DELETE FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.temp') = 'true' AND JSON_VALUE(log_json, '$.state') = 'sleep'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void saveEndCalTimeCreateTemp(String participantUUID, long unixTS){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String json = "{\"state\": \"endcal\", \"end_cal_time\":" + unixTS + ", \"temp\": \"true\"}";
            String query = "INSERT INTO state_log VALUES (?, GETUTCDATE(), ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void removeTempEndCal(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            String query = "DELETE FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.temp') = 'true' AND JSON_VALUE(log_json, '$.state') = 'endcal'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void saveWakeTimeCreateTemp(String participantUUID, long unixTS){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String json = "{\"state\": \"wake\", \"wake_time\":" + unixTS + ", \"temp\": \"true\"}";
            String query = "INSERT INTO state_log VALUES (?, GETUTCDATE(), ?)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.setString(2, json);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void removeTempWake(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        try{
            String query = "DELETE FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.temp') = 'true' AND JSON_VALUE(log_json, '$.state') = 'wake'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }



    public int getLastKnownSuccessCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int successCount = -1;

        try {
            String query = "SELECT JSON_VALUE(log_json, '$.successful_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int count = rs.getInt(1);
                if(!rs.wasNull()){
                    successCount = count;
                    break;
                }
            }
            if (successCount == -1) {
                addInitialSuccessCount(participantUUID);
                successCount = 0;
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return successCount;
    }

    public int getLastKnown8pmCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int successCount = -1;

        try {
            String query = "SELECT JSON_VALUE(log_json, '$.after8pm') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int count = rs.getInt(1);
                if(!rs.wasNull()){
                    successCount = count;
                    break;
                }
            }
            if (successCount == -1) {
                addInitialSuccessCount(participantUUID);
                successCount = 0;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return successCount;
    }

    public void addInitialSuccessCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
            query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.setString(2, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public int getLastKnownTotalCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int totalCount = -1;

        try {
             String query = "SELECT JSON_VALUE(log_json, '$.total_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int count = rs.getInt(1);
                if(!rs.wasNull()){
                    totalCount = count;
                    break;
                }
            }
            if (totalCount == -1) {
                addTotalCount(participantUUID);
                totalCount = 0;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return totalCount;
    }

    public void addTotalCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void setSuccessRate(String participantUUID, boolean wasSuccessful, boolean isRepeat, boolean isAfter8pm){
        Connection conn = null;
        PreparedStatement stmt = null;
        String query;

        try {
            // need to get values from second endcal to update to latest endcal
            int successCount = getLastKnownSuccessCount(participantUUID);
            int count8pm = getLastKnown8pmCount(participantUUID);
            int totalCount = getLastKnownTotalCount(participantUUID);
            boolean wasLastTRESuccessful = wasLastTRESuccessful(participantUUID);
            conn = ds.getConnection();
            if (!isRepeat) {
                if (totalCount == 0 && wasSuccessful) {
                    // initial addition to json
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                    stmt.setString(3, participantUUID);
                } else if (totalCount == 0 && !wasSuccessful) {
                    // initial addition to json
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                    stmt.setString(3, participantUUID);
                } else if (totalCount > 0 && wasSuccessful) {
                    // increment if successful
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        if (count8pm == 0) { count8pm = 1; }
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                    stmt.setString(3, participantUUID);
                } else if (totalCount > 0 && !wasSuccessful) {
                    // don't increment if not successful
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + successCount + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        if (count8pm == 0) { count8pm = 1; }
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                    stmt.setString(3, participantUUID);
                }
            } else {
                if (wasLastTRESuccessful && !wasSuccessful){
                    // decrement success rate, ignore total
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        if (count8pm == 0) { count8pm = 1; }
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                } else if (!wasLastTRESuccessful && wasSuccessful) {
                    // increment success rate, ignore total
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    if (isAfter8pm) {
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    } else {
                        if (count8pm == 0) { count8pm = 1; }
                        query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    }
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                }
            }
            if (stmt != null) {
                stmt.executeUpdate();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public String getSuccessRate(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String successRate = "";

        try {
            // successfulTRE/totalTRE
            String query = "  SELECT TOP 1 JSON_VALUE(log_json, '$.successful_TRE'), JSON_VALUE(log_json, '$.total_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' AND JSON_VALUE(log_json, '$.successful_TRE') IS NOT NULL AND JSON_VALUE(log_json, '$.total_TRE') IS NOT NULL ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int successCount = rs.getInt(1);
                int totalCount = rs.getInt(2);
                if(!rs.wasNull() && totalCount > 0){
                    successRate = String.format("%.2f%%", ((double)successCount/totalCount)*100);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return successRate;
    }

    /**
     * updates the already set success rate for dayoff messages sent after endcal
     * @param wasFastSuccessful boolean was the last endcal time successful
     * */
    public String updateSuccessRate(String participantUUID, boolean wasFastSuccessful, boolean wasAfter8pm){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String updatedSuccessRate = "";
        int successCount = getLastKnownSuccessCount(participantUUID);
        int count8pm = getLastKnown8pmCount(participantUUID);
        int totalCount = getLastKnownTotalCount(participantUUID);

        try {
            conn = ds.getConnection();
            String query = "";
            if (wasFastSuccessful){
                //subtract 1 from success and total
                if(successCount > 0) {
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                }
                if (totalCount > 0) {
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                }
                if (successCount > 0 || totalCount > 0) {
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    if (successCount > 0 && totalCount > 0) {
                        stmt.setString(2, participantUUID);
                    }
                }
            } else {
                // only subtract 1 from total and after 8pm count (if applicable)
                if (wasAfter8pm && count8pm > 0) {
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.after8pm', " + (count8pm - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                }
                if (totalCount > 0) {
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                }
                if ((wasAfter8pm && count8pm > 0) || totalCount > 0) {
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    if ((wasAfter8pm && count8pm > 0) && totalCount > 0) {
                        stmt.setString(2, participantUUID);
                    }
                }
            }

            if (stmt != null) {
                stmt.executeUpdate();
            }

            updatedSuccessRate = getSuccessRate(participantUUID);
            if (updatedSuccessRate.equals("")){
                updatedSuccessRate = "100.00%";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return updatedSuccessRate;
    }

    public boolean wasLastTRESuccessful(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean wasSuccess = false;
        int successNew = -1;
        int successOld = -1;
        int totalNew = -1;
        int totalOld = -1;

        try {
            // successfulTRE/totalTRE
            String query = "SELECT TOP 2 JSON_VALUE(log_json, '$.successful_TRE'), JSON_VALUE(log_json, '$.total_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' AND JSON_VALUE(log_json, '$.successful_TRE') IS NOT NULL AND JSON_VALUE(log_json, '$.total_TRE') IS NOT NULL ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if(rs.next()) {
                if(!rs.wasNull()){
                    successNew = rs.getInt(1);
                    totalNew = rs.getInt(2);
                    if(rs.next()){
                        if(!rs.wasNull()){
                            successOld = rs.getInt(1);
                            totalOld = rs.getInt(2);
                        }
                    }
                } else {
                    return false;
                }
            }

            if (successOld == -1 && successNew == totalNew) {
                // base case, only one ENDCAL
                wasSuccess = true;
            } else if (successOld == -1 && successNew != totalNew) {
                // base case, only one ENDCAL
                wasSuccess = false;
            } else if (successNew > successOld && totalNew > totalOld){
                // multiple endcals check old endcal against new endcal
                wasSuccess = true;
            } else if (successNew == successOld && totalNew > totalOld){
                wasSuccess = false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return wasSuccess;
    }

    public String getLastDayOff(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String timestamp = "";

        try{
            String query = "SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND (JSON_VALUE(log_json, '$.state') = 'dayOffWait' OR JSON_VALUE(log_json, '$.state') = 'dayOffWarn' OR JSON_VALUE(log_json, '$.state') = 'dayOffStartCal' OR JSON_VALUE(log_json, '$.state') = 'dayOffWarnEndCal') ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();
            
            if(rs.next()){
                timestamp = rs.getTimestamp("TS").toString();
                if(!rs.wasNull()){
                    timestamp = timestamp.split("\\.")[0];
                } else {
                    timestamp = "";
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return timestamp;
    }

    public Map<String, String> getProtocolFromParticipantId(String uuid) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Map<String, String> protocol = new HashMap<>();

        try{
            String query = "SELECT study, name FROM protocol_types WHERE protocol_type_uuid IN (SELECT protocol_type_uuid FROM enrollments WHERE participant_uuid = ? AND status = 1)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();

            while (rs.next()){
                protocol.put(rs.getString("study"), rs.getString("name"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return protocol;
    }


    public String getStudyFromParticipantId(String uuid) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String studies = "";

        try{
            String query = "SELECT study FROM participants WHERE participant_uuid=?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();

            if (rs.next()){
                studies = rs.getString("study");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return studies;
    }

    /**
     * Probably won't need this
     * @param protocol the protocol
     * @param participantUUID the participant uuid
     */
    public void addProtocolNameToLog(String protocol, String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.protocol', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WITH (UPDLOCK) WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endProtocol' ORDER BY TS DESC)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, protocol);
            stmt.setString(2, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public String getParticipantTimezone(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String tz = "";

        try{
            String query = "SELECT JSON_VALUE(participant_json, '$.time_zone') AS tz FROM participants WHERE participant_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if(rs.next()){
                tz = rs.getString("tz");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return tz;
    }

    public int getNumberOfCycles(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int numCycles = 0;

        try {
            String query = "SELECT COUNT(participant_uuid) AS numCycles FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'resetEpisodeVariables'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if(rs.next()){
                numCycles = rs.getInt("numCycles");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return numCycles;
    }

    public void checkQueuedMessageDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query = "SELECT message_uuid, study, toNumber, JSON_VALUE(message_json, '$.Body') AS body FROM queued_messages WHERE scheduledFor <= GETUTCDATE()";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String toNumber = rs.getString("toNumber");
                String messageJson = rs.getString("body");
                String messageId = rs.getString("message_uuid");
                String study = rs.getString("study");
                if (toNumber.equals(Launcher.adminPhoneNumber)) {
                    Launcher.msgUtils.sendMessage(toNumber, messageJson, true, study);
                } else {
                    Launcher.msgUtils.sendMessage(toNumber, messageJson, false, study);
                }
                removeFromQueuedMessage(messageId);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public void removeFromQueuedMessage(String messageId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            String query = "DELETE FROM queued_messages where message_uuid = ?";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, messageId);
            stmt.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    /**
     * Adds a study admin as a "participant" that cannot be added to a protocol
     */
    public void addStudyAdmin() {
        Boolean isAdded = checkIfAdminAdded();
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            if (isAdded){
                // if already added update phone number and timezone
                String query = "UPDATE participants SET participant_json = ? WHERE participant_uuid = '00000000-0000-0000-0000-000000000000'";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                String formattedTZ = Launcher.adminTimeZone.replace("/", "\\/");
                stmt.setString(1, "{\"first_name\":\"Study\",\"last_name\":\"Admin\",\"number\":\"" + Launcher.adminPhoneNumber + "\",\"time_zone\":\"" + formattedTZ + "\"}");
                stmt.executeUpdate();
            } else {
                // if not added, add a study admin
                String query = "INSERT INTO participants (participant_uuid, study, participant_json) VALUES (?, ?, ?)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, "00000000-0000-0000-0000-000000000000");
                stmt.setString(2, "ADMIN");
                String formattedTZ = Launcher.adminTimeZone.replace("/", "\\/");
                stmt.setString(3, "{\"first_name\":\"Study\",\"last_name\":\"Admin\",\"number\":\"" + Launcher.adminPhoneNumber + "\",\"time_zone\":\"" + formattedTZ + "\"}");
                stmt.executeUpdate();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public Boolean checkIfAdminAdded() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean isAdded = false;

        try {
            String query = "SELECT COUNT(participant_uuid) AS numAdmins FROM participants WHERE study = 'ADMIN'";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            while (rs.next()) {
                if (rs.getInt("numAdmins") > 0) {
                    isAdded = true;
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return isAdded;
    }

    public long getLastStartCalTime(String participantId){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        long lastTime = -1L;

        try {
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.start_cal_time') AS time FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                lastTime = rs.getLong("time");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return lastTime;
    }
}
