package dev.hlq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Random;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class PrimaryController {

    private static String newMac;

    @FXML
    private Label currentMac;
    @FXML
    private Label randomMac;
    @FXML
    private Label infoLabel;
    @FXML
    private PasswordField passwordField;

    public void initialize() throws IOException{
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("bash", "-c", "ifconfig en0");
        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder getOutput = new StringBuilder();
        while((line = reader.readLine()) != null) { getOutput.append(line); }
        String checkEther = getOutput.toString();
        String macAddress = checkEther.substring(checkEther.indexOf("ether ") + 6, checkEther.indexOf("ether ") + 23);
        currentMac.setText(macAddress);
        randomMac.setText("N/A");
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

    public void setRandomizedMac() throws IOException, InterruptedException{
        if(newMac == null || newMac == ""){
            infoLabel.setText("Please generate a new MAC address using the button above.");
            return;
        }
        if(passwordField.getText().equals("")){
            infoLabel.setText("Please enter your password before setting the MAC address.");
            return;
        }
        String assembledCmd = "sudo -S networksetup -setnetworkserviceenabled Wi-Fi off" + 
        "&& sleep 1 && sudo -S networksetup -setnetworkserviceenabled Wi-Fi on && sudo" + 
        " -S ifconfig en0 ether " + newMac;

        String[] cmdArgs = {"bash", "-c", assembledCmd};
    
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(cmdArgs);

        Process process = processBuilder.start();

        OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream());
        writer.write(passwordField.getText() + "\n");
        writer.flush();
        
        process.waitFor();

        initialize();
    }
}
