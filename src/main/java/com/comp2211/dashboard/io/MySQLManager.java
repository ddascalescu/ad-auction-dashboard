package com.comp2211.dashboard.io;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.apache.commons.dbutils.DbUtils;
import com.comp2211.dashboard.model.data.Demographics.Demographic;
import com.comp2211.dashboard.model.data.Demographics;
import com.comp2211.dashboard.util.Security;
import com.comp2211.dashboard.util.UserSession;

public class MySQLManager extends DatabaseManager {

  public MySQLManager() {
    super("64.227.36.253","3306","seg","seg23","exw3karpouziastinakri", Table.click_table.toString(), Table.impression_table.toString(), Table.server_table.toString());
  }

  public MySQLManager(final String host, final String port, final String db, final String user, final String pw) {
    super( host, port, db, user, pw, Table.click_table.toString(), Table.impression_table.toString(), Table.server_table.toString());
  }

  public MySQLManager(final String host, final String port, final String db, final String user, final String pw, final String c_table, final String i_table, final String s_table) {
    super(host, port, db, user, pw, c_table, i_table, s_table);
  }

  /**
   * Retrieve the total for the specified type.
   * @param type the type of cost to retrieve.
   * @return BigDecimal representing the total cost.
   */
  @Override
  public BigDecimal retrieveTotalCost(Cost type) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      StringBuilder sb = new StringBuilder("SELECT SUM(");
      sb.append(type.toString());
      sb.append(") AS SUM FROM ");
      if (type.equals(Cost.Click_Cost)) {
        sb.append(click_table);
      } else {
        sb.append(impression_table);
      }
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getBigDecimal("SUM");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return BigDecimal.ZERO;
  }

  /**
   * Retrieve the number of bounces by time
   * @param maxSeconds the maximum time in seconds for which a bounce is registered
   * @param allowInf whether entries with no exit time will be counted
   * @return long value of the number of bounces
   */
  @Override
  public long retrieveBouncesCountByTime(long maxSeconds, boolean allowInf) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      StringBuilder sb = new StringBuilder("SELECT COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE (Exit_Date - Entry_Date) <= ?");
      if (allowInf) {
        sb.append(" OR Exit_Date IS NULL");
      }
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      stmt.setLong(1, maxSeconds);
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong("COUNT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return 0L;
  }

  /**
   * Retrieve the number of bounces by number of pages visited
   * @param maxPages the maximum pages visited for which a bounce is registered
   * @return long value of the number of bounces
   */
  @Override
  public long retrieveBouncesCountByPages(byte maxPages) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      StringBuilder sb = new StringBuilder("SELECT COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE Pages_Viewed <= ?");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      stmt.setByte(1, maxPages);
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong("COUNT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return 0L;
  }

  /**
   * Retrieve the total number of acquisitions
   * @return total count
   */
  public long retrieveAcquisitionCount() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      StringBuilder sb = new StringBuilder("SELECT COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE Conversion = 1");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong("COUNT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return 0L;
  }

  /**
   * Retrieve the average acquisition cost.
   * @return the calculated average acquisition cost
   */
  @Override
  public BigDecimal retrieveAverageAcquisitionCost() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      StringBuilder sb = new StringBuilder("SELECT AVG(Click_Cost) AS AVG FROM ");
      sb.append(click_table);
      sb.append(" WHERE ID IN (SELECT DISTINCT ID FROM ");
      sb.append(server_table);
      sb.append(" WHERE Conversion = 1)");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getBigDecimal("AVG");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return BigDecimal.ZERO;
  }

  /**
   * Retrieve the average click cost for each date.
   * @return a map with each date as keys and the avg for that date as a value
   */
  @Override
  public HashMap<String, BigDecimal> retrieveDatedAverageCost(Cost type) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, BigDecimal> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Date) AS DateOnly, AVG(");
      sb.append(type.toString());
      sb.append(") AS AVG FROM ");
      if (type.equals(Cost.Click_Cost)) {
        sb.append(click_table);
      } else {
        sb.append(impression_table);
      }
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getBigDecimal("AVG"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the average acquisition cost for each date.
   * @return a map with each date as keys and the avg for that date as a value
   */
  @Override
  public HashMap<String, BigDecimal> retrieveDatedAverageAcquisitionCost() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, BigDecimal> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Date) AS DateOnly, AVG(Click_Cost) AS AVG FROM ");
      sb.append(click_table);
      sb.append(" WHERE ID IN (SELECT DISTINCT ID FROM ");
      sb.append(server_table);
      sb.append(" WHERE Conversion = 1) GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getBigDecimal("AVG"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of impressions for each date.
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedImpressionTotals() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Date) AS DateOnly, COUNT(*) AS COUNT FROM ");
      sb.append(impression_table);
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of clicks for each date.
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedClickTotals() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Date) AS DateOnly, COUNT(*) AS COUNT FROM ");
      sb.append(click_table);
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of uniques for each date.
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedUniqueTotals() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Date) AS DateOnly, COUNT(DISTINCT ID) AS COUNT FROM ");
      sb.append(click_table);
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of bounces (by time) for each date.
   * @param maxSeconds the maximum time in seconds for which a bounce is registered
   * @param allowInf whether entries with no exit time will be counted
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedBounceTotalsByTime(long maxSeconds, boolean allowInf) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Entry_Date) AS DateOnly, COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE (Exit_Date - Entry_Date) <= ?");
      if (allowInf) {
        sb.append(" OR Exit_Date IS NULL");
      }
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      stmt.setLong(1, maxSeconds);
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of bounces (by pages visited) for each date.
   * @param maxPages the maximum pages visited for which a bounce is registered
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedBounceTotalsByPages(byte maxPages) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Entry_Date) AS DateOnly, COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE Pages_Viewed <= ?");
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      stmt.setByte(1, maxPages);
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the total number of acquisitions for each date.
   * @return a map with each date as keys and the total for that date as a value
   */
  @Override
  public HashMap<String, Long> retrieveDatedAcquisitionTotals() {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT DATE(Entry_Date) AS DateOnly, COUNT(*) AS COUNT FROM ");
      sb.append(server_table);
      sb.append(" WHERE Conversion = 1");
      sb.append(" GROUP BY DateOnly");
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        resultMap.put(rs.getString("DateOnly"), rs.getLong("COUNT"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve demographics with count
   * @param type the type of demographics to retrieve
   * @return a map with each demographic as keys and the count for that demographic as a value
   */
  @Override
  public HashMap<String, Long> retrieveDemographics(Demographic type) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    HashMap<String, Long> resultMap = new LinkedHashMap<>();
    try {
      StringBuilder sb = new StringBuilder("SELECT COUNT(*), ");
      sb.append(type.toString());
      sb.append(" FROM ");
      sb.append(impression_table);
      sb.append(" GROUP BY ");
      sb.append(type.toString());
      stmt = sqlDatabase.getConnection().prepareStatement(sb.toString());
      rs = stmt.executeQuery();
      while (rs.next()) {
        String key = Demographics.getDemographicString(type, rs.getByte(type.toString()));
        resultMap.put(key, rs.getLong("COUNT(*)"));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return resultMap;
  }

  /**
   * Retrieve the amount of entries in the specified database table
   * @param table the Table to check
   * @return the amount of entries found
   */
  @Override
  public long retrieveDataCount(String table, boolean unique) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      String query = unique ? "SELECT COUNT(DISTINCT ID) AS COUNT FROM " : "SELECT COUNT(*) AS COUNT FROM ";
      stmt = sqlDatabase.getConnection().prepareStatement(query + table);
      rs = stmt.executeQuery();
      if (rs.next()) {
        return rs.getLong("COUNT");
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return 0L;
  }

  /**
   * Attempt to login using given credentials and create UserSession
   * @param username the username to use during login
   * @param password the password to use during login
   * @return true if login is successful, false otherwise
   */
  @Override
  public boolean attemptUserLogin(String username, String password) {
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
      stmt = sqlDatabase.getConnection().prepareStatement("SELECT * FROM credentials WHERE username = ? LIMIT 1");
      stmt.setString(1, username);
      rs = stmt.executeQuery();
      if(!rs.next()) {
        return false;
      }
      if(Security.matchPassword(password, rs.getBytes("password"), rs.getBytes("salt"))) {
        String campaigns = rs.getString("campaigns");
        boolean access = rs.getBoolean("full_access");
        UserSession.initializeSession(username, campaigns, access);
        return true;
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(stmt);
    }
    return false;
  }
}
