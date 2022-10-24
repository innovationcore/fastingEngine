package fasting.Database;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fasting.Launcher;
import org.apache.commons.dbcp2.*;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.time.LocalTime;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

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
        if((login == null) && (password == null)) {
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

    //SELECT id, phone_number, participant_type FROM fasting.participants WHERE participant_type='other_participation_type'


    public List<Map<String,String>> getAccessLogs() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        List<Map<String,String>> accessMapList = null;
        try {
            accessMapList = new ArrayList<>();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
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

    String getEnrollmentUUID(String uuid){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String result = null;
        try {
            String query = "SELECT enrollment_uuid FROM enrollments WHERE participant_uuid = ?";
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

    public void uploadSaveState(String stateJSON, String participant_uuid){
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            String enrollment_uuid = getEnrollmentUUID(participant_uuid);
            pruneSaveStateEntries(enrollment_uuid);
            String query = "INSERT INTO save_state (enrollment_uuid, TS, state_json) VALUES (?, GETDATE(), ?)";
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
    }

    public String getParticipantCurrentState(String partUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String result = "";
        try {
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.state') FROM state_log WHERE participant_uuid = ? ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, partUUID);
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
            String query = "SELECT TOP 1 state_json FROM save_state WHERE enrollment_uuid = ? ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, getEnrollmentUUID(participantUUID));
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
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.start_cal_time') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'startcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if (rs.next()) {
                unixString = rs.getString(1);
                if(!rs.wasNull()){
                    unixTS = Long.parseLong(unixString);
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

    public long getEndCalTime(String participantUUID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String unixString = "";
        long unixTS = 0;

        try {
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.end_cal_time') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if (rs.next()) {
                unixString = rs.getString(1);
                if(!rs.wasNull()){
                    unixTS = Long.parseLong(unixString);
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

    public boolean doSuccessFieldsExist(String participantUUID){
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean successSet = false;

        try {
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.total_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if (rs.next()) {
                successSet = !rs.wasNull();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try { rs.close(); }   catch (Exception e) { /* Null Ignored */ }
            try { stmt.close(); } catch (Exception e) { /* Null Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Null Ignored */ }
        }
        return successSet;
    }

    public void setSuccessRate(String participantUUID, boolean wasSucessful){
        Connection conn = null;
        PreparedStatement stmt = null;
        String query = "";
        //success rate = (between 9-11 hours)/total
        try {
            boolean isSuccessIncluded = doSuccessFieldsExist(participantUUID);
            if (!isSuccessIncluded && wasSucessful) {
                // initial addition to json
                query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC);";
                query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                stmt.setString(2, participantUUID);
            } else if (!isSuccessIncluded && !wasSucessful) {
                // initial addition to json
                query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', 0) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC);";
                query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                stmt.setString(2, participantUUID);
            } else if (isSuccessIncluded && wasSucessful) {
                // increment if successful
                query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.successful_TRE', JSON_VALUE(log_json, '$.successful_TRE') + 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC);";
                query += "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', JSON_VALUE(log_json, '$.total_TRE') + 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
                stmt.setString(2, participantUUID);
            } else {
                // don't increment if not successful
                query = "UPDATE state_log SET log_json=JSON_MODIFY(log_json, '$.total_TRE', JSON_VALUE(log_json, '$.total_TRE') + 1) WHERE TS IN (SELECT TOP 1 TS FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC)";
                conn = ds.getConnection();
                stmt = conn.prepareStatement(query);
                stmt.setString(1, participantUUID);
            }
            stmt.executeUpdate();
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
            String query = "SELECT TOP 1 JSON_VALUE(log_json, '$.successful_TRE'), JSON_VALUE(log_json, '$.total_TRE') FROM state_log WHERE participant_uuid = ? AND JSON_VALUE(log_json, '$.state') = 'endcal' ORDER BY TS DESC";
            conn = ds.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, participantUUID);
            rs = stmt.executeQuery();

            if (rs.next()) {
                int successCount = Integer.parseInt(rs.getString(1));
                int totalCount = Integer.parseInt(rs.getString(2));
                successRate = String.format("%.2f%%", (double)successCount/totalCount);
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

}
