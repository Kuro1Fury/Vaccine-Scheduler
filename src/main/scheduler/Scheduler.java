package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // ouTODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (!isPasswordValid(password)) {
            System.out.println("Your password is invalid!");
            return;
        }
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        if (!isPasswordValid(password)) {
            System.out.println("Your password is invalid!");
            return;
        }
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean isPasswordValid(String password) {
        Boolean valid = true;
        if (password.length() < 8) {
            System.out.println("Your password should be at least 8 characters!");
            valid = false;
        }
        if (password.equals(password.toLowerCase()) || password.equals(password.toUpperCase())) {
            System.out.println("Your password should be mix of lower and upper case!");
            valid = false;
        }
        if (true) {
            int count = 0;

            // check digits from 0 to 9
            for (int i = 0; i <= 9; i++) {

                // to convert int to string
                String str1 = Integer.toString(i);

                if (password.contains(str1)) {
                    count = 1;
                }
            }
            if (count == 0) {
                System.out.println("Your password should contain numbers!");
                valid = false;
            }
        }
        if (true) {
            int count = 0;

            // checking capital letters
            for (int i = 65; i <= 90; i++) {

                // type casting
                char c = (char)i;

                String str1 = Character.toString(c);
                if (password.toUpperCase().contains(str1)) {
                    count = 1;
                }
            }
            if (count == 0) {
                System.out.println("Your password should contain letters");
                valid = false;
            }
        }
        if (!(password.contains("!") || password.contains("@")
                || password.contains("#") || password.contains("?"))) {
            System.out.println("Your password should include at least one special character");
            valid =  false;
        }
        return valid;
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        if (tokens.length != 2) {
            System.out.println("Please check your query!");
            return;
        }

        String date = tokens[1];

        try{
            Date d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
            return;
        }

        List<String> allNames = selectAllAvailable(Date.valueOf(date));
        List<String> allReserved = selectAllReserved(Date.valueOf(date));
        allNames.removeAll(allReserved);
        List<String[]> allVacs = getVac();
        if (allNames.size() == 0) {
            System.out.println("There is no available caregiver at that time!");
        } else {
            System.out.println("Available Caregivers are:");
            for (String str : allNames) {
                System.out.println(str);
            }

        }
        System.out.println("Vaccine info here :");
        for(String[] val : allVacs) {
            System.out.println(val[0] + " has " + val[1] + " left");
        }
    }

    private static List<String[]> getVac() {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // Get all names
        List<String[]> allVacs = new ArrayList<String[]>();
        String selectVacs = "SELECT * FROM Vaccines";
        try {
            PreparedStatement statement = con.prepareStatement(selectVacs);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("Name");
                int doses = resultSet.getInt("Doses");
                String[] temp = new String[2];
                temp[0] = name;
                temp[1] = String.valueOf(doses);
                allVacs.add(temp);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return allVacs;
    }

    private static void reserve(String[] tokens) {
        // Check 1 : login
        if (currentPatient == null) {
            System.out.println("Please login as a patient first!");
            return;
        }
        // Check 2 : query length
        if (tokens.length != 3) {
            System.out.println("Please check your query!");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];
        // Check 3: if the query is valid
        try{
            Date d = Date.valueOf(date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
            return;
        }

        // Check 4: if the vaccine name is correct
        if (!nameExistsVaccine(vaccine)) {
            System.out.println("Not valid vaccine name!");
            return;
        }

        // Check 5: if there are enough vaccines
        if (!enoughDoses(vaccine)) {
            System.out.println("Not enough doses, please try again later!");
            return;
        }

        // Check 6: if there is any available time left
        if (!timeExistsAvailability(Date.valueOf(date))) {
            System.out.println("Not a available time, please try again later!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // get the maximum Appointment id
        int max = maxIdFromAppointments();

        // get random cname
        List<String> allNames = selectAllAvailable(Date.valueOf(date));
        List<String> allReserved = selectAllReserved(Date.valueOf(date));
        allNames.removeAll(allReserved);
        if (allNames.size() == 0) {
            System.out.println("All spots reserved, please try again later!");
            return;
        }
        Random rand = new Random();
        String cname = allNames.get(rand.nextInt(allNames.size()));

        // get pname
        String pname = currentPatient.getUsername();

        // make an appointment
        String addAppointment = "INSERT INTO Appointments (aid, Time, cname, pname, vname) VALUES (?, ?, ?, ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, max + 1);
            statement.setDate(2, Date.valueOf(date));
            statement.setString(3, cname);
            statement.setString(4, pname);
            statement.setString(5, vaccine);
            statement.executeUpdate();

        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        Vaccine vac = null;
        try {
            vac = new Vaccine.VaccineGetter(vaccine).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when reducing doses");
            e.printStackTrace();
        }
        try {
            vac.decreaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Error occurred when reducing doses");
            e.printStackTrace();
        }
        System.out.println("*** Reservation Success! ***");
    }


    private static List<String> selectAllAvailable(Date d) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // Get all names
        List<String> allNames = new ArrayList<String>();
        String selectCaregivers = "SELECT Username FROM Availabilities WHERE Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectCaregivers);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("Username");
                allNames.add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return allNames;
    }

    private static List<String> selectAllReserved(Date d) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // Get reserved names
        List<String> allReserved = new ArrayList<String>();
        String selectReserved = "SELECT cname FROM Appointments WHERE Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectReserved);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String name = resultSet.getString("cname");
                allReserved.add(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return allReserved;
    }

    private static int maxIdFromAppointments() {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectMax = "SELECT MAX(aid) AS max FROM Appointments";
        try {
            PreparedStatement statement = con.prepareStatement(selectMax);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                return 0;
            } else {
                resultSet.next();
                return resultSet.getInt("max");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return 0;
    }

    private static boolean timeExistsAvailability(Date d) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // check if there are enough vaccines
        String getVaccine = "SELECT Time FROM Availabilities WHERE Time = ?";
        try{
            PreparedStatement statement = con.prepareStatement(getVaccine);
            statement.setDate(1, d);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking date");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }



    private static boolean enoughDoses(String name) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // check if there are enough vaccines
        String getVaccine = "SELECT Doses FROM Vaccines WHERE Name = ?";
        try{
            PreparedStatement statement = con.prepareStatement(getVaccine);
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                int doses = resultSet.getInt("Doses");
                if (doses < 1) {
                    return false;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when checking vaccine name");
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static boolean nameExistsVaccine(String name) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectName = "SELECT * FROM Vaccines WHERE Name = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectName);
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking vaccine name");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // check 1: check if the token length is valid
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        // check 2: login
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        int aid = Integer.parseInt(tokens[1]);
        String vaccine = null;

        // reduce vaccine doses
        String selectVacName = "Select vname FROM Appointments WHERE aid = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectVacName);
            statement.setInt(1, aid);
            ResultSet resultSet = statement.executeQuery();
            if (!resultSet.isBeforeFirst()) {
                System.out.println("Invalid Appointment ID!");
                return;
            }
            resultSet.next();
            vaccine = resultSet.getString("vname");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Vaccine vac = null;
        try {
            vac = new Vaccine.VaccineGetter(vaccine).get();
            vac.increaseAvailableDoses(1);
        } catch (SQLException e) {
            System.out.println("Error occurred when reducing doses");
            e.printStackTrace();
        }

        // Check the deleted name is matched with the current username
        if (currentPatient != null) {
            String check = "SELECT pname FROM Appointments WHERE aid = ?";
            try {
                PreparedStatement statement = con.prepareStatement(check);
                statement.setInt(1, aid);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                if (!resultSet.getString("pname").equals(currentPatient.getUsername())) {
                    System.out.println("You have no access to change the Appointment!");
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (currentCaregiver != null) {
            String check = "SELECT cname FROM Appointments WHERE aid = ?";
            try {
                PreparedStatement statement = con.prepareStatement(check);
                statement.setInt(1, aid);
                ResultSet resultSet = statement.executeQuery();
                resultSet.next();
                if (!resultSet.getString("cname").equals(currentCaregiver.getUsername())) {
                    System.out.println("You have no access to change the Appointment!");
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // delete
        String deleteApp = "DELETE FROM Appointments WHERE aid = ?";
        try {
            PreparedStatement statement = con.prepareStatement(deleteApp);
            statement.setInt(1, aid);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver != null) {
            String cname = currentCaregiver.getUsername();
            String selectApp = "SELECT aid, vname, Time, pname FROM Appointments WHERE cname = ?";
            try {
                PreparedStatement statement = con.prepareStatement(selectApp);
                statement.setString(1, cname);
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.isBeforeFirst()) {
                    System.out.println("There is no Appointment info");
                    return;
                }
                System.out.println("Your Appointment info are:");
                while (resultSet.next()) {
                    System.out.println("Id: " + resultSet.getInt("aid"));
                    System.out.println("Vaccine Name: " + resultSet.getString("vname"));
                    System.out.println("Time: " + resultSet.getDate("Time"));
                    System.out.println("Patient Name: " + resultSet.getString("pname"));
                    System.out.println();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }
        if (currentPatient != null) {
            String pname = currentPatient.getUsername();
            String selectApp = "SELECT aid, vname, Time, cname FROM Appointments WHERE pname = ?";
            try {
                PreparedStatement statement = con.prepareStatement(selectApp);
                statement.setString(1, pname);
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.isBeforeFirst()) {
                    System.out.println("There is no Appointment info");
                    return;
                }
                System.out.println("Your Appointment info are:");
                while (resultSet.next()) {
                    System.out.println("Id: " + resultSet.getInt("aid"));
                    System.out.println("Vaccine Name: " + resultSet.getString("vname"));
                    System.out.println("Time: " + resultSet.getDate("Time"));
                    System.out.println("Patient Name: " + resultSet.getString("cname"));
                    System.out.println();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        }



    }

    private static void logout(String[] tokens) {
        if (currentPatient == null && currentCaregiver == null) {
            System.out.println("You haven't logged in!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("*** Logout Successfully ***");
    }
}
