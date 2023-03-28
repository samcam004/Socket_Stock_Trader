import javax.xml.crypto.Data;
import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;

public class Server
{

    public Server(int port) {
        // starts server and waits for a connection
        try {
            ServerSocket server = new ServerSocket(port);
            System.out.println("Server started");
            System.out.println("Waiting for a client ...");

            while (!server.isClosed()) {
                //Listens for client
                //initialize server, socket, input stream, and output stream
                Socket socket = server.accept();
                System.out.println("Client accepted\n");

                ClientHandler clientHandler = new ClientHandler(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();
                System.out.println("Thread Started");

                if (clientHandler.client.isClosed()) {
                    System.out.println("Socket Closed");
                }
/*
                // takes input from the client socket
                DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

                // sends output from server to client socket
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String line;

                // reads message from client until socket is closed
                while (!socket.isClosed()) {

                    try {
                        line = in.readUTF();
                        System.out.println("Received: " + line);
                        //Takes input from client and tokenizes it for individual commands.
                        String[] command = line.split("\\s");

                        switch (command[0]) {
                            case "BUY" -> {
                                if (command[1].length() != 4) {
                                    out.write(2);
                                    out.writeUTF("403 Message Format Error");
                                    out.writeUTF("Improper Stock Symbol Format");
                                }
                                else if (Integer.parseInt(command[4]) != 1) {
                                    out.write(2);
                                    out.writeUTF("403 Message Format Error");
                                    out.writeUTF("User does not exist");
                                } else {
                                    buyStock(out, command[1], Double.parseDouble(command[2]),
                                            Double.parseDouble(command[3]), Integer.parseInt(command[4]));
                                }
                            }
                            case "SELL" -> {
                                if (command[1].length() != 4) {
                                    out.write(2);
                                    out.writeUTF("403 Message Format Error");
                                    out.writeUTF("Improper Stock Symbol Format");
                                }
                                else if (Integer.parseInt(command[4]) != 1) {
                                    out.write(2);
                                    out.writeUTF("403 Message Format Error");
                                    out.writeUTF("User does not exist");
                                } else {
                                    sellStock(out, command[1], Double.parseDouble(command[2]),
                                            Double.parseDouble(command[3]), Integer.parseInt(command[4]));
                                }
                            }
                            case "LIST" -> //out.writeUTF("200 OK");
                                    printStock(out);
                            case "BALANCE" -> {
                                out.write(2);
                                out.writeUTF("200 OK");
                                double d = findBalance();
                                out.writeUTF("Balance for user John Doe: $" + d);
                            }
                            case "QUIT" -> {
                                out.write(1);
                                out.writeUTF("200 OK");
                                System.out.println("CLIENT QUIT");
                            }
                            case "SHUTDOWN" -> {
                                out.write(2);
                                out.writeUTF("200 OK");
                                out.writeUTF("SERVER SHUTTING DOWN");
                                System.out.println("SHUTTING DOWN");
                                socket.close();
                                in.close();
                                out.close();
                                server.close();
                            }
                            default -> {
                                System.out.println("INVALID COMMAND");
                                out.write(2);
                                out.writeUTF("400 ERROR");
                                out.writeUTF("INVALID COMMAND");
                            }
                        }
                    } catch (IOException i) {
                        System.out.println(i);
                        //System.out.println("Connection lost");
                        socket.close();
                        in.close();
                    }
                }
                System.out.println("Closing connection");

                // close connection
                socket.close();*/
            }
        }
        catch(IOException i)
        {
            System.out.println("SERVER: " + i);
        }
    }

    //Connects to database, if no database exists, one is created
    public static void connect() {
        Connection c = null;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully\n");
    }

    //Checks table users, if one does not exist, it is created
    public static void userTable() {
        Connection c;
        Statement stmt;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            //System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS users " +
                    "(ID INT PRIMARY_KEY NOT_NULL AUTO_INCREMENT, " +
                    " first_name varchar(255), " +
                    " last_name varchar(255), " +
                    " user_name varchar(255) NOT NULL, " +
                    " password varchar(255), " +
                    " usd_balance DOUBLE NOT NULL, " +
                    " PRIMARY KEY (id))";
            stmt.executeUpdate(sql);

            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Users table created successfully\n");
    }

    //Checks table stocks, if one does not exist, it is created
    public static void stockTable() {
        Connection c;
        Statement stmt;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            //System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS stocks " +
                    "(id int PRIMARY_KEY NOT_NULL AUTO_INCREMENT, " +
                    " stock_symbol varchar(4) NOT NULL, " +
                    " stock_amount DOUBLE, " +
                    " stock_balance DOUBLE, " +
                    " user_id int, " +
                    " PRIMARY KEY (id), " +
                    " FOREIGN KEY (user_id) REFERENCES users (id))";
            stmt.executeUpdate(sql);
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Stock table created successfully\n");
    }

    //Prints a list of users and there key information, if there are none, a user is generated
    public static void printUsers() {
        Connection c;
        Statement stmt;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            //System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet empty = stmt.executeQuery( "SELECT * FROM users;" );

            if (!empty.next()){
                System.out.println("Generating User");
                String sql = "INSERT INTO users (id, first_name,last_name,user_name,password,usd_balance) " +
                        " VALUES (+1, 'ADMIN', 'ADMIN', 'Root', 'Root01', 0.0 )," +
                        " (+2, 'Mary', 'Ann', 'Mary', 'Mary01', 100.00)," +
                        " (+3, 'John', 'Doe', 'John', 'John01', 100.00)," +
                        " (+4, 'Moe', 'Moe', 'Moe', 'Moe01', 100.0 ) ";
                
                stmt.executeUpdate(sql);
                c.commit();
                empty.close();
            }

            System.out.println("Printing Current Users");
            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            while (rs.next()) {
                int id = rs.getInt("id");
                String  fName = rs.getString("first_name");
                String  lName = rs.getString("last_name");
                String  uName = rs.getString("user_name");
                String  pWord = rs.getString("password");
                double usd  = rs.getInt("usd_balance");

                System.out.println( "ID = " + id );
                System.out.println( "First name = " + fName );
                System.out.println( "Last name = " + lName );
                System.out.println( "User name = " + uName );
                System.out.println( "PASSWORD = " + pWord );
                System.out.println( "BALANCE = " + usd );
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Operation done successfully\n");

    }

    //Prints a list of all stocks in the database. Root can see all stocks. Users can only see their stocks.
    public void printStock(DataOutputStream o, int id) {
        Connection c;
        Statement stmt;
        int count = 0;
        int stockNum = 1;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            //System.out.println("Opened database successfully");
            stmt = c.createStatement();
            ResultSet empty = stmt.executeQuery( "SELECT * FROM stocks;" );


            if (!empty.next()) {
                System.out.println("No stocks owned");
                o.write(2);
                o.writeUTF("200 OK");
                o.writeUTF("No stocks in Database");
                empty.close();
            } else {

                if (id == 1) {

                    ResultSet total = stmt.executeQuery("SELECT * FROM stocks;");

                    while (total.next())
                        count++;

                    ResultSet rs = stmt.executeQuery("SELECT * FROM stocks;");

                    o.write(count + 1);
                    o.writeUTF("The list of records in the Stocks database:");

                    while (rs.next()) {
                        //int userID = rs.getInt("id");
                        String symbol = rs.getString("stock_symbol");
                        double amount = rs.getDouble("stock_amount");
                        double balance = rs.getDouble("stock_balance");
                        //String user = rs.getString("user_name");
                        int user_id = rs.getInt("user_id");

                        o.writeUTF(stockNum + " " + symbol + " " + amount + " " + balance + " " + user_id);

                        System.out.print("#" + stockNum);
                        System.out.print(", Stock Symbol = " + symbol);
                        System.out.print(", Stock Amount = " + amount);
                        System.out.print(", Stock Balance = " + balance);
                        System.out.print(", ID = " + id);
                        System.out.println();
                        stockNum++;
                    }
                    total.close();
                    rs.close();
                } else  {

                    ResultSet total = stmt.executeQuery("SELECT * FROM stocks;");

                    while (total.next()) {
                        if (total.getInt("user_id") == id)
                            count++;
                    }

                    System.out.println(count);

                    if (count != 0) {
                        ResultSet rs = stmt.executeQuery("SELECT * FROM stocks;");

                        o.write(count + 1);
                        o.writeUTF("The list of records in the Stocks database for User " + id + ":");

                        while (rs.next()) {
                            if (rs.getInt("user_id") == id) {
                                //int userID = rs.getInt("id");
                                String symbol = rs.getString("stock_symbol");
                                double amount = rs.getDouble("stock_amount");
                                double balance = rs.getDouble("stock_balance");
                                //String user = rs.getString("user_name");
                                //int user_id = rs.getInt("user_id");

                                o.writeUTF(stockNum + " " + symbol + " " + amount + " " + balance);

                                System.out.print("#" + stockNum);
                                System.out.print(", Stock Symbol = " + symbol);
                                System.out.print(", Stock Amount = " + amount);
                                System.out.print(", Stock Balance = " + balance);
                                System.out.print(", ID = " + id);
                                System.out.println();
                                stockNum++;
                            }
                        }
                        total.close();
                        rs.close();
                    } else {
                        o.write(2);
                        o.writeUTF("200 OK");
                        o.writeUTF("User " + id + " has no stocks");
                    }

                }
            }
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Operation done successfully\n");
    }

    //Finds the balance of a user
    public double findBalance(int id) {
        Connection c;
        Statement stmt;
        double usd = 0;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            System.out.println("Finding Balance");

            stmt = c.createStatement();
            ResultSet empty = stmt.executeQuery( "SELECT * FROM users;" );

            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            while (rs.next()) {
                if (id == rs.getInt("ID"))
                    usd  = rs.getInt("usd_balance");
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        //System.out.println("Operation done successfully\n");
        return usd;
    }

    //Adds a stock to the database
    public void buyStock(DataOutputStream o, String symbol, double amount, double price, int id) throws IOException {
        Connection c;
        PreparedStatement stmt;
        Statement search;
        double balance = findBalance(id);
        double cost = amount * price;

        System.out.println("User " + id + " Buying Stock");

        if (cost <= balance) {
            try {
                Class.forName("org.sqlite.JDBC");
                c = DriverManager.getConnection("jdbc:sqlite:stock.db");
                c.setAutoCommit(false);
                System.out.println("Preparing Buy Statement");

                search = c.createStatement();
                ResultSet rs = search.executeQuery( "SELECT * FROM stocks;" );

                while (rs.next()) {
                    String  sS = rs.getString("stock_symbol");
                    double  sA = rs.getDouble("stock_amount");
                    int     sI = rs.getInt("user_id");
                    //double  sB = rs.getDouble("stock_balance");

                    //Checks to see if stock exists, if it does, it is modified with new values
                    if (sS.equals(symbol)) {
                        System.out.println("\nBuying existing stock");
                        String sql = "UPDATE stocks SET stock_amount = ?, stock_balance = ? WHERE stock_symbol = ? AND user_id = ?";
                        stmt = c.prepareStatement(sql);

                        stmt.setDouble(1, sA + amount);
                        stmt.setDouble(2, price);
                        stmt.setString(3, symbol);
                        stmt.setInt(4, id);
                        stmt.executeUpdate();

                        String update = "UPDATE users set usd_balance = ? where ID = ?;";
                        stmt = c.prepareStatement(update);

                        stmt.setDouble(1, balance - cost);
                        stmt.setInt(2, id);

                        stmt.executeUpdate();

                        System.out.println("Bought existing Stock");
                        o.write(2);
                        o.writeUTF("200 OK");
                        o.writeUTF("BOUGHT: New balance: " + amount + " " + symbol + ". USD balance " + (balance - cost));

                        stmt.close();
                        c.commit();
                        c.close();
                        return;
                    }
                }

                //If stock does not exist, then a new stock is added to the stock table
                System.out.println("Buying new stock");
                String sql = "INSERT INTO stocks (stock_symbol,stock_amount,stock_balance,user_id) " +
                        "VALUES (?, ?, ?, ?);";
                stmt = c.prepareStatement(sql);

                stmt.setString(1, symbol);
                stmt.setDouble(2, amount);
                stmt.setDouble(3, price);
                stmt.setInt(4, id);
                stmt.executeUpdate();

                String update = "UPDATE users set usd_balance = ? where ID = ?;";
                stmt = c.prepareStatement(update);

                stmt.setDouble(1, balance - cost);
                stmt.setInt(2, id);

                stmt.executeUpdate();

                stmt.close();
                c.commit();
                c.close();

            } catch (Exception e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(0);
            }
            System.out.println("Bought new Stock");
            o.write(2);
            o.writeUTF("200 OK");
            o.writeUTF("BOUGHT: New balance: " + amount + " " + symbol + ". USD balance " + (balance - cost));
        } else {
            System.out.println("Not Enough Balance");
            o.write(2);
            o.writeUTF("406 ERROR");
            o.writeUTF("EXCEEDS BALANCE");
        }
    }

    //Sells stock from
    public void sellStock(DataOutputStream o, String symbol, double amount, double price, int id) throws IOException {
        Connection c;
        PreparedStatement stmt;
        Statement search;
        Statement findTotal;
        double balance = findBalance(id);
        double cost = amount * price;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            System.out.println("Preparing sell statement");

            int count = 0;
            int numStocks = 0;

            findTotal = c.createStatement();
            ResultSet getCount = findTotal.executeQuery("SELECT * FROM stocks;");

            while (getCount.next())
                count++;
            getCount.close();

            search = c.createStatement();
            ResultSet rs = search.executeQuery( "SELECT * FROM stocks;" );

            while (rs.next()) {
                String sS = rs.getString("stock_symbol");
                double sA = rs.getDouble("stock_amount");
                //double sB = rs.getDouble("stock_balance");

                if (sS.equals(symbol)) {
                    System.out.println("Found Match");
                    if (sA > amount) {

                        System.out.println("Less than owned amount");
                        String sql = "UPDATE stocks SET stock_amount = ?, stock_balance = ? WHERE stock_symbol = ?";
                        stmt = c.prepareStatement(sql);

                        stmt.setDouble(1, (sA - amount));
                        stmt.setDouble(2, cost);
                        stmt.setString(3, symbol);
                        stmt.setInt(4, id);
                        stmt.executeUpdate();
                        c.commit();

                        String update = "UPDATE users set usd_balance = ? where ID = ?;";
                        stmt = c.prepareStatement(update);

                        stmt.setDouble(1, balance + cost);
                        stmt.setInt(2, id);

                        stmt.executeUpdate();
                        c.commit();

                        System.out.println("Sell Transaction Complete");
                        o.write(2);
                        o.writeUTF("200 OK");
                        o.writeUTF("SOLD: Stock balance: " + (sA - amount) + " " + symbol + ". USD balance: $" + findBalance(id));

                    } else if (sA == amount) {
                        System.out.println("Same as owned amount");
                        String sql = "DELETE FROM stocks WHERE stock_symbol = ?";
                        stmt = c.prepareStatement(sql);

                        stmt.setString(1, symbol);
                        stmt.executeUpdate();
                        c.commit();

                        String update = "UPDATE users set usd_balance = ? where ID = ?;";
                        stmt = c.prepareStatement(update);

                        stmt.setDouble(1, balance + cost);
                        stmt.setInt(2, id);
                        stmt.executeUpdate();
                        c.commit();

                        System.out.println("Sell Transaction Complete");
                        o.write(2);
                        o.writeUTF("200 OK");
                        o.writeUTF("SOLD: Stock balance: " + (sA - amount) + " " + symbol + ". USD balance: $" + (balance + cost));

                    } else {
                        System.out.println("Exceeds Amount");
                        o.write(2);
                        o.writeUTF("407 ERROR");
                        o.writeUTF("EXCEEDS STOCK AMOUNT");

                    }
                } else
                    numStocks++;
            }

            rs.close();
            c.close();

            if (numStocks == count) {
                System.out.println("No match");
                o.write(2);
                o.writeUTF("408 ERROR");
                o.writeUTF("STOCK DOES NOT EXIST");
                c.close();
            }

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    //Allows user to log in. Needs correct username and password. Returns string.
    public String logIn(DataOutputStream o, String s, String s1) {
        Connection c;
        Statement stmt;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            System.out.println("\nAttempting Login");

            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            String user;
            //int count = 0;

            while (rs.next()) {
                user = rs.getString("user_name");
                System.out.println(user + " : " + s);

                if (user.equals(s.toString())) {
                    if (rs.getString("password").equals(s1)) {
                        System.out.println("MATCH: " + user);
                        o.write(2);
                        o.writeUTF("200 OK");
                        o.writeUTF("LOGGED IN AS: " + s);

                        rs.close();
                        stmt.close();
                        c.close();

                        return user;
                    } else {
                        o.write(2);
                        o.writeUTF("404 ERROR");
                        o.writeUTF("PASSWORD INCORRECT");

                        rs.close();
                        stmt.close();
                        c.close();

                        return "";
                    }
                }

                //count++;
            }
            o.write(2);
            o.writeUTF("403 ERROR");
            o.writeUTF("USERNAME INCORRECT");

            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        return "";
    }

    //Sets the User's id. Used for all functions.
    public int setID(String userName) {
        Connection c;
        Statement stmt;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:stock.db");
            c.setAutoCommit(false);
            System.out.println("\nSetting user ID");

            stmt = c.createStatement();

            ResultSet rs = stmt.executeQuery("SELECT * FROM users;");

            String user;
            int id;
            int count = 0;

            while (rs.next()) {
                user = rs.getString("user_name");

                if (user.equals(userName.toString())) {
                    id = rs.getInt("ID");
                    System.out.println(userName + " ID: " + id);

                    rs.close();
                    stmt.close();
                    c.close();

                    return id;
                }
                count++;
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
        return 0;
    }

    //Allows user to logout. They need to input the proper username. Returns a boolean.
    public boolean logOut(DataOutputStream o, String s, String userName) {

        System.out.println("Attempting Logout");

        if (s.equals(userName)) {
            System.out.println("User: " + userName + " logging out.");
            return true;
        } else {
            return false;
        }
    }

    //Closes client socket when they quit from the server
    public void closeSocket(Socket socket, DataInputStream in, DataOutputStream out) {
        ClientHandler.clientHandlers.remove(this);
        try {
            if(socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //main
    public static void main(String[] args)
    {
        connect();
        userTable();
        stockTable();
        printUsers();

        Server server = new Server(3339);
    }

    public class ClientHandler implements Runnable{

        public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
        private Socket client;
        private DataInputStream in;
        private DataOutputStream out;
        private String userName;
        private int id;
        private Boolean loggedIn;

        public ClientHandler(Socket socket) {
            try {
                this.client = socket;
                this.out = new DataOutputStream(client.getOutputStream());
                this.in = new DataInputStream(client.getInputStream());
                this.userName = "";
                this.id = 0;
                this.loggedIn = false;
                clientHandlers.add(this);
                System.out.println("Client: " + client.getRemoteSocketAddress() + " connected");
            } catch (IOException e) {
                closeSocket(socket, in, out);
            }
        }

        public void run() {
            String line;
            System.out.println("Client: " + client.getRemoteSocketAddress() + " starting operation");

            while (!client.isClosed()) {
                try {
                    line = in.readUTF();
                    System.out.println("Received: " + line);
                    //Takes input from client and tokenizes it for individual commands.
                    String[] command = line.split("\\s");

                    switch (command[0]) {
                            case "LOGIN" -> {
                                if (command.length == 3) {
                                    userName = logIn(out, command[1], command[2]);
                                    if (!userName.equals("")) {
                                        loggedIn = true;
                                        id = setID(userName);
                                        System.out.println("User " + userName + " (" + id + ") logged in =." + loggedIn);
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            case "LOGOUT" -> {
                                if (command.length == 2) {
                                    if (loggedIn == true) {
                                        if (logOut(out, command[1], userName) && !command[1].isEmpty()) {
                                            userName = "";
                                            id = 0;
                                            loggedIn = false;
                                            out.write(2);
                                            out.writeUTF("200 OK");
                                            out.writeUTF("LOGGING OUT");
                                        } else {
                                            out.write(2);
                                            out.writeUTF("404 ERROR");
                                            out.writeUTF("USERNAME INCORRECT");
                                        }
                                    } else {
                                        out.write(2);
                                        out.writeUTF("401 ERROR");
                                        out.writeUTF("NOT LOGGED IN");
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            case "BUY" -> {
                                if (command.length == 4) {
                                    if (command[1].length() != 4) {
                                        out.write(2);
                                        out.writeUTF("405 ERROR");
                                        out.writeUTF("INCORRECT STOCK SYMBOL FORMAT");
                                    } else if (/*Integer.parseInt(command[4]) != 1*/ !loggedIn) {
                                        out.write(2);
                                        out.writeUTF("401 ERROR");
                                        out.writeUTF("NOT LOGGED IN");
                                    } else {
                                        buyStock(out, command[1], Double.parseDouble(command[2]),
                                                Double.parseDouble(command[3]), id);
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            case "SELL" -> {
                                if (command.length == 4) {
                                    if (command[1].length() != 4) {
                                        out.write(2);
                                        out.writeUTF("405 ERROR");
                                        out.writeUTF("INCORRECT STOCK SYMBOL FORMAT");
                                    } else if (/*Integer.parseInt(command[4]) != 1*/ !loggedIn) {
                                        out.write(2);
                                        out.writeUTF("401 ERROR");
                                        out.writeUTF("NOT LOGGED IN");
                                    } else {
                                        sellStock(out, command[1], Double.parseDouble(command[2]),
                                                Double.parseDouble(command[3]), id);
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            case "LIST" -> {
                                if (id != 0) {
                                    if (command.length == 1) {
                                        printStock(out, id);
                                    } else {
                                        out.write(2);
                                        out.writeUTF("402 ERROR");
                                        out.writeUTF("INCORRECT FORMAT");
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("401 ERROR");
                                    out.writeUTF("NOT LOGGED IN");
                                }
                            }
                            case "BALANCE" -> {
                                if (loggedIn) {
                                    if (command.length == 1) {
                                        out.write(2);
                                        out.writeUTF("200 OK");
                                        double d = findBalance(id);
                                        out.writeUTF("Balance for " + userName + ": $" + d);
                                    } else {
                                        out.write(2);
                                        out.writeUTF("402 ERROR");
                                        out.writeUTF("INCORRECT FORMAT");
                                    }
                                } else {
                                    out.write(2);
                                    out.writeUTF("401 ERROR");
                                    out.writeUTF("NOT LOGGED IN");
                                }
                            }
                            case "QUIT" -> {
                                if (command.length == 1) {
                                    out.write(1);
                                    out.writeUTF("200 OK");
                                    System.out.println("CLIENT: " + userName + " QUIT");
                                    closeSocket(client, in, out);
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            case "SHUTDOWN" -> {
                                if (command.length == 1) {
                                    out.write(2);
                                    out.writeUTF("200 OK");
                                    out.writeUTF("SERVER SHUTTING DOWN");

                                    System.out.println("SHUTTING DOWN");

                                    client.close();
                                    in.close();
                                    out.close();
                                    System.exit(0);
                                } else {
                                    out.write(2);
                                    out.writeUTF("402 ERROR");
                                    out.writeUTF("INCORRECT FORMAT");
                                }
                            }
                            default -> {
                                System.out.println(userName + ": INVALID COMMAND");
                                out.write(2);
                                out.writeUTF("400 ERROR");
                                out.writeUTF("INVALID COMMAND");
                            }
                    }
                } catch (IOException i) {
                    System.out.println("CLIENT HANDLER: " + i);
                    closeSocket(client,in,out);
                }
            }
        }
    }
}