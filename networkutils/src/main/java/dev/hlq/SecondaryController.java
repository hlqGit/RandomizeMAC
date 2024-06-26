package dev.hlq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SecondaryController {

    private static String newMac;
    private static String prevMac;
    private static String selectedInterface;

    private static Process process;

    private static boolean passwordIncorrect;

    //Declares ALL objects that are updated/accessed in the GUI.
    @FXML
    private Label currentMac;
    @FXML
    private Label infoLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ChoiceBox<String> networkInterface;
    @FXML
    private Button setMac;
    @FXML
    private TextField customMac;

    //Button for switching to the other tab
    @FXML
    public void swtichToRandomize() throws IOException{
        App.setRoot("Randomize");
    }
    
    //Function that always runs on startup of program and/or when it is called later on.
    public void initialize() throws IOException {

        //Gets all hardware network ports and adds them to the list of network interfaces.
        if(networkInterface.getItems().isEmpty()){
            //Runs the command to list all hardware network ports
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", "networksetup -listallhardwareports");
            Process process = pb.start();

            //Reads the output of the previous command and appends it to a String.
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder getOutput = new StringBuilder();
            while((line = reader.readLine()) != null) { 
                getOutput.append(line); 
            }
            
            //Creates a pattern that looks for words after the word "Device: "
            Pattern pattern = Pattern.compile("Device: (\\w+)");
            Matcher matcher = pattern.matcher(getOutput.toString());
            
            //Adds those words to an ArrayList and sorts them.
            List<String> devices = new ArrayList<>();
            while (matcher.find()) {
                String toAdd = matcher.group(1);
                devices.add(toAdd.substring(0, toAdd.indexOf("E")));
            }
            Collections.sort(devices);

            //Adds the sorted list of devices to the network interface selectors.
            for(int i = 0; i < devices.size(); i++){
                networkInterface.getItems().add(devices.get(i));
            }

            //Tries to set 'en0' as the default selected one, otherwise it selects the first listed one.
            int defaultSelect = devices.indexOf("en0") > -1 ? devices.indexOf("en0") : 0;
            networkInterface.getSelectionModel().select(defaultSelect);

            //Creates event listeners for the network interface selectors; will update the MAC address displayed if changes happen.
            networkInterface.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    getCurrentMac();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        //Initializes the MAC addresses on launch
        getCurrentMac();
    }

    //Method that runs after attemping a MAC change, checks to see if it failed.
    public void verifyMacChanged() throws IOException{
        //enables the MAC changer buttons
        setMac.setDisable(false);

        //Updates the current MAC address after attempting MAC change
        getCurrentMac();

        //Checks to see if the MAC address is different than the last one.
        if(currentMac.getText().equals(prevMac) && passwordIncorrect){
            infoLabel.setText("Your password is incorrect!");
            passwordIncorrect = false;
        } else if(currentMac.getText().equals(prevMac)){
            infoLabel.setText("Failed to set MAC address");
        } else {
            infoLabel.setText("MAC address updated successfully");
        }
    }

    //Updates the current MAC displayed on page 1
    public void getCurrentMac() throws IOException{
        //Turns the selected network interface on page 1 into a String.
        selectedInterface = networkInterface.getSelectionModel().getSelectedItem();

        //Runs the terminal command to get the MAC of the selected interface
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", "-c", "ifconfig " + selectedInterface);
        Process process = pb.start();

        //Reads the output of the previous command and appends it to a String.
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder getOutput = new StringBuilder();
        while((line = reader.readLine()) != null) { 
            getOutput.append(line); 
        }

        //Grabs the MAC address which appears after the word "ether" and sets that as the current MAC address.
        String checkEther = getOutput.toString();
        String macAddress = checkEther.substring(checkEther.indexOf("ether ") + 6, checkEther.indexOf("ether ") + 23);
        currentMac.setText(macAddress);
    }
    
    //Verifies all necessary fields are filled out and runs the method that sets the custom MAC address.
    public void attemptMacChange() {
        //If the MAC address formatting is incorrect in the field, it won't let you run the command.
        if(!isValidMacAddress(customMac.getText())){
            infoLabel.setText("MAC address is invalid. Please ensure you have propper formatting.");
            return;
        }

        //If there isn't anything in the password field, it won't let you run the command.
        if(passwordField.getText().equals("")){
            infoLabel.setText("Please enter your password before setting the MAC address.");
            return;
        }

        //All necesary fields are filled out past this point
        //Disables the button so it cannot be pressed more than once.
        setMac.setDisable(true);

        infoLabel.setText("Attempting to set new MAC address.");

        //Saves the MAC as the "Previous MAC" so it can check if it changed later.
        prevMac = currentMac.getText();

        //Wait for the terminal command to be done or timeout after 6 seconds
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(6000); // 6 seconds timeout
                if(process.isAlive()){
                    passwordIncorrect = true;
                }
                process.destroyForcibly(); // Terminate the process forcibly after timeout
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        timeoutThread.start();
        
        //Starts a new Thread to set the MAC. New thread is made so GUI can continue to update.
        CompletableFuture.runAsync(() -> {
            try {
                setCustomMac();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    //Validates the MAC and changes the info label to the result.
    public void validateMac(){
        newMac = customMac.getText();
        if(isValidMacAddress(newMac)){
            infoLabel.setText("MAC address is valid!");
        } else {
            infoLabel.setText("MAC address is invalid. Please ensure you have propper formatting.");
        }
    }

    //Used to check if the supplied MAC address is compliant with the traditional format of a MAC address.
    public static boolean isValidMacAddress(String mac) {
        return mac.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");
    }
    
    //Method that attempts to set the MAC address for the currently selected interface to a custom made one.
    public void setCustomMac() throws IOException, InterruptedException {
        //Command that turns off and on wifi, followed by immediatly attempting to change the MAC address.
        String assembledCmd = "sudo -S networksetup -setnetworkserviceenabled Wi-Fi off" + 
        "&& sleep 1 && sudo -S networksetup -setnetworkserviceenabled Wi-Fi on && sudo" + 
        " -S ifconfig " + selectedInterface + " ether " + newMac;
        
        //Runs the assembled command that was built in the last chunk of code
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("bash", "-c", assembledCmd);
        process = processBuilder.start();

        //Writes the password to the terminal for sudo privlages.
        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
        writer.write(passwordField.getText() + "\n");
        writer.flush();

        //Wait for the process to complete
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //After command is done, will verify if MAC address changed.
        Platform.runLater(() -> {
        try {
            verifyMacChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
        });
    }
}
