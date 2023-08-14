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

    private Gson gson;
    private DataSource ds;
    public DBEngine() {

        try {
            gson = new Gson();
            //Driver needs to be identified in order to load the namespace in the JVM
            //String dbDriver = "com.mysql.cj.jdbc.Driver";
            String dbDriver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

            Class.forName(dbDriver).newInstance();

            //String dbConnectionString = "jdbc:mysql://" + Launcher.config.getStringParam("db_host") + "/" + Launcher.config.getStringParam("db_name") + "?" + "user=" + Launcher.config.getStringParam("db_user") + "&password=" + Launcher.config.getStringParam("db_password");
            String dbConnectionString = "jdbc:sqlserver://" + Launcher.config.getStringParam("db_host") +":"+ 1433 + ";databaseName=" + Launcher.config.getStringParam("db_name") + ";encrypt=false";

            //ds = setupDataSource(dbConnectionString);
            ds = setupDataSource(dbConnectionString, Launcher.config.getStringParam("db_user"), Launcher.config.getStringParam("db_password"));

            /*
            if(!databaseExist(databaseName)) {
                System.out.println("No fasting.database, creating " + databaseName);
                initDB();
            } else {
                System.out.println("Database found, removing " + databaseName);
                delete(Paths.get(databaseName).toFile());
                System.out.println("Creating " + databaseName);
                initDB();
            }
             */

            //initDB();
        }

        catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static DataSource setupDataSource(String connectURI) {
        //
        // First, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        //
        ConnectionFactory connectionFactory = null;
        connectionFactory = new DriverManagerConnectionFactory(connectURI, null);


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
        //
        PoolingDataSource<PoolableConnection> dataSource =
                new PoolingDataSource<>(connectionPool);

        return dataSource;
    }

    public static DataSource setupDataSource(String connectURI, String login, String password) {
        //
        // First, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        // We'll use the DriverManagerConnectionFactory,
        // using the connect string passed in the command line
        // arguments.
        //
        ConnectionFactory connectionFactory = null;
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
        //
        PoolingDataSource<PoolableConnection> dataSource =
                new PoolingDataSource<>(connectionPool);

        return dataSource;
    }


    public List<Map<String,String>> getParticipant(String ParticipantType) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Map<String,String>> participantMapList = null;
        try {
            participantMapList = new ArrayList<>();
            String queryString = "SELECT id, participant_id, phone_number, participant_type FROM fasting.participants " +
                    "WHERE participant_type = ?";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, ParticipantType);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> accessMap = new HashMap<>();
                accessMap.put("id", rs.getString("id"));
                accessMap.put("participant_id", rs.getString("participant_id"));
                accessMap.put("phone_number", rs.getString("phone_number"));
                accessMap.put("participant_type", rs.getString("participant_type"));
                participantMapList.add(accessMap);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return participantMapList;
    }

    public String getParticipantIdFromPhoneNumber(String PhoneNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String participantId = null;
        try {
            String queryString = "SELECT participant_uuid FROM participants WHERE JSON_VALUE(participant_json, '$.number') = ?";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, PhoneNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                participantId = rs.getString("participant_uuid");
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

    public List<Map<String,String>> getParticipantMapByGroup(String groupName) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        List<Map<String,String>> participantMaps = null;
        try {
            participantMaps = new ArrayList<>();

            String queryString = "SELECT participant_uuid, participant_json FROM participants WHERE JSON_VALUE(participant_json, '$.group') = ?";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, groupName);
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


    public String getParticipantIdFromPhoneNumberOld(String PhoneNumber) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String participantId = null;
        try {
            String queryString = "SELECT id, participant_id, phone_number, participant_type FROM fasting.participants " +
                    "WHERE phone_number = ?";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(queryString);
            stmt.setString(1, PhoneNumber);
            rs = stmt.executeQuery();
            if (rs.next()) {
                participantId = rs.getString("participant_id");
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

    public boolean databaseExist(String databaseName)  {
        boolean exist = false;
        try {

            if(!ds.getConnection().isClosed()) {
                exist = true;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return exist;
    }

    public boolean tableExist(String tableName)  {
        boolean exist = false;

        ResultSet result = null;
        DatabaseMetaData metadata = null;

        try {
            metadata = ds.getConnection().getMetaData();
            result = metadata.getTables(null, null, tableName.toUpperCase(), null);

            if(result.next()) {
                exist = true;
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { result.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return exist;
    }


    public List<Map<String,String>> getAccessLogs() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<Map<String,String>> accessMapList = null;
        try {
            accessMapList = new ArrayList<>();
            String queryString = "SELECT * FROM accesslog";

            conn = ds.getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(queryString);

            while (rs.next()) {
                Map<String, String> accessMap = new HashMap<>();
                accessMap.put("remote_ip", rs.getString("remote_ip"));
                accessMap.put("access_ts", rs.getString("access_ts"));
                accessMapList.add(accessMap);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return accessMapList;
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
        Connection conn = null;
        PreparedStatement stmt = null;

        try {

            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.start_cal_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";

            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, Long.toString(unixTS));
            stmt.setString(2, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
    }

    public long getStartCalTime(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString = "";
        long unixTS = 0;

        try {
            // changing this from TOP 1 bc a new log without time gets logged which will always make this return null
            String query = "SELECT JSON_VALUE(log_json, '$.start_cal_time') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
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
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return unixTS;
    }

    public void saveEndCalTime(String participantUUID, long unixTS) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.end_cal_time', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, Long.toString(unixTS));
            stmt.setString(2, participantUUID);
            stmt.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
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

    public long getEndCalTime(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString = "";
        long unixTS = 0;

        try {
            String query = "SELECT JSON_VALUE(log_json, '$.end_cal_time') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC";
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
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return unixTS;
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

    public void addInitialSuccessCount(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
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

    public void setSuccessRate(String participantUUID, boolean wasSuccessful, boolean isRepeat){
        Connection conn = null;
        PreparedStatement stmt = null;
        String query = "";

        try {
            // need to get values from second endcal to update to latest endcal
            int successCount = getLastKnownSuccessCount(participantUUID);
            int totalCount = getLastKnownTotalCount(participantUUID);
            boolean wasLastTRESuccessful = wasLastTRESuccessful(participantUUID);
            conn = ds.getConnection();
            if (!isRepeat) {
                if (totalCount == 0 && wasSuccessful) {
                    // initial addition to json
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                } else if (totalCount == 0 && !wasSuccessful) {
                    // initial addition to json
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                } else if (totalCount > 0 && wasSuccessful) {
                    // increment if successful
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                } else if (totalCount > 0 && !wasSuccessful) {
                    // don't increment if not successful
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + successCount + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    stmt.setString(2, participantUUID);
                }
            } else {
                if (wasLastTRESuccessful && !wasSuccessful){
                    // decrement success rate, ignore total
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                } else if (!wasLastTRESuccessful && wasSuccessful) {
                    // increment success rate, ignore total
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount + 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
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
    public String updateSuccessRate(String participantUUID, boolean wasFastSuccessful){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String updatedSuccessRate = "";
        int successCount = getLastKnownSuccessCount(participantUUID);
        int totalCount = getLastKnownTotalCount(participantUUID);

        try {
            conn = ds.getConnection();
            String query = "";
            if (wasFastSuccessful){
                //subtract 1 from success and total
                if(successCount > 0) {
                    query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', " + (successCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC);";
                }
                if (totalCount > 0) {
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                }
                if (successCount > 0 || totalCount > 0) {
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
                    if (successCount > 0 && totalCount > 0) {
                        stmt.setString(2, participantUUID);
                    }
                }
            } else {
                // only subtract 1 from total
                if (totalCount > 0) {
                    query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', " + (totalCount - 1) + ") WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC)";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, participantUUID);
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

    public String getProtocolFromParticipantId(String uuid) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String protocol = "";

        try{
            String query = "SELECT name FROM protocol_types WHERE protocol_type_uuid IN (SELECT protocol_type_uuid FROM enrollments WHERE participant_uuid = ? AND status = 1)";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, uuid);
            rs = stmt.executeQuery();

            if(rs.next()){
                protocol = rs.getString("name");
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

    /**
     * Probably won't need this
     * @param protocol
     * @param participantUUID
     */
    public void addProtocolNameToLog(String protocol, String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            String query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.protocol', ?) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endProtocol' ORDER BY TS DESC)";
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
}
