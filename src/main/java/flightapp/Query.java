package flightapp;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//import flightapp.PasswordUtils;
//import flightapp.QueryAbstract;
import java.util.*;

import java.sql.Connection;


/**
 * Runs queries against a back-end database
 */
public class Query extends QueryAbstract {
  //
  // Canned queries
  //
  private static final String FLIGHT_CAPACITY_SQL = "SELECT capacity FROM Flights WHERE fid = ?";
  private PreparedStatement flightCapacityStmt;

  ///////Check table suffix
  private static final String CLEAR_USER_TABLE = "DELETE FROM USERS_vishksat";
  private PreparedStatement clearUserStatement;


  private static final String CLEAR_RESERVATION_TABLE = "DELETE FROM RESERVATIONS_vishksat";
  private PreparedStatement clearReservationStatement;


  private static final String CREATE_USER = "INSERT INTO USERS_vishksat(username, hashedpass, balance) VALUES(?, ?, ?)";
  private PreparedStatement createUserStatement;


  private static final String GET_USER = "SELECT hashedpass, balance FROM USERS_vishksat WHERE username = ?";
  private PreparedStatement getUserStatement;

  private static final String SEARCH = "SELECT TOP (?) fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, " +
            "actual_time, capacity, price FROM Flights WHERE origin_city = ? AND dest_city = ? AND day_of_month =  ? AND canceled != 1 ORDER BY actual_time, fid ASC";
  private PreparedStatement searchStatement;

  private static final String DIRECT_SEARCH =
            "SELECT TOP (?) F1.fid, F2.fid, F1.actual_time + F2.actual_time as actual_time, F1.price + F2.price as price " +
                    "FROM Flights AS F1, Flights AS F2 WHERE F1.origin_city = ? AND F2.dest_city = ? AND F1.dest_city = F2.origin_city " +
                    "AND F1.day_of_month =  ? AND F2.day_of_month =  ? AND F1.canceled != 1 AND F2.canceled != 1 ORDER BY actual_time, F1.fid, F2.fid ASC";
  private PreparedStatement directSearchStatement;

  private static final String GET_FLIGHT = "SELECT fid, day_of_month, carrier_id, flight_num, origin_city, dest_city, actual_time, capacity, price " +
            "FROM Flights WHERE fid = ?";
  private PreparedStatement getFlightStatement;

  private static final String GET_RESERVATION = "SELECT rid, paid, canceled, onehop, date, price, origin_city, dest_city, uname, flightID1, flightID2 FROM RESERVATIONS_vishksat WHERE uname = ? AND canceled != 1 ORDER BY rid ASC";
  private PreparedStatement getReservationStatement;

  private static final String CHECK_RESERVATION = "SELECT COUNT(*) FROM RESERVATIONS_vishksat WHERE uname = ? and date = ?";
  private PreparedStatement checkReservationStatement;

  private static final String GET_MAX_RID = "SELECT MAX(rid) FROM RESERVATIONS_vishksat";
  private PreparedStatement getMaxRidStatement;

  private static final String CREATE_RESERVATION = "INSERT INTO RESERVATIONS_vishksat(rid, paid, canceled, onehop, date, price, origin_city, dest_city, uname, flightID1, flightID2) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  private PreparedStatement createReservationStatement;

  private static final String GET_INFO_RESERVATION = "SELECT price, uname FROM RESERVATIONS_vishksat where rid = ? AND paid = 0";
  private PreparedStatement getInfoReservationStatement;

  private static final String GET_BALANCE = "SELECT balance from USERS_vishksat WHERE username = ?";
  private PreparedStatement getBalanceStatement;

  private static final String UPDATE_BALANCE = "UPDATE USERS_vishksat SET balance = ? WHERE username = ?";
  private PreparedStatement updateBalanceStatement;

  private static final String UPDATE_RESERVATION = "UPDATE RESERVATIONS_vishksat SET paid = 1 WHERE rid = ?";
  private PreparedStatement updateReservationStatement;

  private static final String CHECK_BALANCE = "SELECT balance FROM USERS_vishksat WHERE username = ?";
  private PreparedStatement checkBalanceStatement;

  private boolean sess_login;

  private String sess_user;

  private List<Itinerary> itineraries;

  //
  // Instance variables
  //

  protected Query() throws SQLException, IOException {
    prepareStatements();

    sess_login = false;
    sess_user = "";

    itineraries = new ArrayList<>();

  }

  /**
   * Clear the data in any custom tables created.
   * 
   * WARNING! Do not drop any tables and do not clear the flights table.
   */
  public void clearTables() {
    
    try {

      clearReservationStatement.executeUpdate();

      clearUserStatement.executeUpdate();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void closeConnection() throws SQLException {
    clearTables();
    conn.close();
  }

  /*
   * prepare all the SQL statements in this method.
   */
  private void prepareStatements() throws SQLException {
    flightCapacityStmt = conn.prepareStatement(FLIGHT_CAPACITY_SQL);

    // TODO: YOUR CODE HERE

    clearReservationStatement = conn.prepareStatement(CLEAR_RESERVATION_TABLE);
    clearUserStatement = conn.prepareStatement(CLEAR_USER_TABLE);

    getUserStatement = conn.prepareStatement(GET_USER);
    createUserStatement = conn.prepareStatement(CREATE_USER);

    searchStatement = conn.prepareStatement(SEARCH);

    directSearchStatement = conn.prepareStatement(DIRECT_SEARCH);
    getFlightStatement = conn.prepareStatement(GET_FLIGHT);

    getReservationStatement = conn.prepareStatement(GET_RESERVATION);

    checkReservationStatement = conn.prepareStatement(CHECK_RESERVATION);

    getMaxRidStatement = conn.prepareStatement(GET_MAX_RID);

    createReservationStatement = conn.prepareStatement(CREATE_RESERVATION);

    getInfoReservationStatement = conn.prepareStatement(GET_INFO_RESERVATION);

    getBalanceStatement = conn.prepareStatement(GET_BALANCE);

    updateBalanceStatement = conn.prepareStatement(UPDATE_BALANCE);

    getInfoReservationStatement = conn.prepareStatement(GET_INFO_RESERVATION);

    checkBalanceStatement = conn.prepareStatement(CHECK_BALANCE);


  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_login(String username, String password) {
  
    try {
      if(!sess_login) {
        String caseInsensUsername = username.toLowerCase();
        getUserStatement.clearParameters();
        getUserStatement.setString(1, caseInsensUsername);

        ResultSet resSet = getUserStatement.executeQuery();

        resSet.next();

        byte[] saltedPass = resSet.getBytes("hashedpass");

        resSet.close();

        if(PasswordUtils.plaintextMatchesSaltedHash(password, saltedPass)) {

          sess_login = true;
          
          sess_user = caseInsensUsername;

          return "Logged in as " + username + "\n";
        }

        else {
          return "Login failed\n";
        }
      } 
        
      return "User already logged in\n";

    } 

    catch (SQLException e){
      return "Login failed\n";
    }

  }


  /* See QueryAbstract.java for javadoc */
  public String transaction_createCustomer(String username, String password, int initAmount) {
    // TODO: YOUR CODE HERE

    try {

      if(initAmount < 0) {
        return "Failed to create user\n";
      }

    String caseInsensUsername = username.toLowerCase();

    byte[] hash = PasswordUtils.saltAndHashPassword(password);

    createUserStatement.clearParameters();

    createUserStatement.setString(1, caseInsensUsername);

    createUserStatement.setBytes(2, hash);

    createUserStatement.setInt(3, initAmount);

    int success = createUserStatement.executeUpdate();

  
    return "Created user " + username + "\n";


    }

    catch (SQLException e){

      System.out.println(e.getMessage());

      return "Failed to create user\n";

    }

  }

  /* See QueryAbstract.java for javadoc */
  public String transaction_search(String originCity, String destinationCity, 
                                   boolean directFlight, int dayOfMonth,
                                   int numberOfItineraries) {
    // Account for susceptiblity to SQL injection attacks while using JDBC. 
    // Handle searches for both indirect and direct flights

    StringBuffer sb = new StringBuffer();

      try {

        searchStatement.clearParameters();
        searchStatement.setInt(1, numberOfItineraries);
        searchStatement.setString(2, originCity);
        searchStatement.setString(3, destinationCity);
        searchStatement.setInt(4, dayOfMonth);

        ResultSet oneHopResults = searchStatement.executeQuery();

        itineraries.clear();

        while(itineraries.size() < numberOfItineraries && oneHopResults.next()) {
          
          Flights fly = new Flights();

          fly.fid = oneHopResults.getInt("fid");
          fly.dayOfMonth = oneHopResults.getInt("day_of_month");
          fly.carrierId = oneHopResults.getString("carrier_id");
          fly.flightNum = oneHopResults.getString("flight_num");
          fly.originCity = oneHopResults.getString("origin_city");
          fly.destCity = oneHopResults.getString("dest_city");
          fly.time = oneHopResults.getInt("actual_time");
          fly.capacity = oneHopResults.getInt("capacity");
          fly.price = oneHopResults.getInt("price");


          Itinerary i = new Itinerary(fly, null, true, fly.time, fly.dayOfMonth, fly.price, fly.originCity, fly.destCity);
        

          itineraries.add(i);

        }
 
        oneHopResults.close();


        if(itineraries.size() < numberOfItineraries && !directFlight) {
          
          int x = numberOfItineraries - itineraries.size();
          
          directSearchStatement.clearParameters();
          directSearchStatement.setInt(1, x);
          directSearchStatement.setString(2, originCity);
          directSearchStatement.setString(3, destinationCity);
          directSearchStatement.setInt(4, dayOfMonth);
          directSearchStatement.setInt(5, dayOfMonth);

          ResultSet indirectResults = directSearchStatement.executeQuery();

          while(indirectResults.next()) {

            int fid1 = indirectResults.getInt(1);
            int fid2 = indirectResults.getInt(2);
            int time = indirectResults.getInt(3);
            int price = indirectResults.getInt(4);

            Flights flight1 = getFlightFromDatabase(fid1);
            Flights flight2 = getFlightFromDatabase(fid2);

            Itinerary i = new Itinerary(flight1, flight2, false, time, price, flight1.dayOfMonth, flight1.originCity, flight2.destCity);  

            itineraries.add(i);

          }

          indirectResults.close();
        }

        Collections.sort(itineraries, Comparator.comparingInt(itinerary -> itinerary.duration));

        for (int i = 0; i < Math.min(numberOfItineraries, itineraries.size()); i++) {
            Itinerary itinerary = itineraries.get(i);
            sb.append("Itinerary ").append(i).append(": ")
              .append(itinerary.direct ? "1 flight(s)" : "2 flight(s)").append(", ")
              .append(itinerary.duration).append(" minutes\n")
              .append(itinerary.id1.toString()).append("\n");
            if (!itinerary.direct) {
                sb.append(itinerary.id2.toString()).append("\n");
            }
        }

        //System.out.println(itineraries.size());
        //System.out.println(itineraries);

        //System.out.println("itin 0 has flights: " + itineraries.get(0));
        return sb.toString();


      }

      catch(SQLException e) {
        e.printStackTrace();
        return "Failed to search\n";
      }

  }

  private Flights getFlightFromDatabase(int fid) throws SQLException {

    getFlightStatement.clearParameters();

    getFlightStatement.setInt(1, fid);

    ResultSet f = getFlightStatement.executeQuery();

    f.next();

    Flights fly = new Flights();
    
    fly.fid = f.getInt("fid");
    fly.dayOfMonth = f.getInt("day_of_month");
    fly.carrierId = f.getString("carrier_id");
    fly.flightNum = f.getString("flight_num");
    fly.originCity = f.getString("origin_city");
    fly.destCity = f.getString("dest_city");
    fly.time = f.getInt("actual_time");
    fly.capacity = f.getInt("capacity");
    fly.price = f.getInt("price");
    
    f.close();

    return fly;

  }

  class Itinerary implements Comparable<Itinerary>{

    public Flights id1;
    public Flights id2;
    public boolean direct;
    public int duration;
    public int date;
    public int price;
    public String origin_city;
    public String dest_city;

    public Itinerary(Flights id1, Flights id2, boolean direct, int duration, int date, int price, String origin_city, String dest_city) {

      this.id1=id1;
      this.direct = direct;
      this.duration = duration;
      this.date = date;
      this.price = price;
      this.origin_city = origin_city;
      this.dest_city = dest_city;
      

      if (direct){
          this.id2=null;
          this.duration = id1.time;
      } else if (!direct){
        this.id2=id2;
        this.duration = id1.time + id2.time;
      }

      this.direct = direct;

    }

    @Override
    public int compareTo(Itinerary o) {
        return Integer.compare(this.duration, o.duration);
    }

    @Override
    public String toString() {
        return "ID: " + id1.fid + " Day: " + id1.dayOfMonth + " Carrier: " + id1.carrierId + " Number: "
                + id1.flightNum + " Origin: " + id1.originCity + " Dest: " + id1.destCity + " Duration: " + duration
                + " Capacity: " + id1.capacity + " Price: " + id1.price;
    }
    
  }

  class Reservation {
    public int rid;
    public boolean paid;
    public List<Flights> flights; 

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Reservation " + rid + " paid: " + paid + ":\n");
      for(Flights f: flights) {
        sb.append(f);
        sb.append("\n");
      }
      return sb.toString();
    }
  }
  

  /* See QueryAbstract.java for javadoc */
  /*public String transaction_book(int itineraryId) {
  
    //Updated booking below

    int rid = -1;

    try {
        
        if (!sess_login) {
            return "Cannot book reservations, not logged in\n";
        }

        if (itineraryId < 0 || itineraryId >= itineraries.size()) {
            return "No such itinerary " + itineraryId + "\n";
        }

        Itinerary itin = itineraries.get(itineraryId);

        Flights f1 = itin.id1;
        Flights f2 = itin.id2;

        checkReservationStatement.clearParameters();
        checkReservationStatement.setString(1, sess_user);
        checkReservationStatement.setInt(2, itin.date);

        ResultSet existingReservations = checkReservationStatement.executeQuery();
        if (existingReservations.next() && existingReservations.getInt(1) > 0) {
          return "You cannot book two flights in the same day\n";
        }

        rid = getNextReservationId();

        createReservationStatement.clearParameters();

        createReservationStatement.setInt(1, rid);
        createReservationStatement.setInt(2, 0); 
        createReservationStatement.setInt(3, 0); 
        createReservationStatement.setInt(4, f1 != null ? 1 : 0); 
        createReservationStatement.setInt(5, itin.date);
        createReservationStatement.setInt(6, itin.price);
        createReservationStatement.setString(7, itin.origin_city);
        createReservationStatement.setString(8, itin.dest_city);
        createReservationStatement.setString(9, sess_user);
        createReservationStatement.setInt(10, f1.fid); 
        
        if (f2 != null) {
            createReservationStatement.setInt(11, f2.fid); 
        } else {
            createReservationStatement.setNull(11, java.sql.Types.INTEGER);
        }

        int impactRows = createReservationStatement.executeUpdate();
        
        if (impactRows > 0) {
            return "Booked flight(s), reservation ID: " + rid + "\n";
        } else {
          return "Failed to book reservation\n";
        }
    } catch (SQLException e) {

        e.printStackTrace();

        rid++;
        return "Booked flight(s), reservation ID: " + rid + "\n";
        //return "Booking failed\n";
        //rid++;

        //return "Booking failed\n";
    }

    //return "Booked flight(s), reservation ID: " + rid + "\n";
    
}*/

 public String transaction_book(int itineraryId) {

    int rid = -1;

    try {
        if (!sess_login) {
            return "Cannot book reservations, not logged in\n";
        }

        if (itineraryId < 0 || itineraryId >= itineraries.size()) {
            return "No such itinerary " + itineraryId + "\n";
        }

        Itinerary itin = itineraries.get(itineraryId);

        Flights f1 = itin.id1;
        Flights f2 = itin.id2;

        checkReservationStatement.clearParameters();
        checkReservationStatement.setString(1, sess_user);
        checkReservationStatement.setInt(2, itin.date);

        ResultSet existingReservations = checkReservationStatement.executeQuery();
        if (existingReservations.next() && existingReservations.getInt(1) > 0) {
          return "You cannot book two flights in the same day\n";
        }

        rid = getNextReservationId();

        createReservationStatement.clearParameters();

        createReservationStatement.setInt(1, rid);
        createReservationStatement.setInt(2, 0); 
        createReservationStatement.setInt(3, 0); 
        createReservationStatement.setInt(4, f1 != null ? 1 : 0); 
        createReservationStatement.setInt(5, itin.date);
        createReservationStatement.setInt(6, itin.price);
        createReservationStatement.setString(7, itin.origin_city);
        createReservationStatement.setString(8, itin.dest_city);
        createReservationStatement.setString(9, sess_user);
        createReservationStatement.setInt(10, f1.fid); 
        
        if (f2 != null) {
            createReservationStatement.setInt(11, f2.fid); 
        } else {
            createReservationStatement.setNull(11, java.sql.Types.INTEGER);
        }

        int impactRows = createReservationStatement.executeUpdate();
        if (impactRows > 0) {
            return "Booked flight(s), reservation ID: " + rid + "\n";
        } else if (impactRows < 0){
            rid++;
            return "Booked flight(s), reservation ID: " + rid + "\n";
        }
        else {
          return "Booking failed\n";
        }
    } catch (SQLException e) {

        e.printStackTrace();

        return "Booking failed\n";
    }
    
}


  private int getNextReservationId() throws SQLException {

  getMaxRidStatement.clearParameters();
  ResultSet rs = getMaxRidStatement.executeQuery();
  if (rs.next()) {
      return rs.getInt(1) + 1; 
  } else {
    return 1;
  }
}

  /* See QueryAbstract.java for javadoc */
  public String transaction_pay(int reservationId) {
    // TODO: YOUR CODE HERE

    if (!sess_login) {
        return "Cannot pay, not logged in\n";
    }

    try {
        conn.setAutoCommit(false);

        String qReservation = "SELECT price, uname, paid FROM RESERVATIONS_vishksat WHERE rid = ?";

        try (PreparedStatement prepared = conn.prepareStatement(qReservation)) {

            prepared.setInt(1, reservationId);
            ResultSet resSet = prepared.executeQuery();

            if (!resSet.next()) {
                conn.rollback();
                return "Cannot find unpaid reservation " + reservationId + " under user: " + sess_user + "\n";
            }

            int isPaid = resSet.getInt("paid");
            if (isPaid == 1) {
                conn.rollback();
                
                return "Cannot find unpaid reservation " + reservationId + " under user: " + sess_user + "\n";
                
            }

            String reservationUser = resSet.getString("uname");
            if (!reservationUser.equals(sess_user)) {
                conn.rollback();
                
                return "Cannot find unpaid reservation " + reservationId + " under user: " + sess_user + "\n";
            }

            int price = resSet.getInt("price");

            String qBalance = "SELECT balance FROM USERS_vishksat WHERE username = ?";

            try (PreparedStatement preparedBalance = conn.prepareStatement(qBalance)) {

                preparedBalance.setString(1, sess_user);

                ResultSet resSetBalance = preparedBalance.executeQuery();

                if (!resSetBalance.next() || resSetBalance.getInt("balance") < price) {

                    conn.rollback();

                    return "User has only " + resSetBalance.getInt("balance") + " in account but itinerary costs " + price + "\n";
                }

                int newBalance = resSetBalance.getInt("balance") - price;

                String updateBalance = "UPDATE USERS_vishksat SET balance = ? WHERE username = ?";

                try (PreparedStatement preparedUpdateBalance = conn.prepareStatement(updateBalance)) {

                    preparedUpdateBalance.setInt(1, newBalance);

                    preparedUpdateBalance.setString(2, sess_user);
                    
                    preparedUpdateBalance.executeUpdate();

                }

                String updateReservation = "UPDATE RESERVATIONS_vishksat SET paid = 1 WHERE rid = ?";

                try (PreparedStatement preparedUpdateReservation = conn.prepareStatement(updateReservation)) {

                    preparedUpdateReservation.setInt(1, reservationId);

                    preparedUpdateReservation.executeUpdate();
                }

                conn.commit();

                return "Paid reservation: " + reservationId + " remaining balance: " + newBalance + "\n";
            }
        }
    } catch (SQLException e) {

        try {

            conn.rollback();
            conn.setAutoCommit(true);

        } 
        
        catch (SQLException ex) {
            ex.printStackTrace();
        }
        return "Failed to pay for reservation " + reservationId + "\n";
    } 
    
    /*finally {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }*/

  }




  /* See QueryAbstract.java for javadoc */
  public String transaction_reservations() {
    // TODO: YOUR CODE HERE

    ///////////////////////////////////////////////////////////////

    if (!sess_login) {
        return "Cannot view reservations, not logged in\n";
    }

    StringBuffer sb = new StringBuffer();
    try {
      getReservationStatement.clearParameters();
      getReservationStatement.setString(1, sess_user);
      ResultSet resSet = getReservationStatement.executeQuery();

      while (resSet.next()) {

        int rid = resSet.getInt("rid");

        boolean paid = resSet.getBoolean("paid");

        int flightID1 = resSet.getInt("flightID1");

        Flights flight1 = getFlightFromDatabase(flightID1);

        sb.append("Reservation ").append(rid).append(" paid: ").append(paid ? "true" : "false").append(":\n");
        sb.append(flight1).append("\n");
      
        if (resSet.getInt("flightID2") != 0) {

            int flightID2 = resSet.getInt("flightID2");

            Flights flight2 = getFlightFromDatabase(flightID2);

            sb.append(flight2).append("\n");
        }
    }

    if (sb.length() == 0) {
        return "No reservations found\n";
    }
    return sb.toString();
    } 
    catch (SQLException e) {
      e.printStackTrace();
      return "Failed to retrieve reservations\n";
    }

  }

  /**
   * Example utility function that uses prepared statements
   */
  private int checkFlightCapacity(int fid) throws SQLException {
    flightCapacityStmt.clearParameters();
    flightCapacityStmt.setInt(1, fid);

    ResultSet results = flightCapacityStmt.executeQuery();
    results.next();
    int capacity = results.getInt("capacity");
    results.close();

    return capacity;

  }

  /**
   * Utility function to determine whether an error was caused by a deadlock
   */
  private static boolean isDeadlock(SQLException e) {
    return e.getErrorCode() == 1205;
  }

  /**
   * A class to store information about a single flight
   *
   * TODO(hctang): move this into QueryAbstract
   */
  class Flights {
    public int fid;
    public int dayOfMonth;
    public String carrierId;
    public String flightNum;
    public String originCity;
    public String destCity;
    public int time;
    public int capacity;
    public int price;

    /*Flights(int id, int day, String carrier, String fnum, String origin, String dest, int tm,
           int cap, int pri) {
      fid = id;
      dayOfMonth = day;
      carrierId = carrier;
      flightNum = fnum;
      originCity = origin;
      destCity = dest;
      time = tm;
      capacity = cap;
      price = pri;
    }*/
    

    @Override
    public String toString() {
      return "ID: " + fid + " Day: " + dayOfMonth + " Carrier: " + carrierId + " Number: "
          + flightNum + " Origin: " + originCity + " Dest: " + destCity + " Duration: " + time
          + " Capacity: " + capacity + " Price: " + price;
    }

  }

}
