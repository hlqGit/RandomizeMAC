package dev.hlq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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

public class PrimaryController {

    private static String newMac;
    private static String prevMac;
    private static String selectedInterface;

    private static String newMacPG2;
    private static String prevMacPG2;
    private static String selectedInterfacePG2;

    @FXML
    private Label currentMac;
    @FXML
    private Label randomMac;
    @FXML
    private Label infoLabel;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ChoiceBox<String> networkInterface;
    @FXML
    private Button setMac;

    @FXML
    private Label currentMacPG2;
    @FXML
    private TextField customMacPG2;
    @FXML
    private Label infoLabelPG2;
    @FXML
    private PasswordField passwordFieldPG2;
    @FXML
    private ChoiceBox<String> networkInterfacePG2;
    @FXML
    private Button setMacPG2;
    
    public void initialize() throws IOException{
        setMac.setDisable(false);
        setMacPG2.setDisable(false);

        if(networkInterface.getItems().isEmpty()){
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", "networksetup -listallhardwareports");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder getOutput = new StringBuilder();
            while((line = reader.readLine()) != null) { getOutput.append(line); }
    
            Pattern pattern = Pattern.compile("Device: (\\w+)");
            Matcher matcher = pattern.matcher(getOutput.toString());
            List<String> devices = new ArrayList<>();
    
            while (matcher.find()) {
                String toAdd = matcher.group(1);
                devices.add(toAdd.substring(0, toAdd.indexOf("E")));
            }
            Collections.sort(devices);
    
            for(int i = 0; i < devices.size(); i++){
                networkInterface.getItems().add(devices.get(i));
                networkInterfacePG2.getItems().add(devices.get(i));
            }
            int defaultSelect = devices.indexOf("en0") > -1 ? devices.indexOf("en0") : 0;
            networkInterface.getSelectionModel().select(defaultSelect);
            networkInterfacePG2.getSelectionModel().select(defaultSelect);

            networkInterface.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    getCurrentMac();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            networkInterfacePG2.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                try {
                    getCurrentMacPG2();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        getCurrentMac();
        getCurrentMacPG2();

        generateRandomMac();
        randomMac.setText(newMac);

        if(currentMac.getText().equals(prevMac)){
            infoLabel.setText("Failed to set MAC address. Please try again.");
        } else if (prevMac != null){
            infoLabel.setText("Success!");
        }
        if(currentMacPG2.getText().equals(prevMacPG2)){
            infoLabelPG2.setText("Failed to set MAC address. Please try again.");
        } else if (prevMacPG2 != null){
            infoLabelPG2.setText("Success!");
        }
    }

    public void getCurrentMac() throws IOException{
        selectedInterface = networkInterface.getSelectionModel().getSelectedItem();
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", "-c", "ifconfig " + selectedInterface);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder getOutput = new StringBuilder();
        while((line = reader.readLine()) != null) { getOutput.append(line); }
        String checkEther = getOutput.toString();
        String macAddress = checkEther.substring(checkEther.indexOf("ether ") + 6, checkEther.indexOf("ether ") + 23);
        currentMac.setText(macAddress);
    }

    public void getCurrentMacPG2() throws IOException{
        selectedInterfacePG2 = networkInterfacePG2.getSelectionModel().getSelectedItem();
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", "-c", "ifconfig " + selectedInterfacePG2);
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder getOutput = new StringBuilder();
        while((line = reader.readLine()) != null) { getOutput.append(line); }
        String checkEther = getOutput.toString();
        String macAddress = checkEther.substring(checkEther.indexOf("ether ") + 6, checkEther.indexOf("ether ") + 23);
        currentMacPG2.setText(macAddress);
    }

    public void generateRandomMac(){
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        rand.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);
    
        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){
    
            if(sb.length() > 0)
                sb.append(":");
    
            sb.append(String.format("%02x", b));
        }
        newMac = sb.toString();
        randomMac.setText(newMac);
    }

    public void attemptMacChange() throws IOException, InterruptedException{
        if(passwordField.getText().equals("")){
            infoLabel.setText("Please enter your password before setting the MAC address.");
            return;
        }
        setMac.setDisable(true);
        infoLabel.setText("Attempting to set new MAC address...");
        prevMac = currentMac.getText();
        CompletableFuture.runAsync(() -> {
            try {
                setRandomizedMac();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void attemptMacChangePG2() throws IOException, InterruptedException{
        
        if(!isValidMacAddress(customMacPG2.getText())){
            infoLabelPG2.setText("This MAC address is invalid. Please ensure you have propper formatting.");
            return;
        }
        if(passwordFieldPG2.getText().equals("")){
            infoLabelPG2.setText("Please enter your password before setting the MAC address.");
            return;
        }
        setMacPG2.setDisable(true);
        infoLabelPG2.setText("Attempting to set new MAC address...");
        prevMacPG2 = currentMacPG2.getText();
        CompletableFuture.runAsync(() -> {
            try {
                setCustomMac();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public void validateMac(){
        newMacPG2 = customMacPG2.getText();
        if(isValidMacAddress(newMacPG2)){
            infoLabelPG2.setText("MAC address is valid!");
        } else {
            infoLabelPG2.setText("This MAC address is invalid. Please ensure you have propper formatting.");
        }
    }

    public static boolean isValidMacAddress(String mac) {
        String regex = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(mac);
        return matcher.matches();
    }


    public void setRandomizedMac() throws IOException, InterruptedException {
        String assembledCmd = "sudo -S networksetup -setnetworkserviceenabled Wi-Fi off" + 
        "&& sleep 1 && sudo -S networksetup -setnetworkserviceenabled Wi-Fi on && sudo" + 
        " -S ifconfig " + selectedInterface + " ether " + newMac;

        String[] cmdArgs = {"bash", "-c", assembledCmd};
    
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmdArgs);

        Process process = processBuilder.start();

        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
        writer.write(passwordField.getText() + "\n");
        writer.flush();

        process.waitFor();

        Platform.runLater(() -> {
        try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        });
    }

    public void setCustomMac() throws IOException, InterruptedException {
        newMacPG2 = customMacPG2.getText();

        String assembledCmd = "sudo -S networksetup -setnetworkserviceenabled Wi-Fi off" + 
        "&& sleep 1 && sudo -S networksetup -setnetworkserviceenabled Wi-Fi on && sudo" + 
        " -S ifconfig " + selectedInterfacePG2 + " ether " + newMacPG2;

        String[] cmdArgs = {"bash", "-c", assembledCmd};
    
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmdArgs);

        Process process = processBuilder.start();

        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
        writer.write(passwordFieldPG2.getText() + "\n");
        writer.flush();

        process.waitFor();

        Platform.runLater(() -> {
        try {
            initialize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        });
    }
}