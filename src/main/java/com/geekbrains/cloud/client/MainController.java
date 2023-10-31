package com.geekbrains.cloud.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    public static final int BUFFER_SIZE = 8192;


    public TextField clientPath;
    public TextField serverPath;
    public ListView <String> clientView;
    public ListView <String> serverView;
    private File currentDirectory;

    private DataInputStream is;
    private DataOutputStream os;
    private byte [] buf;


    // Platform.runLater(()->{})
    private void updateClientView() {
        Platform.runLater(()-> {
            clientPath.setText(currentDirectory.getAbsolutePath());
            clientView.getItems().clear();
            clientView.getItems().add("...");
            clientView.getItems().addAll(currentDirectory.list());
        });
    }

    private void updateServerView(List<String> names) {
        Platform.runLater(()-> {

            serverView.getItems().clear();
            serverView.getItems().add("...");
            serverView.getItems().addAll(names);
        });

    }

    public void download(ActionEvent actionEvent) {
    }

    public void upload(ActionEvent actionEvent) throws IOException {
        String item = clientView.getSelectionModel().getSelectedItem();
        File selected = currentDirectory.toPath().resolve(item).toFile();
        if(selected.isFile()) {
            os.writeUTF("#file_message#");
            os.writeUTF(selected.getName());
            os.writeLong(selected.length());
            try (InputStream fis = new FileInputStream(selected)){
                while (fis.available() > 0 ) {
                    int readBytes = fis.read(buf);
                    os.write(buf, 0, readBytes);
                }
            }
            os.flush();
        }
    }

    private void readFromInputStream() {
        try{
            while (true) {
                String command =  is.readUTF();
                if("#list#".equals(command)){
                    List<String> names = new ArrayList<>();
                    int count = is.readInt();
                    for (int i = 0; i < count; i++) {
                        String name = is.readUTF();
                        names.add(name);
                    }
                    updateServerView(names);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initNetwork() {
        try{
            buf = new byte[BUFFER_SIZE];
            Socket socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread readThread = new Thread(this :: readFromInputStream);
            readThread.setDaemon(true);
            readThread.start();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentDirectory = new File(System.getProperty("user.home"));
        FileSystems.getDefault().getFileStores().forEach(
                System.out::println
        );
        updateClientView();
        initNetwork();
        clientView.setOnMouseClicked(e->{
            if(e.getClickCount() == 2) {
                String item = clientView.getSelectionModel().getSelectedItem();
                if(item.equals("...")){
                    currentDirectory = currentDirectory.getParentFile();
                    updateClientView();
                }else{
                    File selected = currentDirectory.toPath().resolve(item).toFile();
                    if(selected.isDirectory()){
                        currentDirectory = selected;
                        updateClientView();
                    }
                }
            }
        });
    }
}
